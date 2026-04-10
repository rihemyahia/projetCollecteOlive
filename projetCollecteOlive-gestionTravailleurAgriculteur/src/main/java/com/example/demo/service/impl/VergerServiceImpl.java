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
import org.bson.types.ObjectId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VergerServiceImpl implements VergerService {

    private final VergerRepository vergerRepo;
    private final UtilisateurRepository utilisateurRepo;

    @Override
    public VergerResponse creer(VergerRequest req) {
        Utilisateur agriculteur = getAgriculteurOrThrow(req.getAgriculteurId());

        Verger verger = Verger.builder()
                .agriculteur(agriculteur)
                .superficie(req.getSuperficie())
                .typeOlive(req.getTypeOlive())
                .rendementEstime(req.getRendementEstime())
                .maturiteActuelle(req.getMaturiteActuelle())
                .nbArbre(req.getNbArbre())
                .statut(req.getStatut() != null ? req.getStatut() : StatutVerger.NON_RECOLTE)
                .estSupprimer(false)
                .dateCreation(new Date())
                .build();

        return toResponse(vergerRepo.save(verger));
    }

    @Override
    public VergerResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<VergerResponse> getAll() {
        return vergerRepo.findByEstSupprimerFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    public List<VergerResponse> getByAgriculteur(String agriculteurId) {
        getAgriculteurOrThrow(agriculteurId);
        return vergerRepo.findActiveByAgriculteurId(new ObjectId(agriculteurId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VergerResponse> getByStatut(StatutVerger statut) {
        return vergerRepo.findByStatut(statut).stream()
                .filter(v -> Boolean.FALSE.equals(v.getEstSupprimer()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VergerResponse mettreAJour(String id, VergerRequest req) {
        Verger v = findOrThrow(id);
        v.setSuperficie(req.getSuperficie());
        v.setTypeOlive(req.getTypeOlive());
        v.setRendementEstime(req.getRendementEstime());
        v.setMaturiteActuelle(req.getMaturiteActuelle());
        v.setNbArbre(req.getNbArbre());
        if (req.getStatut() != null) v.setStatut(req.getStatut());
        return toResponse(vergerRepo.save(v));
    }

    @Override
    public VergerResponse changerStatut(String id, StatutVerger statut) {
        Verger v = findOrThrow(id);
        v.setStatut(statut);
        if (statut == StatutVerger.RECOLTE) {
            v.setDateDerniereRecolte(new Date());
        }
        return toResponse(vergerRepo.save(v));
    }

    @Override
    public void desactiver(String id) {
        Verger v = findOrThrow(id);
        v.setEstSupprimer(true);
        vergerRepo.save(v);
    }

    @Override
    public void verifierProprietaireVerger(String vergerId, UserDetails userDetails) {
        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));
        if (isResponsable) return;

        Verger v = findOrThrow(vergerId);
        if (!v.getAgriculteur().getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous n'êtes pas le propriétaire de ce verger");
        }
    }

    // ── helpers ──────────────────────────────────────────────

    private Verger findOrThrow(String id) {
        Verger v = vergerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + id));
        if (Boolean.TRUE.equals(v.getEstSupprimer())) {
            throw new ResourceNotFoundException("Verger introuvable (supprimé) : " + id);
        }
        return v;
    }

    private Utilisateur getAgriculteurOrThrow(String id) {
        return utilisateurRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agriculteur introuvable : " + id));
    }

    private VergerResponse toResponse(Verger v) {
        Utilisateur ag = v.getAgriculteur();
        return VergerResponse.builder()
                .id(v.getId())
                .agriculteurId(ag.getId())
                .agriculteurNom(ag.getPrenom() + " " + ag.getNom())
                .agriculteurEmail(ag.getEmail())
                .superficie(v.getSuperficie())
                .typeOlive(v.getTypeOlive())
                .rendementEstime(v.getRendementEstime())
                .maturiteActuelle(v.getMaturiteActuelle())
                .nbArbre(v.getNbArbre())
                .statut(v.getStatut())
                .dateDerniereRecolte(v.getDateDerniereRecolte())
                .estSupprimer(v.getEstSupprimer())
                .dateCreation(v.getDateCreation())
                .build();
    }
    @Override
    public void verifierProprietaire(String agriculteurId, UserDetails userDetails) {
        boolean isResponsable = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE"));
        if (isResponsable) return;

        Utilisateur agriculteur = getAgriculteurOrThrow(agriculteurId);
        if (!agriculteur.getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous ne pouvez pas accéder aux vergers d'un autre agriculteur");
        }
    }
}