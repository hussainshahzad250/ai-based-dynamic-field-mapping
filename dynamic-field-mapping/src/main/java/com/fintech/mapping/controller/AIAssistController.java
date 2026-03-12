package com.fintech.mapping.controller;

import com.fintech.mapping.model.MappingConfig;
import com.fintech.mapping.service.AIMappingGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-assist")
@RequiredArgsConstructor
public class AIAssistController {
    
    private final AIMappingGenerator aiMappingGenerator;
    
    @PostMapping("/generate-mapping")
    public ResponseEntity<MappingConfig> generateMapping(
            @RequestParam String partnerId,
            @RequestBody Map<String, Object> request) {
        
        String partnerSample = (String) request.get("partnerSample");
        String internalSample = (String) request.get("internalSample");
        
        MappingConfig config = aiMappingGenerator.generateMapping(
            partnerId, partnerSample, internalSample);
        
        return ResponseEntity.ok(config);
    }
    
    @PostMapping("/suggest-improvements")
    public ResponseEntity<Map<String, Object>> suggestImprovements(
            @RequestParam String partnerId,
            @RequestBody String partnerJson) {
        
        Map<String, Object> suggestions = aiMappingGenerator.analyzeMappingQuality(
            partnerId, partnerJson);
        
        return ResponseEntity.ok(suggestions);
    }
    
    @PostMapping("/auto-fix")
    public ResponseEntity<MappingConfig> autoFixMapping(
            @RequestParam String partnerId,
            @RequestBody Map<String, Object> request) {
        
        String errorLog = (String) request.get("errorLog");
        String failedJson = (String) request.get("failedJson");
        
        MappingConfig updatedConfig = aiMappingGenerator.autoFixMapping(
            partnerId, errorLog, failedJson);
        
        return ResponseEntity.ok(updatedConfig);
    }
}
