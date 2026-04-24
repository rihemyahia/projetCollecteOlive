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
    private String photoProfile;

    // ========== ATTRIBUTS POUR ROLE: TRANSPORTEUR ==========
    private String permis;
    private List<Ressource> ressources;
    private Boolean disponibleTransport;
    private Date dateObtentionPermis;
    private Integer anneesExperience;
    private List<Tournee> tourneesAssignees;
    private Double tarifKm;
   private boolean estSupprime;

}