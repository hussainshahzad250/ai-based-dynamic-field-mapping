package com.fintech.mapping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.mapping.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingConfigLoader {
    
    private final ObjectMapper objectMapper;
    private final Map<String, MappingConfig> configCache = new ConcurrentHashMap<>();
    
    @Value("${mapping.config-path}")
    private String configPath;
    
    @PostConstruct
    public void loadConfigs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(configPath + "*.json");
            
            for (Resource resource : resources) {
                MappingConfig config = objectMapper.readValue(resource.getInputStream(), MappingConfig.class);
                configCache.put(config.getPartnerId(), config);
                log.info("Loaded mapping config for partner: {}", config.getPartnerId());
            }
        } catch (Exception e) {
            log.error("Failed to load mapping configs", e);
        }
    }
    
    public MappingConfig getConfig(String partnerId) {
        return configCache.get(partnerId);
    }
    
    public void reloadConfig(String partnerId) {
        loadConfigs();
    }
}
