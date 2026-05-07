package com.example.demo.service.impl;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.TravailleurService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravailleurServiceImpl  implements TravailleurService{

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur creerTravailleur(Utilisateur travailleur) {
        System.out.println("👷 Création d'un travailleur: " + travailleur.getEmail());
        if (travailleur.getRole() != Role.TRAVAILLEUR) {
            throw new RuntimeException("Le rôle doit être EQUIPE_RECOLTE");
        }

        // Pas de mot de passe, compte désactivé
        travailleur.setMotDePasse(null);
        travailleur.setCompteActif(false);
        travailleur.setEstActif(false);
        travailleur.setDateCreation(new Date());

        if (travailleur.getCin() == null || travailleur.getCin().trim().isEmpty()) {
            throw new RuntimeException("Le CIN est requis pour un travailleur");
        }
        if (travailleur.getStatutEmploye() == null) {
            throw new RuntimeException("Le statut de l'employé est requis");
        }

        return utilisateurRepository.save(travailleur);
    }

    public List<Utilisateur> listerTravailleurs() {
        return utilisateurRepository.findByRole(Role.TRAVAILLEUR);
    }



    public List<Utilisateur> listerTravailleursParSpecialite(String specialite) {
        return utilisateurRepository.findByRoleAndSpecialitesContaining(Role.TRAVAILLEUR, specialite);
    }

    public Utilisateur trouverTravailleurParId(String id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travailleur non trouvé"));
        if (utilisateur.getRole() != Role.TRAVAILLEUR) {
            throw new RuntimeException("Cet utilisateur n'est pas un travailleur");
        }

        return utilisateur;
    }


    public void supprimerTravailleur(String id) {
        Utilisateur travailleur = trouverTravailleurParId(id);
        utilisateurRepository.delete(travailleur);
    }
}