package com.example.demo.controller;

import com.example.demo.dto.CollecteDetailDTO;
import com.example.demo.dto.CollecteRequest;
import com.example.demo.dto.CollecteResponse;
import com.example.demo.dto.CollecteStatsDTO;
import com.example.demo.model.Collecte;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutCollecte;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.CollecteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collectes")
@RequiredArgsConstructor
@Tag(name = "Collecte", description = "Gestion des campagnes de récolte")
public class CollecteController {

    private final CollecteService collecteService;
    private final UtilisateurRepository utilisateurRepository;
    private final VergerRepository vergerRepository;

    // ═══════════════════════════════════════════════════════════════
    // BASIC CRUD - AVEC FILTRAGE PAR RÔLE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer toutes les collectes (filtrées par rôle)")
    public ResponseEntity<List<CollecteResponse>> getAllCollectes(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        List<Collecte> collectes;
        
        if ("ADMIN".equals(user.getRole().name())) {
            // Admin: voit toutes les collectes
            collectes = collecteService.getAll();
        } else if ("RESPONSABLE".equals(user.getRole().name())) {
            // Responsable: voit uniquement les collectes des vergers qui lui sont assignés
            collectes = collecteService.getCollectesByResponsable(user.getId());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<CollecteResponse> responses = collectes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer une collecte par son ID (avec vérification des droits)")
    public ResponseEntity<CollecteResponse> getCollecteById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Collecte collecte = collecteService.getById(id);
        
        // Vérifier les droits d'accès
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            // Vérifier que le responsable a accès à ce verger
            Verger verger = vergerRepository.findById(collecte.getVergerId())
                    .orElseThrow(() -> new RuntimeException("Verger non trouvé"));
            
            if (!verger.getResponsable().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        return ResponseEntity.ok(toResponse(collecte));
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer une collecte avec toutes ses tournées")
    public ResponseEntity<CollecteDetailDTO> getCollecteWithTournees(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Collecte collecte = collecteService.getById(id);
        
        // Vérifier les droits d'accès
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            Verger verger = vergerRepository.findById(collecte.getVergerId())
                    .orElseThrow(() -> new RuntimeException("Verger non trouvé"));
            
            if (!verger.getResponsable().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        CollecteDetailDTO detail = collecteService.getCollecteWithTournees(id);
        return ResponseEntity.ok(detail);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES BY VERGER - AVEC FILTRAGE RESPONSABLE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/verger/{vergerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer toutes les collectes d'un verger")
    public ResponseEntity<List<CollecteResponse>> getCollectesByVerger(
            @PathVariable String vergerId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Vérifier les droits d'accès au verger
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            Verger verger = vergerRepository.findById(vergerId)
                    .orElseThrow(() -> new RuntimeException("Verger non trouvé"));
            
            if (!verger.getResponsable().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        List<CollecteResponse> responses = collecteService.getByVerger(vergerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/verger/{vergerId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer les collectes actives d'un verger")
    public ResponseEntity<List<CollecteResponse>> getActiveCollectesByVerger(
            @PathVariable String vergerId,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        // Vérifier les droits d'accès
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            Verger verger = vergerRepository.findById(vergerId)
                    .orElseThrow(() -> new RuntimeException("Verger non trouvé"));
            
            if (!verger.getResponsable().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        List<CollecteResponse> responses = collecteService.getByVerger(vergerId).stream()
                .filter(c -> c.getStatut() == StatutCollecte.PLANIFIEE || c.getStatut() == StatutCollecte.EN_COURS)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES BY STATUS - AVEC FILTRAGE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer les collectes par statut")
    public ResponseEntity<List<CollecteResponse>> getCollectesByStatut(
            @PathVariable StatutCollecte statut,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        List<Collecte> collectes = collecteService.getByStatut(statut);
        
        // Filtrer pour le responsable
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            collectes = collectes.stream()
                    .filter(c -> isResponsableHasAccessToCollecte(c, user.getId()))
                    .collect(Collectors.toList());
        }
        
        List<CollecteResponse> responses = collectes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer toutes les collectes actives (PLANIFIEE ou EN_COURS)")
    public ResponseEntity<List<CollecteResponse>> getActiveCollectes(
            @AuthenticationPrincipal UserDetails currentUser) {
        
        List<Collecte> collectes = collecteService.getActiveCollectes();
        
        // Filtrer pour le responsable
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            collectes = collectes.stream()
                    .filter(c -> isResponsableHasAccessToCollecte(c, user.getId()))
                    .collect(Collectors.toList());
        }
        
        List<CollecteResponse> responses = collectes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERIES BY YEAR - AVEC FILTRAGE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/annee/{annee}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    @Operation(summary = "Récupérer les collectes par année de campagne")
    public ResponseEntity<List<CollecteResponse>> getCollectesByAnnee(
            @PathVariable String annee,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        List<Collecte> collectes = collecteService.getByAnnee(annee);
        
        // Filtrer pour le responsable
        String email = currentUser.getUsername();
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if ("RESPONSABLE".equals(user.getRole().name())) {
            collectes = collectes.stream()
                    .filter(c -> isResponsableHasAccessToCollecte(c, user.getId()))
                    .collect(Collectors.toList());
        }
        
        List<CollecteResponse> responses = collectes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private boolean isResponsableHasAccessToCollecte(Collecte collecte, String responsableId) {
        Verger verger = vergerRepository.findById(collecte.getVergerId()).orElse(null);
        if (verger == null) return false;
        if (verger.getResponsable() == null) return false;
        return verger.getResponsable().getId().equals(responsableId);
    }

    private CollecteResponse toResponse(Collecte collecte) {
        return CollecteResponse.builder()
                .id(collecte.getId())
                .code(collecte.getCode())
                .statut(collecte.getStatut())
                .annee(collecte.getAnnee())
                .numero(collecte.getNumero())
                .vergerId(collecte.getVergerId())
                .dateDebutCampagne(collecte.getDateDebutCampagne())
                .dateFinCampagne(collecte.getDateFinCampagne())
                .nbreTournees(collecte.getNbreTournees())
                .quantiteTotaleKg(collecte.getQuantiteTotaleKg())
                .totalArbresRecoltes(collecte.getTotalArbresRecoltes())
                .rendementMoyenParArbre(collecte.getRendementMoyenParArbre())
                .efficaciteMoyenne(collecte.getEfficaciteMoyenne())
                .observations(collecte.getObservations())
                .estCloturee(collecte.getEstCloturee())
                .dateCreation(collecte.getDateCreation())
                .build();
    }
}