package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateAgriculteurRequest {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private String nomExploitation;

    /**
     * List of verger IDs that should be owned by this agriculteur.
     * If replaceOwnedVergers=true, any verger currently owned by this agriculteur but not in this list
     * will be unassigned (agriculteur=null).
     */
    private List<String> ownedVergerIds;

    private Boolean replaceOwnedVergers;
}

