package com.fintech.ai.controller;

import com.fintech.ai.model.MappingConfig;
import com.fintech.ai.service.MappingCacheService;
import com.fintech.ai.utils.ValidationUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final MappingCacheService cacheService;

    @Value("${admin.api-key}")
    private String adminApiKey;

    @PostMapping("/mapping/{partnerId}")
    public ResponseEntity<MappingConfig> saveMapping(
            @PathVariable String partnerId,
            @Valid @RequestBody MappingConfig config,
            @RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        config.setPartnerId(partnerId);
        config.setUpdatedAt(LocalDateTime.now());
        cacheService.cacheMapping(partnerId, config);
        return ResponseEntity.ok(config);
    }

    @PutMapping("/mapping/{partnerId}/status")
    public ResponseEntity<MappingConfig> updateStatus(
            @PathVariable String partnerId,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MappingConfig config = cacheService.getMapping(partnerId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        config.setStatus(request.getStatus());
//        config.setUpdatedAt(LocalDateTime.now());
        cacheService.cacheMapping(partnerId, config);
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/mapping/{partnerId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable String partnerId,
            @RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cacheService.invalidateMapping(partnerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mappings")
    public ResponseEntity<Set<String>> listAllPartners(@RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Set<String> partners = cacheService.getAllPartnerIds();
        return ResponseEntity.ok(partners);
    }

    @PostMapping("/cache/clear/mappings")
    public ResponseEntity<Map<String, String>> clearMappingCache(@RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cacheService.clearAllMappings();
        return ResponseEntity.ok(Map.of("message", "All mapping caches cleared"));
    }

    @PostMapping("/cache/clear/ai-responses")
    public ResponseEntity<Map<String, String>> clearAICache(@RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cacheService.clearAllAIResponses();
        return ResponseEntity.ok(Map.of("message", "All AI response caches cleared"));
    }

    @PostMapping("/cache/invalidate/{partnerId}")
    public ResponseEntity<Map<String, String>> invalidatePartnerCache(
            @PathVariable String partnerId,
            @RequestHeader("X-Admin-Key") String apiKey) {
        if (!ValidationUtils.validateAdminKey(apiKey,adminApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cacheService.invalidateMapping(partnerId);
        return ResponseEntity.ok(Map.of("message", "Cache invalidated for partner: " + partnerId));
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank
        private MappingConfig.ConfigStatus status;
    }
}
