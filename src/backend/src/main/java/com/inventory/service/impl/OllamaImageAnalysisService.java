package com.inventory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.dto.response.ImageAnalysisResult.AnalysisStatus;
import com.inventory.exception.RateLimitExceededException;
import com.inventory.service.IImageAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
public class OllamaImageAnalysisService implements IImageAnalysisService {

    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    // In-memory store: results are lost on restart and not shared across instances.
    // Migrate to Redis with TTL if horizontal scaling is introduced.
    private final ConcurrentHashMap<String, TimestampedResult> results = new ConcurrentHashMap<>();
    private final ExecutorService analysisExecutor;

    private static final long RESULT_TTL_MINUTES = 10;
    private static final int MAX_RESULTS = 500;
    private static final Set<String> DEFAULT_ALLOWED_HOSTS = Set.of("localhost", "127.0.0.1", "host.docker.internal");
    private static final Set<String> VALID_STATUSES = Set.of(
            "AVAILABLE", "TO_VERIFY", "NEEDS_MAINTENANCE", "DAMAGED");

    private final Set<String> allowedHosts;

    public OllamaImageAnalysisService(
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            ObjectMapper objectMapper,
            @Value("${app.ai.ollama.base-url}") String baseUrl,
            @Value("${app.ai.ollama.model}") String model,
            @Value("${app.ai.ollama.thread-pool-size:2}") int threadPoolSize,
            @Value("${app.ai.ollama.allowed-hosts:}") String extraHosts) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        Set<String> hosts = new HashSet<>(DEFAULT_ALLOWED_HOSTS);
        if (extraHosts != null && !extraHosts.isBlank()) {
            Arrays.stream(extraHosts.split(","))
                    .map(String::trim)
                    .filter(h -> !h.isEmpty())
                    .forEach(hosts::add);
        }
        this.allowedHosts = Set.copyOf(hosts);
        this.analysisExecutor = new ThreadPoolExecutor(
                1, threadPoolSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @PostConstruct
    void validateBaseUrl() {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (host == null || scheme == null
                    || (!scheme.equals("http") && !scheme.equals("https"))
                    || !allowedHosts.contains(host.toLowerCase())) {
                throw new IllegalStateException(
                        "app.ai.ollama.base-url must use http(s) and point to an allowed host "
                                + allowedHosts + ", got: " + host);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid app.ai.ollama.base-url: " + e.getMessage(), e);
        }
    }

    @Override
    public String analyzeImage(byte[] imageData, List<CustomFieldDefinition> fieldDefinitions, UUID userId) {
        if (results.size() >= MAX_RESULTS) {
            throw new RateLimitExceededException("Analysis results store is full. Please try again later.");
        }

        String analysisId = UUID.randomUUID().toString();
        results.put(analysisId, new TimestampedResult(ImageAnalysisResult.pending(analysisId), Instant.now(), userId));

        try {
            analysisExecutor.submit(() -> {
                try {
                    ImageAnalysisResult result = performAnalysis(analysisId, imageData, fieldDefinitions);
                    results.put(analysisId, new TimestampedResult(result, Instant.now(), userId));
                } catch (Exception e) {
                    log.error("Image analysis failed for {}", analysisId, e);
                    results.put(analysisId, new TimestampedResult(
                            ImageAnalysisResult.failed(analysisId, "Analysis failed"), Instant.now(), userId));
                }
            });
        } catch (RejectedExecutionException e) {
            results.remove(analysisId);
            throw new RateLimitExceededException("Analysis queue is full. Please try again later.");
        }

        return analysisId;
    }

    @Override
    public Optional<ImageAnalysisResult> getResult(String analysisId, UUID userId) {
        TimestampedResult timestamped = results.get(analysisId);
        if (timestamped == null || !timestamped.userId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(timestamped.result());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private ImageAnalysisResult performAnalysis(String analysisId, byte[] imageData,
                                                 List<CustomFieldDefinition> fieldDefinitions) {
        String base64Image = Base64.getEncoder().encodeToString(imageData);
        String prompt = buildPrompt(fieldDefinitions);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("images", List.of(base64Image));
        requestBody.put("stream", false);
        requestBody.put("format", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = baseUrl + "/api/generate";
            log.debug("Calling Ollama at {} with model {}", url, model);

            var response = ollamaRestTemplate.postForEntity(url, request, String.class);

            if (response.getBody() == null) {
                return ImageAnalysisResult.failed(analysisId, "Empty response from Ollama");
            }

            return parseResponse(analysisId, response.getBody());
        } catch (RestClientException e) {
            log.error("Ollama API call failed: {}", e.getMessage());
            return ImageAnalysisResult.failed(analysisId, "AI service unavailable");
        }
    }

    private static String sanitize(String input, int maxLength) {
        if (input == null) return "";
        String stripped = input.replaceAll("[\"{}\\[\\]\\n\\r\\\\]", "")
                .replaceAll("[\\p{Cntrl}]", "");
        return stripped.substring(0, Math.min(stripped.length(), maxLength));
    }

    private static String sanitizeOrNull(String input, int maxLength) {
        if (input == null || input.isEmpty()) return null;
        String result = sanitize(input, maxLength);
        return result.isEmpty() ? null : result;
    }

    private String buildPrompt(List<CustomFieldDefinition> fieldDefinitions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this image for an inventory management system. ");
        prompt.append("Respond ONLY with valid JSON using this exact structure:\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"short descriptive item name\",\n");
        prompt.append("  \"status\": \"AVAILABLE\",\n");
        prompt.append("  \"stock\": 1\n");

        if (fieldDefinitions != null && !fieldDefinitions.isEmpty()) {
            prompt.append(",\n  \"customFields\": {\n");
            String fields = fieldDefinitions.stream()
                    .map(fd -> "    \"" + sanitize(fd.name(), 50) + "\": \"value for " + sanitize(fd.label(), 100) + " (" + fd.type() + ")\"")
                    .collect(Collectors.joining(",\n"));
            prompt.append(fields);
            prompt.append("\n  }\n");
        }

        prompt.append("}\n\n");
        prompt.append("Rules:\n");
        prompt.append("- name: brief, descriptive name of the item in the image\n");
        prompt.append("- status: one of AVAILABLE, TO_VERIFY, NEEDS_MAINTENANCE, DAMAGED based on visible condition\n");
        prompt.append("- stock: estimated count of items visible (default 1)\n");

        if (fieldDefinitions != null && !fieldDefinitions.isEmpty()) {
            prompt.append("- customFields: fill based on what you can see in the image\n");
            for (CustomFieldDefinition fd : fieldDefinitions) {
                prompt.append("  - ").append(sanitize(fd.name(), 50)).append(" (").append(fd.type()).append("): ")
                        .append(sanitize(fd.label(), 100)).append("\n");
            }
        }

        return prompt.toString();
    }

    private ImageAnalysisResult parseResponse(String analysisId, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String responseText = root.path("response").asText("");

            JsonNode parsed = objectMapper.readTree(responseText);

            String name = sanitizeOrNull(parsed.path("name").asText(null), 255);
            String status = parsed.path("status").asText(null);
            Integer stock = parsed.has("stock") ? parsed.path("stock").asInt(1) : null;

            if (status != null && !VALID_STATUSES.contains(status)) {
                status = null;
            }
            if (stock != null && (stock < 0 || stock > 10_000)) {
                stock = null;
            }

            Map<String, Object> customFields = null;
            if (parsed.has("customFields") && parsed.get("customFields").isObject()) {
                customFields = sanitizeCustomFields(parsed.get("customFields"));
            }

            return new ImageAnalysisResult(analysisId, AnalysisStatus.COMPLETED,
                    name, status, stock, customFields, null);

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Ollama response for analysis {}", analysisId);
            return ImageAnalysisResult.failed(analysisId, "Failed to parse AI response");
        }
    }

    private static final int MAX_CUSTOM_FIELDS = 50;
    private static final int MAX_CUSTOM_FIELD_VALUE_LENGTH = 500;

    private Map<String, Object> sanitizeCustomFields(JsonNode customFieldsNode) {
        Map<String, Object> result = new HashMap<>();
        var fields = customFieldsNode.fields();
        int count = 0;
        while (fields.hasNext() && count < MAX_CUSTOM_FIELDS) {
            var entry = fields.next();
            String key = sanitize(entry.getKey(), 50);
            if (key.isEmpty()) continue;
            JsonNode valueNode = entry.getValue();
            // Only accept primitive values — reject arrays, objects, nulls
            if (valueNode.isTextual()) {
                result.put(key, sanitize(valueNode.asText(), MAX_CUSTOM_FIELD_VALUE_LENGTH));
            } else if (valueNode.isNumber()) {
                result.put(key, valueNode.numberValue());
            } else if (valueNode.isBoolean()) {
                result.put(key, valueNode.booleanValue());
            }
            // Skip non-primitive types (arrays, objects, nulls)
            count++;
        }
        return result.isEmpty() ? null : result;
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanupExpiredResults() {
        Instant cutoff = Instant.now().minusSeconds(RESULT_TTL_MINUTES * 60);
        int removed = 0;
        var iterator = results.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().createdAt().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired analysis results", removed);
        }
    }

    @PreDestroy
    void shutdown() {
        analysisExecutor.shutdown();
        try {
            if (!analysisExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                analysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            analysisExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private record TimestampedResult(ImageAnalysisResult result, Instant createdAt, UUID userId) {}
}
