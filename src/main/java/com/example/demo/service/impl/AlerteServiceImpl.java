package com.example.demo.service.impl;

import com.example.demo.dto.AlerteRequest;
import com.example.demo.dto.AlerteResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.AlerteTerrain;
import com.example.demo.model.Geolocalisation;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.*;
import com.example.demo.repository.AlerteRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.AlerteService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
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
public class AlerteServiceImpl implements AlerteService {

    private final AlerteRepository alerteRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final VergerRepository vergerRepo;

    @Override
    public AlerteResponse signalerProbleme(AlerteRequest req) {
        Utilisateur agriculteur = getUtilisateurOrThrow(req.getAgriculteurId());

        Verger verger = vergerRepo.findById(req.getVergerId())
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + req.getVergerId()));

        // Derive phase from verger's current maturity — no date needed
        PhaseCulturale phase = derivePhase(verger);

        // Compute urgency from the combination of alert type and current phase
        NiveauUrgence urgence = computeUrgence(req.getType(), phase);

        // GeoJSON requires [longitude, latitude] order
        GeoJsonPoint location = new GeoJsonPoint(req.getLongitude(), req.getLatitude());

        AlerteTerrain alerte = AlerteTerrain.builder()
                .agriculteur(agriculteur)
                .verger(verger)
                .type(req.getType())
                .description(req.getDescription())
                .location(location)
                .geolocalisation(Geolocalisation.builder()
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .adresseIndicative(req.getAdresseIndicative())
                        .build())
                .phase(phase)
                .niveauUrgence(urgence)
                .statut(StatutAlerte.EN_ATTENTE)
                .estSupprimer(false)
                .dateSignalement(new Date())
                .build();

        AlerteTerrain saved = alerteRepo.save(alerte);

        // Optional: if 3+ alerts within 500m, urgency escalates to CRITIQUE
        // This is informational — the stored urgency stays as computed above
        long nearbyCount = alerteRepo.countNearbyAlerts(req.getLongitude(), req.getLatitude());
        if (nearbyCount >= 3 && saved.getNiveauUrgence() != NiveauUrgence.CRITIQUE) {
            saved.setNiveauUrgence(NiveauUrgence.CRITIQUE);
            saved = alerteRepo.save(saved);
        }

        return toResponse(saved);
    }

    @Override
    public AlerteResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<AlerteResponse> getAll() {
        return alerteRepo.findByEstSupprimerFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlerteResponse> getByStatut(StatutAlerte statut) {
        return alerteRepo.findByStatutAndNotDeleted(statut).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlerteResponse> getByUrgence(NiveauUrgence urgence) {
        return alerteRepo.findByNiveauUrgenceAndNotDeleted(urgence).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlerteResponse> getMesAlertes(String agriculteurId) {
        getUtilisateurOrThrow(agriculteurId);
        return alerteRepo.findByAgriculteurId(new ObjectId(agriculteurId)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlerteResponse> getByVerger(String vergerId) {
        return alerteRepo.findByVergerId(new ObjectId(vergerId)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlerteResponse> getNearbyAlerts(Double longitude, Double latitude) {
        return alerteRepo.findNearbyAlerts(longitude, latitude).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AlerteResponse marquerTraitee(String id, String commentaire) {
        AlerteTerrain alerte = findOrThrow(id);
        alerte.setStatut(StatutAlerte.TRAITEE);
        alerte.setCommentaireTraitement(commentaire);
        alerte.setDateMiseAJour(new Date());
        return toResponse(alerteRepo.save(alerte));
    }

    @Override
    public AlerteResponse changerStatut(String id, StatutAlerte statut) {
        AlerteTerrain alerte = findOrThrow(id);
        alerte.setStatut(statut);
        alerte.setDateMiseAJour(new Date());
        return toResponse(alerteRepo.save(alerte));
    }

    @Override
    public void supprimer(String id) {
        AlerteTerrain alerte = findOrThrow(id);
        alerte.setEstSupprimer(true);
        alerte.setDateMiseAJour(new Date());
        alerteRepo.save(alerte);
    }

    // In AlerteServiceImpl — same fix
    @Override
    public void verifierProprietaireAlerte(String alerteId, UserDetails userDetails) {
        boolean isPrivileged = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESPONSABLE")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (isPrivileged) return;
        AlerteTerrain alerte = findOrThrow(alerteId);
        if (!alerte.getAgriculteur().getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("Vous n'êtes pas le propriétaire de cette alerte");
        }
    }

    // ── phase & urgency logic ────────────────────────────────

    private PhaseCulturale derivePhase(Verger verger) {
        if (verger == null || verger.getMaturiteActuelle() == null) return PhaseCulturale.INCONNUE;
        int m = verger.getMaturiteActuelle();
        if (m <= 20)  return PhaseCulturale.FLORAISON;
        if (m <= 40)  return PhaseCulturale.NOUAISON;
        if (m <= 65)  return PhaseCulturale.VERDAISON;
        if (m <= 85)  return PhaseCulturale.PRE_RECOLTE;
        return PhaseCulturale.RECOLTE;
    }

    private NiveauUrgence computeUrgence(TypeAlerte type, PhaseCulturale phase) {
        // Critical combinations — pest or disease at harvest window
        if ((type == TypeAlerte.NUISIBLE || type == TypeAlerte.MALADIE)
                && (phase == PhaseCulturale.RECOLTE || phase == PhaseCulturale.PRE_RECOLTE)) {
            return NiveauUrgence.CRITIQUE;
        }
        // High urgency — time-sensitive olive issues
        if (type == TypeAlerte.MATURITE_ACCELEREE || type == TypeAlerte.CHUTE_PREMATUREE
                || type == TypeAlerte.SECURITE_RECOLTE) {
            return phase == PhaseCulturale.RECOLTE ? NiveauUrgence.CRITIQUE : NiveauUrgence.ELEVEE;
        }
        // Medium urgency — quality and logistics
        if (type == TypeAlerte.QUALITE_HUILE || type == TypeAlerte.LOGISTIQUE_MOULIN
                || type == TypeAlerte.RENDEMENT_ANORMAL) {
            return NiveauUrgence.MOYENNE;
        }
        // Low urgency — irrigation or weather with time to react
        if (type == TypeAlerte.IRRIGATION || type == TypeAlerte.METEO) {
            return phase == PhaseCulturale.RECOLTE ? NiveauUrgence.ELEVEE : NiveauUrgence.FAIBLE;
        }
        return NiveauUrgence.FAIBLE;
    }

    // ── helpers ──────────────────────────────────────────────

    private AlerteTerrain findOrThrow(String id) {
        AlerteTerrain a = alerteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable : " + id));
        if (Boolean.TRUE.equals(a.getEstSupprimer())) {
            throw new ResourceNotFoundException("Alerte introuvable (supprimée) : " + id);
        }
        return a;
    }

    private Utilisateur getUtilisateurOrThrow(String id) {
        return utilisateurRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    private AlerteResponse toResponse(AlerteTerrain a) {
        Utilisateur ag = a.getAgriculteur();
        Verger v = a.getVerger();
        return AlerteResponse.builder()
                .id(a.getId())
                .agriculteurId(ag.getId())
                .agriculteurNom(ag.getPrenom() + " " + ag.getNom())
                .agriculteurEmail(ag.getEmail())
                .vergerId(v != null ? v.getId() : null)
                .vergerTypeOlive(v != null ? v.getTypeOlive() : null)
                .type(a.getType())
                .description(a.getDescription())
                .geolocalisation(a.getGeolocalisation())
                .phase(a.getPhase())
                .niveauUrgence(a.getNiveauUrgence())
                .statut(a.getStatut())
                .commentaireTraitement(a.getCommentaireTraitement())
                .estSupprimer(a.getEstSupprimer())
                .dateSignalement(a.getDateSignalement())
                .dateMiseAJour(a.getDateMiseAJour())
                .build();
    }
}