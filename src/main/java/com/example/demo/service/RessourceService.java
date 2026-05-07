package com.example.demo.service;

import com.example.demo.model.Ressource;
import com.example.demo.model.enums.TypeRessource;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
public interface RessourceService {

    // ── CRUD ──────────────────────────────────────────────────────

    Ressource creerRessource(Ressource ressource);

    Ressource getRessourceById(String id);

    List<Ressource> listerToutesLesRessources();

    Ressource mettreAJourRessource(String id, Ressource update);

    void supprimerRessource(String id);

    // ── Search ────────────────────────────────────────────────────

    List<Ressource> listerBennes();

    List<Ressource> listerTracteurs();

    List<Ressource> listerDisponibles();

    List<Ressource> listerDisponiblesParType(TypeRessource type);

    // ── Status info ───────────────────────────────────────────────

    Map<String, Object> obtenirStatut(String id);
}