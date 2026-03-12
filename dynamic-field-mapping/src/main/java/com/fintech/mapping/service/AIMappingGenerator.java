package com.fintech.mapping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.mapping.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIMappingGenerator {
    
    private final ObjectMapper objectMapper;
    private final AIService aiService;
    private final MappingConfigLoader configLoader;
    
    public MappingConfig generateMapping(String partnerId, String partnerSample, String internalSample) {
        String prompt = buildMappingPrompt(partnerSample, internalSample);
        String aiResponse = aiService.callAI(prompt);
        
        try {
            MappingConfig config = objectMapper.readValue(aiResponse, MappingConfig.class);
            config.setPartnerId(partnerId);
            return config;
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("AI mapping generation failed", e);
        }
    }
    
    public Map<String, Object> analyzeMappingQuality(String partnerId, String partnerJson) {
        MappingConfig config = configLoader.getConfig(partnerId);
        
        String prompt = String.format("""
            Analyze this mapping configuration quality:
            
            Partner JSON sample:
            %s
            
            Current mapping config:
            %s
            
            Provide:
            1. Missing field mappings
            2. Potential data type mismatches
            3. Suggested transformations
            4. Performance optimization tips
            
            Return as JSON with keys: missingFields, typeMismatches, suggestedTransformations, optimizations
            """, partnerJson, toJson(config));
        
        String aiResponse = aiService.callAI(prompt);
        return parseAnalysisResponse(aiResponse);
    }
    
    public MappingConfig autoFixMapping(String partnerId, String errorLog, String failedJson) {
        MappingConfig currentConfig = configLoader.getConfig(partnerId);
        
        String prompt = String.format("""
            Fix this mapping configuration based on the error:
            
            Current config:
            %s
            
            Failed JSON:
            %s
            
            Error log:
            %s
            
            Return the corrected mapping configuration as JSON.
            """, toJson(currentConfig), failedJson, errorLog);
        
        String aiResponse = aiService.callAI(prompt);
        
        try {
            MappingConfig fixedConfig = objectMapper.readValue(aiResponse, MappingConfig.class);
            fixedConfig.setPartnerId(partnerId);
            return fixedConfig;
        } catch (Exception e) {
            log.error("Failed to parse fixed config", e);
            throw new RuntimeException("Auto-fix failed", e);
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
    
    private Map<String, Object> parseAnalysisResponse(String aiResponse) {
        try {
            return objectMapper.readValue(aiResponse, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse analysis response", e);
            return Map.of("error", "Failed to parse AI response");
        }
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
