package com.inventory.dto.response;

import java.util.Map;

public record ImageAnalysisResult(
        String analysisId,
        AnalysisStatus status,
        String suggestedName,
        String suggestedStatus,
        Integer suggestedStock,
        Map<String, Object> suggestedCustomFieldValues,
        String errorMessage) {

    public enum AnalysisStatus {
        PENDING, COMPLETED, FAILED
    }

    public static ImageAnalysisResult pending(String analysisId) {
        return new ImageAnalysisResult(analysisId, AnalysisStatus.PENDING, null, null, null, null, null);
    }

    public static ImageAnalysisResult failed(String analysisId, String errorMessage) {
        return new ImageAnalysisResult(analysisId, AnalysisStatus.FAILED, null, null, null, null, errorMessage);
    }
}
