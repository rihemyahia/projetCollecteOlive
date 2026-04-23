package com.example.demo.service.impl;

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
public class AuthServiceImpl implements com.example.demo.service.AuthService {

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

    public void changerMotDePasseAdmin(String id, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
    }

    public Map<String, Object> login(String email, String motDePasse) {
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

        Map<String, Object> response = buildAuthResponse(utilisateur, token);
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

        return buildAuthResponse(utilisateur, token);
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

        return buildAuthResponse(utilisateur, token);
    }

    /**
     * Helper: builds login response including photoProfile.
     */
    private Map<String, Object> buildAuthResponse(Utilisateur utilisateur, String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        response.put("token", token);
        response.put("compteActif", utilisateur.isCompteActif());
        // Include photoProfile so the frontend can display it in the sidebar/navbar immediately after login
        response.put("photoProfile", utilisateur.getPhotoProfile());
        return response;
    }

    // ========== ADMIN : CRÉATION D'UTILISATEURS ==========

    public Map<String, Object> creerUtilisateurParAdmin(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        String motDePasseGenere = passwordGeneratorUtil.generateSecurePassword();

        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseGenere));
        utilisateur.setEstActif(true);
        utilisateur.setCompteActif(true);
        utilisateur.setDateCreation(new Date());

        validateUserByRole(utilisateur);

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        try {
            emailService.envoyerMotDePasse(
                    saved.getEmail(),
                    saved.getNom(),
                    saved.getPrenom(),
                    motDePasseGenere
            );
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Utilisateur créé avec succès");
        response.put("utilisateur", saved);
        response.put("motDePasseGenere", motDePasseGenere);
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
                if (utilisateur.getDatePrisePoste() == null) {
                    utilisateur.setDatePrisePoste(new Date());
                }
                break;
            case AGRICULTEUR:
                break;
            case TRAVAILLEUR:
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

    // ========== GESTION DES UTILISATEURS ==========

    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
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

    // ========== ACTIVATION / DÉSACTIVATION ==========

    public List<Utilisateur> getAgriculteursEnAttente() {
        return utilisateurRepository.findByRoleAndCompteActifFalse(Role.AGRICULTEUR);
    }

    public List<Utilisateur> getTravailleursEnAttente() {
        return utilisateurRepository.findByRoleAndCompteActifFalse(Role.TRAVAILLEUR);
    }

    public List<Utilisateur> getTousUtilisateursEnAttente() {
        return utilisateurRepository.findByCompteActifFalse();
    }

    public Utilisateur activerAgriculteur(String id, String nouveauMotDePasse) {
        Utilisateur agriculteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agriculteur non trouvé"));
        if (agriculteur.getRole() != Role.AGRICULTEUR)
            throw new RuntimeException("Cet utilisateur n'est pas un agriculteur");
        if (agriculteur.isCompteActif())
            throw new RuntimeException("Le compte est déjà activé");
        agriculteur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        agriculteur.setCompteActif(true);
        agriculteur.setEstActif(true);
        Utilisateur sauvegarde = utilisateurRepository.save(agriculteur);
        try {
            emailService.envoyerMotDePasse(agriculteur.getEmail(), agriculteur.getNom(), agriculteur.getPrenom(), nouveauMotDePasse);
        } catch (Exception e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
        }
        return sauvegarde;
    }

    public Utilisateur activerTravailleur(String id, String nouveauMotDePasse) {
        Utilisateur travailleur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travailleur non trouvé"));
        if (travailleur.getRole() != Role.TRAVAILLEUR)
            throw new RuntimeException("Cet utilisateur n'est pas un travailleur");
        if (travailleur.isCompteActif())
            throw new RuntimeException("Le compte est déjà activé");
        travailleur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        travailleur.setCompteActif(true);
        travailleur.setEstActif(true);
        Utilisateur sauvegarde = utilisateurRepository.save(travailleur);
        try {
            emailService.envoyerMotDePasse(travailleur.getEmail(), travailleur.getNom(), travailleur.getPrenom(), nouveauMotDePasse);
        } catch (Exception e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
        }
        return sauvegarde;
    }

    // ========== PROFIL UTILISATEUR ==========

    public Map<String, Object> getProfil(String id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Map<String, Object> profil = new HashMap<>();
        profil.put("id", utilisateur.getId());
        profil.put("email", utilisateur.getEmail());
        profil.put("nom", utilisateur.getNom());
        profil.put("prenom", utilisateur.getPrenom());
        profil.put("telephone", utilisateur.getTelephone());
        profil.put("adresse", utilisateur.getAdresse());
        profil.put("role", utilisateur.getRole());
        // ← Return photo so the profile page can display it
        profil.put("photoProfile", utilisateur.getPhotoProfile());
        return profil;
    }

    public Utilisateur mettreAJourProfil(String id, Map<String, Object> updates) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (updates.containsKey("nom"))
            utilisateur.setNom((String) updates.get("nom"));
        if (updates.containsKey("prenom"))
            utilisateur.setPrenom((String) updates.get("prenom"));
        if (updates.containsKey("telephone"))
            utilisateur.setTelephone((String) updates.get("telephone"));
        if (updates.containsKey("adresse"))
            utilisateur.setAdresse((String) updates.get("adresse"));
        // ← Persist photoProfile when updated from the profile page
        if (updates.containsKey("photoProfile"))
            utilisateur.setPhotoProfile((String) updates.get("photoProfile"));
        return utilisateurRepository.save(utilisateur);
    }

    public void changerMotDePasse(String id, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
    }

    public Utilisateur desactiverCompte(String id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        utilisateur.setCompteActif(false);
        utilisateur.setEstActif(false);
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur activerCompte(String id, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (utilisateur.isCompteActif())
            throw new RuntimeException("Le compte est déjà activé");
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setCompteActif(true);
        utilisateur.setEstActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    public long compterAgriculteursEnAttente() {
        return utilisateurRepository.countByRoleAndCompteActifFalse(Role.AGRICULTEUR);
    }

    public long compterTravailleursEnAttente() {
        return utilisateurRepository.countByRoleAndCompteActifFalse(Role.TRAVAILLEUR);
    }
}