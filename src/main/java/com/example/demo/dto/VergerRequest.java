package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VergerRequest {

    @NotBlank
    private String agriculteurId;

    private String responsableId;

    @Positive
    private Double superficie;

    @NotBlank
    private String typeOlive;

    @Positive
    private Integer nbArbre;

    @PositiveOrZero
    private Double rendementEstime;

    @Min(0) @Max(100)
    private Integer maturiteActuelle;

    private StatutVerger statut;
    private String statutOverrideReason;

    // ── Geolocation fields (optional — a verger can exist without GPS coords) ──
    /**
     * Latitude of the verger (e.g. 34.7400 for Sfax region).
     * Paired with longitude to build the GeoJsonPoint.
     */
    private Double latitude;

    /**
     * Longitude of the verger (e.g. 10.7600 for Sfax region).
     */
    private Double longitude;

    /**
     * Human-readable address / locality (e.g. "Route de Gabès km 12, Sfax").
     * Stored as-is in Geolocalisation.adresseIndicative.
     */
    private String adresseIndicative;
}