package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "collectes")
public class Collecte {

    @Id
    private String id;

    private String vergerId;

    private String type; // planifiee, urgente

    private String statut; // planifiee, en_cours, terminee, annulee

    private Date datePlanifiee;

    private Date dateDebut;

    private Date dateFin;

    private EquipeAssignée equipeAssigned;

    private List<OuvrierAssignée> ouvriers;

    private RessourceAssignée tracteur;

    private RessourceAssignée benne;

    private Double quantiteEstimee;

    private Double quantiteReelle;

    private Integer nombreBennesRemplies;

    private String alerteDeclencheurId;

    private String notes;

    private Date dateCreation;

    private String creePar;
}