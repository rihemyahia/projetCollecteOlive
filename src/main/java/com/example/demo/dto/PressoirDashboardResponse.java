package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PressoirDashboardResponse {
    private Double totalOlivesRecuesKg;
    private Double totalHuileExtraiteL;
    private Double rendementMoyenPourcentage;
    private long tourneesEnAttenteReception;
    private long tourneesEnAttenteExtraction;
    private long extractionsValidees;
    private CollecteHuileResponse meilleureCollecte;
    private CollecteHuileResponse plusFaibleCollecte;
    private List<CollecteHuileResponse> collectesHuile;
}
