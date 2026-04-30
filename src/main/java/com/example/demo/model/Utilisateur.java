package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    private List<Verger> vergers;
    @Indexed(unique = true)    // ========== ATTRIBUTS POUR ROLE: EQUIPE_RECOLTE ==========
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
    // Utilisateur.java - The "back" part (gets serialized with reference ID only)
    @JsonBackReference
    private List<Tournee> tourneesAssignees;
    private Double tarifKm;
   private boolean estSupprime;

}