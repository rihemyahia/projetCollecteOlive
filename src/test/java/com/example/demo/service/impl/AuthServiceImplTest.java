package com.example.demo.service.impl;

import com.example.demo.config.JwtUtils;
import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.enums.TypeTravailleur;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.utils.PasswordGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordGeneratorUtil passwordGeneratorUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Utilisateur utilisateur;
    private Utilisateur admin;
    private Utilisateur responsable;
    private Utilisateur agriculteur;
    private Utilisateur travailleur;
    private Utilisateur transporteur;

    @BeforeEach
    void setUp() {
        // Utilisateur standard
        utilisateur = new Utilisateur();
        utilisateur.setId("user-123");
        utilisateur.setEmail("user@test.com");
        utilisateur.setMotDePasse("encodedPassword");
        utilisateur.setNom("Dupont");
        utilisateur.setPrenom("Jean");
        utilisateur.setRole(Role.AGRICULTEUR);
        utilisateur.setEstActif(true);
        utilisateur.setCompteActif(true);
        utilisateur.setDateCreation(new Date());

        // Admin
        admin = new Utilisateur();
        admin.setId("admin-123");
        admin.setEmail("admin@cooperative.com");
        admin.setMotDePasse("adminEncoded");
        admin.setNom("Admin");
        admin.setPrenom("Super");
        admin.setRole(Role.ADMIN);
        admin.setEstActif(true);
        admin.setCompteActif(true);

        // Responsable
        responsable = new Utilisateur();
        responsable.setId("resp-123");
        responsable.setEmail("responsable@test.com");
        responsable.setMotDePasse("respEncoded");
        responsable.setNom("Martin");
        responsable.setPrenom("Paul");
        responsable.setRole(Role.RESPONSABLE);
        responsable.setEstActif(true);
        responsable.setCompteActif(true);
        responsable.setDatePrisePoste(new Date());

        // Agriculteur
        agriculteur = new Utilisateur();
        agriculteur.setId("agri-123");
        agriculteur.setEmail("agriculteur@test.com");
        agriculteur.setMotDePasse("agriEncoded");
        agriculteur.setNom("Durand");
        agriculteur.setPrenom("Pierre");
        agriculteur.setRole(Role.AGRICULTEUR);
        agriculteur.setEstActif(true);
        agriculteur.setCompteActif(false);

        // Travailleur
        travailleur = new Utilisateur();
        travailleur.setId("trav-123");
        travailleur.setEmail("travailleur@test.com");
        travailleur.setMotDePasse("travEncoded");
        travailleur.setNom("Bernard");
        travailleur.setPrenom("Luc");
        travailleur.setRole(Role.TRAVAILLEUR);
        travailleur.setEstActif(true);
        travailleur.setCompteActif(false);
        travailleur.setCin("12345678");
        travailleur.setStatutEmploye(TypeTravailleur.PERMANENT);

        // Transporteur
        transporteur = new Utilisateur();
        transporteur.setId("trans-123");
        transporteur.setEmail("transporteur@test.com");
        transporteur.setMotDePasse("transEncoded");
        transporteur.setNom("Moreau");
        transporteur.setPrenom("Jacques");
        transporteur.setRole(Role.TRANSPORTEUR);
        transporteur.setEstActif(true);
        transporteur.setCompteActif(true);
        transporteur.setPermis("B");
        transporteur.setTarifKm(1.5);
    }

    // ========== TESTS LOGIN ==========

    @Test
    void login_Success() {
        when(utilisateurRepository.findByEmail("user@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtils.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-123");

        Map<String, Object> result = authService.login("user@test.com", "password123");

        assertNotNull(result);
        assertEquals("user-123", result.get("id"));
        assertEquals("user@test.com", result.get("email"));
        assertEquals("jwt-token-123", result.get("token"));
        verify(utilisateurRepository, times(1)).findByEmail("user@test.com");
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        when(utilisateurRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.login("unknown@test.com", "password");
        });
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        when(utilisateurRepository.findByEmail("user@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.login("user@test.com", "wrongpassword");
        });
    }

    @Test
    void login_AccountInactive_ThrowsException() {
        utilisateur.setEstActif(false);
        when(utilisateurRepository.findByEmail("user@test.com")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.login("user@test.com", "password123");
        });
    }

    // ========== TESTS LOGIN RESPONSABLE ==========

    @Test
    void loginResponsable_Success() {
        when(utilisateurRepository.findByEmailAndRole("responsable@test.com", Role.RESPONSABLE))
                .thenReturn(Optional.of(responsable));
        when(passwordEncoder.matches("resp123", "respEncoded")).thenReturn(true);
        when(jwtUtils.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

        Map<String, Object> result = authService.loginResponsable("responsable@test.com", "resp123");

        assertNotNull(result);
        assertEquals(Role.RESPONSABLE, result.get("role"));
    }

    @Test
    void loginResponsable_NotResponsable_ThrowsException() {
        when(utilisateurRepository.findByEmailAndRole("user@test.com", Role.RESPONSABLE))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.loginResponsable("user@test.com", "password");
        });
    }

    // ========== TESTS LOGIN ADMIN ==========

    @Test
    void loginAdmin_Success() {
        when(utilisateurRepository.findByEmailAndRole("admin@cooperative.com", Role.ADMIN))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "adminEncoded")).thenReturn(true);
        when(jwtUtils.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

        Map<String, Object> result = authService.loginAdmin("admin@cooperative.com", "admin123");

        assertNotNull(result);
        assertEquals(Role.ADMIN, result.get("role"));
    }

    // ========== TESTS CREER UTILISATEUR PAR ADMIN ==========

    @Test
    void creerUtilisateurParAdmin_Success() {
        Utilisateur newUser = new Utilisateur();
        newUser.setEmail("newuser@test.com");
        newUser.setNom("Nouveau");
        newUser.setPrenom("User");
        newUser.setRole(Role.AGRICULTEUR);

        when(utilisateurRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(passwordGeneratorUtil.generateSecurePassword()).thenReturn("GeneratedPass123");
        when(passwordEncoder.encode("GeneratedPass123")).thenReturn("encodedGeneratedPass");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(newUser);
        doNothing().when(emailService).envoyerMotDePasse(anyString(), anyString(), anyString(), anyString());

        Map<String, Object> result = authService.creerUtilisateurParAdmin(newUser);

        assertNotNull(result);
        assertEquals("Utilisateur créé avec succès", result.get("message"));
        verify(utilisateurRepository, times(1)).save(any(Utilisateur.class));
    }

    @Test
    void creerUtilisateurParAdmin_EmailExists_ThrowsException() {
        Utilisateur newUser = new Utilisateur();
        newUser.setEmail("existing@test.com");
        
        when(utilisateurRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.creerUtilisateurParAdmin(newUser);
        });
    }

    // ========== TESTS VALIDATION PAR ROLE ==========

    @Test
    void creerUtilisateurParAdmin_TravailleurSansCIN_ThrowsException() {
        Utilisateur travailleurSansCIN = new Utilisateur();
        travailleurSansCIN.setEmail("travailleur@test.com");
        travailleurSansCIN.setRole(Role.TRAVAILLEUR);
        travailleurSansCIN.setStatutEmploye(TypeTravailleur.SAISONNIER);
        travailleurSansCIN.setCin(null);

        when(utilisateurRepository.existsByEmail("travailleur@test.com")).thenReturn(false);
        when(passwordGeneratorUtil.generateSecurePassword()).thenReturn("pass123");
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");

        assertThrows(RuntimeException.class, () -> {
            authService.creerUtilisateurParAdmin(travailleurSansCIN);
        });
    }

    @Test
    void creerUtilisateurParAdmin_TransporteurSansPermis_ThrowsException() {
        Utilisateur transporteurSansPermis = new Utilisateur();
        transporteurSansPermis.setEmail("transporteur@test.com");
        transporteurSansPermis.setRole(Role.TRANSPORTEUR);
        transporteurSansPermis.setPermis(null);
        transporteurSansPermis.setTarifKm(1.5);

        when(utilisateurRepository.existsByEmail("transporteur@test.com")).thenReturn(false);
        when(passwordGeneratorUtil.generateSecurePassword()).thenReturn("pass123");
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");

        assertThrows(RuntimeException.class, () -> {
            authService.creerUtilisateurParAdmin(transporteurSansPermis);
        });
    }

    // ========== TESTS GESTION DES UTILISATEURS ==========

    @Test
    void listerUtilisateurs_Success() {
        List<Utilisateur> utilisateurs = Arrays.asList(utilisateur, admin);
        when(utilisateurRepository.findByEstSupprimeFalse()).thenReturn(utilisateurs);

        List<Utilisateur> result = authService.listerUtilisateurs();

        assertEquals(2, result.size());
        verify(utilisateurRepository, times(1)).findByEstSupprimeFalse();
    }

    @Test
    void trouverUtilisateurParId_Success() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));

        Optional<Utilisateur> result = authService.trouverUtilisateurParId("user-123");

        assertTrue(result.isPresent());
        assertEquals("user-123", result.get().getId());
    }

    @Test
    void mettreAJourUtilisateur_Success() {
        Utilisateur updatedInfo = new Utilisateur();
        updatedInfo.setNom("NewName");
        updatedInfo.setPrenom("NewFirst");
        updatedInfo.setTelephone("0612345678");
        updatedInfo.setRole(Role.ADMIN);
        updatedInfo.setAdresse("New Address");

        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        Utilisateur result = authService.mettreAJourUtilisateur("user-123", updatedInfo);

        assertNotNull(result);
        verify(utilisateurRepository, times(1)).save(utilisateur);
    }

    @Test
    void supprimerUtilisateur_SoftDelete_Success() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        authService.supprimerUtilisateur("user-123");

        assertTrue(utilisateur.isEstSupprime());
        verify(utilisateurRepository, times(1)).save(utilisateur);
    }

    // ========== TESTS ACTIVATION COMPTES ==========

    @Test
    void getAgriculteursEnAttente_Success() {
        List<Utilisateur> agriculteursAttente = Arrays.asList(agriculteur);
        when(utilisateurRepository.findByRoleAndCompteActifFalse(Role.AGRICULTEUR))
                .thenReturn(agriculteursAttente);

        List<Utilisateur> result = authService.getAgriculteursEnAttente();

        assertEquals(1, result.size());
        assertEquals(Role.AGRICULTEUR, result.get(0).getRole());
        assertFalse(result.get(0).isCompteActif());
    }

    @Test
    void activerAgriculteur_Success() {
        when(utilisateurRepository.findById("agri-123")).thenReturn(Optional.of(agriculteur));
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(agriculteur);
        doNothing().when(emailService).envoyerMotDePasse(anyString(), anyString(), anyString(), anyString());

        Utilisateur result = authService.activerAgriculteur("agri-123", "newPass123");

        assertNotNull(result);
        assertTrue(result.isCompteActif());
        verify(utilisateurRepository, times(1)).save(agriculteur);
    }

    @Test
    void activerAgriculteur_AlreadyActive_ThrowsException() {
        agriculteur.setCompteActif(true);
        when(utilisateurRepository.findById("agri-123")).thenReturn(Optional.of(agriculteur));

        assertThrows(RuntimeException.class, () -> {
            authService.activerAgriculteur("agri-123", "newPass");
        });
    }

    @Test
    void activerTravailleur_Success() {
        when(utilisateurRepository.findById("trav-123")).thenReturn(Optional.of(travailleur));
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(travailleur);
        doNothing().when(emailService).envoyerMotDePasse(anyString(), anyString(), anyString(), anyString());

        Utilisateur result = authService.activerTravailleur("trav-123", "newPass123");

        assertNotNull(result);
        assertTrue(result.isCompteActif());
    }

    // ========== TESTS PROFIL ==========

    @Test
    void getProfil_Success() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));

        Map<String, Object> result = authService.getProfil("user-123");

        assertNotNull(result);
        assertEquals("user-123", result.get("id"));
        assertEquals("user@test.com", result.get("email"));
        assertEquals("Dupont", result.get("nom"));
        assertEquals("Jean", result.get("prenom"));
    }

    @Test
    void getProfil_UserNotFound_ThrowsException() {
        when(utilisateurRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.getProfil("unknown");
        });
    }

    @Test
    void mettreAJourProfil_Success() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nom", "UpdatedName");
        updates.put("telephone", "0698765432");
        updates.put("photoProfile", "http://photo.com/image.jpg");

        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        Utilisateur result = authService.mettreAJourProfil("user-123", updates);

        assertNotNull(result);
        assertEquals("UpdatedName", result.getNom());
        assertEquals("0698765432", result.getTelephone());
        assertEquals("http://photo.com/image.jpg", result.getPhotoProfile());
    }

    @Test
    void changerMotDePasse_Success() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("oldPass123", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        assertDoesNotThrow(() -> {
            authService.changerMotDePasse("user-123", "oldPass123", "newPass123");
        });
        verify(utilisateurRepository, times(1)).save(utilisateur);
    }

    @Test
    void changerMotDePasse_WrongOldPassword_ThrowsException() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.changerMotDePasse("user-123", "wrongPass", "newPass");
        });
    }

    // ========== TESTS CHANGER MOT DE PASSE ADMIN ==========

    @Test
    void changerMotDePasseAdmin_Success() {
        when(utilisateurRepository.findById("admin-123")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("newAdminPass")).thenReturn("encodedNewAdminPass");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(admin);
        doNothing().when(emailService).envoyerMotDePasse(anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> {
            authService.changerMotDePasseAdmin("admin-123", "newAdminPass");
        });
        verify(utilisateurRepository, times(1)).save(admin);
    }

    @Test
    void changerMotDePasseAdmin_EmptyPassword_ThrowsException() {
        when(utilisateurRepository.findById("admin-123")).thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class, () -> {
            authService.changerMotDePasseAdmin("admin-123", "");
        });
    }

    @Test
    void changerMotDePasseAdmin_ShortPassword_ThrowsException() {
        when(utilisateurRepository.findById("admin-123")).thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class, () -> {
            authService.changerMotDePasseAdmin("admin-123", "123");
        });
    }

    // ========== TESTS COMPTE ACTIVATION/DESACTIVATION ==========

    @Test
    void desactiverCompte_Success() {
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        Utilisateur result = authService.desactiverCompte("user-123");

        assertFalse(result.isCompteActif());
        assertFalse(result.getEstActif());
    }

    @Test
    void reactiverCompte_Success() {
        utilisateur.setCompteActif(false);
        utilisateur.setEstActif(false);
        
        when(utilisateurRepository.findById("user-123")).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        Utilisateur result = authService.reactiverCompte("user-123");

        assertTrue(result.isCompteActif());
        assertTrue(result.getEstActif());
    }

    // ========== TESTS COMPTEURS ==========

    @Test
    void compterAgriculteursEnAttente_Success() {
        when(utilisateurRepository.countByRoleAndCompteActifFalse(Role.AGRICULTEUR)).thenReturn(5L);

        long count = authService.compterAgriculteursEnAttente();

        assertEquals(5L, count);
    }

    @Test
    void compterTravailleursEnAttente_Success() {
        when(utilisateurRepository.countByRoleAndCompteActifFalse(Role.TRAVAILLEUR)).thenReturn(3L);

        long count = authService.compterTravailleursEnAttente();

        assertEquals(3L, count);
    }

    // ========== TEST AUTHENTICATE ==========

    @Test
    void authenticate_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        Authentication result = authService.authenticate("user@test.com", "password");

        assertNotNull(result);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_Failure_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Auth failed"));

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate("user@test.com", "wrongpass");
        });
    }
}