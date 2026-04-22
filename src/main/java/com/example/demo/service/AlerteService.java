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
    List<AlerteResponse> getByResponsable(UserDetails userDetails);
    List<AlerteResponse> getByStatut(StatutAlerte statut);
    List<AlerteResponse> getByStatutAndResponsable(StatutAlerte statut, UserDetails userDetails);
    List<AlerteResponse> getByUrgence(NiveauUrgence urgence);
    List<AlerteResponse> getByUrgenceAndResponsable(NiveauUrgence urgence, UserDetails userDetails);
    List<AlerteResponse> getMesAlertes(String agriculteurId);
    List<AlerteResponse> getByVerger(String vergerId);
    List<AlerteResponse> getNearbyAlerts(Double longitude, Double latitude);
    List<AlerteResponse> getNearbyAlertsForResponsable(Double longitude, Double latitude, UserDetails userDetails);
    AlerteResponse marquerTraitee(String id, String commentaire);
    AlerteResponse changerStatut(String id, StatutAlerte statut);
    AlerteResponse changerUrgence(String id, NiveauUrgence urgence, UserDetails userDetails);
    void supprimer(String id);
    void verifierProprietaireAlerte(String alerteId, UserDetails userDetails);
    void verifyResponsableOwnsVerger(String vergerId, UserDetails userDetails);
    void verifyResponsableOwnsAlert(String alerteId, UserDetails userDetails);
    AlerteResponse changerStatutForResponsable(String id, StatutAlerte statut, UserDetails userDetails);
    AlerteResponse changerUrgenceForResponsable(String id, NiveauUrgence urgence, UserDetails userDetails);
    AlerteResponse marquerTraiteeForResponsable(String id, String commentaire, UserDetails userDetails);
}