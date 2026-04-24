package com.example.demo.service.impl;

import com.example.demo.dto.dashboard.AdminDashboardDTO;
import com.example.demo.dto.dashboard.AgriculteurDashboardDTO;
import com.example.demo.dto.dashboard.ResponsableDashboardDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.*;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UtilisateurRepository utilisateurRepo;
    private final VergerRepository vergerRepo;
    private final TourneeRepository tourneeRepo;
    private final CollecteRepository collecteRepo;
    private final AlerteRepository alerteRepo;
    private final RessourceRepository ressourceRepo;

    @Override
    public AdminDashboardDTO getAdminDashboard() {
        List<Verger> allVergers = vergerRepo.findByEstSupprimerFalse();
        List<Tournee> allTournees = tourneeRepo.findAll();
        String anneeCourante = getCampagneAnnee(new Date());

        double qtyAnnee = collecteRepo.findByAnnee(anneeCourante).stream()
                .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0)
                .sum();

        return AdminDashboardDTO.builder()
                .totalUtilisateurs(utilisateurRepo.count())
                .totalAdmins(utilisateurRepo.countByRole(Role.ADMIN))
                .totalResponsables(utilisateurRepo.countByRole(Role.RESPONSABLE))
                .totalAgriculteurs(utilisateurRepo.countByRole(Role.AGRICULTEUR))
                .totalTravailleurs(utilisateurRepo.countByRole(Role.TRAVAILLEUR))
                .compteEnAttente(utilisateurRepo.countByCompteActifFalse())
                .totalVergers(allVergers.size())
                .vergersNonRecolte(allVergers.stream().filter(v -> v.getStatut() == StatutVerger.NON_RECOLTE).count())
                .vergersEnCours(allVergers.stream().filter(v -> v.getStatut() == StatutVerger.EN_COURS).count())
                .vergersRecolte(allVergers.stream().filter(v -> v.getStatut() == StatutVerger.RECOLTE).count())
                .totalBennes(ressourceRepo.findAllBennes().size())
                .bennesDisponibles(ressourceRepo.findBennesByStatut("DISPONIBLE").size())
                .totalTracteurs(ressourceRepo.findAllTracteurs().size())
                .tracteursDisponibles(ressourceRepo.findTracteursByStatut("DISPONIBLE").size())
                .totalTournees(allTournees.size())
                .tourneesEnCours(allTournees.stream().filter(t -> t.getStatut() == StatutTournee.EN_COURS).count())
                .totalCollectes(collecteRepo.count())
                .quantiteTotaleKgRecolteeCetteAnnee(qtyAnnee)
                .totalAlertes(alerteRepo.findByEstSupprimerFalse().size())
                .topVergers(buildTopVergersAdmin(allVergers))
                .recentActivity(buildRecentActivity(allTournees))
                .build();
    }

    @Override
    public ResponsableDashboardDTO getResponsableDashboard(UserDetails userDetails) {
        Utilisateur responsable = utilisateurRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable"));

        List<Verger> mesVergers = vergerRepo.findByResponsableIdAndEstSupprimerFalse(responsable.getId());
        List<String> vergerIds = mesVergers.stream().map(Verger::getId).collect(Collectors.toList());

        long alTotal = 0;
        for (String vid : vergerIds) {
            if (ObjectId.isValid(vid)) {
                alTotal += alerteRepo.findByVergerId(new ObjectId(vid)).stream()
                        .filter(a -> !Boolean.TRUE.equals(a.getEstSupprimer())).count();
            }
        }

        return ResponsableDashboardDTO.builder()
                .totalMesVergers(mesVergers.size())
                .totalMesAlertes(alTotal)
                .totalTravailleurs(utilisateurRepo.countByRole(Role.TRAVAILLEUR))
                .bennesDisponibles(ressourceRepo.findBennesByStatut("DISPONIBLE").size())
                .tracteursDisponibles(ressourceRepo.findTracteursByStatut("DISPONIBLE").size())
                .mesVergers(buildVergerDetail(mesVergers))
                .build();
    }

    @Override
    public AgriculteurDashboardDTO getAgriculteurDashboard(UserDetails userDetails) {
        Utilisateur agriculteur = utilisateurRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Agriculteur introuvable"));

        List<Verger> mesVergers = vergerRepo.findByAgriculteurIdAndEstSupprimerFalse(agriculteur.getId());
        List<AlerteTerrain> mesAlertes = alerteRepo.findByAgriculteurId(new ObjectId(agriculteur.getId()));

        return AgriculteurDashboardDTO.builder()
                .totalMesVergers(mesVergers.size())
                .totalMesAlertes(mesAlertes.stream().filter(a -> !Boolean.TRUE.equals(a.getEstSupprimer())).count())
                .mesVergers(buildMonVergerDTO(mesVergers))
                .build();
    }

    // ── Fixed Helper Methods (Option 2: No null checks for primitives) ────────

    private List<AdminDashboardDTO.VergerStatsDTO> buildTopVergersAdmin(List<Verger> vergers) {
        return vergers.stream()
                .map(v -> {
                    double kg = collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(v.getId())
                            .stream().mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0).sum();
                    return AdminDashboardDTO.VergerStatsDTO.builder()
                            .vergerId(v.getId())
                            .typeOlive(v.getTypeOlive() != null ? v.getTypeOlive() : "Inconnu")
                            .agriculteurNom(v.getAgriculteur() != null ? v.getAgriculteur().getPrenom() + " " + v.getAgriculteur().getNom() : "—")
                            .responsableNom(v.getResponsable() != null ? v.getResponsable().getPrenom() + " " + v.getResponsable().getNom() : "—")
                            .quantiteKg(kg)
                            .nbTournees(tourneeRepo.findByVergerId(v.getId()).size())
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getQuantiteKg(), a.getQuantiteKg()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<ResponsableDashboardDTO.VergerDetailDTO> buildVergerDetail(List<Verger> vergers) {
        return vergers.stream().map(v -> {
            String agNom = v.getAgriculteur() != null ? v.getAgriculteur().getPrenom() + " " + v.getAgriculteur().getNom() : "—";
            return ResponsableDashboardDTO.VergerDetailDTO.builder()
                    .vergerId(v.getId())
                    .typeOlive(v.getTypeOlive())
                    .agriculteurNom(agNom)
                    .statut(v.getStatut() != null ? v.getStatut().name() : "—")
                    // 🔥 FIX: No null checks for primitive int/double
                    .nbArbre(v.getNbArbre())
                    .superficie(v.getSuperficie())
                    .quantiteRecolteKg(collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(v.getId()).stream()
                            .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0).sum())
                    .nbTournees(tourneeRepo.findByVergerId(v.getId()).size())
                    .maturiteActuelle(v.getMaturiteActuelle())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<AgriculteurDashboardDTO.MonVergerDTO> buildMonVergerDTO(List<Verger> vergers) {
        return vergers.stream().map(v -> {
            String respNom = v.getResponsable() != null ? v.getResponsable().getPrenom() + " " + v.getResponsable().getNom() : "—";
            return AgriculteurDashboardDTO.MonVergerDTO.builder()
                    .vergerId(v.getId())
                    .typeOlive(v.getTypeOlive())
                    .responsableNom(respNom)
                    .statut(v.getStatut() != null ? v.getStatut().name() : "—")
                    // 🔥 FIX: maturiteActuelle is int
                    .phaseCulturale(derivePhaseName(v.getMaturiteActuelle()))
                    .quantiteRecolteKg(collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(v.getId()).stream()
                            .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0).sum())
                    .build();
        }).collect(Collectors.toList());
    }

    private String derivePhaseName(int maturite) {
        // 🔥 FIX: No null check needed for int
        if (maturite <= 20) return "FLORAISON";
        if (maturite <= 40) return "NOUAISON";
        if (maturite <= 65) return "VERDAISON";
        if (maturite <= 85) return "PRÉ-RÉCOLTE";
        return "RÉCOLTE";
    }

    private List<AdminDashboardDTO.RecentActivityDTO> buildRecentActivity(List<Tournee> tournees) {
        return tournees.stream()
                .filter(t -> t != null && t.getDateCreation() != null)
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .limit(5)
                .map(t -> AdminDashboardDTO.RecentActivityDTO.builder()
                        .type("TOURNEE")
                        .message("Tournée " + (t.getCode() != null ? t.getCode() : "SANS CODE"))
                        .statut(t.getStatut() != null ? t.getStatut().name() : "—")
                        .date(t.getDateCreation().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private String getCampagneAnnee(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        return (month >= Calendar.AUGUST) ? year + "-" + (year + 1) : (year - 1) + "-" + year;
    }
}