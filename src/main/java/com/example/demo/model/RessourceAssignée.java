package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RessourceAssignée {
    private String ressourceId;
    private String code;
    private String nom;
    private String type; // tracteur, benne, vibrateur, filet
    private Double capacite; // en kg pour les bennes
}