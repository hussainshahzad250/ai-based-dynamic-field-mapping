package com.fintech.mapping.controller;

import com.fintech.mapping.model.MappingConfig;
import com.fintech.mapping.service.MappingConfigLoader;
import com.fintech.mapping.service.MappingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transform")
@RequiredArgsConstructor
public class MappingController {
    
    private final MappingEngine mappingEngine;
    private final MappingConfigLoader configLoader;
    
    @PostMapping("/{partnerId}")
    public ResponseEntity<Map<String, Object>> transform(
            @PathVariable String partnerId,
            @RequestBody String sourceJson) {
        
        MappingConfig config = configLoader.getConfig(partnerId);
        if (config == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Map<String, Object> result = mappingEngine.transform(sourceJson, config);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/reload/{partnerId}")
    public ResponseEntity<Void> reloadConfig(@PathVariable String partnerId) {
        configLoader.reloadConfig(partnerId);
        return ResponseEntity.ok().build();
    }
}
