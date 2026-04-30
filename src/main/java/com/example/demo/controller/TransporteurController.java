package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transporteur")
@PreAuthorize("hasRole('TRANSPORTEUR')")
@CrossOrigin(origins = "http://localhost:4200")
public class TransporteurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private TourneeRepository tourneeRepository;

    /**
     * GET /api/transporteur/mes-tournees
     * Returns all tournées assigned to the logged-in transporteur.
     */
    @GetMapping("/mes-tournees")
    public ResponseEntity<?> getMesTournees(Authentication authentication) {
        try {
            String email = authentication.getName();
            Utilisateur transporteur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));

            if (transporteur.getRole() != Role.TRANSPORTEUR) {
                throw new RuntimeException("Accès refusé");
            }

            // Minimal: use in-memory filter over all tournées.
            // (We don't change repository contracts here.)
            List<Tournee> all = tourneeRepository.findAll();
            List<Tournee> mine = all.stream()
                    .filter(t -> t.getTransporteur() != null
                            && t.getTransporteur().getId() != null
                            && t.getTransporteur().getId().equals(transporteur.getId()))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "transporteurId", transporteur.getId(),
                    "tournees", mine
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/transporteur/tournees/{id}/start-livraison
     * Validate tournee.statut == TERMINEE, then set EN_LIVRAISON + livraisonStartedAt.
     */
    @PatchMapping("/tournees/{id}/start-livraison")
    public ResponseEntity<?> startLivraison(@PathVariable String id, Authentication authentication) {
        try {
            String email = authentication.getName();
            Utilisateur transporteur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));

            if (transporteur.getRole() != Role.TRANSPORTEUR) {
                throw new RuntimeException("Accès refusé");
            }

            Tournee tournee = tourneeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tournée non trouvée"));

            if (tournee.getTransporteur() == null
                    || tournee.getTransporteur().getId() == null
                    || !tournee.getTransporteur().getId().equals(transporteur.getId())) {
                throw new RuntimeException("Accès refusé: tournée non assignée à ce transporteur");
            }

            if (tournee.getStatut() != StatutTournee.TERMINEE) {
                throw new RuntimeException("Livraison non démarrable. Statut actuel: " + tournee.getStatut());
            }

            tournee.setStatut(StatutTournee.EN_LIVRAISON);
            tournee.setLivraisonStartedAt(new Date());
            tourneeRepository.save(tournee);

            return ResponseEntity.ok(Map.of(
                    "message", "Livraison démarrée",
                    "tourneeId", tournee.getId(),
                    "statut", tournee.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public static class CompleteLivraisonBody {
        private String evidenceName;
        private String evidenceBase64;

        public String getEvidenceName() { return evidenceName; }
        public void setEvidenceName(String evidenceName) { this.evidenceName = evidenceName; }
        public String getEvidenceBase64() { return evidenceBase64; }
        public void setEvidenceBase64(String evidenceBase64) { this.evidenceBase64 = evidenceBase64; }
    }

    /**
     * PATCH /api/transporteur/tournees/{id}/complete-livraison
     * Validate statut == EN_LIVRAISON, then set LIVREE + livraisonCompletedAt + evidence.
     */
    @PatchMapping("/tournees/{id}/complete-livraison")
    public ResponseEntity<?> completeLivraison(
            @PathVariable String id,
            @RequestBody CompleteLivraisonBody body,
            Authentication authentication
    ) {
        try {
            String email = authentication.getName();
            Utilisateur transporteur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Transporteur non trouvé"));

            if (transporteur.getRole() != Role.TRANSPORTEUR) {
                throw new RuntimeException("Accès refusé");
            }

            Tournee tournee = tourneeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tournée non trouvée"));

            if (tournee.getTransporteur() == null
                    || tournee.getTransporteur().getId() == null
                    || !tournee.getTransporteur().getId().equals(transporteur.getId())) {
                throw new RuntimeException("Accès refusé: tournée non assignée à ce transporteur");
            }

            if (tournee.getStatut() != StatutTournee.EN_LIVRAISON) {
                throw new RuntimeException("Livraison non terminable. Statut actuel: " + tournee.getStatut());
            }

            if (body == null
                    || body.getEvidenceName() == null || body.getEvidenceName().trim().isEmpty()
                    || body.getEvidenceBase64() == null || body.getEvidenceBase64().trim().isEmpty()) {
                throw new RuntimeException("Preuve manquante");
            }

            tournee.setLivraisonEvidenceName(body.getEvidenceName());
            tournee.setLivraisonEvidenceBase64(body.getEvidenceBase64());
            tournee.setStatut(StatutTournee.LIVREE);
            tournee.setLivraisonCompletedAt(new Date());
            tourneeRepository.save(tournee);

            return ResponseEntity.ok(Map.of(
                    "message", "Livraison terminée",
                    "tourneeId", tournee.getId(),
                    "statut", tournee.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}