package com.example.demo.controller;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.service.VergerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vergers")
@RequiredArgsConstructor
public class VergerController {

    private final VergerService vergerService;

    // Agriculteur ne peut créer que pour lui-même
    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR')")
    public ResponseEntity<VergerResponse> creer(
            @Valid @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        vergerService.verifierProprietaire(req.getProprietaireId(), userDetails);
        return ResponseEntity.ok(vergerService.creer(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VergerResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(vergerService.getById(id));
    }

    // Responsable uniquement — @PreAuthorize now works after @EnableMethodSecurity
    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<List<VergerResponse>> getAll() {
        return ResponseEntity.ok(vergerService.getAll());
    }

    // Agriculteur ne peut voir que ses propres vergers
    @GetMapping("/agriculteur/{agriculteurId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VergerResponse>> getByAgriculteur(
            @PathVariable String agriculteurId,
            @AuthenticationPrincipal UserDetails userDetails) {

        vergerService.verifierProprietaire(agriculteurId, userDetails);
        return ResponseEntity.ok(vergerService.getByAgriculteur(agriculteurId));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<List<VergerResponse>> getEnAttente() {
        return ResponseEntity.ok(vergerService.getEnAttente());
    }

    // Agriculteur ne peut modifier que son propre verger
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR')")
    public ResponseEntity<VergerResponse> mettreAJour(
            @PathVariable String id,
            @Valid @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        vergerService.verifierProprietaireVerger(id, userDetails);
        return ResponseEntity.ok(vergerService.mettreAJour(id, req));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR')")
    public ResponseEntity<VergerResponse> changerStatut(
            @PathVariable String id,
            @RequestParam StatutVerger statut,
            @AuthenticationPrincipal UserDetails userDetails) {
        VergerResponse verger = vergerService.getById(id);
        if (!verger.getEstActif()) {
            throw new RuntimeException("Le verger doit être validé avant toute opération de récolte");
        }
        vergerService.verifierProprietaireVerger(id, userDetails);
        return ResponseEntity.ok(vergerService.changerStatut(id, statut));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<VergerResponse> valider(@PathVariable String id) {
        return ResponseEntity.ok(vergerService.valider(id));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<VergerResponse> rejeter(@PathVariable String id,
                                                  @RequestParam String motif) {
        return ResponseEntity.ok(vergerService.rejeter(id, motif));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<Void> desactiver(@PathVariable String id) {
        vergerService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}