package com.example.demo.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String motDePasse;

    private String prenom;

    private String nom;

    private String telephone;

    private Role role;

    private String adresse;

    private Boolean estActif;

    private Date dateCreation;
    private boolean compteActif;  // Note: changed from 'ACompte' to 'aCompte' pour convention Java
    
    // ========== ATTRIBUTS POUR ROLE: ADMIN ==========
    
    // ========== ATTRIBUTS POUR ROLE: RESPONSABLE ==========
    private String fonction;
    private Date datePrisePoste;
    
    // ========== ATTRIBUTS POUR ROLE: AGRICULTEUR ==========
    private String nomExploitation;
    @Indexed(unique = true)    // ========== ATTRIBUTS POUR ROLE: EQUIPE_RECOLTE ==========
    private String cin;
    private List<String> specialites;
    private List<Collecte> collectesAssignees;
    private Date dateEmbauche;
    private Double salaire;
    private TypeTravailleur statutEmploye;
    
    // ========== ATTRIBUTS POUR ROLE: TRANSPORTEUR ==========
    private String permis;
    private List<Ressource> ressources;
    private Boolean disponibleTransport;
    private Date dateObtentionPermis;
    private Integer anneesExperience;
    private List<Tournee> tourneesAssignees;
    private Double tarifKm;
    
    // ========== MÉTHODES POUR DISPONIBILITÉ (comme dans Ressource) ==========
    
    /**
     * Vérifie si le travailleur est disponible pour une période donnée
     */
    public boolean estDisponiblePour(Date dateDebut, Date dateFin) {
        // Un travailleur sans collectes assignées est toujours disponible
        if (this.collectesAssignees == null || this.collectesAssignees.isEmpty()) {
            return true;
        }
        
        for (Collecte collecte : this.collectesAssignees) {
            if (collecte.getDateDebut() == null || collecte.getDateFin() == null) {
                // Si une collecte n'a pas de date de fin, elle est considérée comme en cours
                return false;
            }
            
            if (datesSeChevauchent(dateDebut, dateFin, collecte.getDateDebut(), collecte.getDateFin())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Vérifie si le travailleur est actuellement en collecte
     */
    public boolean estEnCollecte() {
        if (this.collectesAssignees == null) return false;
        
        Date now = new Date();
        
        for (Collecte collecte : this.collectesAssignees) {
            if (collecte.getDateDebut() != null && collecte.getDateFin() == null) {
                return true;
            }
            if (collecte.getDateDebut() != null && collecte.getDateFin() != null) {
                if (collecte.getDateDebut().before(now) && collecte.getDateFin().after(now)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Vérifie si le travailleur est disponible (pas en collecte)
     */
    public boolean isDisponible() {
        return !estEnCollecte();
    }
    
    /**
     * Compte le nombre de collectes assignées
     */
    public int getNombreCollectes() {
        return this.collectesAssignees != null ? this.collectesAssignees.size() : 0;
    }
    
    /**
     * Ajoute une collecte au travailleur
     */
    public void ajouterCollecte(Collecte collecte) {
        if (this.collectesAssignees == null) {
            this.collectesAssignees = new ArrayList<>();
        }
        this.collectesAssignees.add(collecte);
    }
    
    /**
     * Retire une collecte du travailleur
     */
    public void retirerCollecte(Collecte collecte) {
        if (this.collectesAssignees != null) {
            this.collectesAssignees.remove(collecte);
        }
    }
    
    /**
     * Vérifie si deux périodes se chevauchent
     */
    private boolean datesSeChevauchent(Date debut1, Date fin1, Date debut2, Date fin2) {
        if (debut1 == null || fin1 == null || debut2 == null || fin2 == null) {
            return false;
        }
        return debut1.before(fin2) && debut2.before(fin1);
    }
    
    // ========== MÉTHODES UTILITAIRES ==========
    
    /**
     * Calcule le salaire total basé sur le nombre de collectes
     */
    public double calculerSalaireTotal() {
        if (this.role != Role.EQUIPE_RECOLTE || this.salaire == null) {
            return 0.0;
        }
        
        if (this.collectesAssignees == null) return 0.0;
        
        // Exemple: salaire journalier * nombre de jours de collecte
        // À adapter selon votre logique métier
        return this.salaire * this.collectesAssignees.size();
    }
    
    /**
     * Vérifie si c'est un travailleur saisonnier
     */
    public boolean estSaisonnier() {
        return this.role == Role.EQUIPE_RECOLTE && 
               this.statutEmploye == TypeTravailleur.SAISONNIER;
    }
    
    /**
     * Vérifie si c'est un travailleur permanent
     */
    public boolean estPermanent() {
        return this.role == Role.EQUIPE_RECOLTE && 
               this.statutEmploye == TypeTravailleur.PERMANENT;
    }
    
    /**
     * Vérifie si c'est un travailleur
     */
    public boolean estTravailleur() {
        return this.role == Role.EQUIPE_RECOLTE;
    }
    
    /**
     * Vérifie si c'est un agriculteur
     */
    public boolean estAgriculteur() {
        return this.role == Role.AGRICULTEUR;
    }
    
    /**
     * Vérifie si c'est un transporteur
     */
    public boolean estTransporteur() {
        return this.role == Role.TRANSPORTEUR;
    }
    
    /**
     * Vérifie si c'est un responsable
     */
    public boolean estResponsable() {
        return this.role == Role.RESPONSABLE;
    }
    
    /**
     * Vérifie si c'est un admin
     */
    public boolean estAdmin() {
        return this.role == Role.ADMIN;
    }
}