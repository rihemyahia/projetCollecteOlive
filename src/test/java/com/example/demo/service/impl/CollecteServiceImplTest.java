package com.example.demo.service.impl;

import com.example.demo.dto.CollecteDetailDTO;
import com.example.demo.dto.CollecteRequest;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.StatutCollecte;
import com.example.demo.repository.CollecteRepository;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.MeteoService;
import com.example.demo.service.VergerService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollecteServiceImplTest {

    @Mock private CollecteRepository collecteRepo;
    @Mock private TourneeRepository tourneeRepo;
    @Mock private VergerRepository vergerRepo;
    @Mock private VergerService vergerService;
    @Mock private MeteoService meteoService;

    @InjectMocks
    private CollecteServiceImpl collecteService;

    private Verger verger;
    private Collecte collecte;
    private Tournee tournee1;
    private Tournee tournee2;
    private final String VALID_VERGER_ID = "507f1f77bcf86cd799439013";
    private final String VALID_COLLECTE_ID = "507f1f77bcf86cd799439014";
    private final String VALID_RESPONSABLE_ID = "507f1f77bcf86cd799439012";

    @BeforeEach
    void setUp() {
        // Setup Verger
        verger = new Verger();
        verger.setId(VALID_VERGER_ID);
        verger.setNbArbre(100);
        verger.setEstSupprimer(false);
        
        Geolocalisation geo = new Geolocalisation();
        geo.setLatitude(45.0);
        geo.setLongitude(5.0);
        verger.setGeolocalisation(geo);

        // Setup Collecte
        collecte = new Collecte();
        collecte.setId(VALID_COLLECTE_ID);
        collecte.setCode("C-2024-001");
        collecte.setStatut(StatutCollecte.PLANIFIEE);
        collecte.setAnnee("2024-2025");
        collecte.setNumero(1);
        collecte.setVergerId(VALID_VERGER_ID);
        collecte.setNbreTournees(0);
        collecte.setQuantiteTotaleKg(0.0);
        collecte.setTotalArbresRecoltes(0);
        collecte.setEstCloturee(false);
        collecte.setDateCreation(new Date());

        // Setup Tournees
        tournee1 = new Tournee();
        tournee1.setId("tournee-1");
        tournee1.setStatut(StatutTournee.TERMINEE);
        tournee1.setQuantiteCollecteeKg(500.0);
        tournee1.setNbreArbre(50);
        tournee1.setTempsTotal(3600);
        tournee1.setDistanceTotale(100.0);

        tournee2 = new Tournee();
        tournee2.setId("tournee-2");
        tournee2.setStatut(StatutTournee.TERMINEE);
        tournee2.setQuantiteCollecteeKg(400.0);
        tournee2.setNbreArbre(40);
        tournee2.setTempsTotal(3600);
        tournee2.setDistanceTotale(80.0);
    }

    // ========== TESTS CRÉATION ==========

    @Test
    @DisplayName("Créer une nouvelle collecte avec succès")
    void createNewCollecte_WithValidVerger_ShouldCreateCollecte() throws Exception {
        // ✅ Créer une collecte simulée avec les bonnes valeurs
        Collecte savedCollecte = new Collecte();
        savedCollecte.setId(VALID_COLLECTE_ID);
        savedCollecte.setCode("C-2024-001");
        savedCollecte.setStatut(StatutCollecte.PLANIFIEE);
        savedCollecte.setAnnee("2024-2025");
        savedCollecte.setNumero(1);
        savedCollecte.setVergerId(VALID_VERGER_ID);
        savedCollecte.setPrecipitations(5.2);
        savedCollecte.setTemperature(18.5);
        savedCollecte.setDateCreation(new Date());

        when(collecteRepo.findMaxNumeroByVergerIdAndAnnee(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(collecteRepo.save(any(Collecte.class))).thenReturn(savedCollecte);
        when(meteoService.getPrecipitationForCoordinates(anyDouble(), anyDouble())).thenReturn(5.2);
        when(meteoService.getTemperature(anyDouble(), anyDouble())).thenReturn(18.5);

        Collecte result = collecteService.createNewCollecte(verger, "2024-2025", new Date());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(VALID_COLLECTE_ID);
        assertThat(result.getPrecipitations()).isEqualTo(5.2);
        assertThat(result.getTemperature()).isEqualTo(18.5);
        verify(collecteRepo).save(any(Collecte.class));
    }

    @Test
    @DisplayName("Créer une collecte avec numéro incrémenté")
    void createNewCollecte_WithExistingCollecte_ShouldIncrementNumero() throws Exception {
        // ✅ Créer une collecte simulée avec le bon numéro
        Collecte savedCollecte = new Collecte();
        savedCollecte.setId(VALID_COLLECTE_ID);
        savedCollecte.setCode("C-2024-004");
        savedCollecte.setStatut(StatutCollecte.PLANIFIEE);
        savedCollecte.setAnnee("2024-2025");
        savedCollecte.setNumero(4);  // ← Numéro incrémenté
        savedCollecte.setVergerId(VALID_VERGER_ID);
        savedCollecte.setPrecipitations(0.0);
        savedCollecte.setTemperature(20.0);
        savedCollecte.setDateCreation(new Date());

        when(collecteRepo.findMaxNumeroByVergerIdAndAnnee(anyString(), anyString()))
                .thenReturn(Optional.of(3));
        when(collecteRepo.save(any(Collecte.class))).thenAnswer(invocation -> {
            Collecte c = invocation.getArgument(0);
            savedCollecte.setNumero(c.getNumero());  // Garder le numéro incrémenté
            return savedCollecte;
        });
        when(meteoService.getPrecipitationForCoordinates(anyDouble(), anyDouble())).thenReturn(0.0);
        when(meteoService.getTemperature(anyDouble(), anyDouble())).thenReturn(20.0);

        Collecte result = collecteService.createNewCollecte(verger, "2024-2025", new Date());

        assertThat(result).isNotNull();
        assertThat(result.getNumero()).isEqualTo(4);
    }

    @Test
    @DisplayName("Création collecte échoue si verger sans géolocalisation")
    void createNewCollecte_WithoutGeolocalisation_ShouldThrowException() {
        verger.setGeolocalisation(null);

        assertThatThrownBy(() -> collecteService.createNewCollecte(verger, "2024-2025", new Date()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("pas de géolocalisation");
        
        verify(collecteRepo, never()).save(any());
    }

    @Test
    @DisplayName("Création collecte échoue si météo indisponible")
    void createNewCollecte_WithMeteoException_ShouldThrowException() throws Exception {
        when(meteoService.getPrecipitationForCoordinates(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Météo indisponible"));

        assertThatThrownBy(() -> collecteService.createNewCollecte(verger, "2024-2025", new Date()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erreur lors de la récupération des données météo");
        
        verify(collecteRepo, never()).save(any());
    }

    // ========== TESTS LECTURE ==========

    @Test
    @DisplayName("Récupérer collecte par ID avec succès")
    void getById_ExistingId_ShouldReturnCollecte() {
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));

        Collecte result = collecteService.getById(VALID_COLLECTE_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(VALID_COLLECTE_ID);
    }

    @Test
    @DisplayName("Récupérer collecte par ID inexistant → exception")
    void getById_NonExistingId_ShouldThrowException() {
        when(collecteRepo.findById("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collecteService.getById("invalid"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Collecte non trouvée");
    }

    @Test
    @DisplayName("Récupérer toutes les collectes")
    void getAll_ShouldReturnAllCollectes() {
        List<Collecte> collectes = List.of(collecte);
        when(collecteRepo.findAll()).thenReturn(collectes);

        List<Collecte> result = collecteService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collectes par verger")
    void getByVerger_ShouldReturnCollectes() {
        List<Collecte> collectes = List.of(collecte);
        when(collecteRepo.findByVergerIdOrderByAnneeDescNumeroDesc(VALID_VERGER_ID))
                .thenReturn(collectes);

        List<Collecte> result = collecteService.getByVerger(VALID_VERGER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collectes par responsable")
    void getCollectesByResponsable_ShouldReturnCollectes() {
        List<Verger> vergers = List.of(verger);
        List<Collecte> collectes = List.of(collecte);
        
        when(vergerRepo.findByResponsableIdAndEstSupprimerFalse(any(ObjectId.class)))
                .thenReturn(vergers);
        when(collecteRepo.findByVergerIdIn(anyList())).thenReturn(collectes);

        List<Collecte> result = collecteService.getCollectesByResponsable(VALID_RESPONSABLE_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collectes par responsable sans vergers → liste vide")
    void getCollectesByResponsable_NoVergers_ShouldReturnEmptyList() {
        when(vergerRepo.findByResponsableIdAndEstSupprimerFalse(any(ObjectId.class)))
                .thenReturn(Collections.emptyList());

        List<Collecte> result = collecteService.getCollectesByResponsable(VALID_RESPONSABLE_ID);

        assertThat(result).isEmpty();
        verify(collecteRepo, never()).findByVergerIdIn(any());
    }

    @Test
    @DisplayName("Récupérer collectes par statut")
    void getByStatut_ShouldReturnFilteredCollectes() {
        List<Collecte> collectes = List.of(collecte);
        when(collecteRepo.findByStatut(StatutCollecte.PLANIFIEE)).thenReturn(collectes);

        List<Collecte> result = collecteService.getByStatut(StatutCollecte.PLANIFIEE);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collectes actives")
    void getActiveCollectes_ShouldReturnActiveCollectes() {
        List<Collecte> collectes = List.of(collecte);
        when(collecteRepo.findActiveCollectes()).thenReturn(collectes);

        List<Collecte> result = collecteService.getActiveCollectes();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collectes par année")
    void getByAnnee_ShouldReturnCollectes() {
        List<Collecte> collectes = List.of(collecte);
        when(collecteRepo.findByAnnee("2024-2025")).thenReturn(collectes);

        List<Collecte> result = collecteService.getByAnnee("2024-2025");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer collecte avec ses tournées")
    void getCollecteWithTournees_ShouldReturnDTO() {
        List<Tournee> tournees = List.of(tournee1, tournee2);
        
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(tournees);
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        CollecteDetailDTO result = collecteService.getCollecteWithTournees(VALID_COLLECTE_ID);

        assertThat(result).isNotNull();
        assertThat(result.getCollecte()).isEqualTo(collecte);
        assertThat(result.getTournees()).hasSize(2);
        assertThat(result.getVerger()).isEqualTo(verger);
    }

    // ========== TESTS MISE À JOUR ==========

    @Test
    @DisplayName("Mettre à jour une collecte avec succès")
    void updateCollecte_WithValidRequest_ShouldUpdate() {
        CollecteRequest request = new CollecteRequest();
        request.setObservations("Nouvelle observation");
        request.setDateFinCampagne(new Date());

        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(collecteRepo.save(any(Collecte.class))).thenReturn(collecte);

        Collecte result = collecteService.updateCollecte(VALID_COLLECTE_ID, request);

        assertThat(result).isNotNull();
        assertThat(collecte.getObservations()).isEqualTo("Nouvelle observation");
        verify(collecteRepo).save(collecte);
    }

    @Test
    @DisplayName("Mettre à jour une collecte terminée → exception")
    void updateCollecte_WhenTerminated_ShouldThrowException() {
        collecte.setStatut(StatutCollecte.TERMINEE);
        CollecteRequest request = new CollecteRequest();
        
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> collecteService.updateCollecte(VALID_COLLECTE_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de modifier une collecte terminée");
    }

    @Test
    @DisplayName("Mettre à jour les statistiques d'une collecte")
    void updateCollecteStats_ShouldCalculateAggregates() {
        List<Tournee> tournees = List.of(tournee1, tournee2);
        
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(tournees);
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(collecteRepo.save(any(Collecte.class))).thenReturn(collecte);
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        collecteService.updateCollecteStats(VALID_COLLECTE_ID);

        assertThat(collecte.getNbreTournees()).isEqualTo(2);
        assertThat(collecte.getQuantiteTotaleKg()).isEqualTo(900.0);
        assertThat(collecte.getTotalArbresRecoltes()).isEqualTo(90);
        verify(collecteRepo).save(collecte);
    }

    @Test
    @DisplayName("Mettre à jour stats - collecte complète → clôture auto")
    void updateCollecteStats_WhenAllTreesHarvested_ShouldAutoClose() {
        List<Tournee> tournees = List.of(tournee1, tournee2);
        verger.setNbArbre(90);
        
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(tournees);
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(collecteRepo.save(any(Collecte.class))).thenReturn(collecte);
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        collecteService.updateCollecteStats(VALID_COLLECTE_ID);

        assertThat(collecte.getEstCloturee()).isTrue();
        assertThat(collecte.getStatut()).isEqualTo(StatutCollecte.TERMINEE);
        verify(collecteRepo).save(collecte);
    }

    @Test
    @DisplayName("Mettre à jour stats - aucune tournée → retour sans modification")
    void updateCollecteStats_WithNoTournees_ShouldReturnEarly() {
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(Collections.emptyList());

        collecteService.updateCollecteStats(VALID_COLLECTE_ID);

        verify(collecteRepo, never()).save(any());
    }

    // ========== TESTS SUPPRESSION ==========

    @Test
    @DisplayName("Supprimer une collecte sans tournées associées")
    void deleteCollecte_WithoutTournees_ShouldDelete() {
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(Collections.emptyList());

        collecteService.deleteCollecte(VALID_COLLECTE_ID);

        verify(collecteRepo).delete(collecte);
    }

    @Test
    @DisplayName("Supprimer une collecte avec tournées associées → exception")
    void deleteCollecte_WithTournees_ShouldThrowException() {
        List<Tournee> tournees = List.of(tournee1);
        
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(tournees);

        assertThatThrownBy(() -> collecteService.deleteCollecte(VALID_COLLECTE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de supprimer une collecte qui a des tournées associées");
        
        verify(collecteRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer une collecte en cours → exception")
    void deleteCollecte_WhenInProgress_ShouldThrowException() {
        collecte.setStatut(StatutCollecte.EN_COURS);
        
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(tourneeRepo.findByCollecteId(VALID_COLLECTE_ID)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> collecteService.deleteCollecte(VALID_COLLECTE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de supprimer une collecte en cours");
        
        verify(collecteRepo, never()).delete(any());
    }

    // ========== TESTS TRANSITIONS D'ÉTAT ==========

    @Test
    @DisplayName("Démarrer une collecte planifiée")
    void demarrerCollecte_WhenPlanned_ShouldStart() {
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(collecteRepo.save(any(Collecte.class))).thenReturn(collecte);

        collecteService.demarrerCollecte(VALID_COLLECTE_ID);

        assertThat(collecte.getStatut()).isEqualTo(StatutCollecte.EN_COURS);
        assertThat(collecte.getDateDebutCampagne()).isNotNull();
        verify(collecteRepo).save(collecte);
        verify(vergerService).recomputeStatutForVerger(VALID_VERGER_ID);
    }

    @Test
    @DisplayName("Démarrer une collecte déjà en cours → exception")
    void demarrerCollecte_WhenAlreadyStarted_ShouldThrowException() {
        collecte.setStatut(StatutCollecte.EN_COURS);
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> collecteService.demarrerCollecte(VALID_COLLECTE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seule une collecte PLANIFIEE peut être démarrée");
        
        verify(collecteRepo, never()).save(any());
    }

    @Test
    @DisplayName("Terminer une collecte en cours")
    void terminerCollecte_WhenInProgress_ShouldComplete() {
        collecte.setStatut(StatutCollecte.EN_COURS);
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));
        when(collecteRepo.save(any(Collecte.class))).thenReturn(collecte);

        collecteService.terminerCollecte(VALID_COLLECTE_ID);

        assertThat(collecte.getStatut()).isEqualTo(StatutCollecte.TERMINEE);
        assertThat(collecte.getEstCloturee()).isTrue();
        assertThat(collecte.getDateFinCampagne()).isNotNull();
        verify(collecteRepo).save(collecte);
        verify(vergerService).recomputeStatutForVerger(VALID_VERGER_ID);
    }

    @Test
    @DisplayName("Terminer une collecte planifiée → exception")
    void terminerCollecte_WhenPlanned_ShouldThrowException() {
        collecte.setStatut(StatutCollecte.PLANIFIEE);
        when(collecteRepo.findById(VALID_COLLECTE_ID)).thenReturn(Optional.of(collecte));

        assertThatThrownBy(() -> collecteService.terminerCollecte(VALID_COLLECTE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seule une collecte EN_COURS peut être terminée");
        
        verify(collecteRepo, never()).save(any());
    }

    // ========== TESTS CAMPAGNE ANNÉE ==========

    @Test
    @DisplayName("Campagne année - août à décembre → année-courante/année+1")
    void getCampagneAnnee_FromAugustToDecember_ShouldReturnYearRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        cal.set(Calendar.YEAR, 2024);
        Date date = cal.getTime();

        String result = collecteService.getCampagneAnnee(date);

        assertThat(result).isEqualTo("2024-2025");
    }

    @Test
    @DisplayName("Campagne année - janvier à juillet → année-1/année-courante")
    void getCampagneAnnee_FromJanuaryToJuly_ShouldReturnPreviousYearRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.YEAR, 2025);
        Date date = cal.getTime();

        String result = collecteService.getCampagneAnnee(date);

        assertThat(result).isEqualTo("2024-2025");
    }
}