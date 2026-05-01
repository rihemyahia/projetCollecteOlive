package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.StatutExtractionHuile;
import com.example.demo.repository.ExtractionHuileRepository;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.service.ResponsablePressoirService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ResponsablePressoirServiceImpl implements ResponsablePressoirService {

    private final UtilisateurRepository utilisateurRepository;
    private final TourneeRepository tourneeRepository;
    private final ExtractionHuileRepository extractionHuileRepository;

    @Override
    public List<Tournee> getTourneesLivreesEnAttente(UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        return tourneeRepository.findByResponsablePressoirIdAndStatut(
                        responsable.getId(),
                        StatutTournee.LIVREE,
                        Sort.by(Sort.Direction.ASC, "livraisonCompletedAt"))
                .stream()
                .filter(t -> !extractionHuileRepository.existsByTourneeId(t.getId()))
                .toList();
    }

    @Override
    public ExtractionHuileResponse receptionnerTournee(String tourneeId, ReceptionOlivesRequest request, UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournee introuvable : " + tourneeId));

        assertAssignedToResponsable(tournee, responsable);
        if (tournee.getStatut() != StatutTournee.LIVREE) {
            throw new IllegalStateException("Seule une tournee LIVREE peut etre receptionnee au pressoir.");
        }
        if (tournee.getCollecte() == null) {
            throw new IllegalStateException("La tournee n'est liee a aucune collecte.");
        }
        if (extractionHuileRepository.existsByTourneeId(tourneeId)) {
            throw new IllegalStateException("Cette tournee a deja ete receptionnee.");
        }

        ExtractionHuile extraction = ExtractionHuile.builder()
                .tournee(tournee)
                .collecte(tournee.getCollecte())
                .responsablePressoir(responsable)
                .pressoirSnapshot(responsable.getPressoir())
                .quantiteOlivesRecueKg(request.getQuantiteOlivesRecueKg())
                .dateReception(new Date())
                .statut(StatutExtractionHuile.RECUE)
                .observationsReception(request.getObservations())
                .dateCreation(new Date())
                .build();

        return toResponse(extractionHuileRepository.save(extraction));
    }

    @Override
    public ExtractionHuileResponse extraireHuile(String extractionId, ExtractionHuileRequest request, UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        ExtractionHuile extraction = findExtractionForResponsable(extractionId, responsable);

        if (extraction.getStatut() == StatutExtractionHuile.VALIDEE) {
            throw new IllegalStateException("Une extraction validee ne peut plus etre modifiee.");
        }
        if (extraction.getQuantiteOlivesRecueKg() != null
                && request.getQuantiteHuileExtraiteL() > extraction.getQuantiteOlivesRecueKg()) {
            throw new IllegalArgumentException("La quantite d'huile ne peut pas depasser la quantite d'olives recue.");
        }

        extraction.setQuantiteHuileExtraiteL(request.getQuantiteHuileExtraiteL());
        extraction.setRendementPourcentage(calculateYield(request.getQuantiteHuileExtraiteL(), extraction.getQuantiteOlivesRecueKg()));
        extraction.setDateExtraction(new Date());
        extraction.setStatut(StatutExtractionHuile.EXTRAITE);
        extraction.setQualiteHuile(request.getQualiteHuile());
        extraction.setObservationsExtraction(request.getObservations());

        return toResponse(extractionHuileRepository.save(extraction));
    }

    @Override
    public ExtractionHuileResponse validerExtraction(String extractionId, UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        ExtractionHuile extraction = findExtractionForResponsable(extractionId, responsable);
        if (extraction.getStatut() != StatutExtractionHuile.EXTRAITE) {
            throw new IllegalStateException("Seule une extraction EXTRAITE peut etre validee.");
        }
        extraction.setStatut(StatutExtractionHuile.VALIDEE);
        return toResponse(extractionHuileRepository.save(extraction));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractionHuileResponse> getExtractions(UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        return extractionHuileRepository.findByResponsablePressoirId(responsable.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollecteHuileResponse> getCollectesHuile(UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        return buildCollectesHuile(extractionHuileRepository.findByResponsablePressoirId(responsable.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PressoirDashboardResponse getDashboard(UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);
        List<ExtractionHuile> extractions = extractionHuileRepository.findByResponsablePressoirId(responsable.getId());
        List<CollecteHuileResponse> collectes = buildCollectesHuile(extractions);

        double totalOlives = extractions.stream().mapToDouble(e -> value(e.getQuantiteOlivesRecueKg())).sum();
        double totalHuile = extractions.stream().mapToDouble(e -> value(e.getQuantiteHuileExtraiteL())).sum();
        long enAttenteExtraction = extractions.stream()
                .filter(e -> e.getStatut() == StatutExtractionHuile.RECUE)
                .count();
        long validees = extractions.stream()
                .filter(e -> e.getStatut() == StatutExtractionHuile.VALIDEE)
                .count();

        Comparator<CollecteHuileResponse> byYield = Comparator.comparing(c -> value(c.getRendementMoyenPourcentage()));
        CollecteHuileResponse best = collectes.stream().filter(c -> c.getTotalHuileExtraiteL() != null && c.getTotalHuileExtraiteL() > 0).max(byYield).orElse(null);
        CollecteHuileResponse worst = collectes.stream().filter(c -> c.getTotalHuileExtraiteL() != null && c.getTotalHuileExtraiteL() > 0).min(byYield).orElse(null);

        return PressoirDashboardResponse.builder()
                .totalOlivesRecuesKg(totalOlives)
                .totalHuileExtraiteL(totalHuile)
                .rendementMoyenPourcentage(calculateYield(totalHuile, totalOlives))
                .tourneesEnAttenteReception(getTourneesLivreesEnAttente(currentUser).size())
                .tourneesEnAttenteExtraction(enAttenteExtraction)
                .extractionsValidees(validees)
                .meilleureCollecte(best)
                .plusFaibleCollecte(worst)
                .collectesHuile(collectes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PressoirProfileResponse getProfile(UserDetails currentUser) {
        return toProfile(getCurrentResponsablePressoir(currentUser));
    }

    @Override
    public PressoirProfileResponse updateProfile(PressoirProfileUpdateRequest request, UserDetails currentUser) {
        Utilisateur responsable = getCurrentResponsablePressoir(currentUser);

        if (request.getPrenom() != null) responsable.setPrenom(request.getPrenom());
        if (request.getNom() != null) responsable.setNom(request.getNom());
        if (request.getTelephone() != null) responsable.setTelephone(request.getTelephone());
        if (request.getAdresse() != null) responsable.setAdresse(request.getAdresse());
        if (request.getDisponible() != null) {
            responsable.setDisponible(request.getDisponible());
        }

        if (responsable.getPressoir() == null) {
            responsable.setPressoir(new Pressoir());
            responsable.getPressoir().setId(UUID.randomUUID().toString());
            responsable.getPressoir().setDateCreation(new Date());
        }

        Pressoir pressoir = responsable.getPressoir();
        if (request.getPressoirNom() != null) pressoir.setNom(request.getPressoirNom());
        if (request.getPressoirAdresse() != null) pressoir.setAdresse(request.getPressoirAdresse());
        if (request.getPressoirTelephone() != null) pressoir.setTelephone(request.getPressoirTelephone());
        if (request.getPressoirEmail() != null) pressoir.setEmail(request.getPressoirEmail());
        if (request.getCapaciteJournaliere() != null) pressoir.setCapaciteJournaliere(request.getCapaciteJournaliere());
        if (request.getHoraires() != null) pressoir.setHoraires(request.getHoraires());
        if (request.getHoraireDebut() != null) pressoir.setHoraireDebut(request.getHoraireDebut());
        if (request.getHoraireFin() != null) pressoir.setHoraireFin(request.getHoraireFin());
        if (request.getGeolocalisation() != null) pressoir.setGeolocalisation(request.getGeolocalisation());
        if (request.getDisponible() != null) {
            pressoir.setActif(request.getDisponible());
        }

        return toProfile(utilisateurRepository.save(responsable));
    }

    private List<CollecteHuileResponse> buildCollectesHuile(List<ExtractionHuile> extractions) {
        Map<String, List<ExtractionHuile>> byCollecte = extractions.stream()
                .filter(e -> e.getCollecte() != null && e.getCollecte().getId() != null)
                .collect(Collectors.groupingBy(e -> e.getCollecte().getId()));

        return byCollecte.values().stream()
                .map(this::toCollecteHuile)
                .sorted(Comparator.comparing(CollecteHuileResponse::getCollecteCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private CollecteHuileResponse toCollecteHuile(List<ExtractionHuile> extractions) {
        ExtractionHuile first = extractions.get(0);
        Collecte collecte = first.getCollecte();
        Tournee firstTournee = first.getTournee();
        Verger verger = firstTournee != null ? firstTournee.getVerger() : null;

        double totalOlives = extractions.stream().mapToDouble(e -> value(e.getQuantiteOlivesRecueKg())).sum();
        double totalHuile = extractions.stream().mapToDouble(e -> value(e.getQuantiteHuileExtraiteL())).sum();
        long extraites = extractions.stream()
                .filter(e -> e.getStatut() == StatutExtractionHuile.EXTRAITE || e.getStatut() == StatutExtractionHuile.VALIDEE)
                .count();

        return CollecteHuileResponse.builder()
                .collecteId(collecte != null ? collecte.getId() : null)
                .collecteCode(collecte != null ? collecte.getCode() : null)
                .vergerId(verger != null ? verger.getId() : null)
                .vergerTypeOlive(verger != null ? verger.getTypeOlive() : null)
                .totalOlivesRecuesKg(totalOlives)
                .totalHuileExtraiteL(totalHuile)
                .rendementMoyenPourcentage(calculateYield(totalHuile, totalOlives))
                .nombreTourneesRecues(extractions.size())
                .nombreTourneesExtraites(extraites)
                .build();
    }

    private ExtractionHuile findExtractionForResponsable(String extractionId, Utilisateur responsable) {
        ExtractionHuile extraction = extractionHuileRepository.findById(extractionId)
                .orElseThrow(() -> new ResourceNotFoundException("Extraction introuvable : " + extractionId));
        if (extraction.getResponsablePressoir() == null
                || extraction.getResponsablePressoir().getId() == null
                || !extraction.getResponsablePressoir().getId().equals(responsable.getId())) {
            throw new SecurityException("Vous n'avez pas acces a cette extraction.");
        }
        return extraction;
    }

    private void assertAssignedToResponsable(Tournee tournee, Utilisateur responsable) {
        if (tournee.getResponsablePressoir() == null
                || tournee.getResponsablePressoir().getId() == null
                || !tournee.getResponsablePressoir().getId().equals(responsable.getId())) {
            throw new SecurityException("Cette tournee n'est pas assignee a votre pressoir.");
        }
    }

    private Utilisateur getCurrentResponsablePressoir(UserDetails currentUser) {
        Utilisateur user = utilisateurRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (user.getRole() != Role.RESPONSABLE_PRESSOIR) {
            throw new SecurityException("Acces reserve au responsable pressoir.");
        }
        return user;
    }

    private ExtractionHuileResponse toResponse(ExtractionHuile extraction) {
        Tournee tournee = extraction.getTournee();
        Collecte collecte = extraction.getCollecte();
        Utilisateur responsable = extraction.getResponsablePressoir();
        Pressoir pressoir = extraction.getPressoirSnapshot();

        return ExtractionHuileResponse.builder()
                .id(extraction.getId())
                .tourneeId(tournee != null ? tournee.getId() : null)
                .tourneeCode(tournee != null ? tournee.getCode() : null)
                .collecteId(collecte != null ? collecte.getId() : null)
                .collecteCode(collecte != null ? collecte.getCode() : null)
                .responsablePressoirId(responsable != null ? responsable.getId() : null)
                .responsablePressoirNom(formatName(responsable))
                .pressoirNom(pressoir != null ? pressoir.getNom() : null)
                .quantiteOlivesRecueKg(extraction.getQuantiteOlivesRecueKg())
                .quantiteHuileExtraiteL(extraction.getQuantiteHuileExtraiteL())
                .rendementPourcentage(extraction.getRendementPourcentage())
                .dateReception(extraction.getDateReception())
                .dateExtraction(extraction.getDateExtraction())
                .statut(extraction.getStatut())
                .qualiteHuile(extraction.getQualiteHuile())
                .observationsReception(extraction.getObservationsReception())
                .observationsExtraction(extraction.getObservationsExtraction())
                .build();
    }

    private PressoirProfileResponse toProfile(Utilisateur user) {
        return PressoirProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .prenom(user.getPrenom())
                .nom(user.getNom())
                .telephone(user.getTelephone())
                .adresse(user.getAdresse())
                .disponible(user.getDisponible())
                .pressoir(user.getPressoir())
                .build();
    }

    private String formatName(Utilisateur user) {
        if (user == null) {
            return null;
        }
        return ((user.getPrenom() != null ? user.getPrenom() : "") + " " + (user.getNom() != null ? user.getNom() : "")).trim();
    }

    private double value(Double value) {
        return value != null ? value : 0.0;
    }

    private Double calculateYield(Double huile, Double olives) {
        if (huile == null || olives == null || olives == 0) {
            return 0.0;
        }
        return (huile / olives) * 100.0;
    }
}
