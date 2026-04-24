package com.example.demo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AdminUpdateResponsableRequest {
    private String prenom;
    private String nom;
    private String telephone;
    private String adresse;
    private String fonction;
    private Date datePrisePoste;

    /**
     * List of verger IDs that should be assigned to this responsable.
     * If replaceManagedVergers=true, any verger currently managed by this responsable but not in this list
     * will be unassigned (responsable=null).
     */
    private List<String> managedVergerIds;

    private Boolean replaceManagedVergers;
}

