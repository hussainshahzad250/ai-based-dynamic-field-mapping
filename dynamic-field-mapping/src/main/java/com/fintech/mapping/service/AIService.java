package com.fintech.mapping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.provider:openai}")
    private String provider;
    
    @Value("${ai.api-key:}")
    private String apiKey;
    
    @Value("${ai.model:gpt-4}")
    private String model;
    
    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String endpoint;
    
    public String callAI(String prompt) {
        try {
            log.info("Calling AI service: provider={}, model={}, endpoint={}", provider, model, endpoint);
            
            if ("ollama".equalsIgnoreCase(provider)) {
                return callOllama(prompt);
            } else if ("anthropic".equalsIgnoreCase(provider)) {
                return callAnthropic(prompt);
            } else {
                return callOpenAI(prompt);
            }
        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service unavailable: " + e.getMessage(), e);
        }
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
        
        log.debug("Ollama request: {}", requestBody);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            endpoint, HttpMethod.POST, request,
            (Class<Map<String, Object>>)(Class<?>)Map.class
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Ollama");
        }
        
        log.debug("Ollama response: {}", responseBody);
        
        String responseText = (String) responseBody.get("response");
        
        // Clean up response if needed
        if (responseText != null) {
            responseText = responseText.trim();
            // Remove markdown code blocks if present
            if (responseText.startsWith("```json")) {
                responseText = responseText.substring(7);
            }
            if (responseText.startsWith("```")) {
                responseText = responseText.substring(3);
            }
            if (responseText.endsWith("```")) {
                responseText = responseText.substring(0, responseText.length() - 3);
            }
            responseText = responseText.trim();
        }
        
        return responseText;
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
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("system", "You are a JSON mapping expert. Always return valid JSON.");
        requestBody.put("temperature", 0.3);
        
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
}
