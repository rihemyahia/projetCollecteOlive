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
    public ResponseEntity<List<AlerteResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(alerteService.getAll());  // ADMIN sees all alerts
        }

        // RESPONSABLE sees only alerts from vergers they manage
        return ResponseEntity.ok(alerteService.getByResponsable(userDetails));
    }

    @GetMapping("/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByStatut(
            @RequestParam StatutAlerte statut,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(alerteService.getByStatut(statut));
        }
        return ResponseEntity.ok(alerteService.getByStatutAndResponsable(statut, userDetails));
    }

    // RESPONSABLE triage — filter by computed urgency level
    @GetMapping("/urgence")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByUrgence(
            @RequestParam NiveauUrgence urgence,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(alerteService.getByUrgence(urgence));
        }
        return ResponseEntity.ok(alerteService.getByUrgenceAndResponsable(urgence, userDetails));
    }

    // Cluster view — alerts within 500m of a coordinate
    @GetMapping("/proches")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getNearby(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(alerteService.getNearbyAlerts(longitude, latitude));
        }
        return ResponseEntity.ok(alerteService.getNearbyAlertsForResponsable(longitude, latitude, userDetails));
    }

    @GetMapping("/verger/{vergerId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<AlerteResponse>> getByVerger(
            @PathVariable String vergerId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // Verify responsable owns this verger
            alerteService.verifyResponsableOwnsVerger(vergerId, userDetails);
        }
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

    @PatchMapping("/{id}/urgence")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<AlerteResponse> changerUrgence(
            @PathVariable String id,
            @RequestParam NiveauUrgence urgence,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(alerteService.changerUrgenceForResponsable(id, urgence, userDetails));
        }
        return ResponseEntity.ok(alerteService.changerUrgenceForResponsable(id, urgence, userDetails));
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

    // ═══════════════════════════════════════════════════════════════════════
    // RESPONSABLE-SPECIFIC ALERT MANAGEMENT ENDPOINTS
    // These endpoints allow responsables to see and manage alerts from their vergers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/alertes/responsable
     * Responsable sees all alerts from their managed vergers
     */
    @GetMapping("/responsable")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<List<AlerteResponse>> getAlertesResponsable(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.getByResponsable(userDetails));
    }

    /**
     * GET /api/alertes/responsable/{id}
     * Responsable sees detail of a specific alert (only if from their verger)
     * Returns 403 if they don't own the alert's verger
     */
    @GetMapping("/responsable/{id}")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<AlerteResponse> getAlertDetailResponsable(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Verify responsable owns this alert before showing details
        alerteService.verifyResponsableOwnsAlert(id, userDetails);
        return ResponseEntity.ok(alerteService.getById(id));
    }

    /**
     * GET /api/alertes/responsable/statut?statut=EN_ATTENTE
     * Responsable filters alerts by status (only from their vergers)
     */
    @GetMapping("/responsable/statut")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<List<AlerteResponse>> getAlertesResponsableByStatut(
            @RequestParam StatutAlerte statut,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.getByStatutAndResponsable(statut, userDetails));
    }

    /**
     * GET /api/alertes/responsable/urgence?urgence=CRITIQUE
     * Responsable filters alerts by urgency level (only from their vergers)
     */
    @GetMapping("/responsable/urgence")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<List<AlerteResponse>> getAlertesResponsableByUrgence(
            @RequestParam NiveauUrgence urgence,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.getByUrgenceAndResponsable(urgence, userDetails));
    }

    /**
     * PATCH /api/alertes/responsable/{id}/statut?statut=EN_COURS
     * Responsable changes alert status (EN_ATTENTE → EN_COURS → TRAITEE)
     * Returns 403 if they don't own the alert's verger
     */
    @PatchMapping("/responsable/{id}/statut")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<AlerteResponse> changerStatutResponsable(
            @PathVariable String id,
            @RequestParam StatutAlerte statut,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.changerStatutForResponsable(id, statut, userDetails));
    }

    /**
     * PATCH /api/alertes/responsable/{id}/urgence?urgence=CRITIQUE
     * Responsable changes alert urgency level (only from their vergers)
     * Returns 403 if they don't own the alert's verger
     */
    @PatchMapping("/responsable/{id}/urgence")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<AlerteResponse> changerUrgenceResponsable(
            @PathVariable String id,
            @RequestParam NiveauUrgence urgence,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.changerUrgenceForResponsable(id, urgence, userDetails));
    }

    /**
     * PATCH /api/alertes/responsable/{id}/traiter?commentaire=Problème résolu
     * Responsable marks alert as treated with treatment comment
     * Returns 403 if they don't own the alert's verger
     */
    @PatchMapping("/responsable/{id}/traiter")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<AlerteResponse> marquerTraiteeResponsable(
            @PathVariable String id,
            @RequestParam String commentaire,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alerteService.marquerTraiteeForResponsable(id, commentaire, userDetails));
    }
}