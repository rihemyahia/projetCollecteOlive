package com.example.demo.controller;

import com.example.demo.model.Ressource;
import com.example.demo.service.impl.BenneServiceImpl;
import com.example.demo.service.impl.TracteurServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ressources")
@PreAuthorize("hasRole('ADMIN') or hasRole('RESPONSABLE')")
@CrossOrigin(origins = "http://localhost:4200")
public class RessourceController {

    @Autowired private BenneServiceImpl    benneServiceImpl;
    @Autowired private TracteurServiceImpl tracteurServiceImpl;

    // ══════════════════════════════════════════════════════════════
    // BENNE endpoints
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/bennes")
    public ResponseEntity<?> createBenne(@RequestBody Ressource benne) {
        try {
            if (benne.getNom() == null || benne.getNom().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Le nom de la benne est requis"));
            if (benne.getImmatriculation() == null || benne.getImmatriculation().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "L'immatriculation est requise"));
            if (benne.getCapaciteKg() == null || benne.getCapaciteKg() <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "La capacité doit être positive"));
            return ResponseEntity.ok(benneServiceImpl.creerBenne(benne));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bennes")
    public ResponseEntity<List<Ressource>> getAllBennes() {
        return ResponseEntity.ok(benneServiceImpl.listerBennes());
    }

    @GetMapping("/bennes/{id}")
    public ResponseEntity<?> getBenne(@PathVariable String id) {
        try { return ResponseEntity.ok(benneServiceImpl.getBenneById(id)); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    @PutMapping("/bennes/{id}")
    public ResponseEntity<?> updateBenne(@PathVariable String id, @RequestBody Ressource benne) {
        try { return ResponseEntity.ok(benneServiceImpl.mettreAJourBenne(id, benne)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/bennes/{id}")
    public ResponseEntity<?> deleteBenne(@PathVariable String id) {
        try { benneServiceImpl.supprimerBenne(id); return ResponseEntity.ok(Map.of("message", "Benne supprimée")); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    @PostMapping("/bennes/{id}/charger")
    public ResponseEntity<?> chargerBenne(@PathVariable String id, @RequestParam Double quantite) {
        try { return ResponseEntity.ok(benneServiceImpl.ajouterCharge(id, quantite)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/bennes/{id}/vider")
    public ResponseEntity<?> viderBenne(@PathVariable String id) {
        try { return ResponseEntity.ok(benneServiceImpl.viderBenne(id)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/bennes/{id}/maintenance")
    public ResponseEntity<?> startBenneMaintenance(@PathVariable String id) {
        try { return ResponseEntity.ok(benneServiceImpl.enregistrerMaintenance(id, "Maintenance programmée", null)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/bennes/{id}/maintenance/end")
    public ResponseEntity<?> endBenneMaintenance(@PathVariable String id) {
        try { return ResponseEntity.ok(benneServiceImpl.terminerMaintenance(id)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ══════════════════════════════════════════════════════════════
    // TRACTEUR endpoints
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/tracteurs")
    public ResponseEntity<?> createTracteur(@RequestBody Ressource tracteur) {
        try {
            if (tracteur.getNom() == null || tracteur.getNom().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Le nom du tracteur est requis"));
            if (tracteur.getImmatriculation() == null || tracteur.getImmatriculation().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "L'immatriculation est requise"));
            if (tracteur.getPuissance() == null || tracteur.getPuissance().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "La puissance est requise"));
            if (tracteur.getCarburant() == null || tracteur.getCarburant().trim().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "Le carburant est requis"));
            return ResponseEntity.ok(tracteurServiceImpl.creerTracteur(tracteur));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tracteurs")
    public ResponseEntity<List<Ressource>> getAllTracteurs() {
        return ResponseEntity.ok(tracteurServiceImpl.listerTracteurs());
    }

    @GetMapping("/tracteurs/{id}")
    public ResponseEntity<?> getTracteur(@PathVariable String id) {
        try { return ResponseEntity.ok(tracteurServiceImpl.getTracteurById(id)); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    @PutMapping("/tracteurs/{id}")
    public ResponseEntity<?> updateTracteur(@PathVariable String id, @RequestBody Ressource tracteur) {
        try { return ResponseEntity.ok(tracteurServiceImpl.mettreAJourTracteur(id, tracteur)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/tracteurs/{id}")
    public ResponseEntity<?> deleteTracteur(@PathVariable String id) {
        try { tracteurServiceImpl.supprimerTracteur(id); return ResponseEntity.ok(Map.of("message", "Tracteur supprimé")); }
        catch (RuntimeException e) { return ResponseEntity.notFound().build(); }
    }

    @PutMapping("/tracteurs/{id}/kilometrage")
    public ResponseEntity<?> updateKilometrage(@PathVariable String id, @RequestParam Double km) {
        try { return ResponseEntity.ok(tracteurServiceImpl.mettreAJourKilometrage(id, km)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/tracteurs/{id}/maintenance")
    public ResponseEntity<?> startTracteurMaintenance(@PathVariable String id) {
        try { return ResponseEntity.ok(tracteurServiceImpl.enregistrerMaintenance(id, "Maintenance programmée", null)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/tracteurs/{id}/maintenance/end")
    public ResponseEntity<?> endTracteurMaintenance(@PathVariable String id) {
        try { return ResponseEntity.ok(tracteurServiceImpl.terminerMaintenance(id)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}