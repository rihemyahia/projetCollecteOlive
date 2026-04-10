package com.example.demo.service;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.model.enums.StatutVerger;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface VergerService {
    VergerResponse creer(VergerRequest request);           // RESPONSABLE only
    VergerResponse getById(String id);
    List<VergerResponse> getAll();
    List<VergerResponse> getByAgriculteur(String agriculteurId);
    List<VergerResponse> getByStatut(StatutVerger statut);
    VergerResponse mettreAJour(String id, VergerRequest request);
    VergerResponse changerStatut(String id, StatutVerger statut);
    void desactiver(String id);                           // soft-delete, RESPONSABLE only
    void verifierProprietaireVerger(String vergerId, UserDetails userDetails);
    void verifierProprietaire(String agriculteurId, UserDetails userDetails);
}