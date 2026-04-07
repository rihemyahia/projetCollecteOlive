package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VergerRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String proprietaireId;

    @Positive
    private Double superficie;

    @NotBlank
    private String typeOlive;

    @Min(0)
    private Integer nombreArbres;

    @PositiveOrZero
    private Double rendementEstime;

    @Min(0) @Max(100)
    private Integer maturiteActuelle;

    private StatutVerger statut;
}