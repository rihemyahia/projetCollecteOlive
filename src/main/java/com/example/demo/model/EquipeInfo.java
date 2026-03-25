package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipeInfo {
    private String equipeId;
    private String nomEquipe;
    private String chefId;
    private String chefNom;
    private List<String> membresIds;
    private String niveauExpertise;
}