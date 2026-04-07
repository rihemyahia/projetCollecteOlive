package com.example.demo.dto;

import com.example.demo.model.enums.StatutVerger;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class VergerResponse {
    private String id;
    private String nom;
    private String proprietaireId;
    private String proprietaireNom;        // denormalized for display
    private Double superficie;
    private String typeOlive;
    private Integer nombreArbres;
    private Double rendementEstime;
    private Integer maturiteActuelle;
    private StatutVerger statut;
    private Date dateDerniereRecolte;
    private Boolean estActif;
    private Date dateCreation;
    private String motifRejet;
    private Boolean supprimer;
}