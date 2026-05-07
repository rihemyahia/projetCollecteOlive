package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.impl.AgriculteurServiceImpl;
import com.example.demo.service.impl.TravailleurServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/responsable")
@PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
@CrossOrigin(origins = "http://localhost:4200")
public class ResponsableController {

    @Autowired
    private AgriculteurServiceImpl agriculteurServiceImpl;

    @Autowired
    private TravailleurServiceImpl travailleurServiceImpl;
    @Autowired
    private  UtilisateurRepository utilisateurRepository;

    @GetMapping("/travailleurs")
    public ResponseEntity<List<Utilisateur>> getTravailleurs() {
        List<Utilisateur> travailleurs = utilisateurRepository.findByRole(Role.TRAVAILLEUR);
        System.out.println("📋 Récupération des travailleurs: " + travailleurs.size() + " trouvés");
        return ResponseEntity.ok(travailleurs);
    }

    /**
     * Liste des transporteurs pour l’assignation (ADMIN et RESPONSABLE — voir sécurité classe).
     * Le front préfère {@code GET /api/admin/transporteurs} pour l’admin et cette route pour le responsable.
     */
    @GetMapping("/transporteurs")
    public ResponseEntity<List<Utilisateur>> getTransporteursPourAssignation() {
        List<Utilisateur> transporteurs = utilisateurRepository.findByRole(Role.TRANSPORTEUR);
        return ResponseEntity.ok(transporteurs);
    }
    // ==================== AGRICULTEURS ====================

    @PostMapping("/agriculteurs")
    public ResponseEntity<?> createAgriculteur(@RequestBody Utilisateur agriculteur) {
        Utilisateur created = agriculteurServiceImpl.creerAgriculteur(agriculteur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur enregistré avec succès. En attente d'activation par l'admin");
        response.put("agriculteur", created);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agriculteurs")
    public ResponseEntity<List<Utilisateur>> getAllAgriculteurs() {
        return ResponseEntity.ok(agriculteurServiceImpl.listerAgriculteurs());
    }

    @GetMapping("/agriculteurs/{id}")
    public ResponseEntity<Utilisateur> getAgriculteurById(@PathVariable String id) {
        return ResponseEntity.ok(agriculteurServiceImpl.trouverAgriculteurParId(id));
    }

    @PutMapping("/agriculteurs/{id}")
    public ResponseEntity<?> updateAgriculteur(@PathVariable String id, @RequestBody Utilisateur agriculteur) {
        Utilisateur updated = agriculteurServiceImpl.mettreAJourAgriculteur(id, agriculteur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur modifié avec succès");
        response.put("agriculteur", updated);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/agriculteurs/{id}")
    public ResponseEntity<?> deleteAgriculteur(@PathVariable String id) {
        agriculteurServiceImpl.supprimerAgriculteur(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    // ==================== TRAVAILLEURS ====================

    @PostMapping("/travailleurs")
    public ResponseEntity<?> createTravailleur(@RequestBody Utilisateur travailleur) {
        Utilisateur created = travailleurServiceImpl.creerTravailleur(travailleur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Travailleur enregistré avec succès. En attente d'activation par l'admin");
        response.put("travailleur", created);
        return ResponseEntity.ok(response);
    }

    
    
    
    
    @GetMapping("/travailleurs/specialite/{specialite}")
    public ResponseEntity<List<Utilisateur>> getTravailleursBySpecialite(@PathVariable String specialite) {
        return ResponseEntity.ok(travailleurServiceImpl.listerTravailleursParSpecialite(specialite));
    }

    @GetMapping("/travailleurs/{id}")
    public ResponseEntity<Utilisateur> getTravailleurById(@PathVariable String id) {
        return ResponseEntity.ok(travailleurServiceImpl.trouverTravailleurParId(id));
    }

    

    @DeleteMapping("/travailleurs/{id}")
    public ResponseEntity<?> deleteTravailleur(@PathVariable String id) {
        travailleurServiceImpl.supprimerTravailleur(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Travailleur supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    // ==================== MÉTHODES UTILITAIRES TRAVAILLEURS ====================
    
    
}