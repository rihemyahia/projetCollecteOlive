package com.example.demo.service.impl;

import com.example.demo.dto.VergerRequest;
import com.example.demo.dto.VergerResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Geolocalisation;
import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VergerServiceImplTest {

    @Mock
    private VergerRepository vergerRepo;

    @Mock
    private UtilisateurRepository utilisateurRepo;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private VergerServiceImpl vergerService;

    private Utilisateur agriculteur;
    private Utilisateur responsable;
    private Utilisateur admin;
    private Verger verger;
    private VergerRequest vergerRequest;
    private Geolocalisation geolocalisation;

    @BeforeEach
    void setUp() {
        // Agriculteur
        agriculteur = new Utilisateur();
        agriculteur.setId(new ObjectId().toString());
        agriculteur.setEmail("agriculteur@test.com");
        agriculteur.setNom("Dupont");
        agriculteur.setPrenom("Jean");
        agriculteur.setRole(Role.AGRICULTEUR);

        // Responsable
        responsable = new Utilisateur();
        responsable.setId(new ObjectId().toString());
        responsable.setEmail("responsable@test.com");
        responsable.setNom("Martin");
        responsable.setPrenom("Paul");
        responsable.setFonction("Chef de secteur");
        responsable.setRole(Role.RESPONSABLE);

        // Admin
        admin = new Utilisateur();
        admin.setId(new ObjectId().toString());
        admin.setEmail("admin@cooperative.com");
        admin.setNom("Admin");
        admin.setPrenom("Super");
        admin.setRole(Role.ADMIN);

        // Géolocalisation
        geolocalisation = Geolocalisation.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .adresseIndicative("Paris, France")
                .build();

        // Verger
        verger = Verger.builder()
                .id(new ObjectId().toString())
                .agriculteur(agriculteur)
                .responsable(responsable)
                .superficie(10.5)
                .typeOlive("Chemlali")
                .rendementEstime(120.0)
                .maturiteActuelle(75)
                .nbArbre(150)
                .statut(StatutVerger.NON_RECOLTE)
                .estSupprimer(false)
                .dateCreation(new Date())
                .location(new GeoJsonPoint(2.3522, 48.8566))
                .geolocalisation(geolocalisation)
                .build();

        // VergerRequest
        vergerRequest = new VergerRequest();
        vergerRequest.setAgriculteurId(agriculteur.getId());
        vergerRequest.setResponsableId(responsable.getId());
        vergerRequest.setSuperficie(10.5);
        vergerRequest.setTypeOlive("Chemlali");
        vergerRequest.setRendementEstime(120.0);
        vergerRequest.setMaturiteActuelle(75);
        vergerRequest.setNbArbre(150);
        vergerRequest.setStatut(StatutVerger.NON_RECOLTE);
        vergerRequest.setLatitude(48.8566);
        vergerRequest.setLongitude(2.3522);
        vergerRequest.setAdresseIndicative("Paris, France");
    }

    // ========== TESTS CREATE ==========


    @Test
    void creer_Success_WithoutResponsableId() {
        vergerRequest.setResponsableId(null);
        
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(utilisateurRepo.findByEmail("responsable@test.com")).thenReturn(Optional.of(responsable));
        when(vergerRepo.save(any(Verger.class))).thenReturn(verger);
        when(userDetails.getUsername()).thenReturn("responsable@test.com");

        VergerResponse result = vergerService.creer(vergerRequest, userDetails);

        assertNotNull(result);
        verify(vergerRepo, times(1)).save(any(Verger.class));
    }

    @Test
    void creer_AgriculteurNotFound_ThrowsException() {
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.creer(vergerRequest, userDetails);
        });
    }

    @Test
    void creer_ResponsableNotFound_ThrowsException() {
        vergerRequest.setResponsableId("nonexistent-id");
        
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(utilisateurRepo.findById("nonexistent-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.creer(vergerRequest, userDetails);
        });
    }

    @Test
    void creer_WithoutResponsableAndUserNotFound_ThrowsException() {
        vergerRequest.setResponsableId(null);
        
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(utilisateurRepo.findByEmail("responsable@test.com")).thenReturn(Optional.empty());
        when(userDetails.getUsername()).thenReturn("responsable@test.com");

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.creer(vergerRequest, userDetails);
        });
    }

   



    // ========== TESTS READ ==========

    @Test
    void getById_Success() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));

        VergerResponse result = vergerService.getById(verger.getId());

        assertNotNull(result);
        assertEquals(verger.getId(), result.getId());
        assertEquals(agriculteur.getId(), result.getAgriculteurId());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(vergerRepo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.getById("nonexistent");
        });
    }

    @Test
    void getById_DeletedVerger_ThrowsException() {
        verger.setEstSupprimer(true);
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.getById(verger.getId());
        });
    }

    @Test
    void getAll_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(vergerRepo.findByEstSupprimerFalse()).thenReturn(vergers);

        List<VergerResponse> result = vergerService.getAll();

        assertEquals(1, result.size());
        verify(vergerRepo, times(1)).findByEstSupprimerFalse();
    }

    @Test
    void getByAgriculteur_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(vergerRepo.findActiveByAgriculteurId(new ObjectId(agriculteur.getId())))
                .thenReturn(vergers);

        List<VergerResponse> result = vergerService.getByAgriculteur(agriculteur.getId());

        assertEquals(1, result.size());
    }

    @Test
    void getByAgriculteur_AgriculteurNotFound_ThrowsException() {
        when(utilisateurRepo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.getByAgriculteur("nonexistent");
        });
    }

    @Test
    void getByResponsable_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(utilisateurRepo.findByEmail("responsable@test.com")).thenReturn(Optional.of(responsable));
        when(vergerRepo.findByResponsableIdAndEstSupprimerFalse(new ObjectId(responsable.getId())))
                .thenReturn(vergers);
        when(userDetails.getUsername()).thenReturn("responsable@test.com");

        List<VergerResponse> result = vergerService.getByResponsable(userDetails);

        assertEquals(1, result.size());
    }

    @Test
    void getByResponsable_ResponsableNotFound_ThrowsException() {
        when(utilisateurRepo.findByEmail("responsable@test.com")).thenReturn(Optional.empty());
        when(userDetails.getUsername()).thenReturn("responsable@test.com");

        assertThrows(ResourceNotFoundException.class, () -> {
            vergerService.getByResponsable(userDetails);
        });
    }

    @Test
    void getByStatut_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(vergerRepo.findByStatut(StatutVerger.NON_RECOLTE)).thenReturn(vergers);

        List<VergerResponse> result = vergerService.getByStatut(StatutVerger.NON_RECOLTE);

        assertEquals(1, result.size());
    }

    // ========== TESTS GEOLOCATION ==========

    @Test
    void getAllWithLocation_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(vergerRepo.findAllWithLocation()).thenReturn(vergers);

        List<VergerResponse> result = vergerService.getAllWithLocation();

        assertEquals(1, result.size());
        verify(vergerRepo, times(1)).findAllWithLocation();
    }

    @Test
    void getByAgriculteurWithLocation_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(vergerRepo.findByAgriculteurWithLocation(new ObjectId(agriculteur.getId())))
                .thenReturn(vergers);

        List<VergerResponse> result = vergerService.getByAgriculteurWithLocation(agriculteur.getId());

        assertEquals(1, result.size());
    }

    @Test
    void findNearby_Success() {
        List<Verger> vergers = Arrays.asList(verger);
        when(vergerRepo.findNearby(2.3522, 48.8566, 5000.0)).thenReturn(vergers);

        List<VergerResponse> result = vergerService.findNearby(2.3522, 48.8566, 5000.0);

        assertEquals(1, result.size());
        verify(vergerRepo, times(1)).findNearby(2.3522, 48.8566, 5000.0);
    }

    // ========== TESTS UPDATE ==========

    @Test
    void mettreAJour_Success() {
        VergerRequest updateRequest = new VergerRequest();
        updateRequest.setSuperficie(15.0);
        updateRequest.setTypeOlive("Picholine");
        updateRequest.setRendementEstime(150.0);
        updateRequest.setMaturiteActuelle(80);
        updateRequest.setNbArbre(200);
        updateRequest.setStatut(StatutVerger.EN_COURS);
        updateRequest.setLatitude(48.8570);
        updateRequest.setLongitude(2.3530);
        updateRequest.setAdresseIndicative("Nouvelle adresse");

        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(vergerRepo.save(any(Verger.class))).thenReturn(verger);

        VergerResponse result = vergerService.mettreAJour(verger.getId(), updateRequest);

        assertNotNull(result);
        verify(vergerRepo, times(1)).save(verger);
    }



    @Test
    void mettreAJourLocalisation_Success() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(vergerRepo.save(any(Verger.class))).thenReturn(verger);

        VergerResponse result = vergerService.mettreAJourLocalisation(
                verger.getId(), 48.8600, 2.3600, "Nouvelle adresse");

        assertNotNull(result);
        verify(vergerRepo, times(1)).save(verger);
    }

    @Test
    void mettreAJourLocalisation_NullLatitude_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            vergerService.mettreAJourLocalisation(verger.getId(), null, 2.3600, "Adresse");
        });
    }

    @Test
    void mettreAJourLocalisation_NullLongitude_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            vergerService.mettreAJourLocalisation(verger.getId(), 48.8600, null, "Adresse");
        });
    }

    @Test
    void changerStatut_Success() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(vergerRepo.save(any(Verger.class))).thenReturn(verger);

        VergerResponse result = vergerService.changerStatut(verger.getId(), StatutVerger.RECOLTE);

        assertNotNull(result);
        assertNotNull(verger.getDateDerniereRecolte());
        verify(vergerRepo, times(1)).save(verger);
    }

    @Test
    void desactiver_Success() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(vergerRepo.save(any(Verger.class))).thenReturn(verger);

        vergerService.desactiver(verger.getId());

        assertTrue(verger.getEstSupprimer());
        verify(vergerRepo, times(1)).save(verger);
    }

    // ========== TESTS OWNERSHIP CHECKS ==========

    @Test
    void verifierProprietaireVerger_WithAgriculteur_Success() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(userDetails.getUsername()).thenReturn("agriculteur@test.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptySet());

        assertDoesNotThrow(() -> {
            vergerService.verifierProprietaireVerger(verger.getId(), userDetails);
        });
    }




    @Test
    void verifierProprietaireVerger_UnauthorizedUser_ThrowsAccessDenied() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));
        when(userDetails.getUsername()).thenReturn("other@test.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptySet());

        assertThrows(AccessDeniedException.class, () -> {
            vergerService.verifierProprietaireVerger(verger.getId(), userDetails);
        });
    }

    @Test
    void verifierProprietaire_WithCorrectUser_Success() {
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(userDetails.getUsername()).thenReturn("agriculteur@test.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptySet());

        assertDoesNotThrow(() -> {
            vergerService.verifierProprietaire(agriculteur.getId(), userDetails);
        });
    }



    @Test
    void verifierProprietaire_UnauthorizedUser_ThrowsAccessDenied() {
        when(utilisateurRepo.findById(agriculteur.getId())).thenReturn(Optional.of(agriculteur));
        when(userDetails.getUsername()).thenReturn("other@test.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptySet());

        assertThrows(AccessDeniedException.class, () -> {
            vergerService.verifierProprietaire(agriculteur.getId(), userDetails);
        });
    }

    // ========== TESTS HELPER METHODS ==========

    @Test
    void toResponse_IncludesGeolocalisation() {
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));

        VergerResponse result = vergerService.getById(verger.getId());

        assertNotNull(result);
        assertNotNull(result.getGeolocalisation());
        assertEquals(48.8566, result.getGeolocalisation().getLatitude());
        assertEquals(2.3522, result.getGeolocalisation().getLongitude());
        assertEquals("Paris, France", result.getGeolocalisation().getAdresseIndicative());
    }

    @Test
    void toResponse_WithNullResponsable_ReturnsNullFields() {
        verger.setResponsable(null);
        when(vergerRepo.findById(verger.getId())).thenReturn(Optional.of(verger));

        VergerResponse result = vergerService.getById(verger.getId());

        assertNotNull(result);
        assertNull(result.getResponsableId());
        assertNull(result.getResponsableNom());
        assertNull(result.getResponsableEmail());
        assertNull(result.getResponsableFonction());
    }
}