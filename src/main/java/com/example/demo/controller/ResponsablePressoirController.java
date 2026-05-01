package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/responsables-pressoir")
@CrossOrigin(origins = "http://localhost:4200")
public class ResponsablePressoirController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utilisateur> createResponsablePressoir(@RequestBody Utilisateur responsable) {
        responsable.setRole(Role.RESPONSABLE_PRESSOIR);
        responsable.setEstActif(true);
        responsable.setCompteActif(true);
        responsable.setDateCreation(new Date());
        responsable.setEstSupprime(false);

        if (responsable.getPressoir() != null) {
            if (responsable.getPressoir().getId() == null) {
                responsable.getPressoir().setId(UUID.randomUUID().toString());
            }
            responsable.getPressoir().setActif(true);
            responsable.getPressoir().setDateCreation(new Date());
        }

        if (responsable.getDisponible() == null) {
            responsable.setDisponible(true);
        }

        if (responsable.getDateAffectation() == null) {
            responsable.setDateAffectation(new Date());
        }

        Utilisateur saved = utilisateurRepository.save(responsable);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRESSOIR')")
    public ResponseEntity<List<Utilisateur>> getAllResponsablesPressoir() {
        List<Utilisateur> responsables = utilisateurRepository.findByRole(Role.RESPONSABLE_PRESSOIR);
        return ResponseEntity.ok(responsables);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRESSOIR')")
    public ResponseEntity<Utilisateur> getResponsablePressoirById(@PathVariable String id) {
        Utilisateur responsable = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé"));

        if (responsable.getRole() != Role.RESPONSABLE_PRESSOIR) {
            throw new RuntimeException("Cet utilisateur n'est pas un responsable de pressoir");
        }

        return ResponseEntity.ok(responsable);
    }

    @PatchMapping("/{id}/pressoir")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRESSOIR')")
    public ResponseEntity<Utilisateur> updatePressoir(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {

        Utilisateur responsable = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé"));

        if (responsable.getRole() != Role.RESPONSABLE_PRESSOIR) {
            throw new RuntimeException("Cet utilisateur n'est pas un responsable de pressoir");
        }

        if (responsable.getPressoir() == null) {
            responsable.setPressoir(new com.example.demo.model.Pressoir());
            responsable.getPressoir().setId(UUID.randomUUID().toString());
        }

        if (updates.containsKey("nom")) {
            responsable.getPressoir().setNom((String) updates.get("nom"));
        }
        if (updates.containsKey("adresse")) {
            responsable.getPressoir().setAdresse((String) updates.get("adresse"));
        }
        if (updates.containsKey("telephone")) {
            responsable.getPressoir().setTelephone((String) updates.get("telephone"));
        }
        if (updates.containsKey("email")) {
            responsable.getPressoir().setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("capaciteJournaliere")) {
            responsable.getPressoir().setCapaciteJournaliere((String) updates.get("capaciteJournaliere"));
        }
        if (updates.containsKey("horaires")) {
            responsable.getPressoir().setHoraires((String) updates.get("horaires"));
        }
        if (updates.containsKey("geolocalisation")) {
            responsable.getPressoir().setGeolocalisation((com.example.demo.model.Geolocalisation) updates.get("geolocalisation"));
        }

        return ResponseEntity.ok(utilisateurRepository.save(responsable));
    }

    @PatchMapping("/{id}/disponibilite")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRESSOIR')")
    public ResponseEntity<Utilisateur> updateDisponibilite(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> updates) {

        Utilisateur responsable = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé"));

        if (updates.containsKey("disponible")) {
            responsable.setDisponible(updates.get("disponible"));
            if (responsable.getPressoir() != null) {
                responsable.getPressoir().setActif(updates.get("disponible"));
            }
        }

        return ResponseEntity.ok(utilisateurRepository.save(responsable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteResponsablePressoir(@PathVariable String id) {
        Utilisateur responsable = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé"));

        responsable.setEstSupprime(true);
        responsable.setEstActif(false);
        responsable.setCompteActif(false);
        responsable.setDisponible(false);

        if (responsable.getPressoir() != null) {
            responsable.getPressoir().setActif(false);
        }

        utilisateurRepository.save(responsable);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Responsable de pressoir supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE')")
    public ResponseEntity<List<Utilisateur>> getResponsablesDisponibles() {
        List<Utilisateur> responsables = utilisateurRepository.findByRole(Role.RESPONSABLE_PRESSOIR);

        List<Utilisateur> disponibles = responsables.stream()
                .filter(r -> r.getDisponible() != null && r.getDisponible())
                .filter(r -> r.getEstActif() != null && r.getEstActif())
                .filter(r -> !r.isEstSupprime())
                .toList();

        return ResponseEntity.ok(disponibles);
    }
}