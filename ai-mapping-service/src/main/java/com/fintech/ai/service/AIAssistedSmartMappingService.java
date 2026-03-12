package com.fintech.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ai.controller.AIAssistedSmartMappingController.*;
import com.fintech.ai.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAssistedSmartMappingService {
    
    private final SmartMappingGenerator smartMappingGenerator;
    private final AIMappingGenerator aiMappingGenerator;
    private final MappingCacheService cacheService;
    private final ObjectMapper objectMapper;
    
    /**
     * Hybrid approach: Smart matching first, then AI for unmapped fields
     */
    public MappingConfig generateHybridMapping(String partnerId, String partnerSample,
                                                String internalSample, String createdBy, Boolean useAI) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Generate base mappings using smart matching
            log.info("Generating smart mappings for partner: {}", partnerId);
            MappingConfig smartConfig = smartMappingGenerator.generateSmartMapping(
                partnerId, partnerSample, internalSample, createdBy
            );
            
            if (!useAI) {
                log.info("Smart mapping completed in {}ms with {} mappings", 
                    System.currentTimeMillis() - startTime, smartConfig.getMappings().size());
                return smartConfig;
            }
            
            // Step 2: Identify unmapped fields
            Set<String> mappedInternalFields = smartConfig.getMappings().stream()
                .map(MappingConfig.FieldMapping::getTargetPath)
                .collect(Collectors.toSet());
            
            JsonNode internalNode = objectMapper.readTree(internalSample);
            Set<String> allInternalFields = extractAllFieldPaths(internalNode, "");
            
            Set<String> unmappedFields = new HashSet<>(allInternalFields);
            unmappedFields.removeAll(mappedInternalFields);
            
            log.info("Smart mapping coverage: {}/{} fields. Unmapped: {}", 
                mappedInternalFields.size(), allInternalFields.size(), unmappedFields.size());
            
            // Step 3: Use AI for unmapped fields if significant
            if (!unmappedFields.isEmpty() && unmappedFields.size() > 5) {
                log.info("Using AI to map remaining {} fields", unmappedFields.size());
                
                try {
                    MappingConfig aiConfig = aiMappingGenerator.generateMapping(
                        partnerId + "-ai-enhance", partnerSample, internalSample, createdBy
                    );
                    
                    // Merge AI mappings for unmapped fields only
                    for (MappingConfig.FieldMapping aiMapping : aiConfig.getMappings()) {
                        if (unmappedFields.contains(aiMapping.getTargetPath())) {
                            smartConfig.getMappings().add(aiMapping);
                        }
                    }
                    
                    log.info("AI enhanced {} additional mappings", unmappedFields.size());
                } catch (Exception e) {
                    log.warn("AI enhancement failed, using smart mappings only: {}", e.getMessage());
                }
            }
            
//            smartConfig.setUpdatedAt(LocalDateTime.now());
            cacheService.cacheMapping(partnerId, smartConfig);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Hybrid mapping completed in {}ms with {} total mappings", 
                totalTime, smartConfig.getMappings().size());
            
            return smartConfig;
            
        } catch (Exception e) {
            log.error("Hybrid mapping generation failed", e);
            throw new RuntimeException("Failed to generate hybrid mapping: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyze mapping quality and provide suggestions
     */
    public AnalysisResult analyzeMappingQuality(String partnerId, String partnerSample, Boolean useAI) {
        try {
            MappingConfig config = cacheService.getMapping(partnerId);
            if (config == null) {
                throw new RuntimeException("Mapping not found for partner: " + partnerId);
            }
            
            JsonNode partnerNode = objectMapper.readTree(partnerSample);
            Set<String> allPartnerFields = extractAllFieldPaths(partnerNode, "$");
            
            Set<String> mappedPartnerFields = config.getMappings().stream()
                .map(MappingConfig.FieldMapping::getSourcePath)
                .collect(Collectors.toSet());
            
            Set<String> unmappedFields = new HashSet<>(allPartnerFields);
            unmappedFields.removeAll(mappedPartnerFields);
            
            AnalysisResult result = new AnalysisResult();
            result.setPartnerId(partnerId);
            result.setTotalFields(allPartnerFields.size());
            result.setMappedFields(mappedPartnerFields.size());
            result.setUnmappedFields(unmappedFields.size());
            result.setCoveragePercentage(
                (double) mappedPartnerFields.size() / allPartnerFields.size() * 100
            );
            result.setUnmappedFieldsList(new ArrayList<>(unmappedFields));
            
            // Generate suggestions
            List<String> suggestions = new ArrayList<>();
            if (result.getCoveragePercentage() < 80) {
                suggestions.add("Coverage is below 80%. Consider using AI enhancement.");
            }
            if (unmappedFields.size() > 10) {
                suggestions.add("Many unmapped fields detected. Review partner JSON structure.");
            }
            
            // Check for potential issues
            Map<String, String> issues = new HashMap<>();
            for (MappingConfig.FieldMapping mapping : config.getMappings()) {
                if (mapping.getTransformationType() != null && 
                    mapping.getTransformationConfig() == null) {
                    issues.put(mapping.getTargetPath(), "Transformation type set but config missing");
                }
            }
            
            result.setSuggestions(suggestions);
            result.setPotentialIssues(issues);
            
            return result;
            
        } catch (Exception e) {
            log.error("Analysis failed", e);
            throw new RuntimeException("Failed to analyze mapping: " + e.getMessage(), e);
        }
    }
    
    /**
     * Enhance existing mapping with AI for unmapped fields
     */
    public MappingConfig enhanceWithAI(String partnerId, String partnerSample, String internalSample) {
        try {
            MappingConfig existingConfig = cacheService.getMapping(partnerId);
            if (existingConfig == null) {
                throw new RuntimeException("Mapping not found for partner: " + partnerId);
            }
            
            // Use AI to generate complete mapping
            MappingConfig aiConfig = aiMappingGenerator.generateMapping(
                partnerId + "-enhanced", partnerSample, internalSample, existingConfig.getCreatedBy()
            );
            
            // Merge with existing, preferring AI mappings
            Map<String, MappingConfig.FieldMapping> mergedMappings = new HashMap<>();
            
            // Add existing mappings
            for (MappingConfig.FieldMapping mapping : existingConfig.getMappings()) {
                mergedMappings.put(mapping.getTargetPath(), mapping);
            }
            
            // Override/add AI mappings
            for (MappingConfig.FieldMapping aiMapping : aiConfig.getMappings()) {
                mergedMappings.put(aiMapping.getTargetPath(), aiMapping);
            }
            
            existingConfig.setMappings(new ArrayList<>(mergedMappings.values()));
            existingConfig.setUpdatedAt(LocalDateTime.now());
            
            cacheService.cacheMapping(partnerId, existingConfig);
            
            log.info("Enhanced mapping for {} with {} total mappings", 
                partnerId, existingConfig.getMappings().size());
            
            return existingConfig;
            
        } catch (Exception e) {
            log.error("Enhancement failed", e);
            throw new RuntimeException("Failed to enhance mapping: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get mapping statistics
     */
    public MappingStats getMappingStatistics(String partnerId) {
        MappingConfig config = cacheService.getMapping(partnerId);
        if (config == null) {
            throw new RuntimeException("Mapping not found for partner: " + partnerId);
        }
        
        MappingStats stats = new MappingStats();
        stats.setPartnerId(partnerId);
        stats.setTotalMappings(config.getMappings().size());
        
        long withTransformations = config.getMappings().stream()
            .filter(m -> m.getTransformationType() != null)
            .count();
        
        stats.setWithTransformations((int) withTransformations);
        stats.setWithoutTransformations(config.getMappings().size() - (int) withTransformations);
        
        // Count transformation types
        Map<String, Integer> transformationTypes = new HashMap<>();
        for (MappingConfig.FieldMapping mapping : config.getMappings()) {
            if (mapping.getTransformationType() != null) {
                transformationTypes.merge(mapping.getTransformationType(), 1, Integer::sum);
            }
        }
        stats.setTransformationTypes(transformationTypes);
        
        // Calculate average field depth
        double avgDepth = config.getMappings().stream()
            .mapToInt(m -> m.getTargetPath().split("\\.").length)
            .average()
            .orElse(0.0);
        stats.setAverageFieldDepth(avgDepth);
        
        stats.setGenerationMethod("hybrid");
        
        return stats;
    }
    
    /**
     * Batch generate mappings for multiple partners
     */
    public BatchResult batchGenerateMappings(List<PartnerConfig> partners) {
        long startTime = System.currentTimeMillis();
        
        BatchResult result = new BatchResult();
        result.setTotalPartners(partners.size());
        
        List<String> successful = new ArrayList<>();
        Map<String, String> failures = new HashMap<>();
        
        for (PartnerConfig partner : partners) {
            try {
                generateHybridMapping(
                    partner.getPartnerId(),
                    partner.getPartnerSample(),
                    partner.getInternalSample(),
                    partner.getCreatedBy(),
                    false // Use smart mapping for batch
                );
                successful.add(partner.getPartnerId());
            } catch (Exception e) {
                failures.put(partner.getPartnerId(), e.getMessage());
            }
        }
        
        result.setSuccessCount(successful.size());
        result.setFailureCount(failures.size());
        result.setSuccessfulPartners(successful);
        result.setFailures(failures);
        result.setTotalTimeMs(System.currentTimeMillis() - startTime);
        
        log.info("Batch generation completed: {}/{} successful in {}ms", 
            successful.size(), partners.size(), result.getTotalTimeMs());
        
        return result;
    }
    
    /**
     * Compare smart vs AI mapping approaches
     */
    public ComparisonResult compareMappingApproaches(String partnerId, 
                                                      String partnerSample, String internalSample) {
        ComparisonResult result = new ComparisonResult();
        result.setPartnerId(partnerId);
        
        // Smart mapping
        long smartStart = System.currentTimeMillis();
        try {
            MappingConfig smartConfig = smartMappingGenerator.generateSmartMapping(
                partnerId + "-smart", partnerSample, internalSample, "comparison"
            );
            
            ComparisonResult.SmartMappingResult smartResult = new ComparisonResult.SmartMappingResult();
            smartResult.setMappingsGenerated(smartConfig.getMappings().size());
            smartResult.setTimeMs(System.currentTimeMillis() - smartStart);
            smartResult.setCoveragePercentage(calculateCoverage(smartConfig, internalSample));
            smartResult.setCost("Free");
            
            result.setSmartMapping(smartResult);
        } catch (Exception e) {
            log.error("Smart mapping comparison failed", e);
        }
        
        // AI mapping
        long aiStart = System.currentTimeMillis();
        try {
            MappingConfig aiConfig = aiMappingGenerator.generateMapping(
                partnerId + "-ai", partnerSample, internalSample, "comparison"
            );
            
            ComparisonResult.AIMappingResult aiResult = new ComparisonResult.AIMappingResult();
            aiResult.setMappingsGenerated(aiConfig.getMappings().size());
            aiResult.setTimeMs(System.currentTimeMillis() - aiStart);
            aiResult.setCoveragePercentage(calculateCoverage(aiConfig, internalSample));
            aiResult.setCost("~$0.01-0.05 per request");
            
            result.setAiMapping(aiResult);
        } catch (Exception e) {
            log.error("AI mapping comparison failed", e);
        }
        
        // Recommendation
        if (result.getSmartMapping() != null && result.getAiMapping() != null) {
            if (result.getSmartMapping().getCoveragePercentage() > 85) {
                result.setRecommendation("Smart mapping provides good coverage. Use it for cost efficiency.");
            } else if (result.getAiMapping().getCoveragePercentage() > 
                       result.getSmartMapping().getCoveragePercentage() + 10) {
                result.setRecommendation("AI mapping provides significantly better coverage. Consider hybrid approach.");
            } else {
                result.setRecommendation("Both approaches similar. Use smart mapping for speed and cost.");
            }
        }
        
        return result;
    }
    
    private Set<String> extractAllFieldPaths(JsonNode node, String prefix) {
        Set<String> paths = new HashSet<>();
        extractFieldPathsRecursive(node, prefix, paths);
        return paths;
    }
    
    private void extractFieldPathsRecursive(JsonNode node, String currentPath, Set<String> paths) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                
                if (fieldValue.isObject()) {
                    extractFieldPathsRecursive(fieldValue, newPath, paths);
                } else if (fieldValue.isArray() && fieldValue.size() > 0) {
                    extractFieldPathsRecursive(fieldValue.get(0), newPath + "[0]", paths);
                } else if (fieldValue.isValueNode()) {
                    paths.add(newPath);
                }
            }
        }
    }
    
    private double calculateCoverage(MappingConfig config, String internalSample) {
        try {
            JsonNode internalNode = objectMapper.readTree(internalSample);
            Set<String> allFields = extractAllFieldPaths(internalNode, "");
            Set<String> mappedFields = config.getMappings().stream()
                .map(MappingConfig.FieldMapping::getTargetPath)
                .collect(Collectors.toSet());
            
            return (double) mappedFields.size() / allFields.size() * 100;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
