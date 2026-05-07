package com.example.demo.service.impl;

import com.example.demo.dto.AlerteRequest;
import com.example.demo.dto.AlerteResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.AlerteRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.CloudinaryService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlerteServiceImplTest {

    @Mock private AlerteRepository alerteRepo;
    @Mock private UtilisateurRepository utilisateurRepo;
    @Mock private VergerRepository vergerRepo;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private UserDetails currentUser;

    @InjectMocks
    private AlerteServiceImpl alerteService;

    private Utilisateur agriculteur;
    private Utilisateur responsable;
    private Verger verger;
    private AlerteTerrain alerte;
    private AlerteRequest request;
    private GeoJsonPoint location;
    private Geolocalisation geolocalisation;
    
    // ✅ IDs MongoDB valides (24 caractères hexadécimaux)
    private final String VALID_AGRICULTEUR_ID = "507f1f77bcf86cd799439011";
    private final String VALID_RESPONSABLE_ID = "507f1f77bcf86cd799439012";
    private final String VALID_VERGER_ID = "507f1f77bcf86cd799439013";
    private final String VALID_ALERTE_ID = "507f1f77bcf86cd799439014";

    @BeforeEach
    void setUp() {
        // Setup Agriculteur
        agriculteur = new Utilisateur();
        agriculteur.setId(VALID_AGRICULTEUR_ID);
        agriculteur.setPrenom("Jean");
        agriculteur.setNom("Dupont");
        agriculteur.setEmail("jean@test.com");
        agriculteur.setRole(Role.AGRICULTEUR);

        // Setup Responsable
        responsable = new Utilisateur();
        responsable.setId(VALID_RESPONSABLE_ID);
        responsable.setPrenom("Pierre");
        responsable.setNom("Martin");
        responsable.setEmail("pierre@test.com");
        responsable.setRole(Role.RESPONSABLE);

        // Setup Verger
        verger = new Verger();
        verger.setId(VALID_VERGER_ID);
        verger.setTypeOlive("Olive verte");
        verger.setMaturiteActuelle(50);
        verger.setEstSupprimer(false);
        verger.setResponsable(responsable);
        
        location = new GeoJsonPoint(5.0, 45.0);
        geolocalisation = new Geolocalisation();
        geolocalisation.setLatitude(45.0);
        geolocalisation.setLongitude(5.0);
        verger.setLocation(location);
        verger.setGeolocalisation(geolocalisation);

        // Setup Request
        request = new AlerteRequest();
        request.setAgriculteurId(VALID_AGRICULTEUR_ID);
        request.setVergerId(VALID_VERGER_ID);
        request.setType(TypeAlerte.NUISIBLE);
        request.setDescription("Présence d'oliviers attaqués");

        // Setup Alerte
        alerte = new AlerteTerrain();
        alerte.setId(VALID_ALERTE_ID);
        alerte.setAgriculteur(agriculteur);
        alerte.setVerger(verger);
        alerte.setType(TypeAlerte.NUISIBLE);
        alerte.setDescription("Présence d'oliviers attaqués");
        alerte.setLocation(location);
        alerte.setGeolocalisation(geolocalisation);
        alerte.setPhase(PhaseCulturale.VERDAISON);
        alerte.setNiveauUrgence(NiveauUrgence.ELEVEE);
        alerte.setStatut(StatutAlerte.EN_ATTENTE);
        alerte.setEstSupprimer(false);
        alerte.setDateSignalement(new Date());
        alerte.setPhotoUrls(new ArrayList<>());

        // Setup currentUser - seulement ce qui est nécessaire
        lenient().when(currentUser.getUsername()).thenReturn("pierre@test.com");
    }

    // ========== TESTS CRÉATION ==========

    @Test
    @DisplayName("Créer une alerte avec succès")
    void signalerProbleme_WithValidRequest_ShouldCreateAlert() {
        when(utilisateurRepo.findById(VALID_AGRICULTEUR_ID)).thenReturn(Optional.of(agriculteur));
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));
        when(alerteRepo.save(any(AlerteTerrain.class))).thenReturn(alerte);
        when(alerteRepo.countNearbyAlerts(anyDouble(), anyDouble())).thenReturn(0L);

        AlerteResponse response = alerteService.signalerProbleme(request);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(TypeAlerte.NUISIBLE);
        verify(alerteRepo).save(any(AlerteTerrain.class));
    }

    @Test
    @DisplayName("Échec création : Verger supprimé")
    void signalerProbleme_WithDeletedVerger_ShouldThrowException() {
        verger.setEstSupprimer(true);
        when(utilisateurRepo.findById(VALID_AGRICULTEUR_ID)).thenReturn(Optional.of(agriculteur));
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        assertThatThrownBy(() -> alerteService.signalerProbleme(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Verger introuvable (supprimé)");
    }

    @Test
    @DisplayName("Échec création : Agriculteur inexistant")
    void signalerProbleme_WithUnknownAgriculteur_ShouldThrowException() {
        when(utilisateurRepo.findById(VALID_AGRICULTEUR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alerteService.signalerProbleme(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    // ========== TESTS LECTURE ==========

    @Test
    @DisplayName("Récupérer une alerte par ID avec succès")
    void getById_ExistingAlert_ShouldReturnResponse() {
        when(alerteRepo.findById(VALID_ALERTE_ID)).thenReturn(Optional.of(alerte));

        AlerteResponse response = alerteService.getById(VALID_ALERTE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(VALID_ALERTE_ID);
    }

    @Test
    @DisplayName("Récupérer les alertes par verger")
    void getByVerger_ShouldReturnAlertsForVerger() {
        List<AlerteTerrain> alerts = List.of(alerte);
        when(alerteRepo.findByVergerId(any(ObjectId.class))).thenReturn(alerts);

        List<AlerteResponse> responses = alerteService.getByVerger(VALID_VERGER_ID);

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer les alertes d'un agriculteur")
    void getMesAlertes_WithValidAgriculteur_ShouldReturnAlerts() {
        List<AlerteTerrain> alerts = List.of(alerte);
        when(utilisateurRepo.findById(VALID_AGRICULTEUR_ID)).thenReturn(Optional.of(agriculteur));
        when(alerteRepo.findByAgriculteurId(any(ObjectId.class))).thenReturn(alerts);

        List<AlerteResponse> responses = alerteService.getMesAlertes(VALID_AGRICULTEUR_ID);

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("Récupérer les alertes par responsable")
    void getByResponsable_ShouldReturnAlertsFromManagedVergers() {
        List<Verger> managedVergers = List.of(verger);
        List<AlerteTerrain> alerts = List.of(alerte);
        
        when(utilisateurRepo.findByEmail("pierre@test.com")).thenReturn(Optional.of(responsable));
        when(vergerRepo.findByResponsableIdAndEstSupprimerFalse(any(ObjectId.class)))
                .thenReturn(managedVergers);
        when(alerteRepo.findByVergerId(any(ObjectId.class))).thenReturn(alerts);

        List<AlerteResponse> responses = alerteService.getByResponsable(currentUser);

        assertThat(responses).hasSize(1);
    }

    // ========== TESTS MISE À JOUR ==========

    @Test
    @DisplayName("Marquer une alerte comme traitée")
    void marquerTraitee_WithValidId_ShouldMarkAsTreated() {
        when(alerteRepo.findById(VALID_ALERTE_ID)).thenReturn(Optional.of(alerte));
        when(alerteRepo.save(any(AlerteTerrain.class))).thenReturn(alerte);

        AlerteResponse response = alerteService.marquerTraitee(VALID_ALERTE_ID, "Problème résolu");

        assertThat(response.getStatut()).isEqualTo(StatutAlerte.TRAITEE);
        verify(alerteRepo).save(alerte);
    }

    @Test
    @DisplayName("Changer le statut d'une alerte")
    void changerStatut_ShouldUpdateStatus() {
        when(alerteRepo.findById(VALID_ALERTE_ID)).thenReturn(Optional.of(alerte));
        when(alerteRepo.save(any(AlerteTerrain.class))).thenReturn(alerte);

        AlerteResponse response = alerteService.changerStatut(VALID_ALERTE_ID, StatutAlerte.EN_COURS);

        assertThat(response.getStatut()).isEqualTo(StatutAlerte.EN_COURS);
        verify(alerteRepo).save(alerte);
    }

    // ========== TESTS SUPPRESSION ==========

    @Test
    @DisplayName("Suppression soft d'une alerte")
    void supprimer_ShouldSoftDeleteAlert() {
        when(alerteRepo.findById(VALID_ALERTE_ID)).thenReturn(Optional.of(alerte));
        when(alerteRepo.save(any(AlerteTerrain.class))).thenReturn(alerte);

        alerteService.supprimer(VALID_ALERTE_ID);

        assertThat(alerte.getEstSupprimer()).isTrue();
        verify(alerteRepo).save(alerte);
    }

    // ========== TESTS VÉRIFICATION PROPRIÉTAIRE ==========

    

    // ========== TESTS RESPONSABLE ==========

    @Test
    @DisplayName("Vérification responsable propriétaire du verger : OK")
    void verifyResponsableOwnsVerger_AsOwner_ShouldNotThrow() {
        when(utilisateurRepo.findByEmail("pierre@test.com")).thenReturn(Optional.of(responsable));
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        assertThatCode(() -> alerteService.verifyResponsableOwnsVerger(VALID_VERGER_ID, currentUser))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Vérification responsable propriétaire du verger : KO")
    void verifyResponsableOwnsVerger_NotOwner_ShouldThrow() {
        Utilisateur autreResponsable = new Utilisateur();
        autreResponsable.setId("507f1f77bcf86cd799439099");
        verger.setResponsable(autreResponsable);
        
        when(utilisateurRepo.findByEmail("pierre@test.com")).thenReturn(Optional.of(responsable));
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));

        assertThatThrownBy(() -> alerteService.verifyResponsableOwnsVerger(VALID_VERGER_ID, currentUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ========== TESTS PHOTOS ==========


    // ========== TESTS PHASE ET URGENCE ==========

    @Test
    @DisplayName("Alerte nuisible en phase récolte → urgence CRITIQUE")
    void signalerProbleme_WithPestAndHarvestPhase_ShouldSetCritiqueUrgency() {
        verger.setMaturiteActuelle(90);
        when(utilisateurRepo.findById(VALID_AGRICULTEUR_ID)).thenReturn(Optional.of(agriculteur));
        when(vergerRepo.findById(VALID_VERGER_ID)).thenReturn(Optional.of(verger));
        when(alerteRepo.save(any(AlerteTerrain.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alerteRepo.countNearbyAlerts(anyDouble(), anyDouble())).thenReturn(0L);

        AlerteResponse response = alerteService.signalerProbleme(request);

        assertThat(response.getNiveauUrgence()).isEqualTo(NiveauUrgence.CRITIQUE);
    }
}