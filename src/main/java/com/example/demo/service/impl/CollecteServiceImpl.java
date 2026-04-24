package com.example.demo.service.impl;

import com.example.demo.dto.CollecteDetailDTO;
import com.example.demo.dto.CollecteRequest;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Collecte;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutCollecte;
import com.example.demo.repository.CollecteRepository;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.CollecteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollecteServiceImpl implements CollecteService {

    private final CollecteRepository collecteRepo;
    private final TourneeRepository tourneeRepo;
    private final VergerRepository vergerRepo;

    @Override
    public Collecte getById(String id) {
        return collecteRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecte non trouvée: " + id));
    }
    @Override
    public List<Collecte> getCollectesByResponsable(String responsableId) {
        // Récupérer tous les vergers assignés à ce responsable
        List<Verger> vergersResponsable = vergerRepo.findByResponsableIdAndEstSupprimerFalse(responsableId);
        
        if (vergersResponsable.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Récupérer les IDs des vergers
        List<String> vergerIds = vergersResponsable.stream()
                .map(Verger::getId)
                .collect(Collectors.toList());
        
        // Récupérer les collectes pour ces vergers
        return collecteRepo.findByVergerIdIn(vergerIds);
    }

    @Override
    public List<Collecte> getAll() {
        return collecteRepo.findAll();
    }

    @Override
    public List<Collecte> getByVerger(String vergerId) {
        return collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(vergerId);
    }
    @Override
    public List<Collecte> getByStatut(StatutCollecte statut) {
        return collecteRepo.findByStatut(statut);
    }

    @Override
    public List<Collecte> getActiveCollectes() {
        return collecteRepo.findActiveCollectes();
    }

    @Override
    public List<Collecte> getByAnnee(String annee) {
        return collecteRepo.findByAnnee(annee);
    }

    @Override
    public Collecte updateCollecte(String id, CollecteRequest request) {
        Collecte collecte = getById(id);
        
        // Vérifier qu'on ne peut pas modifier une collecte terminée
        if (collecte.getStatut() == StatutCollecte.TERMINEE) {
            throw new IllegalStateException("Impossible de modifier une collecte terminée");
        }

        if (request.getObservations() != null) {
            collecte.setObservations(request.getObservations());
        }

        if (request.getDateFinCampagne() != null) {
            collecte.setDateFinCampagne(request.getDateFinCampagne());
        }

        return collecteRepo.save(collecte);
    }

    @Override
    public void deleteCollecte(String id) {
        Collecte collecte = getById(id);
        // Vérifier qu'il n'y a pas de tournées associées
        List<Tournee> tournees = tourneeRepo.findByCollecteId(id);
        if (!tournees.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de supprimer une collecte qui a des tournées associées. " +
                            "Nombre de tournées : " + tournees.size()
            );
        }

        // Vérifier qu'on ne supprime pas une collecte en cours
        if (collecte.getStatut() == StatutCollecte.EN_COURS) {
            throw new IllegalStateException("Impossible de supprimer une collecte en cours");
        }
        collecteRepo.delete(collecte);
    }
    @Override
    public CollecteDetailDTO getCollecteWithTournees(String collecteId) {
        // 1 query for collecte
        Collecte collecte = getById(collecteId);

        // 1 query for all tournées (NOT N+1!)
        List<Tournee> tournees = tourneeRepo.findByCollecteId(collecteId);

        // Get verger info
        Verger verger = null;
        if (collecte.getVergerId() != null) {
            verger = vergerRepo.findById(collecte.getVergerId()).orElse(null);
            if (verger != null && Boolean.TRUE.equals(verger.getEstSupprimer())) {
                verger = null;
            }
        } else if (!tournees.isEmpty()) {
            verger = tournees.get(0).getVerger();
        }
        return CollecteDetailDTO.builder()
                .collecte(collecte)
                .tournees(tournees)
                .verger(verger)
                .build();
    }

    @Override
    public Collecte createNewCollecte(Verger verger, String annee, Date dateDebut) {
        // Get next numero for this verger and year
        Integer nextNumero = collecteRepo.findMaxNumeroByVergerIdAndAnnee(verger.getId(), annee)
                .orElse(0) + 1;

        String code = "C-" + verger.getId() + "-" + annee + "-" + String.format("%02d", nextNumero);

        Collecte collecte = Collecte.builder()
                .code(code)
                .statut(StatutCollecte.PLANIFIEE)
                .annee(annee)
                .numero(nextNumero)
                .vergerId(verger.getId())
                .dateDebutCampagne(dateDebut)
                .nbreTournees(0)
                .quantiteTotaleKg(0.0)
                .totalArbresRecoltes(0)
                .estCloturee(false)
                .dateCreation(new Date())
                .build();
        return collecteRepo.save(collecte);
    }

    @Override
    public void updateCollecteStats(String collecteId) {
        System.out.println("=== 📊 updateCollecteStats START ===");
        System.out.println("Collecte ID: " + collecteId);

        // Get all tournées for this collecte (1 query)
        List<Tournee> tournees = tourneeRepo.findByCollecteId(collecteId);
        System.out.println("Nombre de tournées trouvées: " + tournees.size());

        if (tournees.isEmpty()) {
            System.out.println("Aucune tournée trouvée, retour");
            return;
        }

        // Afficher chaque tournée
        for (Tournee t : tournees) {
            System.out.println("  Tournée: " + t.getId() +
                    ", statut: " + t.getStatut() +
                    ", quantite: " + t.getQuantiteCollecteeKg() +
                    ", nbreArbre: " + t.getNbreArbre());
        }

        // Calculate aggregates from TERMINATED tournées only
        int nbreTournees = tournees.size();
        double quantiteTotaleKg = tournees.stream()
                .filter(t -> t.getQuantiteCollecteeKg() != null && t.getStatut() == StatutTournee.TERMINEE)
                .mapToDouble(Tournee::getQuantiteCollecteeKg)
                .sum();
        int totalArbresRecoltes = tournees.stream()
                .filter(t -> t.getNbreArbre() != null && t.getStatut() == StatutTournee.TERMINEE)
                .mapToInt(Tournee::getNbreArbre)
                .sum();
        // Calculer l'efficacité moyenne
        double efficaciteMoyenne = tournees.stream()
                .filter(t -> t.getStatut() == StatutTournee.TERMINEE)
                .mapToDouble(this::calculerEfficacite)
                .average()
                .orElse(0.0);
        System.out.println("Calculs:");
        System.out.println("  nbreTournees (total): " + nbreTournees);
        System.out.println("  quantiteTotaleKg (terminées): " + quantiteTotaleKg);
        System.out.println("  totalArbresRecoltes (terminées): " + totalArbresRecoltes);
        System.out.println("  efficaciteMoyenne: " + efficaciteMoyenne);
        Collecte collecte = getById(collecteId);
        System.out.println("Collecte AVANT mise à jour:");
        System.out.println("  nbreTournees: " + collecte.getNbreTournees());
        System.out.println("  quantiteTotaleKg: " + collecte.getQuantiteTotaleKg());
        System.out.println("  totalArbresRecoltes: " + collecte.getTotalArbresRecoltes());
        collecte.setNbreTournees(nbreTournees);
        collecte.setQuantiteTotaleKg(quantiteTotaleKg);
        collecte.setTotalArbresRecoltes(totalArbresRecoltes);
        collecte.setEfficaciteMoyenne(efficaciteMoyenne);
        if (totalArbresRecoltes > 0) {
            collecte.setRendementMoyenParArbre(quantiteTotaleKg / totalArbresRecoltes);
            System.out.println("  rendementMoyenParArbre: " + collecte.getRendementMoyenParArbre());
        }
        // ✅ Vérifier si le verger est entièrement récolté pour clôturer la collecte
        if (collecte.getVergerId() != null) {
            Verger verger = vergerRepo.findById(collecte.getVergerId()).orElse(null);
            if (verger != null && Boolean.FALSE.equals(verger.getEstSupprimer()) && totalArbresRecoltes >= verger.getNbArbre()) {
                collecte.setEstCloturee(true);
                collecte.setDateFinCampagne(new Date());
                collecte.setStatut(StatutCollecte.TERMINEE);
                System.out.println("🏁🏁🏁 Collecte terminée automatiquement ! Tous les arbres sont récoltés ("
                        + totalArbresRecoltes + "/" + verger.getNbArbre() + " arbres)");
            }
        }

        Collecte saved = collecteRepo.save(collecte);
        System.out.println("Collecte APRÈS mise à jour:");
        System.out.println("  nbreTournees: " + saved.getNbreTournees());
        System.out.println("  quantiteTotaleKg: " + saved.getQuantiteTotaleKg());
        System.out.println("  totalArbresRecoltes: " + saved.getTotalArbresRecoltes());
        System.out.println("  statut: " + saved.getStatut());
        System.out.println("  estCloturee: " + saved.getEstCloturee());
        System.out.println("=== 📊 updateCollecteStats END ===");
    }

    // Ajouter cette méthode helper pour calculer l'efficacité
    private double calculerEfficacite(Tournee t) {
        if (t.getTempsTotal() == null || t.getTempsTotal() == 0) return 0.0;
        if (t.getDistanceTotale() == null || t.getDistanceTotale() == 0) return 0.0;
        if (t.getQuantiteCollecteeKg() == null || t.getQuantiteCollecteeKg() == 0) return 0.0;
        double heures = t.getTempsTotal() / 60.0;
        return Math.min((t.getQuantiteCollecteeKg() / (t.getDistanceTotale() * heures)) * 10.0, 100.0);
    }
    @Override
    public String getCampagneAnnee(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        // Olive harvest season: August to February
        if (month >= Calendar.AUGUST) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    @Override
    public void demarrerCollecte(String collecteId) {
        Collecte collecte = getById(collecteId);
        if (collecte.getStatut() != StatutCollecte.PLANIFIEE) {
            throw new IllegalStateException("Seule une collecte PLANIFIEE peut être démarrée");
        }
        collecte.setStatut(StatutCollecte.EN_COURS);
        collecte.setDateDebutCampagne(new Date());
        collecteRepo.save(collecte);
    }

    @Override
    public void terminerCollecte(String collecteId) {
        Collecte collecte = getById(collecteId);
        if (collecte.getStatut() != StatutCollecte.EN_COURS) {
            throw new IllegalStateException("Seule une collecte EN_COURS peut être terminée");
        }
        collecte.setStatut(StatutCollecte.TERMINEE);
        collecte.setDateFinCampagne(new Date());
        collecte.setEstCloturee(true);
        collecteRepo.save(collecte);
    }
}