package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import com.example.demo.model.enums.TypeRessource;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ressources")
public class Ressource {

    @Id
    private String id;

    private String nom;

    private TypeRessource type;     // BENNE | TRACTEUR

    private String statut;          // DISPONIBLE | OCCUPE | MAINTENANCE

    private String immatriculation;

    // ── BENNE fields ──────────────────────────────────────────────
    private Double capaciteKg;
    private Double quantiteChargeeActuelle;
    private Double tauxRemplissage;
    private Boolean estPleine;

    @DocumentReference(lazy = true)
    private Ressource tracteur;     // benne → tracteur it is attached to (null if none)

    // ── TRACTEUR fields ───────────────────────────────────────────
    private String puissance;
    private String carburant;
    private Double consommationHoraire;
    private Boolean aRemorque;
    private Double kilometrage;

    @DocumentReference(lazy = true)
    private Utilisateur conducteur; // tracteur → the driver assigned (null if none)

    // ── Shared ────────────────────────────────────────────────────
    @DocumentReference(lazy = true)
    private List<Tournee> tournees = new ArrayList<>();  // tournées this resource participated in

    // ── Helper methods (kept — they are simple type checks / charge logic, not business logic) ──

    public void ajouterCharge(Double quantite) {
        if (this.type != TypeRessource.BENNE)
            throw new UnsupportedOperationException("Seules les bennes peuvent recevoir des charges");
        if (this.quantiteChargeeActuelle == null) this.quantiteChargeeActuelle = 0.0;
        double nouvelleCharge = this.quantiteChargeeActuelle + quantite;
        if (nouvelleCharge > this.capaciteKg)
            throw new IllegalArgumentException("Dépassement de capacité de la benne");
        this.quantiteChargeeActuelle = nouvelleCharge;
        this.estPleine = nouvelleCharge >= this.capaciteKg;
        if (this.capaciteKg != null && this.capaciteKg > 0)
            this.tauxRemplissage = (nouvelleCharge / this.capaciteKg) * 100;
    }

    public void vider() {
        if (this.type == TypeRessource.BENNE) {
            this.quantiteChargeeActuelle = 0.0;
            this.tauxRemplissage = 0.0;
            this.estPleine = false;
        }
    }
}