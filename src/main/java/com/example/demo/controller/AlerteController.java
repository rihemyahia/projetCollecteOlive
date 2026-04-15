package com.example.demo.controller;

import com.example.demo.dto.AlerteRequest;
import com.example.demo.dto.AlerteResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.enums.NiveauUrgence;
import com.example.demo.model.enums.StatutAlerte;
import com.example.demo.service.AlerteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.demo.repository.UtilisateurRepository;
import java.util.List;

@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteController {

    private final AlerteService alerteService;
    private final UtilisateurRepository utilisateurRepo;
    @PostMapping
    @PreAuthorize("hasRole('AGRICULTEUR')")
    public ResponseEntity<AlerteResponse> signaler(
            @Valid @RequestBody AlerteRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        Utilisateur authenticatedUser = utilisateurRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!authenticatedUser.getId().equals(req.getAgriculteurId())) {
            throw new AccessDeniedException("Cannot create alert for another farmer");
        }
        return ResponseEntity.ok(alerteService.signalerProbleme(req));
    }
    // Helper method

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getAll() {
        return ResponseEntity.ok(alerteService.getAll());
    }

    @GetMapping("/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByStatut(@RequestParam StatutAlerte statut) {
        return ResponseEntity.ok(alerteService.getByStatut(statut));
    }

    // RESPONSABLE triage — filter by computed urgency level
    @GetMapping("/urgence")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByUrgence(@RequestParam NiveauUrgence urgence) {
        return ResponseEntity.ok(alerteService.getByUrgence(urgence));
    }

    // Cluster view — alerts within 500m of a coordinate
    @GetMapping("/proches")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getNearby(
            @RequestParam Double longitude,
            @RequestParam Double latitude) {
        return ResponseEntity.ok(alerteService.getNearbyAlerts(longitude, latitude));
    }

    @GetMapping("/verger/{vergerId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByVerger(@PathVariable String vergerId) {
        return ResponseEntity.ok(alerteService.getByVerger(vergerId));
    }

    @GetMapping("/mes-alertes/{agriculteurId}")
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getMesAlertes(
            @PathVariable String agriculteurId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        if (isAgriculteur) {
            // Get authenticated user's ID
            Utilisateur authenticatedFarmer = utilisateurRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Verify they are requesting their OWN alerts
            if (!authenticatedFarmer.getId().equals(agriculteurId)) {
                throw new AccessDeniedException("You can only view your own alerts");
            }
        }

        return ResponseEntity.ok(alerteService.getMesAlertes(agriculteurId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE', 'ADMIN')")
    public ResponseEntity<AlerteResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            alerteService.verifierProprietaireAlerte(id, userDetails);
        }
        return ResponseEntity.ok(alerteService.getById(id));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<AlerteResponse> changerStatut(
            @PathVariable String id,
            @RequestParam StatutAlerte statut) {
        return ResponseEntity.ok(alerteService.changerStatut(id, statut));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<AlerteResponse> marquerTraitee(
            @PathVariable String id,
            @RequestParam String commentaire) {
        return ResponseEntity.ok(alerteService.marquerTraitee(id, commentaire));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        alerteService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}