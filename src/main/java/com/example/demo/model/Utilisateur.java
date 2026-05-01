package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "utilisateur")
public class Utilisateur {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
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

    // ========== ATTRIBUTS POUR ROLE: ADMIN ==========

    // ========== ATTRIBUTS POUR ROLE: RESPONSABLE ==========
    private String fonction;
    private Date datePrisePoste;

    // ========== ATTRIBUTS POUR ROLE: AGRICULTEUR ==========
    private String nomExploitation;
    private List<Verger> vergers;

    // ========== ATTRIBUTS POUR ROLE: TRAVAILLEUR (EQUIPE_RECOLTE) ==========
    @Indexed(unique = true, sparse = true)  // ← This allows multiple nulls
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

    @JsonBackReference
    private List<Tournee> tourneesAssignees;
    private Double tarifKm;
    private boolean estSupprime;

    // ========== ATTRIBUTS POUR ROLE: RESPONSABLE_PRESSOIR ==========
    // Embedded Pressoir - directly inside Utilisateur (no @DBRef)
    private Pressoir pressoir;
    private Boolean disponible;
    private Date dateAffectation;
}