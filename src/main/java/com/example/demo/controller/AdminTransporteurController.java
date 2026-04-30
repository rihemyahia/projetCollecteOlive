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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
    
    Page<Tournee> disponibles = tourneeRepository.findByStatutAndTransporteurIsNull(
        StatutTournee.PLANIFIEE, pageable
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

        List<Tournee> assignees = tourneeRepository.findAll().stream()
                .filter(t -> t.getTransporteur() != null
                        && t.getTransporteur().getId() != null
                        && id.equals(t.getTransporteur().getId()))
                .sorted(Comparator.comparing(Tournee::getDateDebut, Comparator.nullsFirst(Date::compareTo)))
                .collect(Collectors.toList());

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

        List<Tournee> allTournees = new ArrayList<>(tourneeRepository.findAll());

        // Validate requested tournées can be assigned to this transporteur.
        for (Tournee t : allTournees) {
            if (!targetIds.contains(t.getId())) continue;
            if (t.getStatut() != StatutTournee.PLANIFIEE) {
                throw new RuntimeException("Seules les tournées PLANIFIEE peuvent être assignées");
            }
            if (t.getTransporteur() != null
                    && t.getTransporteur().getId() != null
                    && !id.equals(t.getTransporteur().getId())) {
                throw new RuntimeException("La tournée " + t.getCode() + " est déjà assignée à un autre transporteur");
            }
        }

        // Replace behavior:
        // - Remove all current assignments from this transporteur that are not in targetIds
        // - Assign targetIds to this transporteur
        for (Tournee t : allTournees) {
            boolean currentlyMine = t.getTransporteur() != null
                    && t.getTransporteur().getId() != null
                    && id.equals(t.getTransporteur().getId());
            boolean shouldBeMine = targetIds.contains(t.getId());

            if (currentlyMine && !shouldBeMine) {
                t.setTransporteur(null);
                tourneeRepository.save(t);
            } else if (shouldBeMine) {
                t.setTransporteur(transporteur);
                tourneeRepository.save(t);
            }
        }

        return getTourneesAssignees(id);
    }
}

