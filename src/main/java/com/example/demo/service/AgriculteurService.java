package com.example.demo.service;

import com.example.demo.model.Utilisateur;

import java.util.List;

public interface AgriculteurService {

    
    Utilisateur trouverAgriculteurParId(String id);
    
    List<Utilisateur> listerAgriculteurs();
    
    void supprimerAgriculteur(String id);
}