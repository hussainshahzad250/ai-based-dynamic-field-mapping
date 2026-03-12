package com.fintech.mapping.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MappingConfig {
    private String partnerId;
    private String version;
    private List<FieldMapping> mappings;
    
    @Data
    public static class FieldMapping {
        private String targetPath;
        private String sourcePath;
        private String defaultValue;
        private String transformationType;
        private Map<String, Object> transformationConfig;
    }
}
