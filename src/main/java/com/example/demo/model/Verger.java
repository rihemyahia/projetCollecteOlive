package com.example.demo.model;

import com.example.demo.model.enums.StatutVerger;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vergers")
public class Verger {

    @Id
    private String id;

    @DocumentReference(lazy = true)
    private Utilisateur agriculteur;

    @DocumentReference(lazy = true)
    private Utilisateur responsable;

    private Double superficie;             // hectares
    private String typeOlive;             // Chemlali, Chétoui, Picholine…
    private Double rendementEstime;        // kg
    private Integer maturiteActuelle;     // 0-100 %
    private int nbArbre;

    private StatutVerger statut;
    private StatutVerger statutOverride;
    private String statutOverrideReason;
    private String statutOverrideByUserId;
    private Date statutOverrideAt;

    private Date dateDerniereRecolte;

    // ── Geolocation fields (same pattern as AlerteTerrain) ─────────────────
    /**
     * GeoJSON Point [longitude, latitude] — used for $nearSphere queries.
     * Requires 2dsphere index (created automatically via @GeoSpatialIndexed).
     */
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    /**
     * Human-readable embedded object — stored inline for display in the frontend.
     * Same pattern used in AlerteTerrain.
     */
    private Geolocalisation geolocalisation;

    @Builder.Default
    private Boolean estSupprimer = false;

    @CreatedDate
    private Date dateCreation;
}