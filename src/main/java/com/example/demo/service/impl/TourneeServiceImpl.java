package com.example.demo.service.impl;

import com.example.demo.dto.TerminerTourneeRequest;
import com.example.demo.dto.TourneeRequest;
import com.example.demo.dto.TourneeResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.repository.*;
import com.example.demo.service.TourneeService;
import com.example.demo.service.CollecteService;
import com.example.demo.service.VergerService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TourneeServiceImpl implements TourneeService {
    @Autowired
    private final TourneeRepository tourneeRepo;
    @Autowired
    private final VergerRepository vergerRepo;
    @Autowired
    private final RessourceRepository ressourceRepo;
    @Autowired
    private final UtilisateurRepository utilisateurRepo;
    @Autowired
    private final CollecteRepository collecteRepo;
    @Autowired
    private final CollecteService collecteService;
    private final VergerService vergerService;

    // Simple in-memory cache
    private final Map<String, List<TourneeResponse>> cache = new ConcurrentHashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    private static final String NO_EXCLUDE = "000000000000000000000000";

    // ========== MÉTHODES UTILITAIRES ==========

    private boolean isAdmin(UserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private Utilisateur getCurrentUserEntity(UserDetails currentUser) {
        return utilisateurRepo.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private void checkResponsableAccess(String vergerId, UserDetails currentUser) {
        if (isAdmin(currentUser)) return;

        Verger verger = vergerRepo.findById(vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Verger non trouvé"));
        Utilisateur responsable = getCurrentUserEntity(currentUser);

        if (verger.getResponsable() == null ||
                !verger.getResponsable().getId().equals(responsable.getId())) {
            throw new SecurityException("Vous n'avez pas accès à ce verger");
        }
    }

    private void checkTourneeAccess(Tournee tournee, UserDetails currentUser) {
        if (isAdmin(currentUser)) return;

        Utilisateur currentUserEntity = getCurrentUserEntity(currentUser);

        if (currentUserEntity.getRole() == Role.TRANSPORTEUR) {
            if (tournee.getTransporteur() != null &&
                    tournee.getTransporteur().getId() != null &&
                    tournee.getTransporteur().getId().equals(currentUserEntity.getId())) {
                return;
            }
            throw new SecurityException("Vous n'avez pas accès à cette tournée");
        }

        Verger verger = tournee.getVerger();
        if (verger == null) throw new SecurityException("Verger non trouvé pour cette tournée");

        if (verger.getResponsable() == null ||
                !verger.getResponsable().getId().equals(currentUserEntity.getId())) {
            throw new SecurityException("Vous n'avez pas accès à cette tournée");
        }
    }
    private List<Tournee> filterByResponsable(List<Tournee> tournees, UserDetails currentUser) {
        if (isAdmin(currentUser)) return tournees;

        Utilisateur responsable = getCurrentUserEntity(currentUser);
        String responsableId = responsable.getId();

        return tournees.stream()
                .filter(t -> {
                    if (t.getVerger() == null) return false;
                    Utilisateur vergerResponsable = t.getVerger().getResponsable();
                    if (vergerResponsable == null) return false;
                    String vergerResponsableId = vergerResponsable.getId();
                    if (vergerResponsableId == null) return false;
                    return vergerResponsableId.equals(responsableId);
                })
                .collect(Collectors.toList());
    }
    // ========== CREATE ==========

    @Override
    public TourneeResponse creer(TourneeRequest req, UserDetails currentUser) {
        checkResponsableAccess(req.getVergerId(), currentUser);
        clearCache();
        return creerInterne(req);
    }

    private TourneeResponse creerInterne(TourneeRequest req) {
        System.out.println("🚀 === DÉBUT CRÉATION TOURNÉE ===");

        Date dateDebut = req.getDateDebut();
        Date dateFin = req.getDateFin();
        validateDates(dateDebut, dateFin);

        Verger verger = vergerRepo.findById(req.getVergerId())
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + req.getVergerId()));

        if (Boolean.TRUE.equals(verger.getEstSupprimer()))
            throw new IllegalStateException("Le verger est supprimé.");

        // === BENNE CHECK ===
        Ressource benne = ressourceRepo.findById(req.getBenneId())
                .orElseThrow(() -> new ResourceNotFoundException("Benne introuvable : " + req.getBenneId()));
        if (benne.getType() != TypeRessource.BENNE)
            throw new IllegalArgumentException(req.getBenneId() + " n'est pas une benne.");
        checkBenneDisponible(benne, dateDebut, dateFin, NO_EXCLUDE);
        benne.setStatut("OCCUPE");
        ressourceRepo.save(benne);  // ✅ ADD THIS - Save the change!

        // === TRACTEUR CHECK ===
        Ressource tracteur = ressourceRepo.findById(req.getTracteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Tracteur introuvable : " + req.getTracteurId()));
        if (tracteur.getType() != TypeRessource.TRACTEUR)

            throw new IllegalArgumentException(req.getTracteurId() + " n'est pas un tracteur.");
        checkTracteurDisponible(tracteur, dateDebut, dateFin, NO_EXCLUDE);

        // === TRAVAILLEURS CHECK ===
        if (req.getTravailleurIds() == null || req.getTravailleurIds().isEmpty())
            throw new IllegalArgumentException("Au moins un travailleur doit être assigné.");

        List<Utilisateur> travailleurs = new ArrayList<>();
        for (String tid : req.getTravailleurIds()) {
            Utilisateur t = utilisateurRepo.findById(tid)
                    .orElseThrow(() -> new ResourceNotFoundException("Travailleur introuvable : " + tid));
            checkTravailleurDisponible(t, dateDebut, dateFin, NO_EXCLUDE);
            travailleurs.add(t);
        }

        int nbreArbre = (req.getNbreArbre() != null && req.getNbreArbre() > 0)
                ? req.getNbreArbre() : Tournee.NB_ARBRES_PAR_TOURNEE;
        verifierArbresRestants(verger, nbreArbre);

        // Collecte management
        String annee = collecteService.getCampagneAnnee(req.getDateDebut());
        Optional<Collecte> existingCollecte = collecteRepo.findByVergerIdAndAnnee(verger.getId(), annee);
        Collecte collecte = existingCollecte.orElseGet(() ->
                collecteService.createNewCollecte(verger, annee, req.getDateDebut()));
tracteur.setStatut("OCCUPE");
        ressourceRepo.save(tracteur);  // ✅ ADD THIS - Save the change!

        if (collecte == null) {
            throw new IllegalStateException("La collecte n'a pas pu être créée");
        }

        // Build Tournee
        Tournee tournee = Tournee.builder()
                .code(genererCode())
                .statut(StatutTournee.PLANIFIEE)
                .verger(verger)
                .collecte(collecte)
                .benne(benne)
                .tracteur(tracteur)
                .travailleurs(travailleurs)
                .nbreArbre(nbreArbre)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .distanceTotale(req.getDistanceTotale())
                .observations(req.getObservations())
                .livraisonDestinationNom(req.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(req.getLivraisonDestinationAdresse())
                .collecteFinalisee(false)
                .dateCreation(new Date())
                .build();

        if (verger.getStatut() == StatutVerger.NON_RECOLTE) {
            verger.setStatut(StatutVerger.EN_COURS);
            vergerRepo.save(verger);
        }

        Tournee saved = tourneeRepo.save(tournee);
        collecteService.updateCollecteStats(collecte.getId());
        vergerService.recomputeStatutForVerger(verger.getId());

        return toResponse(saved);
    }

    // ========== READ METHODS ==========

    @Override
    public TourneeResponse getById(String id, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        return toResponse(tournee);
    }

    @Override
    public Tournee getTourneeById(String id) {
        return findOrThrow(id);
    }

// Add these methods to TourneeServiceImpl.java (after the validateDates method or before getAll)

    private Date getStartOfYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }
    @Override
    public List<TourneeResponse> getAll(UserDetails currentUser) {
        // Get current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Date startOfYear = getStartOfYear(currentYear);
        Date endOfYear = getEndOfYear(currentYear);

        List<Tournee> tournees = tourneeRepo.findByDateDebutBetween(startOfYear, endOfYear);

        return filterByResponsable(tournees, currentUser).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    private void clearCache() {
        cache.clear();
        lastCacheTime = 0;
        System.out.println("🗑️ Cache cleared");
    }

    @Override
    public List<TourneeResponse> getByVerger(String vergerId, UserDetails currentUser) {
        checkResponsableAccess(vergerId, currentUser);
        return tourneeRepo.findByVergerId(vergerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TourneeResponse> getByStatut(StatutTournee statut, UserDetails currentUser) {
        List<Tournee> tournees = tourneeRepo.findByStatut(statut);
        return filterByResponsable(tournees, currentUser).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TourneeResponse> getActive(UserDetails currentUser) {
        List<Tournee> tournees = tourneeRepo.findActive();
        return filterByResponsable(tournees, currentUser).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========== STATE TRANSITIONS ==========

    @Override
    public TourneeResponse demarrer(String id, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        clearCache();
        return demarrerInterne(id);
    }

    private TourneeResponse demarrerInterne(String id) {
        Tournee tournee = findOrThrow(id);
        if (tournee.getStatut() != StatutTournee.PLANIFIEE)
            throw new IllegalStateException("Seule une tournée PLANIFIÉE peut être démarrée.");

        tournee.setStatut(StatutTournee.EN_COURS);

        if (tournee.getCollecte() != null) {
            Collecte collecte = tournee.getCollecte();
            if (collecte.getStatut() == com.example.demo.model.enums.StatutCollecte.PLANIFIEE) {
                collecteService.demarrerCollecte(collecte.getId());
            }
        }

        Tournee saved = tourneeRepo.save(tournee);
        if (saved.getVerger() != null && saved.getVerger().getId() != null) {
            vergerService.recomputeStatutForVerger(saved.getVerger().getId());
        }
        return toResponse(saved);
    }

    @Override
    public TourneeResponse terminer(String id, TerminerTourneeRequest req, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        clearCache();
        return terminerInterne(id, req);
    }

    private TourneeResponse terminerInterne(String id, TerminerTourneeRequest req) {
        Tournee tournee = findOrThrow(id);
        if (tournee.getStatut() != StatutTournee.EN_COURS)
            throw new IllegalStateException("Seule une tournée EN_COURS peut être terminée.");

        tournee.setStatut(StatutTournee.TERMINEE);
        tournee.setQuantiteCollecteeKg(req.getQuantiteCollecteeKg());
        tournee.setCollecteFinalisee(true);

        if (req.getDistanceTotale() != null) tournee.setDistanceTotale(req.getDistanceTotale());
        if (req.getObservations() != null) tournee.setObservations(req.getObservations());

        if (tournee.getDateDebut() != null && tournee.getDateFin() != null) {
            long diffMs = tournee.getDateFin().getTime() - tournee.getDateDebut().getTime();
            tournee.setTempsTotal((int) (diffMs / 1000));
        }

        if (tournee.getBenne() != null) {
            Ressource benne = ressourceRepo.findById(tournee.getBenne().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Benne non trouvée"));
            try {
                benne.ajouterCharge(req.getQuantiteCollecteeKg());
            } catch (IllegalArgumentException e) {
                benne.setQuantiteChargeeActuelle(benne.getCapaciteKg());
                benne.setEstPleine(true);
                benne.setTauxRemplissage(100.0);
            }
            ressourceRepo.save(benne);
        }

        tourneeRepo.save(tournee);

        if (tournee.getCollecte() != null) {
            collecteService.updateCollecteStats(tournee.getCollecte().getId());
        }

        checkAndCloseVerger(tournee.getVerger().getId());
        vergerService.recomputeStatutForVerger(tournee.getVerger().getId());
        return toResponse(tournee);
    }

    @Override
    public TourneeResponse annuler(String id, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        clearCache();
        return annulerInterne(id);
    }

    private TourneeResponse annulerInterne(String id) {
        Tournee tournee = findOrThrow(id);

        if (tournee.getStatut() == StatutTournee.TERMINEE ||
                tournee.getStatut() == StatutTournee.EN_LIVRAISON ||
                tournee.getStatut() == StatutTournee.LIVREE) {
            throw new IllegalStateException("Cette tournée ne peut pas être annulée.");
        }

        tournee.setStatut(StatutTournee.ANNULEE);
        tournee.setCollecteFinalisee(false);
        Tournee saved = tourneeRepo.save(tournee);

        if (saved.getVerger() != null && saved.getVerger().getId() != null) {
            vergerService.recomputeStatutForVerger(saved.getVerger().getId());
        }
        return toResponse(saved);
    }

    // ========== UPDATE / DELETE ==========

    @Override
    public TourneeResponse mettreAJour(String id, TourneeRequest req, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        clearCache();
        return mettreAJourInterne(id, req, currentUser);
    }

    private TourneeResponse mettreAJourInterne(String id, TourneeRequest req, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);

        if (tournee.getStatut() == StatutTournee.EN_LIVRAISON || tournee.getStatut() == StatutTournee.LIVREE) {
            throw new IllegalStateException("Cette tournée ne peut pas être modifiée.");
        }

        if (tournee.getStatut() != StatutTournee.PLANIFIEE) {
            throw new IllegalStateException("Seule une tournée PLANIFIÉE peut être modifiée.");
        }

        Date newDebut = req.getDateDebut() != null ? req.getDateDebut() : tournee.getDateDebut();
        Date newFin = req.getDateFin() != null ? req.getDateFin() : tournee.getDateFin();
        validateDates(newDebut, newFin);

        boolean datesChanged = !newDebut.equals(tournee.getDateDebut()) || !newFin.equals(tournee.getDateFin());

        if (datesChanged) {
            if (tournee.getBenne() != null) {
                checkBenneDisponible(tournee.getBenne(), newDebut, newFin, id);
            }
            if (tournee.getTracteur() != null) {
                checkTracteurDisponible(tournee.getTracteur(), newDebut, newFin, id);
            }
            if (tournee.getTravailleurs() != null && !tournee.getTravailleurs().isEmpty()) {
                for (Utilisateur travailleur : tournee.getTravailleurs()) {
                    checkTravailleurDisponible(travailleur, newDebut, newFin, id);
                }
            }
        }

        tournee.setDateDebut(newDebut);
        tournee.setDateFin(newFin);

        if (req.getNbreArbre() != null && req.getNbreArbre() > 0) {
            if (!req.getNbreArbre().equals(tournee.getNbreArbre())) {
                verifierArbresRestants(tournee.getVerger(), req.getNbreArbre());
            }
            tournee.setNbreArbre(req.getNbreArbre());
        }

        if (req.getDistanceTotale() != null) tournee.setDistanceTotale(req.getDistanceTotale());
        if (req.getObservations() != null) tournee.setObservations(req.getObservations());

        if (req.getBenneId() != null && !req.getBenneId().equals(tournee.getBenne().getId())) {
            Ressource newBenne = ressourceRepo.findById(req.getBenneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Benne introuvable : " + req.getBenneId()));
            if (newBenne.getType() != TypeRessource.BENNE) {
                throw new IllegalArgumentException(req.getBenneId() + " n'est pas une benne.");
            }
            checkBenneDisponible(newBenne, newDebut, newFin, id);
            tournee.setBenne(newBenne);
        }

        if (req.getTracteurId() != null && !req.getTracteurId().equals(tournee.getTracteur().getId())) {
            Ressource newTracteur = ressourceRepo.findById(req.getTracteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tracteur introuvable : " + req.getTracteurId()));
            if (newTracteur.getType() != TypeRessource.TRACTEUR) {
                throw new IllegalArgumentException(req.getTracteurId() + " n'est pas un tracteur.");
            }
            checkTracteurDisponible(newTracteur, newDebut, newFin, id);
            tournee.setTracteur(newTracteur);
        }

        if (req.getTravailleurIds() != null && !req.getTravailleurIds().isEmpty()) {
            List<Utilisateur> newTravailleurs = new ArrayList<>();
            for (String tid : req.getTravailleurIds()) {
                Utilisateur t = utilisateurRepo.findById(tid)
                        .orElseThrow(() -> new ResourceNotFoundException("Travailleur introuvable : " + tid));
                checkTravailleurDisponible(t, newDebut, newFin, id);
                newTravailleurs.add(t);
            }
            tournee.setTravailleurs(newTravailleurs);
        }

        Tournee saved = tourneeRepo.save(tournee);

        if (saved.getVerger() != null && saved.getVerger().getId() != null) {
            vergerService.recomputeStatutForVerger(saved.getVerger().getId());
        }

        return toResponse(saved);
    }

    @Override
    public void supprimer(String id, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
        clearCache();
        supprimerInterne(id);
    }

    private void supprimerInterne(String id) {
        Tournee tournee = findOrThrow(id);
        if (tournee.getStatut() == StatutTournee.EN_COURS || tournee.getStatut() == StatutTournee.TERMINEE)
            throw new IllegalStateException("Seules les tournées PLANIFIÉE ou ANNULÉE peuvent être supprimées.");
        String vergerId = tournee.getVerger() != null ? tournee.getVerger().getId() : null;
        tourneeRepo.delete(tournee);
        if (vergerId != null) {
            vergerService.recomputeStatutForVerger(vergerId);
        }
    }

    // ========== AGGREGATES ==========

    @Override
    public Double getTotalCollecteParVerger(String vergerId, UserDetails currentUser) {
        checkResponsableAccess(vergerId, currentUser);
        return tourneeRepo.findTermineesByVergerId(vergerId).stream()
                .mapToDouble(t -> t.getQuantiteCollecteeKg() != null ? t.getQuantiteCollecteeKg() : 0.0)
                .sum();
    }

    @Override
    public int calculerNbTourneesNecessaires(String vergerId, UserDetails currentUser) {
        checkResponsableAccess(vergerId, currentUser);
        Verger verger = vergerRepo.findById(vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + vergerId));
        return (int) Math.ceil((double) verger.getNbArbre() / Tournee.NB_ARBRES_PAR_TOURNEE);
    }

    // ========== AVAILABILITY CHECKS ==========

    private void checkBenneDisponible(Ressource benne, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByBenne(benne.getId(), debut, fin, excludeId);
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("La benne « " + benne.getNom() + " » est déjà utilisée du "
                    + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin()) + " (tournée " + c.getCode() + ").");
        }
    }

    private void checkTracteurDisponible(Ressource tracteur, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByTracteur(tracteur.getId(), debut, fin, excludeId);
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("Le tracteur « " + tracteur.getNom() + " » est déjà utilisé du "
                    + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin()) + " (tournée " + c.getCode() + ").");
        }
    }

    private void checkTravailleurDisponible(Utilisateur travailleur, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByTravailleur(travailleur.getId(), debut, fin, excludeId);
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("Le travailleur « " + travailleur.getPrenom() + " " + travailleur.getNom()
                    + " » est déjà assigné du " + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin())
                    + " (tournée " + c.getCode() + ").");
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private void validateDates(Date debut, Date fin) {
        if (debut == null || fin == null)
            throw new IllegalArgumentException("La date de début et la date de fin sont obligatoires.");
        if (!fin.after(debut))
            throw new IllegalArgumentException("La date de fin doit être après la date de début.");
    }

    private String fmt(Date d) {
        return d == null ? "?" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(d);
    }

    private Tournee findOrThrow(String id) {
        return tourneeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournée introuvable : " + id));
    }

    private String genererCode() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long count = tourneeRepo.count();
        return "T-" + date + "-" + String.format("%03d", count + 1);
    }

    private void checkAndCloseVerger(String vergerId) {
        Verger verger = vergerRepo.findById(vergerId).orElse(null);
        if (verger == null) return;

        int arbresCouverts = tourneeRepo.findTermineesByVergerId(vergerId).stream()
                .mapToInt(t -> t.getNbreArbre() != null ? t.getNbreArbre() : 0)
                .sum();

        if (arbresCouverts >= verger.getNbArbre()) {
            verger.setStatut(StatutVerger.RECOLTE);
            verger.setDateDerniereRecolte(new Date());
            vergerRepo.save(verger);
        }
    }

    private void verifierArbresRestants(Verger verger, int nbreArbre) {
        List<Tournee> toutesTournees = tourneeRepo.findByVergerId(verger.getId());
        int arbresDejaPlanifies = toutesTournees.stream()
                .filter(t -> t.getStatut() != StatutTournee.ANNULEE)
                .mapToInt(t -> t.getNbreArbre() != null ? t.getNbreArbre() : 0)
                .sum();

        int arbresRestants = verger.getNbArbre() - arbresDejaPlanifies;

        if (nbreArbre > arbresRestants) {
            throw new IllegalStateException(
                    String.format("Impossible de récolter %d arbres. Il reste seulement %d arbres disponibles (total: %d, déjà planifiés: %d).",
                            nbreArbre, arbresRestants, verger.getNbArbre(), arbresDejaPlanifies)
            );
        }
    }

    @Autowired
    private UtilisateurRepository userRep;

    @Override
    public Optional<List<Utilisateur>> getAllTravailleurs() {
        List<Utilisateur> travailleurs = userRep.findByRoleAndEstSupprimeFalse("TRAVAILLEUR");
        return Optional.ofNullable(travailleurs);
    }

    // ========== RESPONSE MAPPING (COMPLETE) ==========

    private TourneeResponse toResponse(Tournee t) {
        // Extract nested data safely
        Verger v = t.getVerger();
        String vergerTypeOlive = null;
        String vergerAgriculteurNom = null;
        Double vergerSuperficie = null;
        String vergerId = null;

        if (v != null) {
            vergerId = v.getId();
            vergerTypeOlive = v.getTypeOlive();
            vergerSuperficie = v.getSuperficie();
            if (v.getAgriculteur() != null) {
                vergerAgriculteurNom = v.getAgriculteur().getPrenom() + " " + v.getAgriculteur().getNom();
            }
        }

        // Extract benne data
        Ressource benne = t.getBenne();
        String benneId = null;
        String benneNom = null;
        Double benneCapaciteKg = null;
        if (benne != null) {
            benneId = benne.getId();
            benneNom = benne.getNom();
            benneCapaciteKg = benne.getCapaciteKg();
        }

        // Extract tracteur data
        Ressource tracteur = t.getTracteur();
        String tracteurId = null;
        String tracteurNom = null;
        String tracteurImmatriculation = null;
        if (tracteur != null) {
            tracteurId = tracteur.getId();
            tracteurNom = tracteur.getNom();
            tracteurImmatriculation = tracteur.getImmatriculation();
        }

        // Extract workers data
        List<String> travailleurIds = new ArrayList<>();
        List<String> travailleurNoms = new ArrayList<>();
        if (t.getTravailleurs() != null) {
            for (Utilisateur u : t.getTravailleurs()) {
                if (u != null) {
                    travailleurIds.add(u.getId());
                    travailleurNoms.add(u.getPrenom() + " " + u.getNom());
                }
            }
        }

        // Extract collecte data
        Collecte collecte = t.getCollecte();
        String collecteId = null;
        String collecteCode = null;
        if (collecte != null) {
            collecteId = collecte.getId();
            collecteCode = collecte.getCode();
        }

        // Calculate efficiency if needed
        Double efficacite = null;
        if (t.getDistanceTotale() != null && t.getDistanceTotale() > 0
                && t.getQuantiteCollecteeKg() != null && t.getQuantiteCollecteeKg() > 0
                && t.getTempsTotal() != null && t.getTempsTotal() > 0) {
            double heures = t.getTempsTotal() / 3600.0;
            efficacite = Math.min((t.getQuantiteCollecteeKg() / (t.getDistanceTotale() * heures)) * 10.0, 100.0);
        }

        return TourneeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .statut(t.getStatut())
                .vergerId(vergerId)
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom)
                .vergerSuperficie(vergerSuperficie)
                .benneId(benneId)
                .benneNom(benneNom)
                .benneCapaciteKg(benneCapaciteKg)
                .tracteurId(tracteurId)
                .tracteurNom(tracteurNom)
                .tracteurImmatriculation(tracteurImmatriculation)
                .travailleurIds(travailleurIds)
                .travailleurNoms(travailleurNoms)
                .nbreArbre(t.getNbreArbre())
                .distanceTotale(t.getDistanceTotale())
                .tempsTotal(t.getTempsTotal())
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .collecteFinalisee(t.getCollecteFinalisee())
                .efficacite(efficacite)
                .observations(t.getObservations())
                .livraisonDestinationNom(t.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                .livraisonStartedAt(t.getLivraisonStartedAt())
                .livraisonCompletedAt(t.getLivraisonCompletedAt())
                .livraisonEvidenceName(t.getLivraisonEvidenceName())
                .livraisonEvidenceUrl(t.getLivraisonEvidenceUrl())
                .dateDebut(t.getDateDebut())
                .dateFin(t.getDateFin())
                .dateCreation(t.getDateCreation())
                .collecteId(collecteId)
                .collecteCode(collecteCode)
                .build();
    }
}