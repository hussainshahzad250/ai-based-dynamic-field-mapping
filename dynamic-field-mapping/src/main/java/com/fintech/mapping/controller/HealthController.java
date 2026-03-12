package com.fintech.mapping.controller;

import com.fintech.mapping.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    
    private final AIService aiService;
    
    @Value("${ai.provider}")
    private String provider;
    
    @Value("${ai.model}")
    private String model;
    
    @Value("${ai.endpoint}")
    private String endpoint;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "ai", Map.of(
                "provider", provider,
                "model", model,
                "endpoint", endpoint
            )
        ));
    }
    
    @GetMapping("/ai-test")
    public ResponseEntity<Map<String, Object>> testAI() {
        try {
            String response = aiService.callAI("Return a simple JSON object with a 'status' field set to 'ok'");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "response", response
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
