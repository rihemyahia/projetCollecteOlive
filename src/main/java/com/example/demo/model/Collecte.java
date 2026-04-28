package com.example.demo.model;

import com.example.demo.model.enums.StatutCollecte;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "collectes")
@CompoundIndex(name = "idx_verger_annee", def = "{'vergerId': 1, 'annee': 1}")
public class Collecte {

    @Id
    private String id;
 // Ajoute ces 2 champs dans ta classe Collecte, après les autres champs

 // ── Weather data (auto-filled from API) ─────────────────────────────────────
 private Double precipitations;   // Précipitations annuelles en mm
 private Double temperature;       // Température moyenne en °C
    private String code;
    private StatutCollecte statut;
    // ── Campaign identification ───────────────────────────────────────────────
    private String annee;           // e.g., "2024-2025"
    private Integer numero;          // 1, 2, 3 for multiple collectes per year
    private String vergerId;     //naarf 9olna nestaamlo les annotations ama lena mahchitch bih l objet andi fl tournee juste bech nsahal ala rouhi el requette w matekhouch akthar wakt
    // ── Campaign dates ───────────────────────────────────────────────────────
    private Date dateDebutCampagne;
    private Date dateFinCampagne;

    // ── Campaign statistics (simple fields) ──────────────────────────────────
    private Integer nbreTournees = 0;
    private Double quantiteTotaleKg = 0.0;
    private Integer totalArbresRecoltes = 0;
    private Double rendementMoyenParArbre;
    private Double efficaciteMoyenne;

    // ── Metadata ─────────────────────────────────────────────────────────────
    private String observations;
    private Boolean estCloturee;

    @CreatedDate
    private Date dateCreation;
}