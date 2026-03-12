package com.fintech.ai.controller;

import com.fintech.ai.model.MappingConfig;
import com.fintech.ai.service.AIAssistedSmartMappingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/ai-smart-mapping")
@RequiredArgsConstructor
public class AIAssistedSmartMappingController {

    private final AIAssistedSmartMappingService aiAssistedService;

    /**
     * Generate mapping using hybrid approach: Smart matching + AI enhancement
     * Best for large JSONs (100+ fields)
     */
    @PostMapping("/generate")
    public ResponseEntity<MappingConfig> generateAIAssistedMapping(
            @Valid @RequestBody GenerateRequest request) {
        MappingConfig config = aiAssistedService.generateHybridMapping(
                request.getPartnerId(),
                request.getPartnerSample(),
                request.getInternalSample(),
                request.getCreatedBy(),
                request.getUseAI()
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Analyze mapping quality and get AI suggestions for improvements
     */
    @PostMapping("/analyze/{partnerId}")
    public ResponseEntity<AnalysisResult> analyzeMappingQuality(
            @PathVariable String partnerId,
            @Valid @RequestBody AnalyzeRequest request) {
        AnalysisResult result = aiAssistedService.analyzeMappingQuality(
                partnerId,
                request.getPartnerSample(),
                request.getUseAI()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Enhance existing smart mapping with AI for unmapped fields
     */
    @PostMapping("/enhance/{partnerId}")
    public ResponseEntity<MappingConfig> enhanceMapping(
            @PathVariable String partnerId,
            @Valid @RequestBody EnhanceRequest request) {
        MappingConfig enhanced = aiAssistedService.enhanceWithAI(
                partnerId,
                request.getPartnerSample(),
                request.getInternalSample()
        );
        return ResponseEntity.ok(enhanced);
    }

    /**
     * Get mapping statistics and coverage report
     */
    @GetMapping("/stats/{partnerId}")
    public ResponseEntity<MappingStats> getMappingStats(@PathVariable String partnerId) {
        MappingStats stats = aiAssistedService.getMappingStatistics(partnerId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Batch generate mappings for multiple partners
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<BatchResult> batchGenerate(
            @Valid @RequestBody BatchGenerateRequest request) {
        BatchResult result = aiAssistedService.batchGenerateMappings(request.getPartners());
        return ResponseEntity.ok(result);
    }

    /**
     * Compare two mapping approaches (Smart vs AI)
     */
    @PostMapping("/compare")
    public ResponseEntity<ComparisonResult> compareMappingApproaches(
            @Valid @RequestBody CompareRequest request) {
        ComparisonResult result = aiAssistedService.compareMappingApproaches(
                request.getPartnerId(),
                request.getPartnerSample(),
                request.getInternalSample()
        );
        return ResponseEntity.ok(result);
    }

    // Request/Response DTOs

    @Data
    public static class GenerateRequest {
        @NotBlank
        private String partnerId;
        @NotBlank
        private String partnerSample;
        @NotBlank
        private String internalSample;
        private String createdBy = "system";
        private Boolean useAI = false; // Use AI for unmapped fields
    }

    @Data
    public static class AnalyzeRequest {
        @NotBlank
        private String partnerSample;
        private Boolean useAI = true;
    }

    @Data
    public static class EnhanceRequest {
        @NotBlank
        private String partnerSample;
        @NotBlank
        private String internalSample;
    }

    @Data
    public static class CompareRequest {
        @NotBlank
        private String partnerId;
        @NotBlank
        private String partnerSample;
        @NotBlank
        private String internalSample;
    }

    @Data
    public static class BatchGenerateRequest {
        private java.util.List<PartnerConfig> partners;
    }

    @Data
    public static class PartnerConfig {
        private String partnerId;
        private String partnerSample;
        private String internalSample;
        private String createdBy;
    }

    @Data
    public static class AnalysisResult {
        private String partnerId;
        private Integer totalFields;
        private Integer mappedFields;
        private Integer unmappedFields;
        private Double coveragePercentage;
        private java.util.List<String> unmappedFieldsList;
        private java.util.List<String> suggestions;
        private java.util.Map<String, String> potentialIssues;
    }

    @Data
    public static class MappingStats {
        private String partnerId;
        private Integer totalMappings;
        private Integer withTransformations;
        private Integer withoutTransformations;
        private java.util.Map<String, Integer> transformationTypes;
        private Double averageFieldDepth;
        private String generationMethod; // "smart", "ai", "hybrid"
    }

    @Data
    public static class BatchResult {
        private Integer totalPartners;
        private Integer successCount;
        private Integer failureCount;
        private java.util.List<String> successfulPartners;
        private java.util.Map<String, String> failures;
        private Long totalTimeMs;
    }

    @Data
    public static class ComparisonResult {
        private String partnerId;
        private SmartMappingResult smartMapping;
        private AIMappingResult aiMapping;
        private String recommendation;

        @Data
        public static class SmartMappingResult {
            private Integer mappingsGenerated;
            private Long timeMs;
            private Double coveragePercentage;
            private String cost = "Free";
        }

        @Data
        public static class AIMappingResult {
            private Integer mappingsGenerated;
            private Long timeMs;
            private Double coveragePercentage;
            private String cost;
        }
    }
}
