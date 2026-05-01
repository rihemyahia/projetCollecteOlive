package com.example.demo.controller;

import com.example.demo.dto.TerminerTourneeRequest;
import com.example.demo.dto.TourneeRequest;
import com.example.demo.dto.TourneeResponse;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.service.TourneeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tournees")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TourneeController {
@Autowired
    private final TourneeService tourneeService;

    // ─── CREATE ────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<TourneeResponse> creer(
            @Valid @RequestBody TourneeRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.creer(request, currentUser));
    }

    // ─── READ ──────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<TourneeResponse>> getAll(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getAll(currentUser));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<TourneeResponse>> getActive(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getActive(currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'TRANSPORTEUR')")
    public ResponseEntity<TourneeResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getById(id, currentUser));
    }


    @GetMapping("/travailleurs")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<Optional<List<Utilisateur>>> getTravailleurs(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getAllTravailleurs());
    }
    
    @GetMapping("/verger/{vergerId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<TourneeResponse>> getByVerger(
            @PathVariable String vergerId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getByVerger(vergerId, currentUser));
    }

    @GetMapping("/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<TourneeResponse>> getByStatut(
            @RequestParam StatutTournee statut,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.getByStatut(statut, currentUser));
    }

    @GetMapping("/verger/{vergerId}/total-collecte")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<Map<String, Object>> getTotalCollecte(
            @PathVariable String vergerId,
            @AuthenticationPrincipal UserDetails currentUser) {
        Double total = tourneeService.getTotalCollecteParVerger(vergerId, currentUser);
        int nbNecessaires = tourneeService.calculerNbTourneesNecessaires(vergerId, currentUser);
        return ResponseEntity.ok(Map.of(
                "vergerId", vergerId,
                "totalCollecteKg", total,
                "nbTourneesNecessaires", nbNecessaires
        ));
    }

    // ─── STATE TRANSITIONS ─────────────────────────────────────────────────
    @PatchMapping("/{id}/demarrer")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'EQUIPE_RECOLTE')")
    public ResponseEntity<TourneeResponse> demarrer(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.demarrer(id, currentUser));
    }

    @PatchMapping("/{id}/terminer")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'EQUIPE_RECOLTE')")
    public ResponseEntity<TourneeResponse> terminer(
            @PathVariable String id,
            @Valid @RequestBody TerminerTourneeRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.terminer(id, request, currentUser));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<TourneeResponse> annuler(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.annuler(id, currentUser));
    }

    // ─── UPDATE / DELETE ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<TourneeResponse> mettreAJour(
            @PathVariable String id,
            @Valid @RequestBody TourneeRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(tourneeService.mettreAJour(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> supprimer(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        tourneeService.supprimer(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Tournée supprimée avec succès"));
    }
}