package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private CloudinaryService cloudinaryService;

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

            List<Tournee> mine = tourneeRepository.findByTransporteurId(
                    transporteur.getId(),
                    Sort.by(Sort.Direction.ASC, "dateDebut")
            );

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

            // mark transporteur unavailable while delivering
            transporteur.setDisponibleTransport(false);
            utilisateurRepository.save(transporteur);

            return ResponseEntity.ok(Map.of(
                    "message", "Livraison démarrée",
                    "tourneeId", tournee.getId(),
                    "statut", tournee.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/transporteur/tournees/{id}/complete-livraison
     * Validate statut == EN_LIVRAISON, then set LIVREE + livraisonCompletedAt + Cloudinary evidence.
     */
    @PatchMapping(value = "/tournees/{id}/complete-livraison", consumes = {"multipart/form-data"})
    public ResponseEntity<?> completeLivraison(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file,
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

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Preuve manquante");
            }

            // Keep Cloudinary behavior isolated from existing shared service contracts.
            String evidenceUrl = cloudinaryService.uploadAlertImage("livraisons/" + id, file);
            tournee.setLivraisonEvidenceName(file.getOriginalFilename());
            tournee.setLivraisonEvidenceUrl(evidenceUrl);
            tournee.setLivraisonEvidenceBase64(null);
            tournee.setStatut(StatutTournee.LIVREE);
            tournee.setLivraisonCompletedAt(new Date());
            tourneeRepository.save(tournee);

                // After completing, recompute transporteur availability: available if no other non-delivered/active tournees
                List<Tournee> assigned = tourneeRepository.findByTransporteurId(transporteur.getId(), Sort.unsorted());
                boolean hasActive = assigned.stream()
                    .anyMatch(tt -> tt.getStatut() != null && tt.getStatut() != StatutTournee.LIVREE && tt.getStatut() != StatutTournee.ANNULEE);
                transporteur.setDisponibleTransport(!hasActive);
                utilisateurRepository.save(transporteur);

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