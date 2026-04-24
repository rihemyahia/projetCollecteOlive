package com.example.demo.controller;

import com.example.demo.dto.AdminResponsableVergersResponse;
import com.example.demo.dto.AdminUpdateAgriculteurRequest;
import com.example.demo.dto.AdminUpdateResponsableRequest;
import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.VergerService;
import com.example.demo.service.impl.AuthServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/admin")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AuthServiceImpl authServiceImpl;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private VergerRepository vergerRepository;

    @Autowired
    private VergerService vergerService;

    // ========== CRUD UTILISATEURS ==========
    
    @PostMapping("/utilisateurs")
    public ResponseEntity<Map<String, Object>> creerUtilisateurParAdmin(@RequestBody Utilisateur utilisateur) {
        Map<String, Object> response = authServiceImpl.creerUtilisateurParAdmin(utilisateur);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/utilisateurs")
    public ResponseEntity<List<Utilisateur>> listerUtilisateurs() {
        List<Utilisateur> utilisateurs = authServiceImpl.listerUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<Utilisateur> trouverUtilisateurParId(@PathVariable String id) {
        Utilisateur utilisateur = authServiceImpl.trouverUtilisateurParId(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));
        return ResponseEntity.ok(utilisateur);
    }

    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<Utilisateur> mettreAJourUtilisateur(@PathVariable String id, @RequestBody Utilisateur utilisateur) {
        Utilisateur utilisateurMisAJour = authServiceImpl.mettreAJourUtilisateur(id, utilisateur);
        return ResponseEntity.ok(utilisateurMisAJour);
    }
    @PreAuthorize("hasRole('ADMIN')") // Ajoutez cette annotation
    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable String id) {
    	System.out.println("suppression de user"+id);
        authServiceImpl.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ========== GESTION DES COMPTES ==========
    
    @PostMapping("/desactiver-compte/{id}")
    public ResponseEntity<Map<String, Object>> desactiverCompte(@PathVariable String id) {
        Utilisateur desactive = authServiceImpl.desactiverCompte(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte désactivé avec succès");
        response.put("utilisateur", desactive);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/reactiver-compte/{id}")
    public ResponseEntity<Map<String, Object>> reactiverCompte(@PathVariable String id) {
        Utilisateur active = authServiceImpl.reactiverCompte(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte activé avec succès");
        response.put("utilisateur", active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/activer-compte/{id}")
    public ResponseEntity<Map<String, Object>> activerCompte(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authServiceImpl.activerCompte(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte activé avec succès");
        response.put("utilisateur", active);
        return ResponseEntity.ok(response);
    }

    // ========== ACTIVATION PAR RÔLE ==========
    @PostMapping("/changer-mot-de-passe/{id}")
    public ResponseEntity<Map<String, String>> changerMotDePasseAdmin(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String nouveauMotDePasse = request.get("nouveauMotDePasse");
        authServiceImpl.changerMotDePasseAdmin(id, nouveauMotDePasse);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mot de passe changé avec succès");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/activer-agriculteur/{id}")
    public ResponseEntity<Map<String, Object>> activerAgriculteur(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authServiceImpl.activerAgriculteur(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte agriculteur activé avec succès");
        response.put("agriculteur", active);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/activer-travailleur/{id}")
    public ResponseEntity<Map<String, Object>> activerTravailleur(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String motDePasse = request.get("nouveauMotDePasse");
        Utilisateur active = authServiceImpl.activerTravailleur(id, motDePasse);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Compte travailleur activé avec succès");
        response.put("travailleur", active);
        return ResponseEntity.ok(response);
    }

    // ========== LISTES DES UTILISATEURS EN ATTENTE ==========
    
    @GetMapping("/agriculteurs/en-attente")
    public ResponseEntity<List<Utilisateur>> getAgriculteursEnAttente() {
        return ResponseEntity.ok(authServiceImpl.getAgriculteursEnAttente());
    }

    @GetMapping("/travailleurs/en-attente")
    public ResponseEntity<List<Utilisateur>> getTravailleursEnAttente() {
        return ResponseEntity.ok(authServiceImpl.getTravailleursEnAttente());
    }

    @GetMapping("/utilisateurs/en-attente")
    public ResponseEntity<List<Utilisateur>> getTousUtilisateursEnAttente() {
        return ResponseEntity.ok(authServiceImpl.getTousUtilisateursEnAttente());
    }

    // ========== STATISTIQUES ==========
    
    @GetMapping("/stats/attente")
    public ResponseEntity<Map<String, Long>> getStatsAttente() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("agriculteursEnAttente", authServiceImpl.compterAgriculteursEnAttente());
        stats.put("travailleursEnAttente", authServiceImpl.compterTravailleursEnAttente());
        return ResponseEntity.ok(stats);
    }

    // ========== ADMIN : VERGERS ==========

    /**
     * Admin-only verger creation: admin MUST specify responsableId in the request body.
     * Reuses the existing verger creation logic.
     */
    @PostMapping("/vergers")
    public ResponseEntity<?> creerVergerParAdmin(
            @RequestBody VergerRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(vergerService.creer(req, userDetails));
    }

    // ========== ADMIN : RESPONSABLES ==========

    /**
     * Update responsable fields and optionally reassign which vergers they manage.
     *
     * If managedVergerIds is provided:
     * - Assigns those vergers to this responsable.
     * - If replaceManagedVergers=true, unassigns any other vergers currently managed by this responsable.
     */
    @PatchMapping("/responsables/{id}")
    public ResponseEntity<Utilisateur> adminUpdateResponsable(
            @PathVariable String id,
            @RequestBody AdminUpdateResponsableRequest req) {

        Utilisateur responsable = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé avec l'id: " + id));

        // Basic field updates (only if provided)
        if (req.getPrenom() != null) responsable.setPrenom(req.getPrenom());
        if (req.getNom() != null) responsable.setNom(req.getNom());
        if (req.getTelephone() != null) responsable.setTelephone(req.getTelephone());
        if (req.getAdresse() != null) responsable.setAdresse(req.getAdresse());
        if (req.getFonction() != null) responsable.setFonction(req.getFonction());
        if (req.getDatePrisePoste() != null) responsable.setDatePrisePoste(req.getDatePrisePoste());

        Utilisateur savedResponsable = utilisateurRepository.save(responsable);

        if (req.getManagedVergerIds() != null) {
            List<String> targetIds = req.getManagedVergerIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            // Assign listed vergers to this responsable
            List<com.example.demo.model.Verger> toAssign = new ArrayList<>(vergerRepository.findAllById(targetIds));
            toAssign = toAssign.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getEstSupprimer()))
                    .collect(Collectors.toList());
            toAssign.forEach(v -> v.setResponsable(savedResponsable));
            vergerRepository.saveAll(toAssign);

            boolean replace = Boolean.TRUE.equals(req.getReplaceManagedVergers());
            if (replace) {
                // Unassign any verger currently managed by this responsable but not in the new list
                List<com.example.demo.model.Verger> currentlyManaged =
                        vergerRepository.findByResponsableIdAndEstSupprimerFalse(savedResponsable.getId());

                List<com.example.demo.model.Verger> toUnassign = currentlyManaged.stream()
                        .filter(v -> v.getId() != null && !targetIds.contains(v.getId()))
                        .collect(Collectors.toList());

                toUnassign.forEach(v -> v.setResponsable(null));
                vergerRepository.saveAll(toUnassign);
            }
        }

        return ResponseEntity.ok(savedResponsable);
    }

    // ========== ADMIN : AGRICULTEURS ==========

    /**
     * Update agriculteur fields and optionally reassign which vergers they own.
     *
     * If ownedVergerIds is provided:
     * - Assigns those vergers to this agriculteur.
     * - If replaceOwnedVergers=true, unassigns any other vergers currently owned by this agriculteur.
     */
    @PatchMapping("/agriculteurs/{id}")
    public ResponseEntity<Utilisateur> adminUpdateAgriculteur(
            @PathVariable String id,
            @RequestBody AdminUpdateAgriculteurRequest req) {

        Utilisateur agriculteur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agriculteur non trouvé avec l'id: " + id));

        if (req.getPrenom() != null) agriculteur.setPrenom(req.getPrenom());
        if (req.getNom() != null) agriculteur.setNom(req.getNom());
        if (req.getTelephone() != null) agriculteur.setTelephone(req.getTelephone());
        if (req.getAdresse() != null) agriculteur.setAdresse(req.getAdresse());
        if (req.getNomExploitation() != null) agriculteur.setNomExploitation(req.getNomExploitation());

        Utilisateur savedAgriculteur = utilisateurRepository.save(agriculteur);

        if (req.getOwnedVergerIds() != null) {
            List<String> targetIds = req.getOwnedVergerIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            List<com.example.demo.model.Verger> toAssign = new ArrayList<>(vergerRepository.findAllById(targetIds));
            toAssign = toAssign.stream()
                    .filter(v -> Boolean.FALSE.equals(v.getEstSupprimer()))
                    .collect(Collectors.toList());
            toAssign.forEach(v -> v.setAgriculteur(savedAgriculteur));
            vergerRepository.saveAll(toAssign);

            boolean replace = Boolean.TRUE.equals(req.getReplaceOwnedVergers());
            if (replace) {
                List<com.example.demo.model.Verger> currentlyOwned =
                        vergerRepository.findByAgriculteurIdAndEstSupprimerFalse(savedAgriculteur.getId());

                List<com.example.demo.model.Verger> toUnassign = currentlyOwned.stream()
                        .filter(v -> v.getId() != null && !targetIds.contains(v.getId()))
                        .collect(Collectors.toList());

                toUnassign.forEach(v -> v.setAgriculteur(null));
                vergerRepository.saveAll(toUnassign);
            }
        }

        return ResponseEntity.ok(savedAgriculteur);
    }

    /**
     * Returns each RESPONSABLE with the list of vergers they manage.
     * Data source: Verger.responsable (not Utilisateur.vergers).
     */
    @GetMapping("/responsables/vergers")
    public ResponseEntity<List<AdminResponsableVergersResponse>> getResponsablesAvecVergers() {
        List<Utilisateur> responsables = utilisateurRepository.findByRole(Role.RESPONSABLE);

        // Fetch all active vergers once, then group by responsableId
        Map<String, List<VergerResponse>> vergersByResponsableId = vergerRepository.findByEstSupprimerFalse().stream()
                .filter(v -> v.getResponsable() != null && v.getResponsable().getId() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getResponsable().getId(),
                        Collectors.mapping(this::toVergerResponse, Collectors.toList())
                ));

        List<AdminResponsableVergersResponse> out = responsables.stream()
                .map(r -> AdminResponsableVergersResponse.builder()
                        .responsableId(r.getId())
                        .responsableNom(((r.getPrenom() != null ? r.getPrenom() : "") + " " + (r.getNom() != null ? r.getNom() : "")).trim())
                        .responsableEmail(r.getEmail())
                        .fonction(r.getFonction())
                        .vergers(vergersByResponsableId.getOrDefault(r.getId(), Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    private VergerResponse toVergerResponse(Verger v) {
        Utilisateur ag = v.getAgriculteur();
        Utilisateur resp = v.getResponsable();

        String agriculteurNom = ag != null
                ? (Optional.ofNullable(ag.getPrenom()).orElse("") + " " + Optional.ofNullable(ag.getNom()).orElse("")).trim()
                : null;

        String responsableNom = resp != null
                ? (Optional.ofNullable(resp.getPrenom()).orElse("") + " " + Optional.ofNullable(resp.getNom()).orElse("")).trim()
                : null;

        return VergerResponse.builder()
                .id(v.getId())
                .agriculteurId(ag != null ? ag.getId() : null)
                .agriculteurNom(agriculteurNom)
                .agriculteurEmail(ag != null ? ag.getEmail() : null)
                .responsableId(resp != null ? resp.getId() : null)
                .responsableNom(responsableNom)
                .responsableEmail(resp != null ? resp.getEmail() : null)
                .responsableFonction(resp != null ? resp.getFonction() : null)
                .superficie(v.getSuperficie())
                .typeOlive(v.getTypeOlive())
                .rendementEstime(v.getRendementEstime())
                .maturiteActuelle(v.getMaturiteActuelle())
                .nbArbre(v.getNbArbre())
                .statut(v.getStatut())
                .dateDerniereRecolte(v.getDateDerniereRecolte())
                .estSupprimer(v.getEstSupprimer())
                .dateCreation(v.getDateCreation())
                .geolocalisation(v.getGeolocalisation())
                .build();
    }
}