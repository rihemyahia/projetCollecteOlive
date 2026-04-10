package com.example.demo.controller;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
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

    // Only RESPONSABLE can create vergers
    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<VergerResponse> creer(@Valid @RequestBody VergerRequest req) {
        return ResponseEntity.ok(vergerService.creer(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<VergerResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }
        return ResponseEntity.ok(vergerService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")    public ResponseEntity<List<VergerResponse>> getAll() {
        return ResponseEntity.ok(vergerService.getAll());
    }

    // Agriculteur sees only their own; RESPONSABLE sees any
    @GetMapping("/agriculteur/{agriculteurId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<VergerResponse>> getByAgriculteur(
            @PathVariable String agriculteurId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(agriculteurId, userDetails);
        }        return ResponseEntity.ok(vergerService.getByAgriculteur(agriculteurId));
    }

    @GetMapping("/statut")

    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<List<VergerResponse>> getByStatut(@RequestParam StatutVerger statut) {
        return ResponseEntity.ok(vergerService.getByStatut(statut));
    }

    // RESPONSABLE or owner AGRICULTEUR can update
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR','ADMIN')")
    public ResponseEntity<VergerResponse> mettreAJour(
            @PathVariable String id,
            @Valid @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }        return ResponseEntity.ok(vergerService.mettreAJour(id, req));
    }

    // RESPONSABLE or owner AGRICULTEUR can change statut
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR','ADMIN')")
    public ResponseEntity<VergerResponse> changerStatut(
            @PathVariable String id,
            @RequestParam StatutVerger statut,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }        return ResponseEntity.ok(vergerService.changerStatut(id, statut));
    }

    // Only RESPONSABLE can soft-delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<Void> desactiver(@PathVariable String id) {
        vergerService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}