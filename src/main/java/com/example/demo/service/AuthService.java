package com.example.demo.service;

import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.config.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
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
    private CustomUserDetailsService userDetailsService;

    // Méthode de connexion standard
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
                utilisateur.getRole(),
                utilisateur.getId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", utilisateur.getId());
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        response.put("token", token);

        System.out.println("✅ Connexion réussie pour: " + email);

        return response;
    }

    // Méthode de connexion pour les responsables
    public Map<String, Object> loginResponsable(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailAndRole(email, "responsable")
                .orElseThrow(() -> new RuntimeException("Accès réservé aux responsables"));

        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        if (!utilisateur.getEstActif()) {
            throw new RuntimeException("Compte désactivé");
        }

        String token = jwtUtils.generateToken(
                utilisateur.getEmail(),
                utilisateur.getRole(),
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

    // Méthode de connexion pour l'admin
    public Map<String, Object> loginAdmin(String email, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailAndRole(email, "admin")
                .orElseThrow(() -> new RuntimeException("Accès réservé à l'admin"));

        if (!passwordEncoder.matches(motDePasse, utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        if (!utilisateur.getEstActif()) {
            throw new RuntimeException("Compte désactivé");
        }

        String token = jwtUtils.generateToken(
                utilisateur.getEmail(),
                utilisateur.getRole(),
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

    // Créer un nouvel utilisateur
    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        utilisateur.setEstActif(true);
        utilisateur.setDateCreation(new Date());
        return utilisateurRepository.save(utilisateur);
    }

    // Trouver un utilisateur par ID
    public Optional<Utilisateur> trouverUtilisateurParId(String id) {
        return utilisateurRepository.findById(id);
    }

    // Lister tous les utilisateurs
    public List<Utilisateur> listerUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    // Mettre à jour un utilisateur
    public Utilisateur mettreAJourUtilisateur(String id, Utilisateur utilisateur) {
        Utilisateur utilisateurExistant = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        utilisateurExistant.setNom(utilisateur.getNom());
        utilisateurExistant.setPrenom(utilisateur.getPrenom());
        utilisateurExistant.setTelephone(utilisateur.getTelephone());
        utilisateurExistant.setRole(utilisateur.getRole());
        utilisateurExistant.setAdresse(utilisateur.getAdresse());

        return utilisateurRepository.save(utilisateurExistant);
    }

    // Supprimer un utilisateur
    public void supprimerUtilisateur(String id) {
        utilisateurRepository.deleteById(id);
    }
}