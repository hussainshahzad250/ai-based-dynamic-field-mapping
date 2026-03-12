package com.fintech.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ai.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIMappingGenerator {
    
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final MappingCacheService cacheService;
    
    public MappingConfig generateMapping(String partnerId, String partnerSample, String internalSample, String createdBy) {
        String prompt = buildMappingPrompt(partnerSample, internalSample);
        String aiResponse = aiService.callAI(prompt);
        
        try {
            MappingConfig config = objectMapper.readValue(aiResponse, MappingConfig.class);
            config.setPartnerId(partnerId);
//            config.setCreatedAt(LocalDateTime.now());
//            config.setUpdatedAt(LocalDateTime.now());
            config.setCreatedBy(createdBy);
            config.setStatus(MappingConfig.ConfigStatus.DRAFT);
            
            // Cache the generated config
            cacheService.cacheMapping(partnerId, config);
            
            return config;
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("AI mapping generation failed: " + e.getMessage(), e);
        }
    }
    
    public MappingConfig improveMapping(String partnerId, String partnerSample) {
        MappingConfig currentConfig = cacheService.getMapping(partnerId);
        if (currentConfig == null) {
            throw new RuntimeException("No mapping found for partner: " + partnerId);
        }
        
        String prompt = buildImprovementPrompt(partnerSample, currentConfig);
        String aiResponse = aiService.callAI(prompt);
        
        try {
            MappingConfig improved = objectMapper.readValue(aiResponse, MappingConfig.class);
            improved.setPartnerId(partnerId);
            improved.setCreatedAt(currentConfig.getCreatedAt());
            improved.setUpdatedAt(LocalDateTime.now());
            improved.setCreatedBy(currentConfig.getCreatedBy());
            improved.setStatus(MappingConfig.ConfigStatus.DRAFT);
            
            return improved;
        } catch (Exception e) {
            log.error("Failed to improve mapping", e);
            throw new RuntimeException("Mapping improvement failed: " + e.getMessage(), e);
        }
    }
    
    private String buildMappingPrompt(String partnerSample, String internalSample) {
        return String.format("""
            Generate a JSON mapping configuration to transform the partner JSON to internal format.
            
            Partner JSON (source):
            %s
            
            Internal JSON (target):
            %s
            
            Rules:
            - Use JSONPath syntax for sourcePath (e.g., $.client.name)
            - Use dot notation for targetPath (e.g., customer.fullName)
            - Detect needed transformations (uppercase, lowercase, dateFormat, multiply, concat)
            - Include defaultValue for optional fields
            - For date transformations, specify inputFormat and outputFormat
            - For numeric transformations, specify factor
            
            Return ONLY valid JSON matching this schema:
            {
              "version": "1.0",
              "mappings": [
                {
                  "targetPath": "string",
                  "sourcePath": "string (JSONPath)",
                  "defaultValue": "string (optional)",
                  "transformationType": "string (optional)",
                  "transformationConfig": {}
                }
              ]
            }
            """, partnerSample, internalSample);
    }
    
    private String buildImprovementPrompt(String partnerSample, MappingConfig currentConfig) {
        try {
            String configJson = objectMapper.writeValueAsString(currentConfig);
            return String.format("""
                Improve this mapping configuration based on the partner JSON sample.
                
                Current mapping:
                %s
                
                Partner JSON sample:
                %s
                
                Analyze and improve:
                1. Add missing field mappings
                2. Fix data type mismatches
                3. Optimize transformations
                4. Add appropriate default values
                
                Return the improved mapping configuration as JSON with the same schema.
                """, configJson, partnerSample);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build improvement prompt", e);
        }
    }
}
