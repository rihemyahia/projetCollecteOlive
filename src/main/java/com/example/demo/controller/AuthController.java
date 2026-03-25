package com.example.demo.controller;

import com.example.demo.model.Utilisateur;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

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

    // Gestion des utilisateurs (réservée à l'admin)
    @PostMapping("/utilisateurs")
    public ResponseEntity<Utilisateur> creerUtilisateur(@RequestBody Utilisateur utilisateur) {
        Utilisateur nouvelUtilisateur = authService.creerUtilisateur(utilisateur);
        return ResponseEntity.ok(nouvelUtilisateur);
    }

    @GetMapping("/utilisateurs")
    public ResponseEntity<List<Utilisateur>> listerUtilisateurs() {
        List<Utilisateur> utilisateurs = authService.listerUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<Utilisateur> trouverUtilisateurParId(@PathVariable String id) {
        Utilisateur utilisateur = authService.trouverUtilisateurParId(id).orElseThrow();
        return ResponseEntity.ok(utilisateur);
    }

    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<Utilisateur> mettreAJourUtilisateur(@PathVariable String id, @RequestBody Utilisateur utilisateur) {
        Utilisateur utilisateurMisAJour = authService.mettreAJourUtilisateur(id, utilisateur);
        return ResponseEntity.ok(utilisateurMisAJour);
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable String id) {
        authService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }
}