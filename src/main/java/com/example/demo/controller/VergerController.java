package com.example.demo.controller;

import com.example.demo.dto.VergerStatutOverrideRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/api/vergers")
@RequiredArgsConstructor
public class VergerController {

    private final VergerService vergerService;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<VergerResponse> creer(
            @Valid @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(vergerService.creer(req, userDetails));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<VergerResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }

        if (isResponsable && !isAdmin) {
            vergerService.verifierResponsableVerger(id, userDetails);
        }

        return ResponseEntity.ok(vergerService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<VergerResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(vergerService.getAll());
        }

        return ResponseEntity.ok(vergerService.getByResponsable(userDetails));
    }

    @GetMapping("/agriculteur/{agriculteurId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<VergerResponse>> getByAgriculteur(
            @PathVariable String agriculteurId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAgriculteur) {
            vergerService.verifierProprietaire(agriculteurId, userDetails);
        }

        if (!isAdmin) {
            vergerService.verifierResponsablePossedeAgriculteur(
                    agriculteurId,
                    userDetails
            );
        }

        return ResponseEntity.ok(vergerService.getByAgriculteur(agriculteurId));
    }

    @GetMapping("/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<List<VergerResponse>> getByStatut(@RequestParam StatutVerger statut) {
        return ResponseEntity.ok(vergerService.getByStatut(statut));
    }

    // ── GEOLOCATION ENDPOINTS ─────────────────────────────────────────────────

    @GetMapping("/carte")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<VergerResponse>> getCarteAll() {
        return ResponseEntity.ok(vergerService.getAllWithLocation());
    }

    @GetMapping("/carte/agriculteur/{agriculteurId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<VergerResponse>> getCarteByAgriculteur(
            @PathVariable String agriculteurId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        if (isAgriculteur) {
            vergerService.verifierProprietaire(agriculteurId, userDetails);
        }

        return ResponseEntity.ok(vergerService.getByAgriculteurWithLocation(agriculteurId));
    }

    @GetMapping("/proches")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<VergerResponse>> getProches(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "10000") Double rayon) {
        return ResponseEntity.ok(vergerService.findNearby(longitude, latitude, rayon));
    }

    @PatchMapping("/{id}/localisation")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<VergerResponse> mettreAJourLocalisation(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {

        Double latitude  = body.get("latitude")  != null ? ((Number) body.get("latitude")).doubleValue()  : null;
        Double longitude = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null;
        String adresse   = body.get("adresseIndicative") != null ? body.get("adresseIndicative").toString() : null;

        return ResponseEntity.ok(vergerService.mettreAJourLocalisation(id, latitude, longitude, adresse));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR','ADMIN')")
    public ResponseEntity<VergerResponse> mettreAJour(
            @PathVariable String id,
            @Valid @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));

        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }

        if (isResponsable && !isAdmin) {
            vergerService.verifierResponsableVerger(id, userDetails);
        }

        if (!isAdmin) {
            req.setResponsableId(null);
        }

        if (isAgriculteur) {
            req.setStatut(null);
            req.setStatutOverrideReason(null);
        }

        return ResponseEntity.ok(vergerService.mettreAJour(id, req));
    }

    @PutMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VergerResponse> mettreAJourAdmin(
            @PathVariable String id,
            @Valid @RequestBody VergerRequest req) {
        return ResponseEntity.ok(vergerService.mettreAJourAdmin(id, req));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'AGRICULTEUR','ADMIN')")
    public ResponseEntity<VergerResponse> changerStatut(
            @PathVariable String id,
            @RequestBody VergerStatutOverrideRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));

        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }

        if (isResponsable && !isAdmin) {
            vergerService.verifierResponsableVerger(id, userDetails);
        }

        if (Boolean.TRUE.equals(req.getClearOverride())) {
            return ResponseEntity.ok(vergerService.clearStatutOverride(id));
        }

        if (req.getStatut() == null) {
            throw new IllegalArgumentException("Le statut est obligatoire");
        }

        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new IllegalArgumentException("La raison du changement manuel est obligatoire");
        }

        return ResponseEntity.ok(
                vergerService.changerStatut(
                        id,
                        req.getStatut(),
                        req.getReason().trim(),
                        userDetails.getUsername()
                )
        );
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<Void> desactiver(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));

        if (isResponsable && !isAdmin) {
            vergerService.verifierResponsableVerger(id, userDetails);
        }

        vergerService.desactiver(id);

        return ResponseEntity.noContent().build();
    }
}