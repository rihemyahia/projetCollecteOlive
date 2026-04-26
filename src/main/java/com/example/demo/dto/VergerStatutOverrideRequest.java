package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import lombok.Data;

@Data
public class VergerStatutOverrideRequest {
    private StatutVerger statut;
    private String reason;
    private Boolean clearOverride;
}
