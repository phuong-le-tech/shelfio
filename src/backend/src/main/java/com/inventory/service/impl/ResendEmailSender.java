package com.inventory.service.impl;

import com.inventory.service.EmailSender;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String EMAIL_PATTERN = "^.+@.+\\..+$";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailSender(
            RestTemplate restTemplate,
            @Value("${app.email.resend-api-key}") String apiKey,
            @Value("${app.email.from}") String fromAddress) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @PostConstruct
    void validateConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "RESEND_API_KEY is required when app.email.provider=resend");
        }
        if (fromAddress == null || fromAddress.isBlank() || !fromAddress.matches(EMAIL_PATTERN)) {
            throw new IllegalStateException(
                "app.email.from must be a valid email address when app.email.provider=resend");
        }
    }

    @Override
    public void send(@NonNull String to, @NonNull String subject, @NonNull String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "from", fromAddress,
                "to", List.of(to),
                "subject", subject,
                "html", htmlContent);

        try {
            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Email sent to {} via Resend", obfuscateEmail(to));
        } catch (HttpClientErrorException e) {
            log.error("Resend API error sending to {}: {} - {}", obfuscateEmail(to), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to send email", e);
        } catch (RestClientException e) {
            log.error("Failed to send email to {} via Resend: {}", obfuscateEmail(to), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private static String obfuscateEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0) return "***";
        if (atIndex <= 1) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
