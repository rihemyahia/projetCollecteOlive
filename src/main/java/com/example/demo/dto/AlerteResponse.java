package com.example.demo.dto;

import com.example.demo.model.Geolocalisation;
import com.example.demo.model.enums.NiveauUrgence;
import com.example.demo.model.enums.PhaseCulturale;
import com.example.demo.model.enums.StatutAlerte;
import com.example.demo.model.enums.TypeAlerte;
import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class AlerteResponse {
    private String id;
    private String agriculteurId;
    private String agriculteurNom;
    private String agriculteurEmail;
    private String vergerId;
    private String vergerTypeOlive;
    private TypeAlerte type;
    private String description;
    private List<String> photoUrls;
    private Geolocalisation geolocalisation;
    private PhaseCulturale phase;
    private NiveauUrgence niveauUrgence;
    private StatutAlerte statut;
    private String commentaireTraitement;
    private Boolean estSupprimer;
    private Date dateSignalement;
    private Date dateMiseAJour;
}