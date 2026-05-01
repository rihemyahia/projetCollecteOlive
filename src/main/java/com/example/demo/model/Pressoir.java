package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pressoir {
    private String id;
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String capaciteJournaliere;
    // In Pressoir.java
    private String horaires;  // Already exists? If not, add it

    private String horaireDebut;
    private String horaireFin;    private Geolocalisation geolocalisation;
    private Boolean actif;
    private Date dateCreation;

}