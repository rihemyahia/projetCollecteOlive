package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor  // 👈 Added for Jackson compatibility
@AllArgsConstructor // 👈 Added for Builder compatibility
public class AgriculteurDashboardDTO {

    // ── Mes vergers ───────────────────────────────────────────────────────
    private long totalMesVergers;
    private long mesVergersNonRecolte;
    private long mesVergersEnCours;
    private long mesVergersRecolte;

    // ── Collectes de mes vergers ──────────────────────────────────────────
    private long totalMesCollectes;
    private long mesCollectesEnCours;
    private long mesCollectesTerminees;
    private double mesQuantiteTotaleKg;

    // ── Tournées planifiées sur mes vergers ───────────────────────────────
    private long mesTourneesPlanifiees;
    private long mesTourneesEnCours;
    private long mesTourneesTerminees;

    // ── Alertes que j'ai signalées ────────────────────────────────────────
    private long mesAlertesEnAttente;
    private long mesAlertesEnCours;
    private long mesAlertesTraitees;
    private long totalMesAlertes;

    // ── Détail de mes vergers ────────────────────────────────────────────
    private List<MonVergerDTO> mesVergers;

    @Data
    @Builder
    @NoArgsConstructor  // 👈 Added for inner class serialization
    @AllArgsConstructor
    public static class MonVergerDTO {
        private String vergerId;
        private String typeOlive;
        private String responsableNom;
        private String responsableEmail;
        private String statut;
        private int nbArbre;
        private double superficie;
        private int maturiteActuelle;
        private double quantiteRecolteKg;
        private int nbTournees;
        private long nbAlertes;
        private String phaseCulturale;
    }
}