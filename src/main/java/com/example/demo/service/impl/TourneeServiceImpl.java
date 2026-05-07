package com.example.demo.service.impl;

import com.example.demo.dto.TerminerTourneeRequest;
import com.example.demo.dto.TourneeRequest;
import com.example.demo.dto.TourneeResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.enums.StatutVerger;
import com.example.demo.model.enums.TypeRessource;
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
    @Override
    public List<TourneeResponse> getTourneesDisponiblesPourTransporteur(
            List<String> vergerIds,
            Integer yearOrNull,
            String searchQuery) {

        // Get ALL tournées
        List<Tournee> allTournees = tourneeRepo.findAll();

        System.out.println("🔍 Total tournees in DB: " + allTournees.size());

        List<StatutTournee> allowedStatuts = Arrays.asList(
                StatutTournee.PLANIFIEE,
                StatutTournee.EN_COURS,
                StatutTournee.TERMINEE
        );

        List<Tournee> filtered = allTournees.stream()
                .filter(t -> {
                    boolean ok = allowedStatuts.contains(t.getStatut());
                    if (!ok) System.out.println("  ❌ SKIP " + t.getCode() + " - statut=" + t.getStatut());
                    return ok;
                })
                .filter(t -> {
                    // Try to get the transporteur ID - if it fails or is null, treat as no transporteur
                    boolean hasTransporteur = false;
                    try {
                        Utilisateur transporter = t.getTransporteur();
                        if (transporter != null) {
                            String id = transporter.getId();
                            hasTransporteur = (id != null && !id.isEmpty());
                        }
                    } catch (Exception e) {
                        // Lazy loading exception - treat as no transporteur
                        hasTransporteur = false;
                    }
                    if (hasTransporteur) System.out.println("  ❌ SKIP " + t.getCode() + " - has transporteur");
                    return !hasTransporteur;
                })
                .filter(t -> {
                    // Use vergerSnapshot instead of getVerger() to avoid lazy loading issues
                    Verger v = t.getVergerSnapshot();
                    boolean ok = v != null && (vergerIds == null || vergerIds.isEmpty() || vergerIds.contains(v.getId()));
                    if (!ok) System.out.println("  ❌ SKIP " + t.getCode() + " - verger not in list");
                    return ok;
                })
                .filter(t -> {
                    if (yearOrNull == null || yearOrNull == 0) return true;
                    if (t.getDateDebut() == null) return false;
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(t.getDateDebut());
                    boolean ok = cal.get(Calendar.YEAR) == yearOrNull;
                    if (!ok) System.out.println("  ❌ SKIP " + t.getCode() + " - wrong year");
                    return ok;
                })
                .filter(t -> {
                    if (searchQuery == null || searchQuery.isBlank()) return true;
                    String q = searchQuery.toLowerCase();
                    return (t.getCode() != null && t.getCode().toLowerCase().contains(q))
                            || (t.getLivraisonDestinationNom() != null && t.getLivraisonDestinationNom().toLowerCase().contains(q))
                            || (t.getObservations() != null && t.getObservations().toLowerCase().contains(q));
                })
                .peek(t -> System.out.println("  ✅ INCLUDED: " + t.getCode() + " statut=" + t.getStatut()))
                .collect(Collectors.toList());

        System.out.println("🔍 After filtering: " + filtered.size() + " tournees");

        return filtered.stream()
                .map(this::toResponseForTransporteurAssignList)
                .collect(Collectors.toList());
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

        // Pour RESPONSABLE, vérifier qu'il est responsable du verger de la tournée
        if (currentUserEntity.getRole() == Role.RESPONSABLE_PRESSOIR) {
            if (tournee.getResponsablePressoir() != null
                    && tournee.getResponsablePressoir().getId() != null
                    && tournee.getResponsablePressoir().getId().equals(currentUserEntity.getId())) {
                return;
            }
            throw new SecurityException("Vous n'avez pas acces a cette tournee");
        }

        Verger verger = tournee.getVerger();
        if (verger == null) {
            throw new SecurityException("Verger non trouvé pour cette tournée");
        }

        if (verger.getResponsable() == null ||
                !verger.getResponsable().getId().equals(currentUserEntity.getId())) {
            throw new SecurityException("Vous n'avez pas accès à cette tournée");
        }
    }
    private List<Tournee> filterByResponsable(List<Tournee> tournees, UserDetails currentUser) {
        if (isAdmin(currentUser)) return tournees;

        Utilisateur currentUserEntity = getCurrentUserEntity(currentUser);
        String responsableId = currentUserEntity.getId();

        // For RESPONSABLE_PRESSOIR
        if (currentUserEntity.getRole() == Role.RESPONSABLE_PRESSOIR) {
            return tournees.stream()
                    .filter(t -> {
                        if (t.getResponsablePressoir() == null) return false;
                        String pressoirResponsableId = t.getResponsablePressoir().getId();
                        if (pressoirResponsableId == null) return false;
                        return pressoirResponsableId.equals(responsableId);
                    })
                    .collect(Collectors.toList());
        }

        // For regular RESPONSABLE (manager of verger) - USE SNAPSHOT!
        if (currentUserEntity.getRole() == Role.RESPONSABLE) {
            return tournees.stream()
                    .filter(t -> {
                        // ✅ USE VERGER SNAPSHOT instead of t.getVerger()
                        Verger v = t.getVergerSnapshot();
                        if (v == null) {
                            System.out.println("⚠️ Tournee " + t.getCode() + " has no vergerSnapshot!");
                            return false;
                        }
                        Utilisateur vergerResponsable = v.getResponsable();
                        if (vergerResponsable == null) {
                            System.out.println("⚠️ Verger " + v.getId() + " has no responsable!");
                            return false;
                        }
                        String vergerResponsableId = vergerResponsable.getId();
                        if (vergerResponsableId == null) {
                            System.out.println("⚠️ Verger responsable has no ID!");
                            return false;
                        }
                        return vergerResponsableId.equals(responsableId);
                    })
                    .collect(Collectors.toList());
        }

        // For TRANSPORTEUR
        if (currentUserEntity.getRole() == Role.TRANSPORTEUR) {
            return tournees.stream()
                    .filter(t -> {
                        if (t.getTransporteur() == null) return false;
                        String transporteurId = t.getTransporteur().getId();
                        if (transporteurId == null) return false;
                        return transporteurId.equals(responsableId);
                    })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
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
        verifierDateNonPassee(dateDebut);  // ← AJOUTER CETTE LIGNE

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

        Utilisateur responsablePressoir = resolveResponsablePressoir(req.getResponsablePressoirId());
        String destinationNom = req.getLivraisonDestinationNom();
        String destinationAdresse = req.getLivraisonDestinationAdresse();
        if (responsablePressoir != null && responsablePressoir.getPressoir() != null) {
            if (destinationNom == null || destinationNom.isBlank()) {
                destinationNom = responsablePressoir.getPressoir().getNom();
            }
            if (destinationAdresse == null || destinationAdresse.isBlank()) {
                destinationAdresse = responsablePressoir.getPressoir().getAdresse();
            }
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
                .responsablePressoir(responsablePressoir)
                .responsablePressoirId(responsablePressoir != null ? responsablePressoir.getId() : null)
                .nbreArbre(nbreArbre)
                .dateDebut(dateDebut)
                .vergerSnapshot(verger)  // ✅ Store COMPLETE copy

                .dateFin(dateFin)
                .distanceTotale(req.getDistanceTotale())
                .observations(req.getObservations())
                .livraisonDestinationNom(destinationNom)
                .livraisonDestinationAdresse(destinationAdresse)
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

    // Replace the getAll method
// TourneeServiceImpl.java
    @Override
    public List<TourneeResponse> getAll(UserDetails currentUser) {
        long start = System.currentTimeMillis();
        System.out.println("🔍 getAll() started for user: " + currentUser.getUsername());

        List<Tournee> tournees = tourneeRepo.findAll();
        System.out.println("📊 DB query took: " + (System.currentTimeMillis() - start) + "ms - Found " + tournees.size() + " records");

        // Apply filtering based on user role
        List<Tournee> filteredTournees = filterByResponsable(tournees, currentUser);
        System.out.println("🔒 After filtering: " + filteredTournees.size() + " tournees accessible");

        long mapStart = System.currentTimeMillis();

        List<TourneeResponse> result = filteredTournees.stream()
                .map(this::toResponseLight)
                .collect(Collectors.toList());

        System.out.println("🔄 Mapping took: " + (System.currentTimeMillis() - mapStart) + "ms");
        System.out.println("✅ getAll() total: " + (System.currentTimeMillis() - start) + "ms");

        return result;
    }

    private TourneeResponse toResponse(Tournee t) {
        if (t == null) {
            return null;
        }

        // ✅ USE THE SNAPSHOT instead of reference
        Verger v = t.getVergerSnapshot();

        String vergerTypeOlive = null;
        String vergerAgriculteurNom = null;
        String vergerResponsableId = null;
        String vergerResponsableNom = null;
        Double vergerSuperficie = null;
        String vergerId = null;

        if (v != null) {
            vergerId = v.getId();
            vergerTypeOlive = v.getTypeOlive();
            vergerSuperficie = v.getSuperficie();

            // ✅ CHANGED: Fetch responsable info from DB using the ID from snapshot
            if (v.getResponsable() != null) {
                String responsableId = v.getResponsable().getId();
                if (responsableId != null) {
                    try {
                        Utilisateur responsable = utilisateurRepo.findById(responsableId).orElse(null);
                        if (responsable != null) {
                            vergerResponsableId = responsable.getId();
                            vergerResponsableNom = (responsable.getPrenom() != null ? responsable.getPrenom() : "")
                                    + " " + (responsable.getNom() != null ? responsable.getNom() : "");
                            vergerResponsableNom = vergerResponsableNom.trim();
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not fetch responsable: " + e.getMessage());
                    }
                }
            }

            // ✅ CHANGED: Fetch agriculteur name from DB using the ID from snapshot
            if (v.getAgriculteur() != null) {
                String agriculteurId = v.getAgriculteur().getId();
                if (agriculteurId != null) {
                    try {
                        Utilisateur agriculteur = utilisateurRepo.findById(agriculteurId).orElse(null);
                        if (agriculteur != null) {
                            vergerAgriculteurNom = (agriculteur.getPrenom() != null ? agriculteur.getPrenom() : "")
                                    + " " + (agriculteur.getNom() != null ? agriculteur.getNom() : "");
                            vergerAgriculteurNom = vergerAgriculteurNom.trim();
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not fetch agriculteur: " + e.getMessage());
                    }
                }
            }
        }

        Double efficacite = calculerEfficacite(t);

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

        // Extract responsable pressoir
        Utilisateur responsablePressoir = t.getResponsablePressoir();
        String responsablePressoirNom = null;
        String pressoirNom = null;
        String pressoirAdresse = null;
        if (responsablePressoir != null) {
            responsablePressoirNom = (responsablePressoir.getPrenom() != null ? responsablePressoir.getPrenom() : "")
                    + " " + (responsablePressoir.getNom() != null ? responsablePressoir.getNom() : "");
            responsablePressoirNom = responsablePressoirNom.trim();

            if (responsablePressoir.getPressoir() != null) {
                pressoirNom = responsablePressoir.getPressoir().getNom();
                pressoirAdresse = responsablePressoir.getPressoir().getAdresse();
            }
        }

        // Calculate efficiency if needed
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
                .vergerResponsableId(vergerResponsableId)
                .vergerResponsableNom(vergerResponsableNom) // ✅ CHANGED: Now has value from DB
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom) // ✅ CHANGED: Now has value from DB
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
                .livraisonEstimeDebut(t.getLivraisonEstimeDebut())
                .livraisonEstimeFin(t.getLivraisonEstimeFin())
                .livraisonNotes(t.getLivraisonNotes())
                .responsablePressoirId(t.getResponsablePressoirId())
                .responsablePressoirNom(responsablePressoirNom)
                .pressoirNom(pressoirNom)
                .pressoirAdresse(pressoirAdresse)
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
 // ========== VÉRIFICATION DATE PASSÉE ==========

    private void verifierDateNonPassee(Date dateDebut) {
        Date aujourdhui = new Date();
        if (dateDebut.before(aujourdhui)) {
            throw new IllegalStateException(
                "Impossible de créer/modifier une tournée avec une date dans le passé. " +
                "📅 Date demandée: " + fmt(dateDebut) + " | 📅 Date actuelle: " + fmt(aujourdhui)
            );
        }
    }
    private TourneeResponse toResponseLight(Tournee t) {
        // ✅ USE THE SNAPSHOT instead of the reference!
        Verger v = t.getVergerSnapshot();

        String vergerTypeOlive = null;
        String vergerAgriculteurNom = null;
        String vergerId = null;
        String vergerResponsableId = null;
        String vergerResponsableNom = null;

        if (v != null) {
            vergerId = v.getId();
            vergerTypeOlive = v.getTypeOlive();

            // ✅ CHANGED: Fetch responsable info from DB using the ID from snapshot
            if (v.getResponsable() != null) {
                String responsableId = v.getResponsable().getId();
                if (responsableId != null) {
                    try {
                        Utilisateur responsable = utilisateurRepo.findById(responsableId).orElse(null);
                        if (responsable != null) {
                            vergerResponsableId = responsable.getId();
                            vergerResponsableNom = (responsable.getPrenom() != null ? responsable.getPrenom() : "")
                                    + " " + (responsable.getNom() != null ? responsable.getNom() : "");
                            vergerResponsableNom = vergerResponsableNom.trim();
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not fetch responsable: " + e.getMessage());
                    }
                }
            }

            // ✅ CHANGED: Fetch agriculteur name from DB using the ID from snapshot
            if (v.getAgriculteur() != null) {
                String agriculteurId = v.getAgriculteur().getId();
                if (agriculteurId != null) {
                    try {
                        Utilisateur agriculteur = utilisateurRepo.findById(agriculteurId).orElse(null);
                        if (agriculteur != null) {
                            vergerAgriculteurNom = (agriculteur.getPrenom() != null ? agriculteur.getPrenom() : "")
                                    + " " + (agriculteur.getNom() != null ? agriculteur.getNom() : "");
                            vergerAgriculteurNom = vergerAgriculteurNom.trim();
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Could not fetch agriculteur: " + e.getMessage());
                    }
                }
            }
        }

        // ✅ ADDED: Calculate efficiency
        Double efficacite = calculerEfficacite(t);

        return TourneeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .statut(t.getStatut())
                .dateDebut(t.getDateDebut())
                .dateFin(t.getDateFin())
                .dateCreation(t.getDateCreation())
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .distanceTotale(t.getDistanceTotale())
                .tempsTotal(t.getTempsTotal())  // ✅ ADDED
                .observations(t.getObservations())
                .livraisonDestinationNom(t.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                .vergerId(vergerId)
                .vergerResponsableId(vergerResponsableId)
                .vergerResponsableNom(vergerResponsableNom)  // ✅ CHANGED: Now has value
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom)  // ✅ CHANGED: Now has value
                .efficacite(efficacite)  // ✅ ADDED
                .build();
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

        // Calculate tempsTotal based on original planned duration
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
        verifierDateNonPassee(req.getDateDebut());  // ← AJOUTER CETTE LIGNE

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

        if (req.getObservations() != null) {
            tournee.setObservations(req.getObservations());
        }

        if (req.getResponsablePressoirId() != null) {
            Utilisateur responsablePressoir = resolveResponsablePressoir(req.getResponsablePressoirId());
            tournee.setResponsablePressoir(responsablePressoir);
            tournee.setResponsablePressoirId(responsablePressoir.getId());
            if (responsablePressoir.getPressoir() != null) {
                if (req.getLivraisonDestinationNom() == null || req.getLivraisonDestinationNom().isBlank()) {
                    tournee.setLivraisonDestinationNom(responsablePressoir.getPressoir().getNom());
                }
                if (req.getLivraisonDestinationAdresse() == null || req.getLivraisonDestinationAdresse().isBlank()) {
                    tournee.setLivraisonDestinationAdresse(responsablePressoir.getPressoir().getAdresse());
                }
            }
        }

        if (req.getLivraisonDestinationNom() != null) {
            tournee.setLivraisonDestinationNom(req.getLivraisonDestinationNom());
        }

        if (req.getLivraisonDestinationAdresse() != null) {
            tournee.setLivraisonDestinationAdresse(req.getLivraisonDestinationAdresse());
        }

        // Check if resources changed
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
        return getTotalCollecteParVergerInterne(vergerId);
    }

    private Double getTotalCollecteParVergerInterne(String vergerId) {
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
    // ========== MÉTHODES UTILITAIRES (CONSERVÉES INTACTES) ==========

    private Utilisateur resolveResponsablePressoir(String responsablePressoirId) {
        if (responsablePressoirId == null || responsablePressoirId.isBlank()) {
            return null;
        }
        Utilisateur responsablePressoir = utilisateurRepo.findById(responsablePressoirId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsable pressoir introuvable : " + responsablePressoirId));
        if (responsablePressoir.getRole() != Role.RESPONSABLE_PRESSOIR) {
            throw new IllegalArgumentException("L'utilisateur selectionne n'est pas un responsable pressoir.");
        }
        if (Boolean.FALSE.equals(responsablePressoir.getEstActif()) || responsablePressoir.isEstSupprime()) {
            throw new IllegalStateException("Le responsable pressoir selectionne n'est pas actif.");
        }
        if (responsablePressoir.getPressoir() == null) {
            throw new IllegalStateException("Le responsable pressoir selectionne n'a pas de pressoir configure.");
        }
        return responsablePressoir;
    }

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

    // ========== EFFICACITÉ ==========

    private double calculerEfficacite(Tournee t) {
        if (t.getDistanceTotale() == null || t.getDistanceTotale() == 0) return 0.0;
        if (t.getQuantiteCollecteeKg() == null || t.getQuantiteCollecteeKg() == 0) return 0.0;
        if (t.getTempsTotal() == null || t.getTempsTotal() == 0) return 0.0;

        double heures = t.getTempsTotal() / 3600.0;
        double efficiency = (t.getQuantiteCollecteeKg() / (t.getDistanceTotale() * heures)) * 10.0;

        return Math.min(efficiency, 100.0);
    }


    @Autowired
    private UtilisateurRepository userRep;
    @Autowired

    @Override
    public Optional<List<Utilisateur>> getAllTravailleurs() {
        List<Utilisateur> travailleurs = userRep.findByRoleAndEstSupprimeFalse("TRAVAILLEUR");
        return Optional.ofNullable(travailleurs);
    }

    // ========== RESPONSE MAPPING ==========

    @Override
    public TourneeResponse toResponseForTransporteurAssignList(Tournee t) {
        if (t == null) {
            return null;
        }

        // ✅ USE THE SNAPSHOT
        Verger v = t.getVergerSnapshot();

        String vergerTypeOlive = null;
        String vergerAgriculteurNom = null;
        Double vergerSuperficie = null;
        String vergerId = null;
        String vergerResponsableId = null;
        String vergerResponsableNom = null;

        if (v != null) {
            vergerId = v.getId();
            vergerTypeOlive = v.getTypeOlive();
            vergerSuperficie = v.getSuperficie();

            if (v.getResponsable() != null) {
                vergerResponsableId = v.getResponsable().getId();
                // Fetch responsable name from DB
                try {
                    Utilisateur responsable = utilisateurRepo.findById(vergerResponsableId).orElse(null);
                    if (responsable != null) {
                        vergerResponsableNom = (responsable.getPrenom() != null ? responsable.getPrenom() : "")
                                + " " + (responsable.getNom() != null ? responsable.getNom() : "");
                        vergerResponsableNom = vergerResponsableNom.trim();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }

            if (v.getAgriculteur() != null) {
                try {
                    String agriculteurId = v.getAgriculteur().getId();
                    Utilisateur agriculteur = utilisateurRepo.findById(agriculteurId).orElse(null);
                    if (agriculteur != null) {
                        vergerAgriculteurNom = (agriculteur.getPrenom() != null ? agriculteur.getPrenom() : "")
                                + " " + (agriculteur.getNom() != null ? agriculteur.getNom() : "");
                        vergerAgriculteurNom = vergerAgriculteurNom.trim();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        Ressource benne = t.getBenne();
        Ressource tracteur = t.getTracteur();

        // Extract travailleur names
        List<String> travailleurNoms = new ArrayList<>();
        if (t.getTravailleurs() != null) {
            for (Utilisateur u : t.getTravailleurs()) {
                if (u != null) {
                    travailleurNoms.add(u.getPrenom() + " " + u.getNom());
                }
            }
        }

        // Extract responsable pressoir name
        Utilisateur rp = t.getResponsablePressoir();
        String responsablePressoirNom = null;
        String pressoirNom = null;
        if (rp != null) {
            responsablePressoirNom = (rp.getPrenom() != null ? rp.getPrenom() : "")
                    + " " + (rp.getNom() != null ? rp.getNom() : "");
            if (rp.getPressoir() != null) {
                pressoirNom = rp.getPressoir().getNom();
            }
        }

        return TourneeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .statut(t.getStatut())
                .vergerId(vergerId)
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom)
                .vergerSuperficie(vergerSuperficie)
                .vergerResponsableId(vergerResponsableId)
                .vergerResponsableNom(vergerResponsableNom)  // ✅ ADDED
                .benneId(benne != null ? benne.getId() : null)
                .benneNom(benne != null ? benne.getNom() : null)
                .benneCapaciteKg(benne != null ? benne.getCapaciteKg() : null)
                .tracteurId(tracteur != null ? tracteur.getId() : null)
                .tracteurNom(tracteur != null ? tracteur.getNom() : null)
                .tracteurImmatriculation(tracteur != null ? tracteur.getImmatriculation() : null)
                .travailleurIds(java.util.Collections.emptyList())
                .travailleurNoms(travailleurNoms)  // ✅ ADDED
                .nbreArbre(t.getNbreArbre())
                .distanceTotale(t.getDistanceTotale())
                .tempsTotal(t.getTempsTotal())
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .collecteFinalisee(t.getCollecteFinalisee())
                .efficacite(calculerEfficacite(t))  // ✅ Include efficiency
                .observations(t.getObservations())
                .livraisonDestinationNom(t.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                .livraisonEstimeDebut(t.getLivraisonEstimeDebut())
                .livraisonEstimeFin(t.getLivraisonEstimeFin())
                .livraisonNotes(t.getLivraisonNotes())
                .responsablePressoirId(t.getResponsablePressoirId())
                .responsablePressoirNom(responsablePressoirNom)  // ✅ ADDED
                .pressoirNom(pressoirNom)  // ✅ ADDED
                .pressoirAdresse(null)
                .livraisonStartedAt(t.getLivraisonStartedAt())
                .livraisonCompletedAt(t.getLivraisonCompletedAt())
                .livraisonEvidenceName(t.getLivraisonEvidenceName())
                .livraisonEvidenceUrl(t.getLivraisonEvidenceUrl())
                .dateDebut(t.getDateDebut())
                .dateFin(t.getDateFin())
                .dateCreation(t.getDateCreation())
                .totalCollecteVergerKg(null)
                .collecteId(null)
                .collecteCode(null)
                .build();
    }}