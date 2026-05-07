package com.example.demo.service.impl;

import com.example.demo.dto.TerminerTourneeRequest;
import com.example.demo.dto.TourneeRequest;
import com.example.demo.dto.TourneeResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.repository.*;
import com.example.demo.service.CollecteService;
import com.example.demo.service.VergerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.demo.model.enums.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourneeServiceImplTest {

    @Mock private TourneeRepository tourneeRepo;
    @Mock private VergerRepository vergerRepo;
    @Mock private RessourceRepository ressourceRepo;
    @Mock private UtilisateurRepository utilisateurRepo;
    @Mock private CollecteRepository collecteRepo;
    @Mock private CollecteService collecteService;
    @Mock private VergerService vergerService;
    @Mock private UserDetails currentUser;

    @InjectMocks
    private TourneeServiceImpl tourneeService;

    private Verger verger;
    private Ressource benne;
    private Ressource tracteur;
    private Utilisateur travailleur;
    private Utilisateur responsablePressoir;
    private Pressoir pressoir;
    private Collecte collecte;
    private TourneeRequest request;
    private Tournee tournee;

    @BeforeEach
    void setUp() {
        pressoir = new Pressoir();
        pressoir.setId("pressoir-1");
        pressoir.setNom("Pressoir de Test");

        responsablePressoir = new Utilisateur();
        responsablePressoir.setId("rp-1");
        responsablePressoir.setRole(Role.RESPONSABLE_PRESSOIR);
        responsablePressoir.setPressoir(pressoir);

        verger = new Verger();
        verger.setId("verger-1");
        verger.setNbArbre(100);
        verger.setStatut(StatutVerger.NON_RECOLTE);
        verger.setEstSupprimer(false);

        benne = new Ressource();
        benne.setId("benne-1");
        benne.setType(TypeRessource.BENNE);
        benne.setStatut("DISPONIBLE");
        benne.setCapaciteKg(1000.0);

        tracteur = new Ressource();
        tracteur.setId("tracteur-1");
        tracteur.setType(TypeRessource.TRACTEUR);
        tracteur.setStatut("DISPONIBLE");

        travailleur = new Utilisateur();
        travailleur.setId("worker-1");
        travailleur.setRole(Role.TRAVAILLEUR);

        collecte = new Collecte();
        collecte.setId("collecte-1");

        request = new TourneeRequest();
        request.setVergerId("verger-1");
        request.setBenneId("benne-1");
        request.setTracteurId("tracteur-1");
        request.setTravailleurIds(List.of("worker-1"));
        request.setResponsablePressoirId("rp-1");
        request.setNbreArbre(50);
        request.setDateDebut(new Date());
        request.setDateFin(new Date(System.currentTimeMillis() + 3600000));

        tournee = new Tournee();
        tournee.setId("tournee-1");
        tournee.setVerger(verger);
        tournee.setStatut(StatutTournee.PLANIFIEE);

        lenient().when(currentUser.getUsername()).thenReturn("admin@test.com");
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(currentUser).getAuthorities();
    }

    // --- TESTS DE CRÉATION ---

    @Test
    @DisplayName("Créer une tournée avec succès")
    void creer_Tournee_Succes() {
        when(vergerRepo.findById("verger-1")).thenReturn(Optional.of(verger));
        when(ressourceRepo.findById("benne-1")).thenReturn(Optional.of(benne));
        when(ressourceRepo.findById("tracteur-1")).thenReturn(Optional.of(tracteur));
        when(utilisateurRepo.findById("worker-1")).thenReturn(Optional.of(travailleur));
        when(utilisateurRepo.findById("rp-1")).thenReturn(Optional.of(responsablePressoir));
        
        when(collecteService.getCampagneAnnee(any())).thenReturn("2024");
        when(collecteRepo.findByVergerIdAndAnnee(anyString(), anyString())).thenReturn(Optional.of(collecte));
        when(tourneeRepo.save(any(Tournee.class))).thenReturn(tournee);

        TourneeResponse response = tourneeService.creer(request, currentUser);

        assertThat(response).isNotNull();
        verify(tourneeRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("Échec création : Verger inexistant")
    void creer_Tournee_VergerInexistant_Erreur() {
        when(vergerRepo.findById("verger-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourneeService.creer(request, currentUser))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- TESTS DE CYCLE DE VIE ---

    @Test
    @DisplayName("Démarrer une tournée planifiée")
    void demarrer_Tournee_Succes() {
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));
        when(tourneeRepo.save(any())).thenReturn(tournee);

        TourneeResponse response = tourneeService.demarrer("tournee-1", currentUser);

        assertThat(response.getStatut()).isEqualTo(StatutTournee.EN_COURS);
    }

    @Test
    @DisplayName("Terminer une tournée en cours")
    void terminer_Tournee_Succes() {
        tournee.setStatut(StatutTournee.EN_COURS);
        tournee.setBenne(benne);
        
        TerminerTourneeRequest termRequest = new TerminerTourneeRequest();
        termRequest.setQuantiteCollecteeKg(500.0);

        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));
        when(ressourceRepo.findById("benne-1")).thenReturn(Optional.of(benne));
        when(tourneeRepo.save(any())).thenReturn(tournee);

        TourneeResponse response = tourneeService.terminer("tournee-1", termRequest, currentUser);

        assertThat(response.getStatut()).isEqualTo(StatutTournee.TERMINEE);
        verify(vergerService).recomputeStatutForVerger(any());
    }

    @Test
    @DisplayName("Échec démarrage : Tournée déjà terminée")
    void demarrer_Tournee_DejaTerminee_Erreur() {
        tournee.setStatut(StatutTournee.TERMINEE);
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));

        assertThatThrownBy(() -> tourneeService.demarrer("tournee-1", currentUser))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PLANIFIÉE");
    }

    // --- TESTS DE SÉCURITÉ ET ACCÈS ---

    @Test
    @DisplayName("Échec accès : Utilisateur non autorisé")
    void getById_NonAutorise_Erreur() {
        // Simuler un utilisateur sans ROLE_ADMIN et sans lien avec le verger
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(currentUser).getAuthorities();
        when(utilisateurRepo.findByEmail(any())).thenReturn(Optional.of(new Utilisateur())); // Utilisateur lambda
        
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));

        assertThatThrownBy(() -> tourneeService.getById("tournee-1", currentUser))
            .isInstanceOf(SecurityException.class);
    }

    // --- TESTS DE RECHERCHE ET SUPPRESSION ---

    @Test
    @DisplayName("Récupérer une tournée par ID")
    void getById_Succes() {
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));
        TourneeResponse response = tourneeService.getById("tournee-1", currentUser);
        assertThat(response.getId()).isEqualTo("tournee-1");
    }

    @Test
    @DisplayName("Supprimer une tournée planifiée")
    void supprimer_Tournee_Succes() {
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));
        
        tourneeService.supprimer("tournee-1", currentUser);
        
        verify(tourneeRepo).delete(tournee);
    }

    @Test
    @DisplayName("Échec suppression : Tournée déjà en cours")
    void supprimer_Tournee_EnCours_Erreur() {
        tournee.setStatut(StatutTournee.EN_COURS);
        when(tourneeRepo.findById("tournee-1")).thenReturn(Optional.of(tournee));

        assertThatThrownBy(() -> tourneeService.supprimer("tournee-1", currentUser))
            .isInstanceOf(IllegalStateException.class);
    }
}