package com.example.demo.service.impl;

import com.example.demo.model.Ressource;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.enums.TypeRessource;
import com.example.demo.repository.RessourceRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.RessourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TracteurServiceImpl implements com.example.demo.service.TracteurService {

    @Autowired private RessourceRepository   ressourceRepository;
    @Autowired private RessourceService      ressourceService;
    @Autowired private UtilisateurRepository utilisateurRepository;

    public Ressource creerTracteur(Ressource tracteur) {
        tracteur.setType(TypeRessource.TRACTEUR);
        if (tracteur.getPuissance() == null || tracteur.getPuissance().trim().isEmpty())
            throw new RuntimeException("La puissance du tracteur est requise");
        if (tracteur.getCarburant() == null || tracteur.getCarburant().trim().isEmpty())
            throw new RuntimeException("Le type de carburant est requis");
        if (tracteur.getKilometrage()        == null) tracteur.setKilometrage(0.0);
        if (tracteur.getConsommationHoraire() == null) tracteur.setConsommationHoraire(0.0);
        if (tracteur.getARemorque()          == null) tracteur.setARemorque(false);
        if (tracteur.getStatut()             == null) tracteur.setStatut("DISPONIBLE");
        return ressourceRepository.save(tracteur);
    }

    public Ressource getTracteurById(String id) {
        Ressource r = ressourceService.getRessourceById(id);
        if (r.getType() != TypeRessource.TRACTEUR)
            throw new RuntimeException("Cette ressource n'est pas un tracteur");
        return r;
    }

    public List<Ressource> listerTracteurs()           { return ressourceRepository.findAllTracteurs(); }
    public List<Ressource> listerTracteurDisponibles() { return ressourceRepository.findTracteursByStatut("DISPONIBLE"); }

    public Ressource mettreAJourTracteur(String id, Ressource update) {
        Ressource tracteur = getTracteurById(id);
        tracteur.setNom(update.getNom());
        tracteur.setImmatriculation(update.getImmatriculation());
        tracteur.setStatut(update.getStatut());
        if (update.getPuissance()          != null && !update.getPuissance().isEmpty())  tracteur.setPuissance(update.getPuissance());
        if (update.getCarburant()          != null && !update.getCarburant().isEmpty())  tracteur.setCarburant(update.getCarburant());
        if (update.getConsommationHoraire() != null) tracteur.setConsommationHoraire(update.getConsommationHoraire());
        if (update.getARemorque()          != null)  tracteur.setARemorque(update.getARemorque());
        return ressourceRepository.save(tracteur);
    }

    public void supprimerTracteur(String id) {
        ressourceRepository.delete(getTracteurById(id));
    }

    public Ressource mettreAJourKilometrage(String id, Double km) {
        Ressource tracteur = getTracteurById(id);
        if (km == null || km < 0) throw new RuntimeException("Le kilométrage doit être positif");
        if (km < tracteur.getKilometrage()) throw new RuntimeException("Le kilométrage ne peut pas diminuer");
        tracteur.setKilometrage(km);
        return ressourceRepository.save(tracteur);
    }

    public Ressource enregistrerMaintenance(String id, String description, Double cout) {
        Ressource tracteur = getTracteurById(id);
        tracteur.setStatut("MAINTENANCE");
        return ressourceRepository.save(tracteur);
    }

    public Ressource terminerMaintenance(String id) {
        Ressource tracteur = getTracteurById(id);
        if (!"MAINTENANCE".equals(tracteur.getStatut()))
            throw new RuntimeException("Ce tracteur n'est pas en maintenance");
        tracteur.setStatut("DISPONIBLE");
        return ressourceRepository.save(tracteur);
    }

    /** Assign a conducteur (Utilisateur object) to this tracteur. */
    public Ressource assignerConducteur(String tracteurId, String conducteurId) {
        Ressource tracteur = getTracteurById(tracteurId);
        Utilisateur conducteur = utilisateurRepository.findById(conducteurId)
                .orElseThrow(() -> new RuntimeException("Conducteur introuvable : " + conducteurId));
        tracteur.setConducteur(conducteur);   // object, not ID
        return ressourceRepository.save(tracteur);
    }

    public Ressource retirerConducteur(String tracteurId) {
        Ressource tracteur = getTracteurById(tracteurId);
        tracteur.setConducteur(null);
        return ressourceRepository.save(tracteur);
    }

    public List<Ressource> listerTracteursDuConducteur(String conducteurId) {
        return ressourceRepository.findTracteursByConducteurId(conducteurId);
    }

    public List<Ressource> listerAvecRemorque() {
        return listerTracteurs().stream()
                .filter(t -> Boolean.TRUE.equals(t.getARemorque()))
                .collect(Collectors.toList());
    }
}
