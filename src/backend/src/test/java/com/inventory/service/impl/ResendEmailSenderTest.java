package com.inventory.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResendEmailSender Tests")
class ResendEmailSenderTest {

    @Mock
    private RestTemplate restTemplate;

    private ResendEmailSender resendEmailSender;

    @BeforeEach
    void setUp() {
        resendEmailSender = new ResendEmailSender(restTemplate, "re_test_api_key", "noreply@example.com");
    }

    @Nested
    @DisplayName("validateConfig")
    class ValidateConfigTests {

        @Test
        @DisplayName("should pass validation with valid config")
        void validateConfig_validConfig_passes() {
            resendEmailSender.validateConfig();
        }

        @Test
        @DisplayName("should throw when API key is blank")
        void validateConfig_blankApiKey_throws() {
            ResendEmailSender sender = new ResendEmailSender(restTemplate, "", "noreply@example.com");

            assertThatThrownBy(sender::validateConfig)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESEND_API_KEY");
        }

        @Test
        @DisplayName("should throw when API key is null")
        void validateConfig_nullApiKey_throws() {
            ResendEmailSender sender = new ResendEmailSender(restTemplate, null, "noreply@example.com");

            assertThatThrownBy(sender::validateConfig)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESEND_API_KEY");
        }

        @Test
        @DisplayName("should throw when from address is blank")
        void validateConfig_blankFromAddress_throws() {
            ResendEmailSender sender = new ResendEmailSender(restTemplate, "re_test_key", "");

            assertThatThrownBy(sender::validateConfig)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.email.from");
        }

        @Test
        @DisplayName("should throw when from address is invalid email")
        void validateConfig_invalidFromAddress_throws() {
            ResendEmailSender sender = new ResendEmailSender(restTemplate, "re_test_key", "not-an-email");

            assertThatThrownBy(sender::validateConfig)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.email.from");
        }

        @Test
        @DisplayName("should throw when from address is null")
        void validateConfig_nullFromAddress_throws() {
            ResendEmailSender sender = new ResendEmailSender(restTemplate, "re_test_key", null);

            assertThatThrownBy(sender::validateConfig)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("app.email.from");
        }
    }

    @Nested
    @DisplayName("send")
    class SendTests {

        @Test
        @DisplayName("should send email successfully via Resend API")
        void send_success_callsResendApi() {
            when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("{}"));

            resendEmailSender.send("user@example.com", "Test Subject", "<p>Hello</p>");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<HttpEntity<Object>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), captor.capture(), eq(String.class));

            HttpHeaders headers = captor.getValue().getHeaders();
            assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer re_test_api_key");
        }

        @Test
        @DisplayName("should throw RuntimeException on HttpClientErrorException")
        void send_httpClientError_throwsRuntimeException() {
            when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.BAD_REQUEST, "Bad Request",
                            HttpHeaders.EMPTY, "error".getBytes(), null));

            assertThatThrownBy(() -> resendEmailSender.send("user@example.com", "Subject", "<p>Body</p>"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }

        @Test
        @DisplayName("should throw RuntimeException on RestClientException")
        void send_restClientError_throwsRuntimeException() {
            when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RestClientException("Connection timeout"));

            assertThatThrownBy(() -> resendEmailSender.send("user@example.com", "Subject", "<p>Body</p>"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send email");
        }
    }
}
