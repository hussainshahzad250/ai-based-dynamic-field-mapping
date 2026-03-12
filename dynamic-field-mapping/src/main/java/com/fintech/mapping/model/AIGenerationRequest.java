package com.fintech.mapping.model;

import lombok.Data;

@Data
public class AIGenerationRequest {
    private String partnerId;
    private String partnerSample;
    private String internalSample;
    private String description;
}
