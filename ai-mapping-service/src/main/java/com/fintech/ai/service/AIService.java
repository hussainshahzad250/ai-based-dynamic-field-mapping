package com.fintech.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final RestTemplate restTemplate;
    private final MappingCacheService cacheService;
    
    @Value("${ai.provider}")
    private String provider;
    
    @Value("${ai.api-key:}")
    private String apiKey;
    
    @Value("${ai.model}")
    private String model;
    
    @Value("${ai.endpoint}")
    private String endpoint;
    
    @Value("${ai.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${ai.cache.ttl-hours:24}")
    private int cacheTtlHours;
    
    public String callAI(String prompt) {
        if (cacheEnabled) {
            String hash = generateHash(prompt);
            String cached = cacheService.getAIResponse(hash);
            if (cached != null) {
                log.info("Returning cached AI response");
                return cached;
            }
            
            String response = callAIProvider(prompt);
            cacheService.cacheAIResponse(hash, response, Duration.ofHours(cacheTtlHours));
            return response;
        }
        
        return callAIProvider(prompt);
    }
    
    private String callAIProvider(String prompt) {
        try {
            log.info("Calling AI: provider={}, model={}", provider, model);
            
            return switch (provider.toLowerCase()) {
                case "ollama" -> callOllama(prompt);
                case "anthropic" -> callAnthropic(prompt);
                default -> callOpenAI(prompt);
            };
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service unavailable: " + e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private String callOllama(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String fullPrompt = "You are a JSON mapping expert. Always return valid JSON.\n\n" + prompt;
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", fullPrompt);
        requestBody.put("stream", false);
        requestBody.put("format", "json");
        requestBody.put("options", Map.of("temperature", 0.3));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint, HttpMethod.POST, request,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Ollama");
        }
        
        String responseText = (String) responseBody.get("response");
        return cleanJsonResponse(responseText);
    }
    
    @SuppressWarnings("unchecked")
    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a JSON mapping expert. Always return valid JSON."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);
        requestBody.put("response_format", Map.of("type", "json_object"));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint, HttpMethod.POST, request,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from OpenAI");
        }
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
    
    @SuppressWarnings("unchecked")
    private String callAnthropic(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 4096);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("system", "You are a JSON mapping expert. Always return valid JSON.");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint, HttpMethod.POST, request,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Anthropic");
        }
        
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        return (String) content.get(0).get("text");
    }
    
    private String cleanJsonResponse(String response) {
        if (response == null) return null;
        
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
    
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
