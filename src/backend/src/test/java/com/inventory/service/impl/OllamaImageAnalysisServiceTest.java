package com.inventory.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.dto.response.ImageAnalysisResult.AnalysisStatus;
import com.inventory.enums.CustomFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaImageAnalysisService Tests")
class OllamaImageAnalysisServiceTest {

    @Mock
    private RestTemplate ollamaRestTemplate;

    private OllamaImageAnalysisService service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID testUserId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OllamaImageAnalysisService(
                ollamaRestTemplate, objectMapper,
                "http://localhost:11434", "moondream:latest", 2, "");
    }

    private void waitForAnalysis(String analysisId, UUID userId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 50; i++) {
            Optional<ImageAnalysisResult> r = service.getResult(analysisId, userId);
            if (r.isPresent() && r.get().status() != AnalysisStatus.PENDING) {
                latch.countDown();
                break;
            }
            Thread.sleep(50);
        }
        assertThat(latch.await(100, TimeUnit.MILLISECONDS))
                .as("Analysis should complete within timeout")
                .isTrue();
    }

    @Test
    @DisplayName("should return analysis ID and store PENDING result")
    void analyzeImage_returnsPendingResult() {
        org.mockito.Mockito.lenient().when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"response\": \"{}\"}", HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);

        assertThat(analysisId).isNotNull().isNotEmpty();
        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("should parse successful Ollama response")
    void analyzeImage_parsesCompletedResponse() throws Exception {
        String ollamaResponse = objectMapper.writeValueAsString(
                java.util.Map.of("response", "{\"name\":\"Blue Keyboard\",\"status\":\"AVAILABLE\",\"stock\":1}"));

        when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(ollamaResponse, HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);
        waitForAnalysis(analysisId, testUserId);

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.get().suggestedName()).isEqualTo("Blue Keyboard");
        assertThat(result.get().suggestedStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get().suggestedStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("should handle Ollama connection failure gracefully")
    void analyzeImage_ollamaUnavailable_returnsFailed() throws Exception {
        when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);
        waitForAnalysis(analysisId, testUserId);

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    @DisplayName("should handle invalid JSON from Ollama")
    void analyzeImage_invalidJson_returnsFailed() throws Exception {
        String ollamaResponse = objectMapper.writeValueAsString(
                java.util.Map.of("response", "this is not json at all"));

        when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(ollamaResponse, HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);
        waitForAnalysis(analysisId, testUserId);

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    @DisplayName("should sanitize invalid status values")
    void analyzeImage_invalidStatus_setsNull() throws Exception {
        String ollamaResponse = objectMapper.writeValueAsString(
                java.util.Map.of("response", "{\"name\":\"Item\",\"status\":\"INVALID_STATUS\",\"stock\":1}"));

        when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(ollamaResponse, HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);
        waitForAnalysis(analysisId, testUserId);

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.get().suggestedStatus()).isNull();
    }

    @Test
    @DisplayName("should include custom field definitions in prompt")
    void analyzeImage_withCustomFields_sendsPromptWithFields() throws Exception {
        List<CustomFieldDefinition> fields = List.of(
                new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0));

        String ollamaResponse = objectMapper.writeValueAsString(
                java.util.Map.of("response", "{\"name\":\"Item\",\"customFields\":{\"color\":\"Red\"}}"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Object>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(ollamaRestTemplate.postForEntity(anyString(), captor.capture(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(ollamaResponse, HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, fields, testUserId);
        waitForAnalysis(analysisId, testUserId);

        HttpEntity<?> captured = captor.getValue();
        assertThat(captured.getBody().toString()).contains("color");

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().suggestedCustomFieldValues()).containsEntry("color", "Red");
    }

    @Test
    @DisplayName("should return empty for unknown analysis ID")
    void getResult_unknownId_returnsEmpty() {
        Optional<ImageAnalysisResult> result = service.getResult("nonexistent-id", testUserId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty when different user requests result")
    void getResult_differentUser_returnsEmpty() throws Exception {
        org.mockito.Mockito.lenient().when(ollamaRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"response\": \"{}\"}", HttpStatus.OK));

        String analysisId = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);

        Optional<ImageAnalysisResult> result = service.getResult(analysisId, otherUserId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should report as available")
    void isAvailable_returnsTrue() {
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("cleanup should not throw when no results exist")
    void cleanupExpiredResults_noResults_doesNotThrow() {
        service.cleanupExpiredResults();
    }
}
