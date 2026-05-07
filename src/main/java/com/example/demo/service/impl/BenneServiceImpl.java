package com.example.demo.service.impl;

import com.example.demo.model.Ressource;
import com.example.demo.model.enums.TypeRessource;
import com.example.demo.repository.RessourceRepository;
import com.example.demo.service.BenneServices;
import com.example.demo.service.RessourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BenneServiceImpl implements BenneServices {

    @Autowired private RessourceRepository ressourceRepository;
    @Autowired private RessourceService    ressourceService;

    public Ressource creerBenne(Ressource benne) {
        benne.setType(TypeRessource.BENNE);
        if (benne.getCapaciteKg() == null || benne.getCapaciteKg() <= 0)
            throw new RuntimeException("La capacité d'une benne doit être positive");
        benne.setQuantiteChargeeActuelle(0.0);
        benne.setTauxRemplissage(0.0);
        benne.setEstPleine(false);
        if (benne.getStatut() == null) benne.setStatut("DISPONIBLE");
        return ressourceRepository.save(benne);
    }

    public Ressource getBenneById(String id) {
        Ressource r = ressourceService.getRessourceById(id);
        if (r.getType() != TypeRessource.BENNE)
            throw new RuntimeException("Cette ressource n'est pas une benne");
        return r;
    }

    public List<Ressource> listerBennes()            { return ressourceRepository.findAllBennes(); }
    public List<Ressource> listerBennesDisponibles() { return ressourceRepository.findBennesByStatut("DISPONIBLE"); }
    public List<Ressource> listerBennesPleines()     { return ressourceRepository.findFullBennes(); }

    public Ressource mettreAJourBenne(String id, Ressource update) {
        Ressource benne = getBenneById(id);
        benne.setNom(update.getNom());
        benne.setImmatriculation(update.getImmatriculation());
        benne.setStatut(update.getStatut());
        if (update.getCapaciteKg() != null && update.getCapaciteKg() > 0)
            benne.setCapaciteKg(update.getCapaciteKg());
        return ressourceRepository.save(benne);
    }

    public void supprimerBenne(String id) {
        ressourceRepository.delete(getBenneById(id));
    }

    public Ressource ajouterCharge(String id, Double quantite) {
        if (quantite == null || quantite <= 0)
            throw new RuntimeException("La quantité à ajouter doit être positive");
        Ressource benne = getBenneById(id);
        try {
            benne.ajouterCharge(quantite);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Erreur : " + e.getMessage());
        }
        return ressourceRepository.save(benne);
    }

    public Ressource viderBenne(String id) {
        Ressource benne = getBenneById(id);
        benne.vider();
        return ressourceRepository.save(benne);
    }

    public Ressource enregistrerMaintenance(String id, String description, Double cout) {
        Ressource benne = getBenneById(id);
        benne.setStatut("MAINTENANCE");
        return ressourceRepository.save(benne);
    }

    public Ressource terminerMaintenance(String id) {
        Ressource benne = getBenneById(id);
        if (!"MAINTENANCE".equals(benne.getStatut()))
            throw new RuntimeException("Cette benne n'est pas en maintenance");
        benne.setStatut("DISPONIBLE");
        return ressourceRepository.save(benne);
    }

    /** Attach a tracteur object to this benne. */
    public Ressource assignerTracteur(String benneId, String tracteurId) {
        Ressource benne   = getBenneById(benneId);
        Ressource tracteur = ressourceService.getRessourceById(tracteurId);
        if (tracteur.getType() != TypeRessource.TRACTEUR)
            throw new RuntimeException("Cette ressource n'est pas un tracteur");
        benne.setTracteur(tracteur);   // object, not ID
        return ressourceRepository.save(benne);
    }

    public Ressource retirerTracteur(String benneId) {
        Ressource benne = getBenneById(benneId);
        benne.setTracteur(null);
        return ressourceRepository.save(benne);
    }

    public List<Ressource> listerBennesDuTracteur(String tracteurId) {
        return ressourceRepository.findBennesByTracteurId(tracteurId);
    }
}