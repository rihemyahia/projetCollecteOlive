package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VergerRequest {

    @NotBlank
    private String agriculteurId; // renamed from proprietaireId

    @Positive
    private Double superficie;

    @NotBlank
    private String typeOlive;

    @Positive
    private Integer nbArbre; // renamed from nombreArbres

    @PositiveOrZero
    private Double rendementEstime;

    @Min(0) @Max(100)
    private Integer maturiteActuelle;

    private StatutVerger statut;
}