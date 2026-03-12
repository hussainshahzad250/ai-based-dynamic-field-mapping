package com.fintech.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ai.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String MAPPING_PREFIX = "mapping:";
    private static final String AI_RESPONSE_PREFIX = "ai:response:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    public void cacheMapping(String partnerId, MappingConfig config) {
        String key = MAPPING_PREFIX + partnerId;
        redisTemplate.opsForValue().set(key, config, DEFAULT_TTL);
        log.info("Cached mapping for partner: {}", partnerId);
    }
    
    public MappingConfig getMapping(String partnerId) {
        String key = MAPPING_PREFIX + partnerId;
        Object cached = redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            log.debug("Cache hit for partner: {}", partnerId);
            return objectMapper.convertValue(cached, MappingConfig.class);
        }
        
        log.debug("Cache miss for partner: {}", partnerId);
        return null;
    }
    
    public void invalidateMapping(String partnerId) {
        String key = MAPPING_PREFIX + partnerId;
        redisTemplate.delete(key);
        log.info("Invalidated cache for partner: {}", partnerId);
    }
    
    public void cacheAIResponse(String requestHash, String response, Duration ttl) {
        String key = AI_RESPONSE_PREFIX + requestHash;
        redisTemplate.opsForValue().set(key, response, ttl);
        log.debug("Cached AI response with hash: {}", requestHash);
    }
    
    public String getAIResponse(String requestHash) {
        String key = AI_RESPONSE_PREFIX + requestHash;
        Object cached = redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            log.debug("AI response cache hit: {}", requestHash);
            return cached.toString();
        }
        
        return null;
    }
    
    public Set<String> getAllPartnerIds() {
        Set<String> keys = redisTemplate.keys(MAPPING_PREFIX + "*");
        if (keys != null) {
            return keys.stream()
                .map(key -> key.replace(MAPPING_PREFIX, ""))
                .collect(java.util.stream.Collectors.toSet());
        }
        return Set.of();
    }
    
    public void clearAllMappings() {
        Set<String> keys = redisTemplate.keys(MAPPING_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} mapping caches", keys.size());
        }
    }
    
    public void clearAllAIResponses() {
        Set<String> keys = redisTemplate.keys(AI_RESPONSE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} AI response caches", keys.size());
        }
    }
}
