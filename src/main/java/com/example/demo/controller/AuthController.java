package com.example.demo.controller;

import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.impl.AuthServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthServiceImpl authServiceImpl;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // ========== AUTHENTIFICATION ==========

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(authServiceImpl.login(request.get("email"), request.get("motDePasse")));
    }

    @PostMapping("/login/responsable")
    public ResponseEntity<Map<String, Object>> loginResponsable(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(authServiceImpl.loginResponsable(request.get("email"), request.get("motDePasse")));
    }

    @PostMapping("/login/admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(authServiceImpl.loginAdmin(request.get("email"), request.get("motDePasse")));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyToken() {
        return ResponseEntity.ok(Map.of("message", "Token valide", "status", "success"));
    }

    // ========== NEW: Admin photo update endpoint ==========
    // All other admin CRUD/activation endpoints stay in AdminController — do NOT move them here.

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/utilisateurs/{id}/photo")
    public ResponseEntity<Map<String, Object>> updatePhotoAdmin(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        utilisateur.setPhotoProfile((String) body.get("photoProfile"));
        utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(Map.of("message", "Photo mise à jour"));
    }
}