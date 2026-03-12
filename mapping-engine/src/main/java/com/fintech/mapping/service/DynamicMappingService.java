package com.fintech.mapping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.mapping.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicMappingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.service.url:http://localhost:8081}")
    private String aiServiceUrl;
    
    private static final String MAPPING_PREFIX = "mapping:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    
    public MappingConfig getMappingConfig(String partnerId) {
        // Try cache first
        String key = MAPPING_PREFIX + partnerId;
        Object cached = redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            log.debug("Cache hit for partner: {}", partnerId);
            return objectMapper.convertValue(cached, MappingConfig.class);
        }
        
        // Fetch from AI service
        log.info("Fetching mapping from AI service for partner: {}", partnerId);
        try {
            String url = aiServiceUrl + "/api/ai-mapping/" + partnerId;
            MappingConfig config = restTemplate.getForObject(url, MappingConfig.class);
            
            if (config != null) {
                // Cache it
                redisTemplate.opsForValue().set(key, config, CACHE_TTL);
                log.info("Cached mapping for partner: {}", partnerId);
            }
            
            return config;
        } catch (Exception e) {
            log.error("Failed to fetch mapping from AI service", e);
            throw new RuntimeException("Mapping not found for partner: " + partnerId, e);
        }
    }
    
    public void invalidateCache(String partnerId) {
        String key = MAPPING_PREFIX + partnerId;
        redisTemplate.delete(key);
        log.info("Invalidated cache for partner: {}", partnerId);
    }
}
