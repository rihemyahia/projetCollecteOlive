package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravailleurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur creerTravailleur(Utilisateur travailleur) {
        System.out.println("👷 Création d'un travailleur: " + travailleur.getEmail());
        
        if (travailleur.getRole() != Role.EQUIPE_RECOLTE) {
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
        return utilisateurRepository.findByRole(Role.EQUIPE_RECOLTE);
    }

    public List<Utilisateur> listerTravailleursDisponibles() {
        List<Utilisateur> tousLesTravailleurs = utilisateurRepository.findByRole(Role.EQUIPE_RECOLTE);
        return tousLesTravailleurs.stream()
                .filter(Utilisateur::isDisponible)
                .collect(Collectors.toList());
    }

    public List<Utilisateur> listerTravailleursDisponiblesPourPeriode(Date dateDebut, Date dateFin) {
        List<Utilisateur> tousLesTravailleurs = utilisateurRepository.findByRole(Role.EQUIPE_RECOLTE);
        return tousLesTravailleurs.stream()
                .filter(travailleur -> travailleur.estDisponiblePour(dateDebut, dateFin))
                .collect(Collectors.toList());
    }

    public List<Utilisateur> listerTravailleursParSpecialite(String specialite) {
        return utilisateurRepository.findByRoleAndSpecialitesContaining(Role.EQUIPE_RECOLTE, specialite);
    }

    public Utilisateur trouverTravailleurParId(String id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travailleur non trouvé"));
        
        if (utilisateur.getRole() != Role.EQUIPE_RECOLTE) {
            throw new RuntimeException("Cet utilisateur n'est pas un travailleur");
        }
        
        return utilisateur;
    }

    public Utilisateur mettreAJourTravailleur(String id, Utilisateur travailleur) {
        Utilisateur existant = trouverTravailleurParId(id);
        
        existant.setNom(travailleur.getNom());
        existant.setPrenom(travailleur.getPrenom());
        existant.setTelephone(travailleur.getTelephone());
        existant.setAdresse(travailleur.getAdresse());
        existant.setCin(travailleur.getCin());
        existant.setSpecialites(travailleur.getSpecialites());
        existant.setDateEmbauche(travailleur.getDateEmbauche());
        existant.setSalaire(travailleur.getSalaire());
        existant.setStatutEmploye(travailleur.getStatutEmploye());
        
        return utilisateurRepository.save(existant);
    }

    public void supprimerTravailleur(String id) {
        Utilisateur travailleur = trouverTravailleurParId(id);
        utilisateurRepository.delete(travailleur);
    }
}