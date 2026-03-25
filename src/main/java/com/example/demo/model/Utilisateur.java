package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "utilisateurs")
public class Utilisateur {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String motDePasse;

    private String prenom;

    private String nom;

    private String telephone;

    private String role; // admin, responsable, agriculteur, equipe_recolte, transporteur, pressoir

    private String adresse;

    private List<String> vergersIds;

    private EquipeInfo infoEquipe;

    private PressoirInfo infoPressoir;

    private Boolean estActif;

    private Date dateCreation;
}