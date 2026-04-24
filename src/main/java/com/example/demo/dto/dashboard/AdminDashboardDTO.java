package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor  // 👈 CRITICAL: Jackson needs this
@AllArgsConstructor // 👈 Required by Builder when NoArgsConstructor is present
public class AdminDashboardDTO {

    // ── Utilisateurs ────────────────────────────────────────────────────────
    private long totalUtilisateurs;
    private long totalAdmins;
    private long totalResponsables;
    private long totalAgriculteurs;
    private long totalTravailleurs;
    private long compteEnAttente;

    // ── Vergers ──────────────────────────────────────────────────────────────
    private long totalVergers;
    private long vergersNonRecolte;
    private long vergersEnCours;
    private long vergersRecolte;

    // ── Ressources ────────────────────────────────────────────────────────────
    private long totalBennes;
    private long bennesDisponibles;
    private long bennesMaintenance;
    private long totalTracteurs;
    private long tracteursDisponibles;
    private long tracteursMaintenance;

    // ── Tournées ─────────────────────────────────────────────────────────────
    private long totalTournees;
    private long tourneesEnCours;
    private long tourneesTerminees;
    private long tourneesPlanifiees;
    private long tourneesAnnulees;

    // ── Collectes ─────────────────────────────────────────────────────────────
    private long totalCollectes;
    private long collectesEnCours;
    private long collectesTerminees;
    private double quantiteTotaleKgRecolteeCetteAnnee;

    // ── Alertes ──────────────────────────────────────────────────────────────
    private long totalAlertes;
    private long alertesEnAttente;
    private long alertesEnCours;
    private long alertesTraitees;
    private long alertesCritiques;

    // ── Activité récente ─────────────────────────────────────────────────────
    private List<RecentActivityDTO> recentActivity;

    // ── Statistiques par verger ───────────────────────────────────────────────
    private List<VergerStatsDTO> topVergers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDTO {
        private String type;
        private String message;
        private String date;
        private String statut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VergerStatsDTO {
        private String vergerId;
        private String typeOlive;
        private String agriculteurNom;
        private String responsableNom;
        private double quantiteKg;
        private int nbTournees;
    }
}