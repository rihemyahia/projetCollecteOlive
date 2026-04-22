package com.example.demo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    private boolean compteActif;

    // ========== PHOTO DE PROFIL (Base64) ==========
    // Stored as a Base64 data URL string, e.g. "data:image/jpeg;base64,..."
    private String photoProfile;

    // ========== ATTRIBUTS POUR ROLE: RESPONSABLE ==========
    private String fonction;
    private Date datePrisePoste;

    // ========== ATTRIBUTS POUR ROLE: AGRICULTEUR ==========
    private String nomExploitation;
    private List<Verger> vergers;

    // ========== ATTRIBUTS POUR ROLE: EQUIPE_RECOLTE ==========
    @Indexed(unique = true)
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

    // ========== MÉTHODES UTILITAIRES ==========

    public int getNombreCollectes() {
        return this.collectesAssignees != null ? this.collectesAssignees.size() : 0;
    }

    public void ajouterCollecte(Collecte collecte) {
        if (this.collectesAssignees == null) {
            this.collectesAssignees = new ArrayList<>();
        }
        this.collectesAssignees.add(collecte);
    }

    public void retirerCollecte(Collecte collecte) {
        if (this.collectesAssignees != null) {
            this.collectesAssignees.remove(collecte);
        }
    }

    private boolean datesSeChevauchent(Date debut1, Date fin1, Date debut2, Date fin2) {
        if (debut1 == null || fin1 == null || debut2 == null || fin2 == null) {
            return false;
        }
        return debut1.before(fin2) && debut2.before(fin1);
    }

    public double calculerSalaireTotal() {
        if (this.role != Role.TRAVAILLEUR || this.salaire == null) {
            return 0.0;
        }
        if (this.collectesAssignees == null) return 0.0;
        return this.salaire * this.collectesAssignees.size();
    }

    public boolean estSaisonnier() {
        return this.role == Role.TRAVAILLEUR && this.statutEmploye == TypeTravailleur.SAISONNIER;
    }

    public boolean estPermanent() {
        return this.role == Role.TRAVAILLEUR && this.statutEmploye == TypeTravailleur.PERMANENT;
    }

    public boolean estTravailleur() { return this.role == Role.TRAVAILLEUR; }
    public boolean estAgriculteur() { return this.role == Role.AGRICULTEUR; }
    public boolean estTransporteur() { return this.role == Role.TRANSPORTEUR; }
    public boolean estResponsable() { return this.role == Role.RESPONSABLE; }
    public boolean estAdmin() { return this.role == Role.ADMIN; }
}