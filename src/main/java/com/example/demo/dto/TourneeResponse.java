package com.example.demo.dto;

import com.example.demo.model.StatutTournee;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class TourneeResponse {

    private String id;
    private String code;
    private StatutTournee statut;

    // Verger info
    private String vergerId;
    /** Responsable terrain rattaché au verger — pour filtrage UI (assignation transporteur). */
    private String vergerResponsableId;
    private String vergerTypeOlive;
    private String vergerAgriculteurNom;
    private Double vergerSuperficie;

    // Resources
    private String benneId;
    private String benneNom;
    private Double benneCapaciteKg;
    private String collecteId;
    private String collecteCode;
    private String tracteurId;
    private String tracteurNom;
    private String tracteurImmatriculation;

    private List<String> travailleurIds;
    private List<String> travailleurNoms;   // convenience: "Prénom Nom"

    // Field data
    private Integer nbreArbre;
    private Double distanceTotale;
    private Integer tempsTotal;             // minutes

    // Harvest result
    private Double quantiteCollecteeKg;
    private Boolean collecteFinalisee;
    private Double efficacite;             // 0-100, computed on-the-fly

    // Metadata
    private String observations;
    private String livraisonDestinationNom;
    private String livraisonDestinationAdresse;
    private Date livraisonEstimeDebut;
    private Date livraisonEstimeFin;
    private String livraisonNotes;
    private String responsablePressoirId;
    private String responsablePressoirNom;
    private String pressoirNom;
    private String pressoirAdresse;
    private Date livraisonStartedAt;
    private Date livraisonCompletedAt;
    private String livraisonEvidenceName;
    private String livraisonEvidenceUrl;
    private Date dateDebut;
    private Date dateFin;
    private Date dateCreation;

    // Verger aggregate
    private Double totalCollecteVergerKg;  // sum of all terminated tournées of same verger
}
