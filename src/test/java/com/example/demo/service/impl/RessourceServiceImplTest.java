package com.example.demo.service.impl;

import com.example.demo.model.Ressource;
import com.example.demo.model.TypeRessource;
import com.example.demo.repository.RessourceRepository;
import com.example.demo.service.RessourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RessourceServiceImplTest {

    @Mock
    private RessourceRepository ressourceRepository;

    @Mock
    private RessourceService ressourceService;

    @InjectMocks
    private BenneServiceImpl benneService;

    private Ressource benne;
    private Ressource tracteur;

    @BeforeEach
    void setUp() {
        benne = new Ressource();
        benne.setId("benne-123");
        benne.setNom("Benne Test");
        benne.setType(TypeRessource.BENNE);
        benne.setCapaciteKg(1000.0);
        benne.setQuantiteChargeeActuelle(0.0);
        benne.setTauxRemplissage(0.0);
        benne.setEstPleine(false);
        benne.setStatut("DISPONIBLE");

        tracteur = new Ressource();
        tracteur.setId("tracteur-123");
        tracteur.setNom("Tracteur Test");
        tracteur.setType(TypeRessource.TRACTEUR);
        tracteur.setStatut("DISPONIBLE");
    }

    @Test
    void testCreerBenne_Success() {
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.creerBenne(benne);

        assertNotNull(result);
        assertEquals(TypeRessource.BENNE, result.getType());
        assertEquals(0.0, result.getQuantiteChargeeActuelle());
        assertEquals(0.0, result.getTauxRemplissage());
        assertFalse(result.getEstPleine());
        assertEquals("DISPONIBLE", result.getStatut());
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testCreerBenne_NullCapacity_ThrowsException() {
        benne.setCapaciteKg(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.creerBenne(benne);
        });
        assertEquals("La capacité d'une benne doit être positive", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testCreerBenne_ZeroCapacity_ThrowsException() {
        benne.setCapaciteKg(0.0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.creerBenne(benne);
        });
        assertEquals("La capacité d'une benne doit être positive", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testGetBenneById_Success() {
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);

        Ressource result = benneService.getBenneById("benne-123");

        assertNotNull(result);
        assertEquals("benne-123", result.getId());
        assertEquals(TypeRessource.BENNE, result.getType());
        verify(ressourceService, times(1)).getRessourceById("benne-123");
    }

    @Test
    void testGetBenneById_NotABenne_ThrowsException() {
        Ressource nonBenne = new Ressource();
        nonBenne.setType(TypeRessource.TRACTEUR);
        when(ressourceService.getRessourceById("tracteur-123")).thenReturn(nonBenne);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.getBenneById("tracteur-123");
        });
        assertEquals("Cette ressource n'est pas une benne", exception.getMessage());
    }

    @Test
    void testListerBennes() {
        List<Ressource> bennes = Arrays.asList(benne, new Ressource());
        when(ressourceRepository.findAllBennes()).thenReturn(bennes);

        List<Ressource> result = benneService.listerBennes();

        assertEquals(2, result.size());
        verify(ressourceRepository, times(1)).findAllBennes();
    }

    @Test
    void testListerBennesDisponibles() {
        List<Ressource> bennesDisponibles = Arrays.asList(benne);
        when(ressourceRepository.findBennesByStatut("DISPONIBLE")).thenReturn(bennesDisponibles);

        List<Ressource> result = benneService.listerBennesDisponibles();

        assertEquals(1, result.size());
        verify(ressourceRepository, times(1)).findBennesByStatut("DISPONIBLE");
    }

    @Test
    void testListerBennesPleines() {
        List<Ressource> bennesPleines = Arrays.asList(benne);
        when(ressourceRepository.findFullBennes()).thenReturn(bennesPleines);

        List<Ressource> result = benneService.listerBennesPleines();

        assertEquals(1, result.size());
        verify(ressourceRepository, times(1)).findFullBennes();
    }

    @Test
    void testMettreAJourBenne_Success() {
        Ressource update = new Ressource();
        update.setNom("Nouveau Nom");
        update.setImmatriculation("ABC-123");
        update.setStatut("MAINTENANCE");
        update.setCapaciteKg(2000.0);

        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.mettreAJourBenne("benne-123", update);

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testMettreAJourBenne_WithoutCapacity() {
        Ressource update = new Ressource();
        update.setNom("Nouveau Nom");
        update.setImmatriculation("ABC-123");
        update.setStatut("MAINTENANCE");

        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.mettreAJourBenne("benne-123", update);

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testSupprimerBenne_Success() {
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        doNothing().when(ressourceRepository).delete(benne);

        benneService.supprimerBenne("benne-123");

        verify(ressourceRepository, times(1)).delete(benne);
    }

    @Test
    void testAjouterCharge_Success() {
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.ajouterCharge("benne-123", 500.0);

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testAjouterCharge_NegativeQuantity_ThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.ajouterCharge("benne-123", -100.0);
        });
        assertEquals("La quantité à ajouter doit être positive", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testAjouterCharge_NullQuantity_ThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.ajouterCharge("benne-123", null);
        });
        assertEquals("La quantité à ajouter doit être positive", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testViderBenne_Success() {
        benne.setQuantiteChargeeActuelle(500.0);
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.viderBenne("benne-123");

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testEnregistrerMaintenance() {
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.enregistrerMaintenance("benne-123", "Test maintenance", 100.0);

        assertNotNull(result);
        assertEquals("MAINTENANCE", result.getStatut());
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testTerminerMaintenance_Success() {
        benne.setStatut("MAINTENANCE");
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.terminerMaintenance("benne-123");

        assertNotNull(result);
        assertEquals("DISPONIBLE", result.getStatut());
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testTerminerMaintenance_NotInMaintenance_ThrowsException() {
        benne.setStatut("DISPONIBLE");
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.terminerMaintenance("benne-123");
        });
        assertEquals("Cette benne n'est pas en maintenance", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testAssignerTracteur_Success() {
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceService.getRessourceById("tracteur-123")).thenReturn(tracteur);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.assignerTracteur("benne-123", "tracteur-123");

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testAssignerTracteur_InvalidResource_ThrowsException() {
        Ressource nonTracteur = new Ressource();
        nonTracteur.setType(TypeRessource.BENNE);
        
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceService.getRessourceById("autre-123")).thenReturn(nonTracteur);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            benneService.assignerTracteur("benne-123", "autre-123");
        });
        assertEquals("Cette ressource n'est pas un tracteur", exception.getMessage());
        verify(ressourceRepository, never()).save(any());
    }

    @Test
    void testRetirerTracteur_Success() {
        benne.setTracteur(tracteur);
        when(ressourceService.getRessourceById("benne-123")).thenReturn(benne);
        when(ressourceRepository.save(any(Ressource.class))).thenReturn(benne);

        Ressource result = benneService.retirerTracteur("benne-123");

        assertNotNull(result);
        verify(ressourceRepository, times(1)).save(benne);
    }

    @Test
    void testListerBennesDuTracteur() {
        List<Ressource> bennes = Arrays.asList(benne);
        when(ressourceRepository.findBennesByTracteurId("tracteur-123")).thenReturn(bennes);

        List<Ressource> result = benneService.listerBennesDuTracteur("tracteur-123");

        assertEquals(1, result.size());
        verify(ressourceRepository, times(1)).findBennesByTracteurId("tracteur-123");
    }
}