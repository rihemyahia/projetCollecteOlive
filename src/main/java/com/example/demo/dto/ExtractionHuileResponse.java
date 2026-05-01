package com.example.demo.dto;

import com.example.demo.model.enums.QualiteHuile;
import com.example.demo.model.enums.StatutExtractionHuile;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ExtractionHuileResponse {
    private String id;
    private String tourneeId;
    private String tourneeCode;
    private String collecteId;
    private String collecteCode;
    private String responsablePressoirId;
    private String responsablePressoirNom;
    private String pressoirNom;
    private Double quantiteOlivesRecueKg;
    private Double quantiteHuileExtraiteL;
    private Double rendementPourcentage;
    private Date dateReception;
    private Date dateExtraction;
    private StatutExtractionHuile statut;
    private QualiteHuile qualiteHuile;
    private String observationsReception;
    private String observationsExtraction;
}
