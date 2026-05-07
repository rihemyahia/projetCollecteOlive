package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.QualiteHuile;
import com.example.demo.model.enums.StatutExtractionHuile;
import com.example.demo.repository.ExtractionHuileRepository;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponsablePressoirServiceImplTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private TourneeRepository tourneeRepository;
    @Mock private ExtractionHuileRepository extractionHuileRepository;
    @Mock private UserDetails currentUser;

    @InjectMocks
    private ResponsablePressoirServiceImpl responsablePressoirService;

    private Utilisateur responsable;
    private Pressoir pressoir;
    private Tournee tournee;
    private ExtractionHuile extraction;
    private final String RESPONSABLE_ID = "resp-123";
    private final String TOURNEE_ID = "tour-456";
    private final String EXTRACTION_ID = "ext-789";

    @BeforeEach
    void setUp() {
        pressoir = new Pressoir();
        pressoir.setId("press-1");
        pressoir.setNom("Moulin Test");

        responsable = new Utilisateur();
        responsable.setId(RESPONSABLE_ID);
        responsable.setEmail("responsable@test.com");
        responsable.setRole(Role.RESPONSABLE_PRESSOIR);
        responsable.setPressoir(pressoir);

        tournee = new Tournee();
        tournee.setId(TOURNEE_ID);
        tournee.setStatut(StatutTournee.LIVREE);
        tournee.setResponsablePressoir(responsable);
        tournee.setCollecte(new Collecte());

        extraction = ExtractionHuile.builder()
                .id(EXTRACTION_ID)
                .responsablePressoir(responsable)
                .quantiteOlivesRecueKg(1000.0)
                .statut(StatutExtractionHuile.RECUE)
                .build();

        lenient().when(currentUser.getUsername()).thenReturn("responsable@test.com");
        lenient().when(utilisateurRepository.findByEmail(anyString())).thenReturn(Optional.of(responsable));
    }

    // --- TEST 1 : RÉCEPTION D'UNE TOURNÉE ---

    @Test
    @DisplayName("Réceptionner une tournée avec succès")
    void receptionnerTournee_Success() {
        ReceptionOlivesRequest request = new ReceptionOlivesRequest();
        request.setQuantiteOlivesRecueKg(1000.0);
        request.setObservations("Olives de bonne qualité");

        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee));
        when(extractionHuileRepository.existsByTourneeId(TOURNEE_ID)).thenReturn(false);
        when(extractionHuileRepository.save(any(ExtractionHuile.class))).thenAnswer(i -> i.getArguments()[0]);

        ExtractionHuileResponse result = responsablePressoirService.receptionnerTournee(TOURNEE_ID, request, currentUser);

        assertThat(result).isNotNull();
        assertThat(result.getStatut()).isEqualTo(StatutExtractionHuile.RECUE);
        assertThat(result.getQuantiteOlivesRecueKg()).isEqualTo(1000.0);
        verify(extractionHuileRepository).save(any());
    }

    @Test
    @DisplayName("Échec réception : Tournée déjà réceptionnée")
    void receptionnerTournee_AlreadyReceived() {
        when(tourneeRepository.findById(TOURNEE_ID)).thenReturn(Optional.of(tournee));
        when(extractionHuileRepository.existsByTourneeId(TOURNEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> responsablePressoirService.receptionnerTournee(TOURNEE_ID, new ReceptionOlivesRequest(), currentUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja ete receptionnee");
    }

    // --- TEST 2 : EXTRACTION DE L'HUILE ---

    @Test
    @DisplayName("Enregistrer l'extraction avec succès")
    void extraireHuile_Success() {
        ExtractionHuileRequest request = new ExtractionHuileRequest();
        request.setQuantiteHuileExtraiteL(180.0); // 18% rendement
        request.setQualiteHuile(QualiteHuile.EXTRA_VIERGE);

        when(extractionHuileRepository.findById(EXTRACTION_ID)).thenReturn(Optional.of(extraction));
        when(extractionHuileRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ExtractionHuileResponse result = responsablePressoirService.extraireHuile(EXTRACTION_ID, request, currentUser);

        assertThat(result.getStatut()).isEqualTo(StatutExtractionHuile.EXTRAITE);
        assertThat(result.getRendementPourcentage()).isEqualTo(18.0);
    }

    @Test
    @DisplayName("Échec extraction : Quantité huile > Quantité olives")
    void extraireHuile_InvalidQuantities() {
        ExtractionHuileRequest request = new ExtractionHuileRequest();
        request.setQuantiteHuileExtraiteL(1500.0); // > 1000kg d'olives

        when(extractionHuileRepository.findById(EXTRACTION_ID)).thenReturn(Optional.of(extraction));

        assertThatThrownBy(() -> responsablePressoirService.extraireHuile(EXTRACTION_ID, request, currentUser))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- TEST 3 : VALIDATION FINALE ---

    @Test
    @DisplayName("Valider l'extraction avec succès")
    void validerExtraction_Success() {
        extraction.setStatut(StatutExtractionHuile.EXTRAITE);
        when(extractionHuileRepository.findById(EXTRACTION_ID)).thenReturn(Optional.of(extraction));
        when(extractionHuileRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ExtractionHuileResponse result = responsablePressoirService.validerExtraction(EXTRACTION_ID, currentUser);

        assertThat(result.getStatut()).isEqualTo(StatutExtractionHuile.VALIDEE);
    }

    // --- TEST 4 : SÉCURITÉ ---

    @Test
    @DisplayName("Échec : Accès à une extraction d'un autre pressoir")
    void security_AccessDenied() {
        Utilisateur autreResponsable = new Utilisateur();
        autreResponsable.setId("autre-id");
        extraction.setResponsablePressoir(autreResponsable);

        when(extractionHuileRepository.findById(EXTRACTION_ID)).thenReturn(Optional.of(extraction));

        assertThatThrownBy(() -> responsablePressoirService.validerExtraction(EXTRACTION_ID, currentUser))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Échec : Utilisateur n'est pas un responsable pressoir")
    void security_WrongRole() {
        responsable.setRole(Role.ADMIN); // On change le rôle du mock chargé par setUp

        assertThatThrownBy(() -> responsablePressoirService.getExtractions(currentUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acces reserve");
    }

    // --- TEST 5 : DASHBOARD ---

    @Test
    @DisplayName("Générer le dashboard avec calculs corrects")
    void getDashboard_Calculations() {
        List<ExtractionHuile> list = List.of(
                ExtractionHuile.builder().quantiteOlivesRecueKg(1000.0).quantiteHuileExtraiteL(200.0).statut(StatutExtractionHuile.VALIDEE).responsablePressoir(responsable).build(),
                ExtractionHuile.builder().quantiteOlivesRecueKg(500.0).quantiteHuileExtraiteL(100.0).statut(StatutExtractionHuile.RECUE).responsablePressoir(responsable).build()
        );

        when(extractionHuileRepository.findByResponsablePressoirId(RESPONSABLE_ID)).thenReturn(list);
        when(tourneeRepository.findByResponsablePressoirIdAndStatut(anyString(), any(), any())).thenReturn(Collections.emptyList());

        PressoirDashboardResponse result = responsablePressoirService.getDashboard(currentUser);

        assertThat(result.getTotalOlivesRecuesKg()).isEqualTo(1500.0);
        assertThat(result.getTotalHuileExtraiteL()).isEqualTo(300.0);
        assertThat(result.getRendementMoyenPourcentage()).isEqualTo(20.0);
        assertThat(result.getTourneesEnAttenteExtraction()).isEqualTo(1);
    }
}