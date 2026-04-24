package com.example.demo.service;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.model.enums.StatutVerger;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface VergerService {

    VergerResponse creer(VergerRequest request, UserDetails userDetails);
    VergerResponse getById(String id);
    List<VergerResponse> getAll();
    List<VergerResponse> getByAgriculteur(String agriculteurId);
    List<VergerResponse> getByResponsable(UserDetails userDetails);
    List<VergerResponse> getByStatut(StatutVerger statut);
    VergerResponse mettreAJour(String id, VergerRequest request);
    VergerResponse mettreAJourAdmin(String id, VergerRequest request);
    VergerResponse changerStatut(String id, StatutVerger statut);
    void desactiver(String id);
    void verifierProprietaireVerger(String vergerId, UserDetails userDetails);
    void verifierProprietaire(String agriculteurId, UserDetails userDetails);

    // ── Geolocation ─────────────────────────────────────────────────────────

    /**
     * Returns all non-deleted vergers that have GPS coordinates.
     * Accessible by ADMIN and RESPONSABLE.
     */
    List<VergerResponse> getAllWithLocation();

    /**
     * Returns all non-deleted vergers belonging to the given agriculteur
     * that have GPS coordinates.
     */
    List<VergerResponse> getByAgriculteurWithLocation(String agriculteurId);

    /**
     * Update ONLY the GPS location of a verger (latitude, longitude, adresseIndicative).
     * Useful when a responsable wants to pin a location on the map without changing
     * any other verger data.
     */
    VergerResponse mettreAJourLocalisation(String id, Double latitude, Double longitude, String adresseIndicative);

    /**
     * Find vergers within maxDistanceMetres of the given coordinate.
     */
    List<VergerResponse> findNearby(Double longitude, Double latitude, Double maxDistanceMetres);
}