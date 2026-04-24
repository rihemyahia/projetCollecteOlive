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
        // ... (Keep your existing working Admin code exactly as it is)
        List<Verger> allVergers = vergerRepo.findByEstSupprimerFalse();
        List<Tournee> allTournees = tourneeRepo.findAll();
        List<AlerteTerrain> allAlertes = alerteRepo.findByEstSupprimerFalse();
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
                .tourneesPlanifiees(allTournees.stream().filter(t -> t.getStatut() == StatutTournee.PLANIFIEE).count())
                .tourneesTerminees(allTournees.stream().filter(t -> t.getStatut() == StatutTournee.TERMINEE).count())
                .tourneesAnnulees(allTournees.stream().filter(t -> t.getStatut() == StatutTournee.ANNULEE).count())
                .totalCollectes(collecteRepo.count())
                .quantiteTotaleKgRecolteeCetteAnnee(qtyAnnee)
                .totalAlertes(allAlertes.size())
                .alertesEnAttente(allAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_ATTENTE).count())
                .alertesEnCours(allAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_COURS).count())
                .alertesTraitees(allAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.TRAITEE).count())
                .alertesCritiques(allAlertes.stream().filter(a -> a.getNiveauUrgence() == NiveauUrgence.CRITIQUE).count())
                .topVergers(buildTopVergersAdmin(allVergers))
                .recentActivity(buildRecentActivity(allTournees))
                .build();
    }

    @Override
    public ResponsableDashboardDTO getResponsableDashboard(UserDetails userDetails) {
        Utilisateur responsable = utilisateurRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable"));

        // 🔥 FIX: Convert String ID to ObjectId for MongoDB Query
        ObjectId responsableId = new ObjectId(responsable.getId());
        List<Verger> mesVergers = vergerRepo.findByResponsableIdAndEstSupprimerFalse(responsableId);
        List<String> vergerIds = mesVergers.stream().map(Verger::getId).collect(Collectors.toList());

        List<Tournee> mesTournees = tourneeRepo.findAll().stream()
                .filter(t -> t.getVerger() != null && vergerIds.contains(t.getVerger().getId()))
                .collect(Collectors.toList());

        List<AlerteTerrain> mesAlertes = alerteRepo.findByEstSupprimerFalse().stream()
                .filter(a -> a.getVerger() != null && vergerIds.contains(a.getVerger().getId()))
                .collect(Collectors.toList());

        double mesQuantiteKg = collecteRepo.findAll().stream()
                .filter(c -> c.getVergerId() != null && vergerIds.contains(c.getVergerId()))
                .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0)
                .sum();

        return ResponsableDashboardDTO.builder()
                .totalMesVergers(mesVergers.size())
                .mesVergersNonRecolte(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.NON_RECOLTE).count())
                .mesVergersEnCours(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.EN_COURS).count())
                .mesVergersRecolte(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.RECOLTE).count())

                .totalMesTournees(mesTournees.size())
                .mesTourneesEnCours(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.EN_COURS).count())
                .mesTourneesTerminees(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.TERMINEE).count())
                .mesTourneesPlanifiees(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.PLANIFIEE).count())

                .totalMesAlertes(mesAlertes.size())
                .mesAlertesEnAttente(mesAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_ATTENTE).count())
                .mesAlertesEnCours(mesAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_COURS).count())
                .mesAlertesCritiques(mesAlertes.stream().filter(a -> a.getNiveauUrgence() == NiveauUrgence.CRITIQUE).count())

                .mesQuantiteTotaleKg(mesQuantiteKg)
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

        // 🔥 FIX: Convert String ID to ObjectId for MongoDB Query
        ObjectId agriculteurId = new ObjectId(agriculteur.getId());

        // Ensure this method exists in repository
        List<Verger> mesVergers = vergerRepo.findActiveByAgriculteurId(agriculteurId);
        List<String> vergerIds = mesVergers.stream().map(Verger::getId).collect(Collectors.toList());

        List<Tournee> mesTournees = tourneeRepo.findAll().stream()
                .filter(t -> t.getVerger() != null && vergerIds.contains(t.getVerger().getId()))
                .collect(Collectors.toList());

        List<AlerteTerrain> mesAlertes = alerteRepo.findByAgriculteurId(agriculteurId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getEstSupprimer()))
                .collect(Collectors.toList());

        List<Collecte> mesCollectes = collecteRepo.findAll().stream()
                .filter(c -> c.getVergerId() != null && vergerIds.contains(c.getVergerId()))
                .collect(Collectors.toList());

        double mesQuantiteKg = mesCollectes.stream()
                .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0)
                .sum();

        return AgriculteurDashboardDTO.builder()
                .totalMesVergers(mesVergers.size())
                .mesVergersNonRecolte(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.NON_RECOLTE).count())
                .mesVergersEnCours(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.EN_COURS).count())
                .mesVergersRecolte(mesVergers.stream().filter(v -> v.getStatut() == StatutVerger.RECOLTE).count())

                .totalMesCollectes(mesCollectes.size())
                .mesQuantiteTotaleKg(mesQuantiteKg)

                .mesTourneesPlanifiees(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.PLANIFIEE).count())
                .mesTourneesEnCours(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.EN_COURS).count())
                .mesTourneesTerminees(mesTournees.stream().filter(t -> t.getStatut() == StatutTournee.TERMINEE).count())

                .totalMesAlertes(mesAlertes.size())
                .mesAlertesEnAttente(mesAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_ATTENTE).count())
                .mesAlertesEnCours(mesAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.EN_COURS).count())
                .mesAlertesTraitees(mesAlertes.stream().filter(a -> a.getStatut() == StatutAlerte.TRAITEE).count())

                .mesVergers(buildMonVergerDTO(mesVergers))
                .build();
    }

    // ── Fixed Helper Methods ────────

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
                    .phaseCulturale(derivePhaseName(v.getMaturiteActuelle()))
                    .quantiteRecolteKg(collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(v.getId()).stream()
                            .mapToDouble(c -> c.getQuantiteTotaleKg() != null ? c.getQuantiteTotaleKg() : 0.0).sum())
                    .build();
        }).collect(Collectors.toList());
    }

    private String derivePhaseName(int maturite) {
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