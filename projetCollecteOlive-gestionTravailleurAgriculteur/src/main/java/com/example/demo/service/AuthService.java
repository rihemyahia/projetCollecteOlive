package com.example.demo.service;

import com.example.demo.config.JwtUtils;
import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.utils.PasswordGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordGeneratorUtil passwordGeneratorUtil;

    @Autowired
    private EmailService emailService;

    // ========== MÉTHODES D'AUTHENTIFICATION ==========
    
    public Map<String, Object> login(String email, String motDePasse) {
        System.out.println("🔐 Tentative de connexion pour: " + email);
        
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        if (!utilisateur.getEstActif()) {
            throw new RuntimeException("Compte désactivé");
        }
        
        String token = jwtUtils.generateToken(
                utilisateur.getEmail(),
                utilisateur.getRole().toString(),
                utilisateur.getId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        response.put("token", token);
        response.put("compteActif", utilisateur.isCompteActif());
        
        return response;
    }

    public Map<String, Object> loginResponsable(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailAndRole(email, Role.RESPONSABLE)
                .orElseThrow(() -> new RuntimeException("Accès réservé aux responsables"));
        
        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        String token = jwtUtils.generateToken(
                utilisateur.getEmail(),
                utilisateur.getRole().toString(),
                utilisateur.getId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        response.put("token", token);
        
        return response;
    }

    public Map<String, Object> loginAdmin(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailAndRole(email, Role.ADMIN)
                .orElseThrow(() -> new RuntimeException("Accès réservé à l'admin"));
        
        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        
        String token = jwtUtils.generateToken(
                utilisateur.getEmail(),
                utilisateur.getRole().toString(),
                utilisateur.getId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        response.put("token", token);
        response.put("compteActif", utilisateur.isCompteActif());
        
        return response;
    }

    // ========== ADMIN : CRÉATION D'UTILISATEURS AVEC MOT DE PASSE GÉNÉRÉ ==========
    
    public Map<String, Object> creerUtilisateurParAdmin(Utilisateur utilisateur) {
        System.out.println("👑 Admin création utilisateur: " + utilisateur.getEmail() + " avec rôle: " + utilisateur.getRole());
        
        // Vérifier que l'email n'existe pas déjà
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }
        
        // Générer un mot de passe sécurisé
        String motDePasseGenere = passwordGeneratorUtil.generateSecurePassword();
        System.out.println("🔑 Mot de passe généré pour " + utilisateur.getEmail() + ": " + motDePasseGenere);
        
        // Encoder le mot de passe
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseGenere));
        utilisateur.setEstActif(true);
        utilisateur.setCompteActif(true);
        utilisateur.setDateCreation(new Date());
        
        // Validation selon le rôle
        validateUserByRole(utilisateur);
        
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        System.out.println("✅ Admin a créé un utilisateur: " + saved.getEmail() + " avec rôle: " + saved.getRole());
        
        // Envoyer l'email avec le mot de passe généré
        try {
            emailService.envoyerMotDePasse(
                saved.getEmail(),
                saved.getNom(),
                saved.getPrenom(),
                motDePasseGenere
            );
            System.out.println("✅ Email envoyé à " + saved.getEmail() + " avec le mot de passe");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Utilisateur créé avec succès");
        response.put("utilisateur", saved);
        response.put("motDePasseGenere", motDePasseGenere); // Pour debug, à retirer en prod
        
        return response;
    }

    private void validateUserByRole(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null) {
            throw new RuntimeException("Le rôle est requis");
        }
        
        switch (utilisateur.getRole()) {
            case ADMIN:
                break;
            case RESPONSABLE:
                if (utilisateur.getFonction() == null || utilisateur.getFonction().trim().isEmpty()) {
                    throw new RuntimeException("La fonction est requise pour un responsable");
                }
                if (utilisateur.getDatePrisePoste() == null) {
                    utilisateur.setDatePrisePoste(new Date());
                }
                break;
            case AGRICULTEUR:
                if (utilisateur.getNomExploitation() == null || utilisateur.getNomExploitation().trim().isEmpty()) {
                    throw new RuntimeException("Le nom d'exploitation est requis pour un agriculteur");
                }
                break;
            case EQUIPE_RECOLTE:
                if (utilisateur.getCin() == null || utilisateur.getCin().trim().isEmpty()) {
                    throw new RuntimeException("Le CIN est requis pour un travailleur");
                }
                if (utilisateur.getStatutEmploye() == null) {
                    throw new RuntimeException("Le statut de l'employé est requis");
                }
                break;
            case TRANSPORTEUR:
                if (utilisateur.getPermis() == null || utilisateur.getPermis().trim().isEmpty()) {
                    throw new RuntimeException("Le permis est requis pour un transporteur");
                }
                if (utilisateur.getTarifKm() == null) {
                    throw new RuntimeException("Le tarif au km est requis pour un transporteur");
                }
                break;
            default:
                break;
        }
    }

    // ========== MÉTHODES DE GESTION DES UTILISATEURS ==========
    
    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        System.out.println("📝 Création d'un nouvel utilisateur: " + utilisateur.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        utilisateur.setEstActif(true);
        utilisateur.setCompteActif(true);
        utilisateur.setDateCreation(new Date());
        return utilisateurRepository.save(utilisateur);
    }

    public Optional<Utilisateur> trouverUtilisateurParId(String id) {
        return utilisateurRepository.findById(id);
    }

    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Utilisateur mettreAJourUtilisateur(String id, Utilisateur utilisateur) {
        Utilisateur existant = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        existant.setNom(utilisateur.getNom());
        existant.setPrenom(utilisateur.getPrenom());
        existant.setTelephone(utilisateur.getTelephone());
        existant.setRole(utilisateur.getRole());
        existant.setAdresse(utilisateur.getAdresse());
        
        return utilisateurRepository.save(existant);
    }

    public void supprimerUtilisateur(String id) {
        utilisateurRepository.deleteById(id);
    }

    public Authentication authenticate(String email, String motDePasse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, motDePasse)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentification échouée: " + e.getMessage());
        }
    }

    // ========== MÉTHODES POUR L'ADMIN (ACTIVATION) ==========
    
    public List<Utilisateur> getAgriculteursEnAttente() {
        return utilisateurRepository.findByRoleAndCompteActifFalse(Role.AGRICULTEUR);
    }

    public List<Utilisateur> getTravailleursEnAttente() {
        return utilisateurRepository.findByRoleAndCompteActifFalse(Role.EQUIPE_RECOLTE);
    }

    public List<Utilisateur> getTousUtilisateursEnAttente() {
        return utilisateurRepository.findByCompteActifFalse();
    }

    public Utilisateur activerAgriculteur(String id, String nouveauMotDePasse) {
        Utilisateur agriculteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agriculteur non trouvé"));
        
        if (agriculteur.getRole() != Role.AGRICULTEUR) {
            throw new RuntimeException("Cet utilisateur n'est pas un agriculteur");
        }
        
        if (agriculteur.isCompteActif()) {
            throw new RuntimeException("Le compte de cet agriculteur est déjà activé");
        }
        
        agriculteur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        agriculteur.setCompteActif(true);
        agriculteur.setEstActif(true);
        
        Utilisateur sauvegarde = utilisateurRepository.save(agriculteur);
        
        try {
            emailService.envoyerMotDePasse(
                agriculteur.getEmail(),
                agriculteur.getNom(),
                agriculteur.getPrenom(),
                nouveauMotDePasse
            );
        } catch (Exception e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
        }
        
        return sauvegarde;
    }

    public Utilisateur activerTravailleur(String id, String nouveauMotDePasse) {
        Utilisateur travailleur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travailleur non trouvé"));
        
        if (travailleur.getRole() != Role.EQUIPE_RECOLTE) {
            throw new RuntimeException("Cet utilisateur n'est pas un travailleur");
        }
        
        if (travailleur.isCompteActif()) {
            throw new RuntimeException("Le compte de ce travailleur est déjà activé");
        }
        
        travailleur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        travailleur.setCompteActif(true);
        travailleur.setEstActif(true);
        
        Utilisateur sauvegarde = utilisateurRepository.save(travailleur);
        
        try {
            emailService.envoyerMotDePasse(
                travailleur.getEmail(),
                travailleur.getNom(),
                travailleur.getPrenom(),
                nouveauMotDePasse
            );
        } catch (Exception e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
        }
        
        return sauvegarde;
    }

    public Utilisateur activerCompte(String id, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if (utilisateur.isCompteActif()) {
            throw new RuntimeException("Le compte est déjà activé");
        }
        
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setCompteActif(true);
        utilisateur.setEstActif(true);
        
        return utilisateurRepository.save(utilisateur);
    }

    public long compterAgriculteursEnAttente() {
        return utilisateurRepository.countByRoleAndCompteActifFalse(Role.AGRICULTEUR);
    }

    public long compterTravailleursEnAttente() {
        return utilisateurRepository.countByRoleAndCompteActifFalse(Role.EQUIPE_RECOLTE);
    }
}