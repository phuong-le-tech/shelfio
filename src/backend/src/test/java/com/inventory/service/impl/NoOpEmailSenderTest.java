package com.inventory.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NoOpEmailSender Tests")
class NoOpEmailSenderTest {

    private final NoOpEmailSender noOpEmailSender = new NoOpEmailSender();

    @Test
    @DisplayName("should log email without actually sending")
    void send_logsEmailWithoutSending() {
        assertThatCode(() -> noOpEmailSender.send("user@example.com", "Test Subject", "<p>Hello World</p>"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should handle long HTML content")
    void send_longContent_logsCharCount() {
        String longHtml = "<p>" + "x".repeat(10000) + "</p>";
        assertThatCode(() -> noOpEmailSender.send("user@example.com", "Subject", longHtml))
                .doesNotThrowAnyException();
    }
}
