package com.example.demo.service;

import com.example.demo.dto.CollecteDetailDTO;
import com.example.demo.dto.CollecteRequest;
import com.example.demo.model.Collecte;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutCollecte;

import java.util.Date;
import java.util.List;

public interface CollecteService {
    
    Collecte getById(String id);
List<Collecte> getByStatut(StatutCollecte statut);
    
    // Pour les collectes actives
    List<Collecte> getActiveCollectes();
    
    // Pour les requêtes par année
    List<Collecte> getByAnnee(String annee);
    
    // Pour la mise à jour
    Collecte updateCollecte(String id, CollecteRequest request);
    
    // Pour la suppression
    void deleteCollecte(String id);
    List<Collecte> getAll();
    
    List<Collecte> getByVerger(String vergerId);
    
    CollecteDetailDTO getCollecteWithTournees(String collecteId);
    
    Collecte createNewCollecte(Verger verger, String annee, Date dateDebut);
    
    void updateCollecteStats(String collecteId);
    
    String getCampagneAnnee(Date date);
    
    void demarrerCollecte(String collecteId);
    
    void terminerCollecte(String collecteId);
	List<Collecte> getCollectesByResponsable(String responsableId);
}