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
@Document(collection = "tournees")
public class Tournee {
    
    @Id
    private String id;
    
    private String code;
    
    private StatutTournee statut;
    
    private Date dateDebut;
    
    private Date dateFin;
    
    private Double distanceTotale;
    
    private String observations;
    
    private Integer tempsTotal;
    
    @DBRef
    private List<Ressource> ressources;
    
    public void demarrer() {
        this.statut = StatutTournee.EN_COURS;
        this.dateDebut = new Date();
    }
    
    public void terminer() {
        this.statut = StatutTournee.TERMINEE;
        this.dateFin = new Date();
        
        if (this.dateDebut != null) {
            long diff = this.dateFin.getTime() - this.dateDebut.getTime();
            this.tempsTotal = (int) (diff / (1000 * 60));
        }
    }
    
    public void annuler() {
        this.statut = StatutTournee.ANNULEE;
        this.dateFin = new Date();
    }
    
    public void ajouterRessource(Ressource ressource) {
        if (this.ressources == null) {
            this.ressources = new ArrayList<>();
        }
        
        if (ressource.estDisponiblePour(this.dateDebut, this.dateFin)) {
            this.ressources.add(ressource);
            ressource.ajouterTournee(this);
        } else {
            throw new IllegalStateException("Ressource non disponible: " + ressource.getNom());
        }
    }
    
    public void retirerRessource(Ressource ressource) {
        if (this.ressources != null) {
            this.ressources.remove(ressource);
        }
    }
    
    public double getQuantiteTotaleCollectee() {
        double total = 0.0;
        if (this.ressources != null) {
            for (Ressource r : this.ressources) {
                if (r.estBenne()) {
                    total += r.getQuantiteCollectee();
                }
            }
        }
        return total;
    }
    
    public double getTauxRemplissageMoyen() {
        if (this.ressources == null || this.ressources.isEmpty()) {
            return 0.0;
        }
        
        double total = 0.0;
        int nbBennes = 0;
        
        for (Ressource r : this.ressources) {
            if (r.estBenne() && r.getTauxRemplissage() != null) {
                total += r.getTauxRemplissage();
                nbBennes++;
            }
        }
        
        return nbBennes > 0 ? total / nbBennes : 0.0;
    }
    
    public double calculerEfficacite() {
        if (tempsTotal == null || tempsTotal == 0 || distanceTotale == null || distanceTotale == 0) {
            return 0.0;
        }
        
        double quantite = getQuantiteTotaleCollectee();
        double heures = tempsTotal / 60.0;
        
        if (distanceTotale * heures == 0) {
            return 0.0;
        }
        
        double efficacite = (quantite / (distanceTotale * heures)) * 10;
        return Math.min(efficacite, 100.0);
    }
    
    public List<Ressource> getBennes() {
        if (this.ressources == null) return new ArrayList<>();
        
        List<Ressource> bennes = new ArrayList<>();
        for (Ressource r : this.ressources) {
            if (r.estBenne()) {
                bennes.add(r);
            }
        }
        return bennes;
    }
    
    public List<Ressource> getTracteurs() {
        if (this.ressources == null) return new ArrayList<>();
        
        List<Ressource> tracteurs = new ArrayList<>();
        for (Ressource r : this.ressources) {
            if (r.estTracteur()) {
                tracteurs.add(r);
            }
        }
        return tracteurs;
    }
    
    public List<Ressource> getTravailleurs() {
        if (this.ressources == null) return new ArrayList<>();
        
        List<Ressource> travailleurs = new ArrayList<>();
        for (Ressource r : this.ressources) {
            if (r.estTravailleur()) {
                travailleurs.add(r);
            }
        }
        return travailleurs;
    }
}