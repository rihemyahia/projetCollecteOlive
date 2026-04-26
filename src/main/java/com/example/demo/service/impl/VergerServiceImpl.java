package com.example.demo.service.impl;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Collecte;
import com.example.demo.model.Geolocalisation;
import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutCollecte;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.repository.CollecteRepository;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.VergerService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VergerServiceImpl implements VergerService {

    private final VergerRepository      vergerRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final CollecteRepository collecteRepo;
    private final TourneeRepository tourneeRepo;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public VergerResponse creer(VergerRequest req, UserDetails userDetails) {
        Utilisateur agriculteur = getAgriculteurOrThrow(req.getAgriculteurId());

        Utilisateur responsable;
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (req.getResponsableId() == null || req.getResponsableId().isBlank()) {
                throw new IllegalArgumentException("En tant qu'admin, vous devez préciser responsableId");
            }
            responsable = getUtilisateurOrThrow(req.getResponsableId());
        } else if (req.getResponsableId() != null && !req.getResponsableId().isBlank()) {
            // Allow a responsable to explicitly set responsableId only if needed (kept for compatibility)
            responsable = getUtilisateurOrThrow(req.getResponsableId());
        } else {
            responsable = utilisateurRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable"));
        }

        if (responsable.getRole() != Role.RESPONSABLE) {
            throw new IllegalArgumentException("responsableId doit référencer un utilisateur avec le rôle RESPONSABLE");
        }

        Verger.VergerBuilder builder = Verger.builder()
                .agriculteur(agriculteur)
                .responsable(responsable)
                .superficie(req.getSuperficie())
                .typeOlive(req.getTypeOlive())
                .rendementEstime(req.getRendementEstime())
                .maturiteActuelle(req.getMaturiteActuelle())
                .nbArbre(req.getNbArbre())
                .statut(StatutVerger.NON_RECOLTE)
                .estSupprimer(false)
                .dateCreation(new Date());

        // ── Attach GPS coordinates if provided ───────────────────────────────
        if (req.getLatitude() != null && req.getLongitude() != null) {
            // GeoJSON stores coordinates as [longitude, latitude]
            builder.location(new GeoJsonPoint(req.getLongitude(), req.getLatitude()));
            builder.geolocalisation(Geolocalisation.builder()
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .adresseIndicative(req.getAdresseIndicative())
                    .build());
        }

        Verger saved = vergerRepo.save(builder.build());
        return recomputeStatutForVerger(saved.getId());
    }

    // ── READ ──────────────────────────────────────────────────────────────────

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
        public List<VergerResponse> getByResponsable(UserDetails userDetails) {
        Utilisateur responsable = utilisateurRepo.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable"));

        return vergerRepo.findByResponsableIdAndEstSupprimerFalse(new ObjectId(responsable.getId()))
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        }

    @Override
    public List<VergerResponse> getByStatut(StatutVerger statut) {
        return vergerRepo.findByEstSupprimerFalse().stream()
                .filter(v -> getEffectiveStatut(v) == statut)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── GEOLOCATION READ ──────────────────────────────────────────────────────

    @Override
    public List<VergerResponse> getAllWithLocation() {
        return vergerRepo.findAllWithLocation().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VergerResponse> getByAgriculteurWithLocation(String agriculteurId) {
        getAgriculteurOrThrow(agriculteurId);
        return vergerRepo.findByAgriculteurWithLocation(new ObjectId(agriculteurId)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VergerResponse> findNearby(Double longitude, Double latitude, Double maxDistanceMetres) {
        return vergerRepo.findNearby(longitude, latitude, maxDistanceMetres).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public VergerResponse mettreAJour(String id, VergerRequest req) {
        Verger v = findOrThrow(id);
        v.setSuperficie(req.getSuperficie());
        v.setTypeOlive(req.getTypeOlive());
        v.setRendementEstime(req.getRendementEstime());
        v.setMaturiteActuelle(req.getMaturiteActuelle());
        v.setNbArbre(req.getNbArbre());

        // Update geolocation if latitude/longitude are explicitly provided
        if (req.getLatitude() != null && req.getLongitude() != null) {
            v.setLocation(new GeoJsonPoint(req.getLongitude(), req.getLatitude()));
            v.setGeolocalisation(Geolocalisation.builder()
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .adresseIndicative(req.getAdresseIndicative())
                    .build());
        }

        applyManualOverrideIfRequested(v, req, null);
        vergerRepo.save(v);
        return recomputeStatutForVerger(v.getId());
    }

    @Override
    public VergerResponse mettreAJourAdmin(String id, VergerRequest req) {
        Verger v = findOrThrow(id);

        // Allow admin to change agriculteur (ownership)
        if (req.getAgriculteurId() != null && !req.getAgriculteurId().isBlank()) {
            Utilisateur agriculteur = getAgriculteurOrThrow(req.getAgriculteurId());
            v.setAgriculteur(agriculteur);
        }

        // Allow admin to change responsable (manager)
        if (req.getResponsableId() != null && !req.getResponsableId().isBlank()) {
            Utilisateur responsable = getUtilisateurOrThrow(req.getResponsableId());
            if (responsable.getRole() != Role.RESPONSABLE) {
                throw new IllegalArgumentException("responsableId doit référencer un utilisateur avec le rôle RESPONSABLE");
            }
            v.setResponsable(responsable);
        }

        // Update normal fields too
        v.setSuperficie(req.getSuperficie());
        v.setTypeOlive(req.getTypeOlive());
        v.setRendementEstime(req.getRendementEstime());
        v.setMaturiteActuelle(req.getMaturiteActuelle());
        v.setNbArbre(req.getNbArbre());

        if (req.getLatitude() != null && req.getLongitude() != null) {
            v.setLocation(new GeoJsonPoint(req.getLongitude(), req.getLatitude()));
            v.setGeolocalisation(Geolocalisation.builder()
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .adresseIndicative(req.getAdresseIndicative())
                    .build());
        }

        applyManualOverrideIfRequested(v, req, null);
        vergerRepo.save(v);
        return recomputeStatutForVerger(v.getId());
    }

    @Override
    public VergerResponse mettreAJourLocalisation(String id,
                                                  Double latitude,
                                                  Double longitude,
                                                  String adresseIndicative) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("La latitude et la longitude sont obligatoires");
        }
        Verger v = findOrThrow(id);
        // GeoJSON: [longitude, latitude]
        v.setLocation(new GeoJsonPoint(longitude, latitude));
        v.setGeolocalisation(Geolocalisation.builder()
                .latitude(latitude)
                .longitude(longitude)
                .adresseIndicative(adresseIndicative)
                .build());
        return toResponse(vergerRepo.save(v));
    }

    @Override
    public VergerResponse changerStatut(String id, StatutVerger statut, String reason, String updatedByUserId) {
        Verger v = findOrThrow(id);
        v.setStatutOverride(statut);
        v.setStatutOverrideReason(reason);
        v.setStatutOverrideByUserId(updatedByUserId);
        v.setStatutOverrideAt(new Date());
        if (statut == StatutVerger.RECOLTE) {
            v.setDateDerniereRecolte(new Date());
        }
        return toResponse(vergerRepo.save(v));
    }

    @Override
    public VergerResponse clearStatutOverride(String id) {
        Verger v = findOrThrow(id);
        v.setStatutOverride(null);
        v.setStatutOverrideReason(null);
        v.setStatutOverrideByUserId(null);
        v.setStatutOverrideAt(null);
        vergerRepo.save(v);
        return recomputeStatutForVerger(v.getId());
    }

    @Override
    public VergerResponse recomputeStatutForVerger(String id) {
        Verger verger = findOrThrow(id);
        StatutVerger computed = computeStatut(verger.getId());
        verger.setStatut(computed);
        if (computed == StatutVerger.RECOLTE && verger.getDateDerniereRecolte() == null) {
            verger.setDateDerniereRecolte(new Date());
        }
        if (computed != StatutVerger.RECOLTE) {
            verger.setDateDerniereRecolte(null);
        }
        return toResponse(vergerRepo.save(verger));
    }

    @Override
    public void desactiver(String id) {
        Verger v = findOrThrow(id);
        v.setEstSupprimer(true);
        vergerRepo.save(v);
    }

    // ── OWNERSHIP CHECKS ─────────────────────────────────────────────────────

    @Override
    public void verifierProprietaireVerger(String vergerId, UserDetails userDetails) {
        boolean isPrivileged = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (isPrivileged) return;

        Verger v = findOrThrow(vergerId);
        if (!v.getAgriculteur().getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous n'êtes pas le propriétaire de ce verger");
        }
    }

    @Override
    public void verifierProprietaire(String agriculteurId, UserDetails userDetails) {
        boolean isPrivileged = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (isPrivileged) return;

        Utilisateur agriculteur = getAgriculteurOrThrow(agriculteurId);
        if (!agriculteur.getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous ne pouvez pas accéder aux vergers d'un autre agriculteur");
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

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

    private Utilisateur getUtilisateurOrThrow(String id) {
        return utilisateurRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    private VergerResponse toResponse(Verger v) {
        Utilisateur ag = v.getAgriculteur();
        Utilisateur resp = v.getResponsable();
        StatutVerger effectiveStatut = getEffectiveStatut(v);
        String statutSource = v.getStatutOverride() != null ? "OVERRIDE" : "COMPUTED";
        return VergerResponse.builder()
                .id(v.getId())
                .agriculteurId(ag.getId())
                .agriculteurNom(ag.getPrenom() + " " + ag.getNom())
                .agriculteurEmail(ag.getEmail())
                .responsableId(resp != null ? resp.getId() : null)
                .responsableNom(resp != null ? resp.getPrenom() + " " + resp.getNom() : null)
                .responsableEmail(resp != null ? resp.getEmail() : null)
                .responsableFonction(resp != null ? resp.getFonction() : null)
                .superficie(v.getSuperficie())
                .typeOlive(v.getTypeOlive())
                .rendementEstime(v.getRendementEstime())
                .maturiteActuelle(v.getMaturiteActuelle())
                .nbArbre(v.getNbArbre())
                .statut(effectiveStatut)
                .statutSource(statutSource)
                .statutOverride(v.getStatutOverride())
                .statutOverrideReason(v.getStatutOverrideReason())
                .statutOverrideByUserId(v.getStatutOverrideByUserId())
                .statutOverrideAt(v.getStatutOverrideAt())
                .dateDerniereRecolte(v.getDateDerniereRecolte())
                .estSupprimer(v.getEstSupprimer())
                .dateCreation(v.getDateCreation())
                .geolocalisation(v.getGeolocalisation())   // ← new field
                .build();
    }

    private StatutVerger getEffectiveStatut(Verger verger) {
        if (verger.getStatutOverride() != null) {
            return verger.getStatutOverride();
        }
        return computeStatut(verger.getId());
    }

    private StatutVerger computeStatut(String vergerId) {
        String currentCampaign = getCampagneAnnee(new Date());

        Optional<Collecte> collecteOpt = collecteRepo.findByVergerIdAndAnnee(vergerId, currentCampaign);
        if (collecteOpt.isPresent()) {
            Collecte collecte = collecteOpt.get();
            if (collecte.getStatut() == StatutCollecte.TERMINEE || Boolean.TRUE.equals(collecte.getEstCloturee())) {
                return StatutVerger.RECOLTE;
            }
            if (collecte.getStatut() == StatutCollecte.EN_COURS) {
                return StatutVerger.EN_COURS;
            }
        }

        List<Tournee> campagneTournees = tourneeRepo.findByVergerId(vergerId).stream()
                .filter(t -> t.getDateDebut() != null && currentCampaign.equals(getCampagneAnnee(t.getDateDebut())))
                .collect(Collectors.toList());

        boolean hasInProgress = campagneTournees.stream()
                .anyMatch(t -> t.getStatut() == StatutTournee.EN_COURS);
        if (hasInProgress) {
            return StatutVerger.EN_COURS;
        }

        boolean hasTerminee = campagneTournees.stream()
                .anyMatch(t -> t.getStatut() == StatutTournee.TERMINEE);
        if (hasTerminee) {
            return StatutVerger.EN_COURS;
        }

        return StatutVerger.NON_RECOLTE;
    }

    private String getCampagneAnnee(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        if (month >= java.util.Calendar.AUGUST) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }

    private void applyManualOverrideIfRequested(Verger verger, VergerRequest req, String updatedByUserId) {
        if (req.getStatut() == null) return;
        if (req.getStatutOverrideReason() == null || req.getStatutOverrideReason().isBlank()) {
            throw new IllegalArgumentException("La raison du changement manuel de statut est obligatoire");
        }
        verger.setStatutOverride(req.getStatut());
        verger.setStatutOverrideReason(req.getStatutOverrideReason().trim());
        verger.setStatutOverrideByUserId(updatedByUserId);
        verger.setStatutOverrideAt(new Date());
        if (req.getStatut() == StatutVerger.RECOLTE) {
            verger.setDateDerniereRecolte(new Date());
        }
    }
}