package com.fintech.mapping.service;

import com.fintech.mapping.model.MappingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class TransformationService {
    
    public Object transform(Object value, MappingConfig.FieldMapping mapping) {
        if (value == null) return null;
        
        try {
            String transformationType = mapping.getTransformationType();
            if (transformationType == null) {
                return value;
            }
            
            return switch (transformationType) {
                case "uppercase" -> value.toString().toUpperCase();
                case "lowercase" -> value.toString().toLowerCase();
                case "dateFormat" -> formatDate(value, mapping);
                case "multiply" -> multiplyValue(value, mapping);
                case "concat" -> concatenate(value, mapping);
                default -> value;
            };
        } catch (Exception e) {
            log.error("Transformation failed for type {}: {}", mapping.getTransformationType(), e.getMessage());
            return value; // Return original value on error
        }
    }
    
    private String formatDate(Object value, MappingConfig.FieldMapping mapping) {
        try {
            String inputFormat = (String) mapping.getTransformationConfig().get("inputFormat");
            String outputFormat = (String) mapping.getTransformationConfig().get("outputFormat");
            
            if (inputFormat == null || outputFormat == null) {
                log.warn("Date format transformation missing inputFormat or outputFormat config");
                return value.toString();
            }
            
            String dateString = value.toString();
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
            
            // Try parsing as LocalDateTime first
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);
                return dateTime.format(outputFormatter);
            } catch (DateTimeParseException e) {
                // If that fails, try LocalDate
                LocalDate date = LocalDate.parse(dateString, inputFormatter);
                return date.format(outputFormatter);
            }
        } catch (Exception e) {
            log.error("Date format transformation failed: {}", e.getMessage());
            return value.toString();
        }
    }
    
    private Object multiplyValue(Object value, MappingConfig.FieldMapping mapping) {
        try {
            double multiplier = ((Number) mapping.getTransformationConfig().get("factor")).doubleValue();
            return ((Number) value).doubleValue() * multiplier;
        } catch (Exception e) {
            log.error("Multiply transformation failed: {}", e.getMessage());
            return value;
        }
    }
    
    private String concatenate(Object value, MappingConfig.FieldMapping mapping) {
        try {
            String prefix = (String) mapping.getTransformationConfig().getOrDefault("prefix", "");
            String suffix = (String) mapping.getTransformationConfig().getOrDefault("suffix", "");
            return prefix + value.toString() + suffix;
        } catch (Exception e) {
            log.error("Concatenate transformation failed: {}", e.getMessage());
            return value.toString();
        }
    }
}
