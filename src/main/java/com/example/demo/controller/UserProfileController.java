package com.example.demo.controller;

import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.impl.AuthServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.service.CloudinaryService;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {
    private final CloudinaryService cloudinaryService;

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
    @PutMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updatePhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Photo manquante"));
        }

        try {
            // Upload to Cloudinary (using your existing Cloudinary service)
            String cloudinaryUrl = cloudinaryService.uploadAlertImage("profile",file); // Your existing method

            // Save the Cloudinary URL in the database
            utilisateur.setPhotoProfile(cloudinaryUrl);
            utilisateurRepository.save(utilisateur);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Photo de profil mise à jour avec succès");
            response.put("photoProfile", utilisateur.getPhotoProfile());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du téléchargement de la photo: " + e.getMessage()));
        }
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