package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipeAssignée {
    private String equipeId;
    private String nomEquipe;
    private String chefId;
    private String chefNom;
}