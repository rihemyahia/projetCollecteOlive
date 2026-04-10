package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class VergerResponse {
    private String id;
    private String agriculteurId;
    private String agriculteurNom;
    private String agriculteurEmail;
    private Double superficie;
    private String typeOlive;
    private Integer nbArbre;
    private Double rendementEstime;
    private Integer maturiteActuelle;
    private StatutVerger statut;
    private Date dateDerniereRecolte;
    private Boolean estSupprimer;
    private Date dateCreation;
}