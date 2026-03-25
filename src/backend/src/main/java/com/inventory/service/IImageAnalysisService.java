package com.inventory.service;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.response.ImageAnalysisResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IImageAnalysisService {

    String analyzeImage(byte[] imageData, List<CustomFieldDefinition> fieldDefinitions, UUID userId);

    Optional<ImageAnalysisResult> getResult(String analysisId, UUID userId);

    boolean isAvailable();
}
