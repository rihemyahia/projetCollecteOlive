package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/transporteurs")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransporteurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private TourneeRepository tourneeRepository;

    @GetMapping("/tournees-disponibles")
    public ResponseEntity<Map<String, Object>> getTourneesDisponibles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateDebut").ascending());

        // Include both PLANIFIEE and TERMINEE so admin can assign transporteurs after harvest
        Page<Tournee> disponibles = tourneeRepository.findByStatutInAndTransporteurIsNull(
                java.util.Arrays.asList(StatutTournee.PLANIFIEE, StatutTournee.EN_COURS, StatutTournee.TERMINEE),
                pageable
        );

        Map<String, Object> response = new HashMap<>();
        response.put("content", disponibles.getContent());
        response.put("totalPages", disponibles.getTotalPages());
        response.put("totalElements", disponibles.getTotalElements());
        response.put("currentPage", disponibles.getNumber());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}/tournees")
    public ResponseEntity<Map<String, Object>> getTourneesAssignees(@PathVariable String id) {
        Utilisateur transporteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));
        if (transporteur.getRole() != Role.TRANSPORTEUR) {
            throw new RuntimeException("L'utilisateur n'est pas un transporteur");
        }

        List<Tournee> assignees = tourneeRepository.findByTransporteurId(
                id,
                Sort.by(Sort.Direction.ASC, "dateDebut")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("transporteur", transporteur);
        response.put("tournees", assignees);
        return ResponseEntity.ok(response);
    }

    public static class AssignTourneesRequest {
        private List<String> tourneesIds;
        public List<String> getTourneesIds() { return tourneesIds; }
        public void setTourneesIds(List<String> tourneesIds) { this.tourneesIds = tourneesIds; }
    }

    /**
     * REPLACES the full assignment list for this transporteur.
     */
    @PatchMapping("/{id}/tournees")
    public ResponseEntity<Map<String, Object>> replaceTourneesAssignees(
            @PathVariable String id,
            @RequestBody AssignTourneesRequest request
    ) {
        Utilisateur transporteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));
        if (transporteur.getRole() != Role.TRANSPORTEUR) {
            throw new RuntimeException("L'utilisateur n'est pas un transporteur");
        }

        Set<String> targetIds = request != null && request.getTourneesIds() != null
                ? request.getTourneesIds().stream().filter(x -> x != null && !x.isBlank()).collect(Collectors.toSet())
                : Set.of();

        List<Tournee> currentlyAssigned = tourneeRepository.findByTransporteurId(id, Sort.unsorted());
        List<Tournee> requestedTournees = new java.util.ArrayList<>();
        if (!targetIds.isEmpty()) {
            tourneeRepository.findAllById(targetIds).forEach(requestedTournees::add);
        }

        if (!targetIds.isEmpty() && requestedTournees.size() != targetIds.size()) {
            throw new RuntimeException("Certaines tournées demandées sont introuvables");
        }

        // Validate requested tournées can be assigned to this transporteur.
        for (Tournee t : requestedTournees) {
            // Allow assigning PLANIFIEE or TERMINEE so admin can assign transporteur after harvest
            if (t.getStatut() != StatutTournee.PLANIFIEE
                    && t.getStatut() != StatutTournee.EN_COURS
                    && t.getStatut() != StatutTournee.TERMINEE) {
                throw new RuntimeException("Seules les tournées PLANIFIEE, EN_COURS ou TERMINEE peuvent être assignées");
            }
            if (t.getTransporteur() != null
                    && t.getTransporteur().getId() != null
                    && !id.equals(t.getTransporteur().getId())) {
                throw new RuntimeException("La tournée " + t.getCode() + " est déjà assignée à un autre transporteur");
            }

            // Dates are required to check for time overlaps
            if (t.getDateDebut() == null || t.getDateFin() == null) {
                throw new RuntimeException("La tournée " + t.getCode() + " n'a pas de dates valides pour vérification de conflit");
            }

            // Check transporteur conflicts: ensure this transporteur has no other assigned tournées overlapping
            List<Tournee> transConflicts = tourneeRepository.findConflictsByTransporteur(
                    transporteur.getId(), t.getDateDebut(), t.getDateFin(), t.getId());
            if (!transConflicts.isEmpty()) {
                throw new RuntimeException("Le transporteur a une tournée en conflit avec " + t.getCode());
            }

            // Check benne and tracteur conflicts to avoid double-booking vehicles when reassigning
            if (t.getBenne() != null && t.getBenne().getId() != null) {
                List<Tournee> benneConflicts = tourneeRepository.findConflictsByBenne(
                        t.getBenne().getId(), t.getDateDebut(), t.getDateFin(), t.getId());
                if (!benneConflicts.isEmpty()) {
                    throw new RuntimeException("La benne est en conflit pour la tournée " + t.getCode());
                }
            }
            if (t.getTracteur() != null && t.getTracteur().getId() != null) {
                List<Tournee> tracteurConflicts = tourneeRepository.findConflictsByTracteur(
                        t.getTracteur().getId(), t.getDateDebut(), t.getDateFin(), t.getId());
                if (!tracteurConflicts.isEmpty()) {
                    throw new RuntimeException("Le tracteur est en conflit pour la tournée " + t.getCode());
                }
            }
        }

        // Replace behavior using targeted datasets:
        // - Remove current assignments not present in targetIds
        // - Assign requested tournées to this transporteur
        for (Tournee t : currentlyAssigned) {
            if (!targetIds.contains(t.getId())) {
                t.setTransporteur(null);
                tourneeRepository.save(t);
            }
        }
        for (Tournee t : requestedTournees) {
            t.setTransporteur(transporteur);
            tourneeRepository.save(t);
        }

        // After changing assignments, recompute and persist transporteur availability flag
        List<Tournee> nowAssigned = tourneeRepository.findByTransporteurId(transporteur.getId(), Sort.unsorted());
        boolean hasActive = nowAssigned.stream()
                .anyMatch(tt -> tt.getStatut() != null && tt.getStatut() != StatutTournee.LIVREE && tt.getStatut() != StatutTournee.ANNULEE);
        transporteur.setDisponibleTransport(!hasActive);
        utilisateurRepository.save(transporteur);

        return getTourneesAssignees(id);
    }
}

