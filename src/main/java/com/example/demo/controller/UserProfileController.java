package com.example.demo.controller;

import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.impl.AuthServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    @Autowired
    private AuthServiceImpl authServiceImpl;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfil(Authentication authentication) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(authServiceImpl.getProfil(utilisateur.getId()));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> mettreAJourProfil(
            Authentication authentication,
            @RequestBody Map<String, Object> updates) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Utilisateur misAJour = authServiceImpl.mettreAJourProfil(utilisateur.getId(), updates);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profil mis à jour avec succès");
        response.put("utilisateur", misAJour);
        return ResponseEntity.ok(response);
    }

    /**
     * Dedicated endpoint for uploading the logged-in user's profile photo.
     * Body: { "photoProfile": "data:image/jpeg;base64,..." }
     * Kept separate so it is clear and the base64 payload doesn't mix with
     * other profile fields in the general PUT.
     */
    @PutMapping("/photo")
    public ResponseEntity<Map<String, Object>> updatePhoto(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String photoBase64 = (String) body.get("photoProfile");
        if (photoBase64 == null || photoBase64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Photo manquante"));
        }

        utilisateur.setPhotoProfile(photoBase64);
        utilisateurRepository.save(utilisateur);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Photo de profil mise à jour avec succès");
        response.put("photoProfile", utilisateur.getPhotoProfile());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/changer-mot-de-passe")
    public ResponseEntity<Map<String, String>> changerMotDePasse(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        authServiceImpl.changerMotDePasse(
                utilisateur.getId(),
                request.get("ancienMotDePasse"),
                request.get("nouveauMotDePasse")
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe changé avec succès");
        return ResponseEntity.ok(response);
    }
}