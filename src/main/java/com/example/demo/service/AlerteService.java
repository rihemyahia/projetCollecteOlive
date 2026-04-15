package com.example.demo.service;

import com.example.demo.dto.AlerteRequest;
import com.example.demo.dto.AlerteResponse;
import com.example.demo.model.enums.NiveauUrgence;
import com.example.demo.model.enums.StatutAlerte;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface AlerteService {
    AlerteResponse signalerProbleme(AlerteRequest request);
    AlerteResponse getById(String id);
    List<AlerteResponse> getAll();
    List<AlerteResponse> getByStatut(StatutAlerte statut);
    List<AlerteResponse> getByUrgence(NiveauUrgence urgence);
    List<AlerteResponse> getMesAlertes(String agriculteurId);
    List<AlerteResponse> getByVerger(String vergerId);
    List<AlerteResponse> getNearbyAlerts(Double longitude, Double latitude);
    AlerteResponse marquerTraitee(String id, String commentaire);
    AlerteResponse changerStatut(String id, StatutAlerte statut);
    void supprimer(String id);
    void verifierProprietaireAlerte(String alerteId, UserDetails userDetails);
}