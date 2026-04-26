package com.example.demo.dto;

import com.example.demo.model.Geolocalisation;
import com.example.demo.model.enums.StatutVerger;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class VergerResponse {
    private String id;
    private String agriculteurId;
    private String agriculteurNom;
    private String agriculteurEmail;
    private String responsableId;
    private String responsableNom;
    private String responsableEmail;
    private String responsableFonction;
    private Double superficie;
    private String typeOlive;
    private Integer nbArbre;
    private Double rendementEstime;
    private Integer maturiteActuelle;
    private StatutVerger statut;
    private String statutSource;
    private StatutVerger statutOverride;
    private String statutOverrideReason;
    private String statutOverrideByUserId;
    private Date statutOverrideAt;
    private Date dateDerniereRecolte;
    private Boolean estSupprimer;
    private Date dateCreation;

    // ── Geolocation ─────────────────────────────────────────────────────────
    /**
     * Embedded human-readable geolocation (latitude, longitude, adresseIndicative).
     * Null when no GPS coordinates have been set for this verger.
     */
    private Geolocalisation geolocalisation;
}