package com.example.demo.service;

import com.example.demo.model.Utilisateur;

import java.util.List;

public interface TravailleurService {

    
    List<Utilisateur> listerTravailleurs();
    
    List<Utilisateur> listerTravailleursParSpecialite(String specialite);
    
    Utilisateur trouverTravailleurParId(String id);
    
    
    void supprimerTravailleur(String id);
}