package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AgriculteurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur creerAgriculteur(Utilisateur agriculteur) {
        System.out.println("🌾 Création d'un agriculteur: " + agriculteur.getEmail());
        
        if (agriculteur.getRole() != Role.AGRICULTEUR) {
            throw new RuntimeException("Le rôle doit être AGRICULTEUR");
        }
        
        // Pas de mot de passe, compte désactivé
        agriculteur.setMotDePasse(null);
        agriculteur.setCompteActif(false);
        agriculteur.setEstActif(false);
        agriculteur.setDateCreation(new Date());
        
        if (agriculteur.getNomExploitation() == null || agriculteur.getNomExploitation().trim().isEmpty()) {
            throw new RuntimeException("Le nom d'exploitation est requis pour un agriculteur");
        }
        
        return utilisateurRepository.save(agriculteur);
    }

    public Utilisateur mettreAJourAgriculteur(String id, Utilisateur agriculteur) {
        Utilisateur existant = trouverAgriculteurParId(id);
        
        existant.setNom(agriculteur.getNom());
        existant.setPrenom(agriculteur.getPrenom());
        existant.setTelephone(agriculteur.getTelephone());
        existant.setAdresse(agriculteur.getAdresse());
        existant.setNomExploitation(agriculteur.getNomExploitation());
        
        return utilisateurRepository.save(existant);
    }

    public Utilisateur trouverAgriculteurParId(String id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agriculteur non trouvé"));
        
        if (utilisateur.getRole() != Role.AGRICULTEUR) {
            throw new RuntimeException("Cet utilisateur n'est pas un agriculteur");
        }
        
        return utilisateur;
    }

    public List<Utilisateur> listerAgriculteurs() {
        return utilisateurRepository.findByRole(Role.AGRICULTEUR);
    }

    public void supprimerAgriculteur(String id) {
        Utilisateur agriculteur = trouverAgriculteurParId(id);
        utilisateurRepository.delete(agriculteur);
    }
}