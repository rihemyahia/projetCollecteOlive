package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ========== ADMIN : CRÉATION D'UTILISATEURS AVEC TOUS LES RÔLES ==========
    @PostMapping("/admin/utilisateurs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> creerUtilisateurParAdmin(@RequestBody Utilisateur utilisateur) {
        Map<String, Object> response = authService.creerUtilisateurParAdmin(utilisateur);
        return ResponseEntity.ok(response);
    }
    private void validateUserByRole(Utilisateur utilisateur) {
        if (utilisateur.getRole() == null) {
            throw new RuntimeException("Le rôle est requis");
        }
        
        switch (utilisateur.getRole()) {
            case ADMIN:
                // Pas de validation spécifique pour ADMIN
                System.out.println("✅ Création d'un compte ADMIN");
                break;
                
            case RESPONSABLE:
                if (utilisateur.getFonction() == null || utilisateur.getFonction().trim().isEmpty()) {
                    throw new RuntimeException("La fonction est requise pour un responsable");
                }
                System.out.println("✅ Création d'un compte RESPONSABLE - Fonction: " + utilisateur.getFonction());
                break;
                
            case AGRICULTEUR:
                if (utilisateur.getNomExploitation() == null || utilisateur.getNomExploitation().trim().isEmpty()) {
                    throw new RuntimeException("Le nom d'exploitation est requis pour un agriculteur");
                }
                System.out.println("✅ Création d'un compte AGRICULTEUR - Exploitation: " + utilisateur.getNomExploitation());
                break;
                
            case EQUIPE_RECOLTE:
                if (utilisateur.getCin() == null || utilisateur.getCin().trim().isEmpty()) {
                    throw new RuntimeException("Le CIN est requis pour un travailleur");
                }
                if (utilisateur.getStatutEmploye() == null) {
                    throw new RuntimeException("Le statut de l'employé est requis");
                }
                System.out.println("✅ Création d'un compte EQUIPE_RECOLTE - CIN: " + utilisateur.getCin());
                break;
                
            case TRANSPORTEUR:
                if (utilisateur.getPermis() == null || utilisateur.getPermis().trim().isEmpty()) {
                    throw new RuntimeException("Le permis est requis pour un transporteur");
                }
                if (utilisateur.getTarifKm() == null) {
                    throw new RuntimeException("Le tarif au km est requis pour un transporteur");
                }
                System.out.println("✅ Création d'un compte TRANSPORTEUR - Permis: " + utilisateur.getPermis());
                break;
                
            default:
                throw new RuntimeException("Rôle non reconnu: " + utilisateur.getRole());
        }
    }

    // ===== AUTHENTICATION ENDPOINTS =====
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String motDePasse = request.get("motDePasse");
        Map<String, Object> response = authService.login(email, motDePasse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/responsable")
    public ResponseEntity<Map<String, Object>> loginResponsable(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String motDePasse = request.get("motDePasse");
        Map<String, Object> response = authService.loginResponsable(email, motDePasse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String motDePasse = request.get("motDePasse");
        Map<String, Object> response = authService.loginAdmin(email, motDePasse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyToken() {
        Map<String, String> response = Map.of("message", "Token valide", "status", "success");
        return ResponseEntity.ok(response);
    }

    // ===== USER MANAGEMENT ENDPOINTS =====
    
    @PostMapping("/utilisateurs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    public ResponseEntity<Utilisateur> creerUtilisateur(@RequestBody Utilisateur utilisateur) {
        Utilisateur nouvelUtilisateur = authService.creerUtilisateur(utilisateur);
        return ResponseEntity.ok(nouvelUtilisateur);
    }

    @GetMapping("/utilisateurs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    public ResponseEntity<List<Utilisateur>> listerUtilisateurs() {
        List<Utilisateur> utilisateurs = authService.listerUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    public ResponseEntity<Utilisateur> trouverUtilisateurParId(@PathVariable String id) {
        Utilisateur utilisateur = authService.trouverUtilisateurParId(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));
        return ResponseEntity.ok(utilisateur);
    }

    @PutMapping("/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
    public ResponseEntity<Utilisateur> mettreAJourUtilisateur(@PathVariable String id, @RequestBody Utilisateur utilisateur) {
        Utilisateur utilisateurMisAJour = authService.mettreAJourUtilisateur(id, utilisateur);
        return ResponseEntity.ok(utilisateurMisAJour);
    }

    @DeleteMapping("/utilisateurs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable String id) {
        authService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ===== ENDPOINTS ADMIN POUR ACTIVATION =====
    
    @GetMapping("/admin/agriculteurs/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utilisateur>> getAgriculteursEnAttente() {
        return ResponseEntity.ok(authService.getAgriculteursEnAttente());
    }

    @GetMapping("/admin/travailleurs/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utilisateur>> getTravailleursEnAttente() {
        return ResponseEntity.ok(authService.getTravailleursEnAttente());
    }

    @GetMapping("/admin/utilisateurs/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utilisateur>> getTousUtilisateursEnAttente() {
        return ResponseEntity.ok(authService.getTousUtilisateursEnAttente());
    }

    @GetMapping("/admin/stats/attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatsAttente() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("agriculteursEnAttente", authService.compterAgriculteursEnAttente());
        stats.put("travailleursEnAttente", authService.compterTravailleursEnAttente());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/admin/activer-agriculteur/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activerAgriculteur(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authService.activerAgriculteur(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte agriculteur activé avec succès");
        response.put("agriculteur", active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/activer-travailleur/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activerTravailleur(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authService.activerTravailleur(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte travailleur activé avec succès");
        response.put("travailleur", active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/activer-compte/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activerCompte(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authService.activerCompte(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte activé avec succès");
        response.put("utilisateur", active);
        return ResponseEntity.ok(response);
    }
}