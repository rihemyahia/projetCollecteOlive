package com.example.demo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AdminUpdateResponsableRequest {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private String fonction;
    private Date datePrisePoste;

}

