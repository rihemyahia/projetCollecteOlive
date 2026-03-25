package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alertes")
public class Alerte {

    @Id
    private String id;

    private String vergerId;

    private String agriculteurId;

    private String type; // maturite, probleme

    private String statut; // en_attente, traitee, ignoree

    private Integer niveauMaturite;

    private Date dateRecolteEstimee;

    private String typeProbleme; // pluie, parasites, maladie, autre

    private String description;

    private String gravite; // basse, moyenne, haute

    private Localisation localisation;

    private Date dateCreation;

    private Date dateTraitement;

    private String traitePar;

    private String collecteGenereeId;
}