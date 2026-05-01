package com.example.demo.dto;

import com.example.demo.model.enums.QualiteHuile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ExtractionHuileRequest {
    @NotNull(message = "La quantite d'huile extraite est obligatoire")
    @Positive(message = "La quantite d'huile extraite doit etre positive")
    private Double quantiteHuileExtraiteL;

    private QualiteHuile qualiteHuile;
    private String observations;
}
