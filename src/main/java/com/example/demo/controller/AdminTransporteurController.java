package com.example.demo.controller;

import com.example.demo.dto.TourneeResponse;
import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.model.Verger;
import com.example.demo.service.TourneeAssignListPressoirEnricher;
import com.example.demo.service.TourneeDisponiblesQueryService;
import com.example.demo.service.TourneeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.bson.types.ObjectId;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/transporteurs")
@PreAuthorize("hasAnyRole('ADMIN','RESPONSABLE')")
public class AdminTransporteurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private TourneeRepository tourneeRepository;

    @Autowired
    private VergerRepository vergerRepository;

    @Autowired
    private TourneeService tourneeService;

    @Autowired
    private TourneeAssignListPressoirEnricher tourneeAssignListPressoirEnricher;

    @Autowired
    private TourneeDisponiblesQueryService tourneeDisponiblesQueryService;

    private Utilisateur currentAuthenticatedUtilisateur() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return utilisateurRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    private static boolean ownsVergerTournee(Tournee t, String responsableTerrainId) {
        if (t == null || t.getVerger() == null || responsableTerrainId == null || responsableTerrainId.isBlank()) {
            return false;
        }
        Verger v = t.getVerger();
        Utilisateur r = v.getResponsable();
        return r != null && responsableTerrainId.equals(r.getId());
    }

    private static String fmtDeliveryWindow(Date d) {
        if (d == null) {
            return "?";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(d);
    }

    /**
     * Liste des transporteurs pour l’assignation des tournées — réservé à l’ADMIN.
     * Les responsables terrain utilisent {@code GET /api/responsable/transporteurs}.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Utilisateur>> listTransporteursPourAssignationAdmin() {
        List<Utilisateur> transporteurs = utilisateurRepository.findByRole(Role.TRANSPORTEUR);
        return ResponseEntity.ok(transporteurs);
    }
    @GetMapping("/tournees-disponibles")
    public ResponseEntity<Map<String, Object>> getTourneesDisponibles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String q
    ) {
        Utilisateur actor = currentAuthenticatedUtilisateur();

        List<String> vergerIds = null;
        if (actor.getRole() == Role.RESPONSABLE) {
            List<Verger> vergers = vergerRepository.findByResponsableId(actor.getId());
            vergerIds = vergers.stream()
                    .filter(v -> v != null && !Boolean.TRUE.equals(v.getEstSupprimer()))
                    .map(Verger::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (vergerIds.isEmpty()) {
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("content", List.of());
                emptyResponse.put("totalPages", 0);
                emptyResponse.put("totalElements", 0);
                emptyResponse.put("currentPage", 0);
                return ResponseEntity.ok(emptyResponse);
            }
        }

        List<TourneeResponse> allDisponibles = tourneeService.getTourneesDisponiblesPourTransporteur(
                vergerIds, year, q);

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allDisponibles.size());
        List<TourneeResponse> paged = start < allDisponibles.size()
                ? allDisponibles.subList(start, end)
                : List.of();

        Map<String, Object> response = new HashMap<>();
        response.put("content", paged);
        response.put("totalPages", (int) Math.ceil((double) allDisponibles.size() / size));
        response.put("totalElements", allDisponibles.size());
        response.put("currentPage", page);

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
                .filter(t -> t.getStatut() != StatutTournee.ANNULEE)
                .collect(java.util.stream.Collectors.toList());

        System.out.println("[DEBUG] Filtered from " + assignees.size() + " to " + forPanel.size() + " tournees (panel)");

        List<TourneeResponse> tourneesLight = forPanel.stream()
                .map(tourneeService::toResponseForTransporteurAssignList)
                .collect(Collectors.toList());
        tourneeAssignListPressoirEnricher.enrichPressoirDisplayFields(tourneesLight, forPanel);

        Map<String, Object> response = new HashMap<>();
        response.put("transporteur", transporteur);
        response.put("tournees", tourneesLight);
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

        Utilisateur actor = currentAuthenticatedUtilisateur();
        Utilisateur transporteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));
        if (transporteur.getRole() != Role.TRANSPORTEUR) {
            throw new RuntimeException("L'utilisateur n'est pas un transporteur");
        }

        Set<String> bodyIds = request != null && request.getTourneesIds() != null
                ? request.getTourneesIds().stream().filter(x -> x != null && !x.isBlank()).collect(Collectors.toSet())
                : Set.of();

        List<Tournee> currentlyAssigned = tourneeRepository.findByTransporteurId(id, Sort.unsorted());

        Set<String> lockedTourneeIds = new java.util.HashSet<>();
        if (actor.getRole() == Role.RESPONSABLE) {
            for (Tournee ct : currentlyAssigned) {
                if (!ownsVergerTournee(ct, actor.getId())) {
                    lockedTourneeIds.add(ct.getId());
                }
            }
        }

        Set<String> targetIds = new java.util.HashSet<>(bodyIds);
        targetIds.addAll(lockedTourneeIds);

        System.out.println("[DEBUG] Body IDs: " + bodyIds + ", effective target IDs (avec verrous responsable): " + targetIds);

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

        java.util.Set<String> previouslyAssignedIds = currentlyAssigned.stream()
                .map(Tournee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean adminActor = actor.getRole() == Role.ADMIN;

        for (Tournee t : requestedTournees) {
            System.out.println("[DEBUG] Requested tournee id=" + t.getId() + " statut=" + t.getStatut());
            StatutTournee st = t.getStatut();
            boolean gardeLivraisonEnCoursMemeTransporteur =
                    st == StatutTournee.EN_LIVRAISON && previouslyAssignedIds.contains(t.getId());
            if (st == null || (!allowedSet.contains(st) && !gardeLivraisonEnCoursMemeTransporteur)) {
                throw new RuntimeException("La tournée " + (t.getCode() != null ? t.getCode() : t.getId())
                        + " ne peut pas être assignée ou conservée dans cette liste : statut « " + st + " ». "
                        + "Seules les tournées planifiées, en cours de collecte ou terminées (collecte) peuvent être assignées ; "
                        + "une tournée déjà en livraison sur ce transporteur peut rester dans la liste tant que la livraison n’est pas terminée.");
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

            boolean newlyAssigned = !previouslyAssignedIds.contains(t.getId());
            if (!adminActor && newlyAssigned && !ownsVergerTournee(t, actor.getId())) {
                throw new RuntimeException("Vous ne pouvez pas assigner la tournée " + t.getCode()
                        + " : le verger est hors de votre périmètre.");
            }
            if (newlyAssigned && (t.getLivraisonEstimeDebut() == null || t.getLivraisonEstimeFin() == null)) {
                throw new RuntimeException("Créneau de livraison obligatoire (début et fin) pour l’assignation de la tournée "
                        + t.getCode()
                        + ". Les horaires du créneau servent à détecter les chevauchements pour le transporteur.");
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
                Tournee ti = requestedTournees.get(i);
                Tournee tj = requestedTournees.get(j);
                if (deliveryWindowsOverlap(ti, tj)) {
                    throw new RuntimeException(String.format(
                            "Chevauchement des créneaux de livraison pour ce transporteur : la tournée %s (%s → %s)"
                                    + " chevauche la tournée %s (%s → %s). Ajustez les horaires du créneau de livraison.",
                            ti.getCode(),
                            fmtDeliveryWindow(deliveryWindowStart(ti)),
                            fmtDeliveryWindow(deliveryWindowEnd(ti)),
                            tj.getCode(),
                            fmtDeliveryWindow(deliveryWindowStart(tj)),
                            fmtDeliveryWindow(deliveryWindowEnd(tj))));
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

