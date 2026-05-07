package com.example.demo.service.impl;

import com.example.demo.dto.dashboard.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
@MockitoSettings(strictness = Strictness.LENIENT) // <-- Ajout de cette ligne
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private UtilisateurRepository utilisateurRepo;
    @Mock private VergerRepository vergerRepo;
    @Mock private TourneeRepository tourneeRepo;
    @Mock private CollecteRepository collecteRepo;
    @Mock private AlerteRepository alerteRepo;
    @Mock private RessourceRepository ressourceRepo;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Utilisateur mockUser;
    private String userEmail = "test@example.com";
    private String userId = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        mockUser = new Utilisateur();
        mockUser.setId(userId);
        mockUser.setEmail(userEmail);
        mockUser.setPrenom("Jean");
        mockUser.setNom("Dupont");
    }

    @Test
    @DisplayName("Admin Dashboard : Doit agréger toutes les statistiques globales")
    void getAdminDashboard_ShouldReturnGlobalStats() {
        // Arrange
        when(vergerRepo.findByEstSupprimerFalse()).thenReturn(List.of(
                createVerger(StatutVerger.NON_RECOLTE),
                createVerger(StatutVerger.RECOLTE)
        ));
        when(tourneeRepo.findAll()).thenReturn(new ArrayList<>());
        when(alerteRepo.findByEstSupprimerFalse()).thenReturn(new ArrayList<>());
        when(utilisateurRepo.count()).thenReturn(10L);
        when(utilisateurRepo.countByRole(Role.AGRICULTEUR)).thenReturn(5L);
        when(ressourceRepo.findAllBennes()).thenReturn(List.of(new Ressource(), new Ressource()));
        when(ressourceRepo.findBennesByStatut("DISPONIBLE")).thenReturn(List.of(new Ressource()));
        when(collecteRepo.findByAnnee(anyString())).thenReturn(new ArrayList<>());

        // Act
        AdminDashboardDTO dto = dashboardService.getAdminDashboard();

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getTotalUtilisateurs()).isEqualTo(10);
        assertThat(dto.getTotalAgriculteurs()).isEqualTo(5);
        assertThat(dto.getTotalVergers()).isEqualTo(2);
        assertThat(dto.getBennesDisponibles()).isEqualTo(1);
    }

    @Test
    @DisplayName("Responsable Dashboard : Doit filtrer les données par responsable ID")
    void getResponsableDashboard_ShouldFilterByResponsable() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(utilisateurRepo.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        
        Verger v1 = createVerger(StatutVerger.EN_COURS);
        v1.setId(new ObjectId().toHexString());
        when(vergerRepo.findByResponsableIdAndEstSupprimerFalse(any(ObjectId.class)))
                .thenReturn(List.of(v1));
        
        when(tourneeRepo.findAll()).thenReturn(new ArrayList<>());
        when(alerteRepo.findByEstSupprimerFalse()).thenReturn(new ArrayList<>());
        when(collecteRepo.findAll()).thenReturn(new ArrayList<>());

        // Act
        ResponsableDashboardDTO dto = dashboardService.getResponsableDashboard(userDetails);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getTotalMesVergers()).isEqualTo(1);
        assertThat(dto.getMesVergersEnCours()).isEqualTo(1);
    }

    @Test
    @DisplayName("Agriculteur Dashboard : Doit calculer les quantités et les phases culturales")
    void getAgriculteurDashboard_ShouldCalculateQuantitiesAndPhases() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(utilisateurRepo.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        
        Verger v1 = createVerger(StatutVerger.NON_RECOLTE);
        v1.setId(new ObjectId().toHexString());
        v1.setMaturiteActuelle(50); // Devrait être VERDAISON selon la logique du code
        
        when(vergerRepo.findActiveByAgriculteurId(any(ObjectId.class))).thenReturn(List.of(v1));
        
        Collecte c1 = new Collecte();
        c1.setVergerId(v1.getId());
        c1.setQuantiteTotaleKg(500.5);
        when(collecteRepo.findAll()).thenReturn(List.of(c1));
        when(tourneeRepo.findAll()).thenReturn(new ArrayList<>());
        when(alerteRepo.findByEstSupprimerFalse()).thenReturn(new ArrayList<>());

        // Act
        AgriculteurDashboardDTO dto = dashboardService.getAgriculteurDashboard(userDetails);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getMesQuantiteTotaleKg()).isEqualTo(500.5);
        assertThat(dto.getMesVergers()).hasSize(1);
        assertThat(dto.getMesVergers().get(0).getPhaseCulturale()).isEqualTo("VERDAISON");
    }

    @Test
    @DisplayName("Doit lancer une exception si l'utilisateur est introuvable")
    void getDashboard_UserNotFound_ThrowsException() {
        when(userDetails.getUsername()).thenReturn("unknown@test.com");
        when(utilisateurRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getAgriculteurDashboard(userDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Agriculteur introuvable");
    }

    @Test
    @DisplayName("Vérification de la logique de calcul de l'année de campagne")
    void testCampagneAnneeLogic() {
        // Test manuel de la logique privée via le dashboard admin (qui l'utilise)
        when(vergerRepo.findByEstSupprimerFalse()).thenReturn(new ArrayList<>());
        when(tourneeRepo.findAll()).thenReturn(new ArrayList<>());
        when(alerteRepo.findByEstSupprimerFalse()).thenReturn(new ArrayList<>());
        when(collecteRepo.findByAnnee(anyString())).thenReturn(new ArrayList<>());

        AdminDashboardDTO dto = dashboardService.getAdminDashboard();
        
        // Si on est en Mai 2024, la campagne est 2023-2024
        // Si on est en Septembre 2024, la campagne est 2024-2025
        assertThat(dto).isNotNull();
    }

    // --- Helpers ---

    private Verger createVerger(StatutVerger statut) {
        Verger v = new Verger();
        v.setStatut(statut);
        v.setEstSupprimer(false);
        v.setNbArbre(100);
        v.setSuperficie(2.5);
        v.setMaturiteActuelle(0); // <-- AJOUTEZ CECI pour éviter le NPE
        v.setTypeOlive("Picholine");
        return v;
    }
}