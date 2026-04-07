package com.example.demo.service;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutVerger;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface VergerService {
    VergerResponse creer(VergerRequest request);
    VergerResponse getById(String id);
    List<VergerResponse> getAll();
    List<VergerResponse> getByAgriculteur(String proprietaireId);
    List<VergerResponse> getByStatut(StatutVerger statut);
    VergerResponse mettreAJour(String id, VergerRequest request);
    VergerResponse changerStatut(String id, StatutVerger statut);
    void desactiver(String id);
    void verifierProprietaire(String agriculteurId, UserDetails userDetails);
    void verifierProprietaireVerger(String vergeId, UserDetails userDetails);
    List<VergerResponse> getEnAttente();
    VergerResponse valider(String id);
    VergerResponse rejeter(String id, String motif);}