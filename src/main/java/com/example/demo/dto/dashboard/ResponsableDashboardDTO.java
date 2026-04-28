package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor  // 👈 Fixes Jackson "Message Not Readable" errors
@AllArgsConstructor // 👈 Required for @Builder with NoArgs
public class ResponsableDashboardDTO {

    // ── Vergers sous ma responsabilité ───────────────────────────────────────
    private long totalMesVergers;
    private long mesVergersNonRecolte;
    private long mesVergersEnCours;
    private long mesVergersRecolte;

    // ── Tournées de mes vergers ────────────────────────────────────────────
    private long totalMesTournees;
    private long mesTourneesEnCours;
    private long mesTourneesTerminees;
    private long mesTourneesPlanifiees;

    // ── Collectes de mes vergers ───────────────────────────────────────────
    private long totalMesCollectes;
    private long mesCollectesEnCours;
    private double mesQuantiteTotaleKg;

    // ── Alertes de mes vergers ─────────────────────────────────────────────
    private long mesAlertesEnAttente;
    private long mesAlertesEnCours;
    private long mesAlertesCritiques;
    private long totalMesAlertes;

    // ── Travailleurs ──────────────────────────────────────────────────────
    private long totalTravailleurs;

    // ── Ressources disponibles ────────────────────────────────────────────
    private long bennesDisponibles;
    private long tracteursDisponibles;

    // ── Détail des vergers ────────────────────────────────────────────────
    private List<VergerDetailDTO> mesVergers;

    @Data
    @Builder
    @NoArgsConstructor  // 👈 Fixes serialization of the nested list
    @AllArgsConstructor
    public static class VergerDetailDTO {
        private String vergerId;
        private String typeOlive;
        private String agriculteurNom;
        private String statut;
        private int nbArbre;
        private double superficie;
        private double quantiteRecolteKg;
        private int nbTournees;
        private long nbAlertes;
        private int maturiteActuelle;
    }
}