package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OuvrierAssignée {
    private String ouvrierId;
    private String nom;
    private String prenom;
    private String role; // chef_equipe, ouvrier, chauffeur
}