package com.example.demo.dto;

import com.example.demo.model.Geolocalisation;
import lombok.Data;

@Data
public class PressoirProfileUpdateRequest {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private Boolean disponible;

    private String pressoirNom;
    private String pressoirAdresse;
    private String pressoirTelephone;
    private String pressoirEmail;
    private String capaciteJournaliere;
    private String horaires;
    private String horaireDebut;
    private String horaireFin;
    private Geolocalisation geolocalisation;
}
