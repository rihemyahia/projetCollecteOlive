package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ressources")
public class Ressource {
    
    @Id
    private String id;
    
    private String nom;
    
    private TypeRessource type;
    
    private String statut;
    
    private String immatriculation;
    
    // ===== CHAMPS POUR BENNE UNIQUEMENT =====
    private Double capaciteKg;
    private Double quantiteChargeeActuelle;
    private Double tauxRemplissage;
    private Boolean estPleine;
    private String tracteurAttacheId;
    
    // ===== CHAMPS POUR TRACTEUR UNIQUEMENT =====
    private String puissance;
    private String carburant;
    private Double consommationHoraire;
    private Boolean aRemorque;
    private Double kilometrage;
    private String conducteurId;
    
    // ===== CHAMPS POUR TRAVAILLEUR UNIQUEMENT =====
    private String prenom;
    private String telephone;
    private Date dateEmbauche;
    private String specialite;
    private Double salaireJournalier;
    private TypeTravailleur typeTravailleur;  // PERMANENT, SAISONNIER, CDD
    
    // ===== RELATIONS =====
    @DBRef
    private List<Tournee> tournees;
    
    // ===== MÉTHODES =====
    
    public String getTourneeActuelleId() {
        if (this.tournees == null) return null;
        
        Date now = new Date();
        
        for (Tournee t : this.tournees) {
            if (t.getDateDebut() != null && t.getDateFin() != null) {
                if (t.getDateDebut().before(now) && t.getDateFin().after(now)) {
                    return t.getId();
                }
            }
        }
        return null;
    }
    
    public boolean estDisponiblePour(Date dateDebut, Date dateFin) {
        if (this.tournees == null) return true;
        
        for (Tournee t : this.tournees) {
            if (t.getDateFin() == null) {
                return false;
            }
            
            if (datesSeChevauchent(dateDebut, dateFin, t.getDateDebut(), t.getDateFin())) {
                return false;
            }
        }
        return true;
    }
    
    public boolean estEnTournee() {
        if (this.tournees == null) return false;
        
        for (Tournee t : this.tournees) {
            if (t.getDateFin() == null) {
                return true;
            }
        }
        return false;
    }
    
    public void ajouterTournee(Tournee tournee) {
        if (this.tournees == null) {
            this.tournees = new ArrayList<>();
        }
        this.tournees.add(tournee);
    }
    
    public int getNombreTournees() {
        return this.tournees != null ? this.tournees.size() : 0;
    }
    
    private boolean datesSeChevauchent(Date debut1, Date fin1, Date debut2, Date fin2) {
        if (debut1 == null || fin1 == null || debut2 == null || fin2 == null) {
            return false;
        }
        return debut1.before(fin2) && debut2.before(fin1);
    }
    
    public void ajouterCharge(Double quantite) {
        if (this.type != TypeRessource.BENNE) {
            throw new UnsupportedOperationException("Seules les bennes peuvent recevoir des charges");
        }
        
        if (this.quantiteChargeeActuelle == null) {
            this.quantiteChargeeActuelle = 0.0;
        }
        
        double nouvelleCharge = this.quantiteChargeeActuelle + quantite;
        
        if (nouvelleCharge > this.capaciteKg) {
            throw new IllegalArgumentException("Dépassement de capacité de la benne");
        }
        
        this.quantiteChargeeActuelle = nouvelleCharge;
        this.estPleine = nouvelleCharge >= this.capaciteKg;
        
        if (this.capaciteKg != null && this.capaciteKg > 0) {
            this.tauxRemplissage = (nouvelleCharge / this.capaciteKg) * 100;
        }
    }
    
    public void vider() {
        if (this.type == TypeRessource.BENNE) {
            this.quantiteChargeeActuelle = 0.0;
            this.tauxRemplissage = 0.0;
            this.estPleine = false;
        }
    }
    
    public double getQuantiteCollectee() {
        if (this.type == TypeRessource.BENNE) {
            return this.quantiteChargeeActuelle != null ? this.quantiteChargeeActuelle : 0.0;
        }
        return 0.0;
    }
    
    public double calculerPaie() {
        if (this.type == TypeRessource.TRAVAILLEUR) {
            return this.salaireJournalier != null ? this.salaireJournalier : 0.0;
        }
        return 0.0;
    }
    
    public boolean estSaisonnier() {
        return this.type == TypeRessource.TRAVAILLEUR && 
               this.typeTravailleur == TypeTravailleur.SAISONNIER;
    }
    
    public boolean estPermanent() {
        return this.type == TypeRessource.TRAVAILLEUR && 
               this.typeTravailleur == TypeTravailleur.PERMANENT;
    }
    
    public boolean estBenne() {
        return this.type == TypeRessource.BENNE;
    }
    
    public boolean estTracteur() {
        return this.type == TypeRessource.TRACTEUR;
    }
    
    public boolean estTravailleur() {
        return this.type == TypeRessource.TRAVAILLEUR;
    }
}