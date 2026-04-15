package com.example.demo.model;

import com.example.demo.model.enums.NiveauUrgence;
import com.example.demo.model.enums.PhaseCulturale;
import com.example.demo.model.enums.StatutAlerte;
import com.example.demo.model.enums.TypeAlerte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
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
@Document(collection = "alertes_terrain")
public class AlerteTerrain {

    @Id
    private String id;

    @DocumentReference(lazy = true)
    private Utilisateur agriculteur;

    @DocumentReference(lazy = true)
    private Verger verger;

    private TypeAlerte type;
    private String description;

    // For geospatial queries — Spring Data MongoDB GeoJSON Point [longitude, latitude]
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    // Human-readable embedded object for display — stored inline in the document
    private Geolocalisation geolocalisation;

    // Phase at the moment of signaling — computed from verger.maturiteActuelle in service
    private PhaseCulturale phase;

    // Urgency computed from type + phase — stored so it can be filtered/sorted
    private NiveauUrgence niveauUrgence;

    @Builder.Default
    private StatutAlerte statut = StatutAlerte.EN_ATTENTE;

    private String commentaireTraitement;

    @Builder.Default
    private Boolean estSupprimer = false;

    @CreatedDate
    private Date dateSignalement;

    private Date dateMiseAJour;
}