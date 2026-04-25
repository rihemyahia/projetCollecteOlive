package com.example.demo.dto;

import lombok.Data;


@Data
public class AdminUpdateAgriculteurRequest {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private String nomExploitation;

}

