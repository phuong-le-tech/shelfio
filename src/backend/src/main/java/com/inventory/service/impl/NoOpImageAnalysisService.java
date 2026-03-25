package com.inventory.service.impl;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.service.IImageAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "noop", matchIfMissing = true)
public class NoOpImageAnalysisService implements IImageAnalysisService {

    @Override
    public String analyzeImage(byte[] imageData, List<CustomFieldDefinition> fieldDefinitions, UUID userId) {
        return "noop";
    }

    @Override
    public Optional<ImageAnalysisResult> getResult(String analysisId, UUID userId) {
        return Optional.of(ImageAnalysisResult.failed(analysisId, "AI analysis not configured"));
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
