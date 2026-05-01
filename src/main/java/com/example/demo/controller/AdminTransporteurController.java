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
    System.out.println("[DEBUG] GET /api/admin/transporteurs/tournees-disponibles page=" + page + " size=" + size);
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("dateDebut").ascending());

    // Compute allowed assignable statuses dynamically: everything except
    // EN_LIVRAISON, LIVREE, ANNULEE. This keeps frontend and backend in sync
    // if we later add new statuses.
    java.util.List<StatutTournee> allowed = java.util.Arrays.stream(StatutTournee.values())
            .filter(s -> s != StatutTournee.EN_LIVRAISON && s != StatutTournee.LIVREE && s != StatutTournee.ANNULEE)
            .collect(java.util.stream.Collectors.toList());

    System.out.println("[DEBUG] Allowed statuts: " + allowed);

    Page<Tournee> disponibles = tourneeRepository.findByStatutInAndTransporteurIsNull(allowed, pageable);
    
    System.out.println("[DEBUG] Found " + disponibles.getNumberOfElements() + " tournees");
    if (disponibles.getNumberOfElements() > 0) {
        System.out.println("[DEBUG] First 3 tournees:");
        for (int i = 0; i < Math.min(3, disponibles.getNumberOfElements()); i++) {
            Tournee t = disponibles.getContent().get(i);
            System.out.println("  [" + i + "] id=" + t.getId() + " statut=" + t.getStatut() + " code=" + t.getCode());
        }
    }

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

        // Debug: print ids and statuts to help diagnose status mapping issues
        for (Tournee t : assignees) {
            System.out.println("[DEBUG] Assigned tournee id=" + t.getId() + " statut=" + t.getStatut());
        }
        
        // Include EN_LIVRAISON so admin UI can show active delivery and compute date conflicts.
        // Exclude only finished / cancelled (reassignment irrelevant).
        List<Tournee> forPanel = assignees.stream()
                .filter(t -> t.getStatut() != StatutTournee.LIVREE
                          && t.getStatut() != StatutTournee.ANNULEE)
                .collect(java.util.stream.Collectors.toList());

        System.out.println("[DEBUG] Filtered from " + assignees.size() + " to " + forPanel.size() + " tournees (panel)");

        Map<String, Object> response = new HashMap<>();
        response.put("transporteur", transporteur);
        response.put("tournees", forPanel);
        return ResponseEntity.ok(response);
    }

    /** Optionnel : créneau de livraison estimé par tournée (ISO-8601 / timestamp JSON). */
    public static class LivraisonEstimationItem {
        private String tourneeId;
        private Date livraisonEstimeDebut;
        private Date livraisonEstimeFin;
        private String livraisonNotes;

        public String getTourneeId() { return tourneeId; }
        public void setTourneeId(String tourneeId) { this.tourneeId = tourneeId; }
        public Date getLivraisonEstimeDebut() { return livraisonEstimeDebut; }
        public void setLivraisonEstimeDebut(Date livraisonEstimeDebut) { this.livraisonEstimeDebut = livraisonEstimeDebut; }
        public Date getLivraisonEstimeFin() { return livraisonEstimeFin; }
        public void setLivraisonEstimeFin(Date livraisonEstimeFin) { this.livraisonEstimeFin = livraisonEstimeFin; }
        public String getLivraisonNotes() { return livraisonNotes; }
        public void setLivraisonNotes(String livraisonNotes) { this.livraisonNotes = livraisonNotes; }
    }

    public static class AssignTourneesRequest {
        private List<String> tourneesIds;
        private List<LivraisonEstimationItem> livraisonEstimations;

        public List<String> getTourneesIds() { return tourneesIds; }
        public void setTourneesIds(List<String> tourneesIds) { this.tourneesIds = tourneesIds; }
        public List<LivraisonEstimationItem> getLivraisonEstimations() { return livraisonEstimations; }
        public void setLivraisonEstimations(List<LivraisonEstimationItem> livraisonEstimations) {
            this.livraisonEstimations = livraisonEstimations;
        }
    }

    /**
     * REPLACES the full assignment list for this transporteur.
     */
    @PatchMapping("/{id}/tournees")
    public ResponseEntity<Map<String, Object>> replaceTourneesAssignees(
            @PathVariable String id,
            @RequestBody AssignTourneesRequest request
    ) {
        System.out.println("[DEBUG] ===== PATCH /api/admin/transporteurs/{id}/tournees =====");
        System.out.println("[DEBUG] Transporteur ID: " + id);

        Utilisateur transporteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));
        if (transporteur.getRole() != Role.TRANSPORTEUR) {
            throw new RuntimeException("L'utilisateur n'est pas un transporteur");
        }

        Set<String> targetIds = request != null && request.getTourneesIds() != null
                ? request.getTourneesIds().stream().filter(x -> x != null && !x.isBlank()).collect(Collectors.toSet())
                : Set.of();
        
        System.out.println("[DEBUG] Target IDs after filtering: " + targetIds);

        List<Tournee> currentlyAssigned = tourneeRepository.findByTransporteurId(id, Sort.unsorted());
        List<Tournee> requestedTournees = new java.util.ArrayList<>();
        if (!targetIds.isEmpty()) {
            tourneeRepository.findAllById(targetIds).forEach(requestedTournees::add);
        }
        
        System.out.println("[DEBUG] Found " + requestedTournees.size() + " tournees from DB");

        if (!targetIds.isEmpty() && requestedTournees.size() != targetIds.size()) {
            System.out.println("[DEBUG] ERROR: Requested " + targetIds.size() + " but found " + requestedTournees.size());
            throw new RuntimeException("Certaines tournées demandées sont introuvables");
        }

        // Validate requested tournées can be assigned to this transporteur.
        java.util.Set<StatutTournee> allowedSet = java.util.Arrays.stream(StatutTournee.values())
                .filter(s -> s != StatutTournee.EN_LIVRAISON && s != StatutTournee.LIVREE && s != StatutTournee.ANNULEE)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, LivraisonEstimationItem> estimationByTourneeId = new HashMap<>();
        if (request != null && request.getLivraisonEstimations() != null) {
            for (LivraisonEstimationItem item : request.getLivraisonEstimations()) {
                if (item != null && item.getTourneeId() != null && !item.getTourneeId().isBlank()) {
                    estimationByTourneeId.put(item.getTourneeId(), item);
                }
            }
        }

        for (Tournee t : requestedTournees) {
            System.out.println("[DEBUG] Requested tournee id=" + t.getId() + " statut=" + t.getStatut());
            StatutTournee st = t.getStatut();
            if (st == null || !allowedSet.contains(st)) {
                throw new RuntimeException("Tournée non assignable (id=" + t.getId() + ", statut=" + st + ") - seules les tournées PLANIFIEE, EN_COURS ou TERMINEE peuvent être assignées");
            }
            if (t.getTransporteur() != null
                    && t.getTransporteur().getId() != null
                    && !id.equals(t.getTransporteur().getId())) {
                throw new RuntimeException("La tournée " + t.getCode() + " est déjà assignée à un autre transporteur");
            }

            if (t.getDateDebut() == null || t.getDateFin() == null) {
                throw new RuntimeException("La tournée " + t.getCode() + " n'a pas de dates valides pour vérification de conflit");
            }

            LivraisonEstimationItem est = estimationByTourneeId.get(t.getId());
            if (est != null) {
                Date ed = est.getLivraisonEstimeDebut();
                Date ef = est.getLivraisonEstimeFin();
                if ((ed != null) != (ef != null)) {
                    throw new RuntimeException("Créneau livraison incomplet pour " + t.getCode() + " (début et fin requis ensemble)");
                }
                if (ed != null && ef != null) {
                    if (!ef.after(ed)) {
                        throw new RuntimeException("Créneau livraison invalide pour " + t.getCode() + " (la fin doit être après le début)");
                    }
                    t.setLivraisonEstimeDebut(ed);
                    t.setLivraisonEstimeFin(ef);
                }
                if (est.getLivraisonNotes() != null) {
                    t.setLivraisonNotes(est.getLivraisonNotes());
                }
            }

            // Benne / tracteur : toujours sur la période de récolte (ressources sur le terrain)
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

        // Conflit transporteur : chevauchement des créneaux de livraison estimés (sinon période tournée)
        for (int i = 0; i < requestedTournees.size(); i++) {
            for (int j = i + 1; j < requestedTournees.size(); j++) {
                if (deliveryWindowsOverlap(requestedTournees.get(i), requestedTournees.get(j))) {
                    throw new RuntimeException("Créneaux de livraison en conflit pour le transporteur entre "
                            + requestedTournees.get(i).getCode() + " et " + requestedTournees.get(j).getCode());
                }
            }
        }

        // Replace behavior using targeted datasets:
        // - Remove current assignments not present in targetIds
        // - Assign requested tournées to this transporteur
        for (Tournee t : currentlyAssigned) {
            if (!targetIds.contains(t.getId())) {
                t.setTransporteur(null);
                t.setLivraisonEstimeDebut(null);
                t.setLivraisonEstimeFin(null);
                t.setLivraisonNotes(null);
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

    private static Date deliveryWindowStart(Tournee t) {
        if (t.getLivraisonEstimeDebut() != null && t.getLivraisonEstimeFin() != null) {
            return t.getLivraisonEstimeDebut();
        }
        return t.getDateDebut();
    }

    private static Date deliveryWindowEnd(Tournee t) {
        if (t.getLivraisonEstimeDebut() != null && t.getLivraisonEstimeFin() != null) {
            return t.getLivraisonEstimeFin();
        }
        return t.getDateFin();
    }

    /** Interval overlap (strict) for [start, end) style; works with inclusive intervals if end.after(start). */
    private static boolean deliveryWindowsOverlap(Tournee a, Tournee b) {
        Date a0 = deliveryWindowStart(a);
        Date a1 = deliveryWindowEnd(a);
        Date b0 = deliveryWindowStart(b);
        Date b1 = deliveryWindowEnd(b);
        if (a0 == null || a1 == null || b0 == null || b1 == null) {
            return false;
        }
        if (!a1.after(a0) || !b1.after(b0)) {
            return false;
        }
        return a0.before(b1) && b0.before(a1);
    }
}

