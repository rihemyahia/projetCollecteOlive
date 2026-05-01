package com.example.demo.dto;

import com.example.demo.model.Pressoir;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PressoirProfileResponse {
    private String id;
    private String email;
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private Boolean disponible;
    private Pressoir pressoir;
}
