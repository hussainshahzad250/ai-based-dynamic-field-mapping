package com.fintech.ai.controller;

import com.fintech.ai.model.MappingConfig;
import com.fintech.ai.service.SmartMappingGenerator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/smart-mapping")
@RequiredArgsConstructor
public class SmartMappingController {

    private final SmartMappingGenerator smartMappingGenerator;

    @PostMapping("/generate")
    public ResponseEntity<MappingConfig> generateSmartMapping(
            @Valid @RequestBody GenerateSmartMappingRequest request) {

        MappingConfig config = smartMappingGenerator.generateSmartMapping(
                request.getPartnerId(),
                request.getPartnerSample(),
                request.getInternalSample(),
                request.getCreatedBy()
        );

        return ResponseEntity.ok(config);
    }

    @Data
    public static class GenerateSmartMappingRequest {
        @NotBlank
        private String partnerId;
        @NotBlank
        private String partnerSample;
        @NotBlank
        private String internalSample;
        private String createdBy = "system";
    }
}
