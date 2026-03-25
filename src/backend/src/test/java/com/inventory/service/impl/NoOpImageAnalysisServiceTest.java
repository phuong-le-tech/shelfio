package com.inventory.service.impl;

import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.dto.response.ImageAnalysisResult.AnalysisStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoOpImageAnalysisService Tests")
class NoOpImageAnalysisServiceTest {

    private final NoOpImageAnalysisService service = new NoOpImageAnalysisService();
    private final UUID testUserId = UUID.randomUUID();

    @Test
    @DisplayName("should return noop analysis ID")
    void analyzeImage_returnsNoopId() {
        String id = service.analyzeImage(new byte[]{1, 2, 3}, List.of(), testUserId);
        assertThat(id).isEqualTo("noop");
    }

    @Test
    @DisplayName("should return FAILED result for any ID")
    void getResult_returnsFailedResult() {
        Optional<ImageAnalysisResult> result = service.getResult("any-id", testUserId);
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(result.get().errorMessage()).isEqualTo("AI analysis not configured");
    }

    @Test
    @DisplayName("should report as not available")
    void isAvailable_returnsFalse() {
        assertThat(service.isAvailable()).isFalse();
    }
}
