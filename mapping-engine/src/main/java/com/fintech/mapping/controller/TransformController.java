package com.fintech.mapping.controller;

import com.fintech.mapping.model.MappingConfig;
import com.fintech.mapping.service.DynamicMappingService;
import com.fintech.mapping.service.MappingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transform")
@RequiredArgsConstructor
public class TransformController {
    
    private final MappingEngine mappingEngine;
    private final DynamicMappingService dynamicMappingService;
    
    @PostMapping("/{partnerId}")
    public ResponseEntity<Map<String, Object>> transform(
            @PathVariable String partnerId,
            @RequestBody String sourceJson) {
        
        // Fetch mapping dynamically from cache or AI service
        MappingConfig config = dynamicMappingService.getMappingConfig(partnerId);
        
        if (config == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Map<String, Object> result = mappingEngine.transform(sourceJson, config);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/invalidate/{partnerId}")
    public ResponseEntity<Map<String, String>> invalidateCache(@PathVariable String partnerId) {
        dynamicMappingService.invalidateCache(partnerId);
        return ResponseEntity.ok(Map.of("message", "Cache invalidated for partner: " + partnerId));
    }
}
