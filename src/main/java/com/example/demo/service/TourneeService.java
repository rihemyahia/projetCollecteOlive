package com.example.demo.service;

import com.example.demo.dto.TerminerTourneeRequest;
import com.example.demo.dto.TourneeRequest;
import com.example.demo.dto.TourneeResponse;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TourneeService {
    
    // CREATE
    TourneeResponse creer(TourneeRequest request, UserDetails currentUser);
    
    // READ
    TourneeResponse getById(String id, UserDetails currentUser);
    Tournee getTourneeById(String id);
    List<TourneeResponse> getAll(UserDetails currentUser);
    List<TourneeResponse> getByVerger(String vergerId, UserDetails currentUser);
    List<TourneeResponse> getByStatut(StatutTournee statut, UserDetails currentUser);
    List<TourneeResponse> getActive(UserDetails currentUser);
    
    // STATE TRANSITIONS
    TourneeResponse demarrer(String id, UserDetails currentUser);
    TourneeResponse terminer(String id, TerminerTourneeRequest request, UserDetails currentUser);
    TourneeResponse annuler(String id, UserDetails currentUser);
    
    // UPDATE / DELETE
    TourneeResponse mettreAJour(String id, TourneeRequest request, UserDetails currentUser);
    void supprimer(String id, UserDetails currentUser);
    
    // AGGREGATES
    Double getTotalCollecteParVerger(String vergerId, UserDetails currentUser);
    int calculerNbTourneesNecessaires(String vergerId, UserDetails currentUser);

	Optional<List<Utilisateur>> getAllTravailleurs();

	/**
	 * DTO léger pour listes admin (assignation transporteur) : évite de sérialiser
	 * tout le graphe Mongo {@link Tournee} (travailleurs, collecte agrégée, etc.).
	 */
	TourneeResponse toResponseForTransporteurAssignList(Tournee t);

}