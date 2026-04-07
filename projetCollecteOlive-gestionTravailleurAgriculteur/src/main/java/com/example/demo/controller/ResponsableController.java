package com.example.demo.controller;

import com.example.demo.model.Utilisateur;
import com.example.demo.service.AgriculteurService;
import com.example.demo.service.TravailleurService;
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
@PreAuthorize("hasRole('RESPONSABLE')")
@CrossOrigin(origins = "http://localhost:4200")
public class ResponsableController {
    
    @Autowired
    private AgriculteurService agriculteurService;
    
    @Autowired
    private TravailleurService travailleurService;
    
    // ==================== AGRICULTEURS ====================
    
    @PostMapping("/agriculteurs")
    public ResponseEntity<?> createAgriculteur(@RequestBody Utilisateur agriculteur) {
        Utilisateur created = agriculteurService.creerAgriculteur(agriculteur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur enregistré avec succès. En attente d'activation par l'admin");
        response.put("agriculteur", created);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/agriculteurs")
    public ResponseEntity<List<Utilisateur>> getAllAgriculteurs() {
        return ResponseEntity.ok(agriculteurService.listerAgriculteurs());
    }
    
    @GetMapping("/agriculteurs/{id}")
    public ResponseEntity<Utilisateur> getAgriculteurById(@PathVariable String id) {
        return ResponseEntity.ok(agriculteurService.trouverAgriculteurParId(id));
    }
    
    @PutMapping("/agriculteurs/{id}")
    public ResponseEntity<?> updateAgriculteur(@PathVariable String id, @RequestBody Utilisateur agriculteur) {
        Utilisateur updated = agriculteurService.mettreAJourAgriculteur(id, agriculteur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur modifié avec succès");
        response.put("agriculteur", updated);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/agriculteurs/{id}")
    public ResponseEntity<?> deleteAgriculteur(@PathVariable String id) {
        agriculteurService.supprimerAgriculteur(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Agriculteur supprimé avec succès");
        return ResponseEntity.ok(response);
    }
    
    // ==================== TRAVAILLEURS ====================
    
    @PostMapping("/travailleurs")
    public ResponseEntity<?> createTravailleur(@RequestBody Utilisateur travailleur) {
        Utilisateur created = travailleurService.creerTravailleur(travailleur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Travailleur enregistré avec succès. En attente d'activation par l'admin");
        response.put("travailleur", created);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/travailleurs")
    public ResponseEntity<List<Utilisateur>> getAllTravailleurs() {
        return ResponseEntity.ok(travailleurService.listerTravailleurs());
    }
    
    @GetMapping("/travailleurs/disponibles")
    public ResponseEntity<List<Utilisateur>> getTravailleursDisponibles() {
        return ResponseEntity.ok(travailleurService.listerTravailleursDisponibles());
    }
    
    @GetMapping("/travailleurs/disponibles/periode")
    public ResponseEntity<List<Utilisateur>> getTravailleursDisponiblesPourPeriode(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date dateDebut,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date dateFin) {
        return ResponseEntity.ok(travailleurService.listerTravailleursDisponiblesPourPeriode(dateDebut, dateFin));
    }
    
    @GetMapping("/travailleurs/specialite/{specialite}")
    public ResponseEntity<List<Utilisateur>> getTravailleursBySpecialite(@PathVariable String specialite) {
        return ResponseEntity.ok(travailleurService.listerTravailleursParSpecialite(specialite));
    }
    
    @GetMapping("/travailleurs/{id}")
    public ResponseEntity<Utilisateur> getTravailleurById(@PathVariable String id) {
        return ResponseEntity.ok(travailleurService.trouverTravailleurParId(id));
    }
    
    @PutMapping("/travailleurs/{id}")
    public ResponseEntity<?> updateTravailleur(@PathVariable String id, @RequestBody Utilisateur travailleur) {
        Utilisateur updated = travailleurService.mettreAJourTravailleur(id, travailleur);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Travailleur modifié avec succès");
        response.put("travailleur", updated);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/travailleurs/{id}")
    public ResponseEntity<?> deleteTravailleur(@PathVariable String id) {
        travailleurService.supprimerTravailleur(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Travailleur supprimé avec succès");
        return ResponseEntity.ok(response);
    }
    
    // ==================== MÉTHODES UTILITAIRES TRAVAILLEURS ====================
    
    @GetMapping("/travailleurs/{id}/disponible")
    public ResponseEntity<Map<String, Boolean>> isTravailleurDisponible(@PathVariable String id) {
        Utilisateur travailleur = travailleurService.trouverTravailleurParId(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("disponible", travailleur.isDisponible());
        response.put("enCollecte", travailleur.estEnCollecte());
        return ResponseEntity.ok(response);
    }
}