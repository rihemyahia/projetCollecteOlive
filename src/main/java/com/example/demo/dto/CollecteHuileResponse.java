package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollecteHuileResponse {
    private String collecteId;
    private String collecteCode;
    private String vergerId;
    private String vergerTypeOlive;
    private Double totalOlivesRecuesKg;
    private Double totalHuileExtraiteL;
    private Double rendementMoyenPourcentage;
    private long nombreTourneesRecues;
    private long nombreTourneesExtraites;
}
