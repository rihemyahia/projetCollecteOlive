package com.example.demo.service.impl;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.VergerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VergerServiceImpl implements VergerService {

    private final VergerRepository vergerRepo;
    private final UtilisateurRepository utilisateurRepo;

    @Override
    public VergerResponse creer(VergerRequest req) {
        Utilisateur agriculteur = getAgriculteur(req.getProprietaireId());

        Verger verger = Verger.builder()
                .nom(req.getNom())
                .proprietaireId(req.getProprietaireId())
                .superficie(req.getSuperficie())
                .typeOlive(req.getTypeOlive())
                .rendementEstime(req.getRendementEstime())
                .maturiteActuelle(req.getMaturiteActuelle())
                .supprimer(false)
                .statut(req.getStatut() != null ? req.getStatut() : StatutVerger.NON_RECOLTE)

                .estActif(false)
                .dateCreation(new Date())
                .build();

        return toResponse(vergerRepo.save(verger), agriculteur);
    }

    @Override
    public VergerResponse getById(String id) {
        Verger v = findOrThrow(id);
        return toResponse(v, getAgriculteur(v.getProprietaireId()));
    }

    @Override
    public List<VergerResponse> getAll() {
        return vergerRepo.findByEstActifTrueAndSupprimerFalse().stream()
                .map(v -> toResponse(v, getAgriculteur(v.getProprietaireId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<VergerResponse> getByAgriculteur(String proprietaireId) {
        Utilisateur ag = getAgriculteur(proprietaireId);
        return vergerRepo.findByProprietaireIdAndEstActifTrueAndSupprimerFalse(proprietaireId)
                .stream().map(v -> toResponse(v, ag))
                .collect(Collectors.toList());
    }

    @Override
    public List<VergerResponse> getByStatut(StatutVerger statut) {
        return vergerRepo.findByStatut(statut).stream()
                .map(v -> toResponse(v, getAgriculteur(v.getProprietaireId())))
                .collect(Collectors.toList());
    }

    @Override
    public VergerResponse mettreAJour(String id, VergerRequest req) {
        Verger v = findOrThrow(id);
        v.setNom(req.getNom());
        v.setSuperficie(req.getSuperficie());
        v.setTypeOlive(req.getTypeOlive());
        v.setRendementEstime(req.getRendementEstime());
        v.setMaturiteActuelle(req.getMaturiteActuelle());
        if (req.getStatut() != null) v.setStatut(req.getStatut());
        return toResponse(vergerRepo.save(v), getAgriculteur(v.getProprietaireId()));
    }

    @Override
    public VergerResponse changerStatut(String id, StatutVerger statut) {
        Verger v = findOrThrow(id);
        v.setStatut(statut);
        if (statut == StatutVerger.RECOLTE) v.setDateDerniereRecolte(new Date());
        return toResponse(vergerRepo.save(v), getAgriculteur(v.getProprietaireId()));
    }

    @Override
    public void desactiver(String id) {
        Verger v = findOrThrow(id);
        v.setSupprimer(true);  // soft-delete
        vergerRepo.save(v);
    }

    // ── helpers ──────────────────────────────────────────────

    private Verger findOrThrow(String id) {
        Verger v = vergerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + id));

        if (Boolean.TRUE.equals(v.getSupprimer())) {  // <--- add this check
            throw new ResourceNotFoundException("Verger introuvable (supprimé) : " + id);
        }
        return v;
    }

    private Utilisateur getAgriculteur(String id) {
        return utilisateurRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agriculteur introuvable : " + id));
    }

    private VergerResponse toResponse(Verger v, Utilisateur ag) {
        return VergerResponse.builder()
                .id(v.getId())
                .nom(v.getNom())
                .proprietaireId(v.getProprietaireId())
                .proprietaireNom(ag.getPrenom() + " " + ag.getNom())
                .superficie(v.getSuperficie())
                .typeOlive(v.getTypeOlive())
                .rendementEstime(v.getRendementEstime())
                .maturiteActuelle(v.getMaturiteActuelle())
                .statut(v.getStatut())
                .motifRejet(v.getMotifRejet())
                .dateDerniereRecolte(v.getDateDerniereRecolte())
                .estActif(v.getEstActif())
                .supprimer(v.getSupprimer())
                .dateCreation(v.getDateCreation())
                .build();
    }
    @Override
    public void verifierProprietaire(String agriculteurId, UserDetails userDetails) {
        // RESPONSABLE can act on any agriculteur — skip check
        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));
        if (isResponsable) return;

        // For AGRICULTEUR: the logged-in user's email must match the agriculteur's email
        Utilisateur agriculteur = getUtilisateurOrThrow(agriculteurId);
        if (!agriculteur.getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous ne pouvez pas agir pour un autre agriculteur");
        }
    }

    @Override
    public void verifierProprietaireVerger(String vergerId, UserDetails userDetails) {
        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));
        if (isResponsable) return;

        Verger v = findOrThrow(vergerId);
        Utilisateur agriculteur = getUtilisateurOrThrow(v.getProprietaireId());
        if (!agriculteur.getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous n'êtes pas le propriétaire de ce verger");
        }
    }
    @Override
    public List<VergerResponse> getEnAttente() {
        return vergerRepo.findByEstActifFalseAndSupprimerFalse().stream()
                .map(v -> toResponse(v, getAgriculteur(v.getProprietaireId())))
                .collect(Collectors.toList());
    }
    @Override
    public VergerResponse valider(String id) {
        Verger v = findOrThrow(id);
        v.setEstActif(true);
        v.setMotifRejet(null);
        return toResponse(vergerRepo.save(v), getAgriculteur(v.getProprietaireId()));

    }
    @Override
    public VergerResponse rejeter(String id, String motif) {
        Verger v = findOrThrow(id);
        v.setEstActif(false);


        v.setMotifRejet(motif);
        Verger saved = vergerRepo.save(v);

        return toResponse(vergerRepo.save(v), getAgriculteur(v.getProprietaireId()));
    }
    private Utilisateur getUtilisateurOrThrow(String id) {
        return utilisateurRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}