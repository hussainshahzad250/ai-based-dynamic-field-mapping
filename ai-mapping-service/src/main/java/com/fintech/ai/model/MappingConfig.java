package com.fintech.ai.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class MappingConfig implements Serializable {
    private String partnerId;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private ConfigStatus status;
    private List<FieldMapping> mappings;

    @Data
    public static class FieldMapping implements Serializable {
        private String targetPath;
        private String sourcePath;
        private String defaultValue;
        private String transformationType;
        private Map<String, Object> transformationConfig;
    }

    public enum ConfigStatus {
        DRAFT, ACTIVE, INACTIVE, ARCHIVED
    }
}
