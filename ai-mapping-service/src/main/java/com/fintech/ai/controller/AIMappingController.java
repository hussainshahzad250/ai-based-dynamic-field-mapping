package com.fintech.ai.controller;

import com.fintech.ai.model.MappingConfig;
import com.fintech.ai.service.AIMappingGenerator;
import com.fintech.ai.service.MappingCacheService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/ai-mapping")
@RequiredArgsConstructor
public class AIMappingController {

    private final AIMappingGenerator mappingGenerator;
    private final MappingCacheService cacheService;

    @PostMapping("/generate")
    public ResponseEntity<MappingConfig> generateMapping(@Valid @RequestBody GenerateMappingRequest request) {
        MappingConfig config = mappingGenerator.generateMapping(
                request.getPartnerId(),
                request.getPartnerSample(),
                request.getInternalSample(),
                request.getCreatedBy()
        );
        return ResponseEntity.ok(config);
    }

    @PostMapping("/improve/{partnerId}")
    public ResponseEntity<MappingConfig> improveMapping(
            @PathVariable String partnerId,
            @Valid @RequestBody ImproveMappingRequest request) {

        MappingConfig improved = mappingGenerator.improveMapping(partnerId, request.getPartnerSample());
        return ResponseEntity.ok(improved);
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<MappingConfig> getMapping(@PathVariable String partnerId) {
        MappingConfig config = cacheService.getMapping(partnerId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @Data
    public static class GenerateMappingRequest {
        @NotBlank
        private String partnerId;
        @NotBlank
        private String partnerSample;
        @NotBlank
        private String internalSample;
        private String createdBy = "system";
    }

    @Data
    public static class ImproveMappingRequest {
        @NotBlank
        private String partnerSample;
    }
}
