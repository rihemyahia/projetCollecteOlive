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
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
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
        if (isAgriculteur) {
            vergerService.verifierProprietaire(agriculteurId, userDetails);
        }
        return ResponseEntity.ok(vergerService.getByAgriculteur(agriculteurId));
    }

    @GetMapping("/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<List<VergerResponse>> getByStatut(@RequestParam StatutVerger statut) {
        return ResponseEntity.ok(vergerService.getByStatut(statut));
    }

    // ── GEOLOCATION ENDPOINTS ─────────────────────────────────────────────────

    /**
     * GET /api/vergers/carte
     * Returns all georeferenced vergers — for the RESPONSABLE/ADMIN map view.
     * Only returns vergers that have a location set.
     */
    @GetMapping("/carte")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN')")
    public ResponseEntity<List<VergerResponse>> getCarteAll() {
        return ResponseEntity.ok(vergerService.getAllWithLocation());
    }

    /**
     * GET /api/vergers/carte/agriculteur/{agriculteurId}
     * Returns georeferenced vergers belonging to the given agriculteur.
     * An AGRICULTEUR can only see their own vergers.
     */
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

    /**
     * GET /api/vergers/proches?longitude=10.76&latitude=34.74&rayon=5000
     * Find vergers within 'rayon' metres of the given point.
     * Default radius: 10 000 m (10 km).
     */
    @GetMapping("/proches")
    @PreAuthorize("hasAnyRole('RESPONSABLE', 'ADMIN', 'AGRICULTEUR')")
    public ResponseEntity<List<VergerResponse>> getProches(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "10000") Double rayon) {
        return ResponseEntity.ok(vergerService.findNearby(longitude, latitude, rayon));
    }

    /**
     * PATCH /api/vergers/{id}/localisation
     * Update ONLY the GPS location of a verger (no other fields changed).
     * Body: { "latitude": 34.74, "longitude": 10.76, "adresseIndicative": "..." }
     */
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
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }
        // Only ADMIN can change responsable/agriculteur relationship fields.
        if (!isAdmin) {
            req.setResponsableId(null);
        }
        return ResponseEntity.ok(vergerService.mettreAJour(id, req));
    }

    /**
     * Admin-only update: can change responsableId and agriculteurId (ownership/management).
     */
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
            @RequestParam StatutVerger statut,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAgriculteur = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGRICULTEUR"));
        if (isAgriculteur) {
            vergerService.verifierProprietaireVerger(id, userDetails);
        }
        return ResponseEntity.ok(vergerService.changerStatut(id, statut));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMIN')")
    public ResponseEntity<Void> desactiver(@PathVariable String id) {
        vergerService.desactiver(id);
        return ResponseEntity.noContent().build();
    }
}