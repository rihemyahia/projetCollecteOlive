package com.example.demo.service;

import java.util.List;

import com.example.demo.model.Ressource;

public interface BenneServices {
	 Ressource creerBenne(Ressource benne);
	    
	    Ressource getBenneById(String id);
	    
	    List<Ressource> listerBennes();
	    
	    List<Ressource> listerBennesDisponibles();
	    
	    List<Ressource> listerBennesPleines();
	    
	    Ressource mettreAJourBenne(String id, Ressource update);
	    
	    void supprimerBenne(String id);
	    
	    Ressource ajouterCharge(String id, Double quantite);
	    
	    Ressource viderBenne(String id);
	    
	    Ressource enregistrerMaintenance(String id, String description, Double cout);
	    
	    Ressource terminerMaintenance(String id);
	    
	    Ressource assignerTracteur(String benneId, String tracteurId);
	    
	    Ressource retirerTracteur(String benneId);
	    
	    List<Ressource> listerBennesDuTracteur(String tracteurId);

}
