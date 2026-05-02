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
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
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


    private static final String NO_EXCLUDE = "000000000000000000000000";

    // ========== MÉTHODES UTILITAIRES POUR LA GESTION DES DROITS ==========

    private boolean isAdmin(UserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }



    private Utilisateur getCurrentUserEntity(UserDetails currentUser) {
        return utilisateurRepo.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private void checkResponsableAccess(String vergerId, UserDetails currentUser) {
        if (isAdmin(currentUser)) {
            return; // Admin a accès à tout
        }

        // Pour RESPONSABLE, vérifier qu'il est responsable du verger
        Verger verger = vergerRepo.findById(vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Verger non trouvé"));

        Utilisateur responsable = getCurrentUserEntity(currentUser);

        if (verger.getResponsable() == null ||
                !verger.getResponsable().getId().equals(responsable.getId())) {
            throw new SecurityException("Vous n'avez pas accès à ce verger");
        }
    }

    private void checkTourneeAccess(Tournee tournee, UserDetails currentUser) {
        if (isAdmin(currentUser)) {
            return; // Admin a accès à tout
        }

        Utilisateur currentUserEntity = getCurrentUserEntity(currentUser);

        // Transporteur : uniquement les tournées qui lui sont assignées
        if (currentUserEntity.getRole() == Role.TRANSPORTEUR) {
            if (tournee.getTransporteur() != null
                    && tournee.getTransporteur().getId() != null
                    && tournee.getTransporteur().getId().equals(currentUserEntity.getId())) {
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
        if (isAdmin(currentUser)) {
            return tournees;
        }

        Utilisateur responsable = getCurrentUserEntity(currentUser);
        return tournees.stream()
                .filter(t -> t.getVerger() != null &&
                        t.getVerger().getResponsable() != null &&
                        t.getVerger().getResponsable().getId().equals(responsable.getId()))
                .collect(Collectors.toList());
    }

    // ========== CREATE ==========

    @Override
    public TourneeResponse creer(TourneeRequest req, UserDetails currentUser) {
        // Vérifier que le responsable a accès au verger
        checkResponsableAccess(req.getVergerId(), currentUser);
        return creerInterne(req);
    }

    private TourneeResponse creerInterne(TourneeRequest req) {
        System.out.println("🚀 === DÉBUT CRÉATION TOURNÉE ===");

        // DON'T modify timezone - use dates as-is
        Date dateDebut = req.getDateDebut();
        Date dateFin = req.getDateFin();

        validateDates(dateDebut, dateFin);

        System.out.println("🕒 Dates → Début: " + fmt(dateDebut) + " | Fin: " + fmt(dateFin));

        // Resolve verger
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
        System.out.println("✅ Benne disponible: " + benne.getNom());

        // === TRACTEUR CHECK ===
        Ressource tracteur = ressourceRepo.findById(req.getTracteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Tracteur introuvable : " + req.getTracteurId()));
        if (tracteur.getType() != TypeRessource.TRACTEUR)
            throw new IllegalArgumentException(req.getTracteurId() + " n'est pas un tracteur.");
        checkTracteurDisponible(tracteur, dateDebut, dateFin, NO_EXCLUDE);
        System.out.println("✅ Tracteur disponible: " + tracteur.getNom());

        // === TRAVAILLEURS CHECK ===
        if (req.getTravailleurIds() == null || req.getTravailleurIds().isEmpty())
            throw new IllegalArgumentException("Au moins un travailleur doit être assigné.");

        List<Utilisateur> travailleurs = new ArrayList<>();
        for (String tid : req.getTravailleurIds()) {
            Utilisateur t = utilisateurRepo.findById(tid)
                    .orElseThrow(() -> new ResourceNotFoundException("Travailleur introuvable : " + tid));
            checkTravailleurDisponible(t, dateDebut, dateFin, NO_EXCLUDE);
            travailleurs.add(t);
            System.out.println("✅ Travailleur disponible: " + t.getPrenom() + " " + t.getNom());
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
                .dateFin(dateFin)
                .distanceTotale(req.getDistanceTotale())
                .observations(req.getObservations())
                .livraisonDestinationNom(destinationNom)
                .livraisonDestinationAdresse(destinationAdresse)
                .collecteFinalisee(false)
                .dateCreation(new Date())
                .build();

        // Update verger status if needed
        if (verger.getStatut() == StatutVerger.NON_RECOLTE) {
            verger.setStatut(StatutVerger.EN_COURS);
            vergerRepo.save(verger);
        }

        Tournee saved = tourneeRepo.save(tournee);
        collecteService.updateCollecteStats(collecte.getId());
        vergerService.recomputeStatutForVerger(verger.getId());

        System.out.println("✅ Tournée créée avec succès: " + saved.getCode());
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

    @Override
    public List<TourneeResponse> getAll(UserDetails currentUser) {
        long start = System.currentTimeMillis();

        String cacheKey = "tournees_minimal_" + (isAdmin(currentUser) ? "admin" : getCurrentUserEntity(currentUser).getId());


        System.out.println("🔍 getAll() minimal started");

        // Use the minimal query that only fetches needed fields - NO lazy loading!
        List<Tournee> tournees = tourneeRepo.findAllMinimal();
        System.out.println("📊 DB query took: " + (System.currentTimeMillis() - start) + "ms - Found " + tournees.size() + " records");

        long filterStart = System.currentTimeMillis();
        List<Tournee> filtered = filterByResponsable(tournees, currentUser);
        System.out.println("🔒 Filter by responsable took: " + (System.currentTimeMillis() - filterStart) + "ms");

        long mapStart = System.currentTimeMillis();

        // ⚠️ CRITICAL: Do NOT call any getters that access referenced documents!
        List<TourneeResponse> result = filtered.stream()
                .map(t -> TourneeResponse.builder()
                        .id(t.getId())
                        .code(t.getCode())
                        .statut(t.getStatut())
                        .dateDebut(t.getDateDebut())
                        .dateFin(t.getDateFin())
                        .dateCreation(t.getDateCreation())
                        .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                        .distanceTotale(t.getDistanceTotale())
                        .observations(t.getObservations())
                        .livraisonDestinationNom(t.getLivraisonDestinationNom())
                        .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                        // ⚠️ DO NOT call t.getVerger() - it triggers lazy loading!
                        .vergerId(null)
                        .benneId(null)
                        .tracteurId(null)
                        .build())
                .collect(Collectors.toList());

        System.out.println("🔄 Minimal mapping took: " + (System.currentTimeMillis() - mapStart) + "ms");
        System.out.println("✅ getAll() minimal total: " + (System.currentTimeMillis() - start) + "ms");

        return result;
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
            System.out.println("⏱️ Planned duration: " + tournee.getTempsTotal() + " seconds");
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
        return annulerInterne(id);
    }
    private TourneeResponse annulerInterne(String id) {
        Tournee tournee = findOrThrow(id);

        // Cannot cancel tournées that are already finished or in delivery
        if (tournee.getStatut() == StatutTournee.TERMINEE) {
            throw new IllegalStateException("Une tournée TERMINÉE ne peut pas être annulée.");
        }
        if (tournee.getStatut() == StatutTournee.EN_LIVRAISON) {
            throw new IllegalStateException("Une tournée EN LIVRAISON ne peut pas être annulée.");
        }
        if (tournee.getStatut() == StatutTournee.LIVREE) {
            throw new IllegalStateException("Une tournée LIVRÉE ne peut pas être annulée.");
        }

        // Set status to cancelled
        tournee.setStatut(StatutTournee.ANNULEE);
        tournee.setCollecteFinalisee(false);

        Tournee saved = tourneeRepo.save(tournee);

        // Recompute verger status for the associated verger
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
        return mettreAJourInterne(id, req, currentUser);
    }

    private TourneeResponse mettreAJourInterne(String id, TourneeRequest req, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);

        // ========== STATUS CHECKS ==========
        if (tournee.getStatut() == StatutTournee.EN_LIVRAISON || tournee.getStatut() == StatutTournee.LIVREE) {
            throw new IllegalStateException("Cette tournée ne peut pas être modifiée car elle est en livraison ou déjà livrée.");
        }

        if (tournee.getStatut() != StatutTournee.PLANIFIEE) {
            throw new IllegalStateException("Seule une tournée PLANIFIÉE peut être modifiée.");
        }

        // ========== DATE VALIDATION ==========
        Date newDebut = req.getDateDebut() != null ? req.getDateDebut() : tournee.getDateDebut();
        Date newFin = req.getDateFin() != null ? req.getDateFin() : tournee.getDateFin();

        validateDates(newDebut, newFin);

        // ========== CHECK IF DATES HAVE CHANGED ==========
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

        // ========== UPDATE FIELDS ==========
        tournee.setDateDebut(newDebut);
        tournee.setDateFin(newFin);

        if (req.getNbreArbre() != null && req.getNbreArbre() > 0) {
            if (!req.getNbreArbre().equals(tournee.getNbreArbre())) {
                verifierArbresRestants(tournee.getVerger(), req.getNbreArbre());
            }
            tournee.setNbreArbre(req.getNbreArbre());
        }

        if (req.getDistanceTotale() != null) {
            tournee.setDistanceTotale(req.getDistanceTotale());
        }

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

        // Recompute verger status if needed
        if (saved.getVerger() != null && saved.getVerger().getId() != null) {
            vergerService.recomputeStatutForVerger(saved.getVerger().getId());
        }

        return toResponse(saved);
    }
    @Override
    public void supprimer(String id, UserDetails currentUser) {
        Tournee tournee = findOrThrow(id);
        checkTourneeAccess(tournee, currentUser);
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
        return calculerNbTourneesNecessairesInterne(vergerId);
    }

    private int calculerNbTourneesNecessairesInterne(String vergerId) {
        Verger verger = vergerRepo.findById(vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable : " + vergerId));
        return (int) Math.ceil((double) verger.getNbArbre() / Tournee.NB_ARBRES_PAR_TOURNEE);
    }

    // ========== AVAILABILITY CHECKS (CONSERVÉES INTACTES) ==========

    private void checkBenneDisponible(Ressource benne, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByBenne(benne.getId(), debut, fin, excludeId);
        System.out.println("🔍 Checking benne conflicts: " + benne.getNom() + " - found: " + conflicts.size());
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("La benne « " + benne.getNom() + " » est déjà utilisée du "
                    + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin()) + " (tournée " + c.getCode() + ").");
        }
    }

    private void checkTracteurDisponible(Ressource tracteur, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByTracteur(tracteur.getId(), debut, fin, excludeId);
        System.out.println("🔍 Checking tracteur conflicts: " + tracteur.getNom() + " - found: " + conflicts.size());
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("Le tracteur « " + tracteur.getNom() + " » est déjà utilisé du "
                    + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin()) + " (tournée " + c.getCode() + ").");
        }
    }

    private void checkTravailleurDisponible(Utilisateur travailleur, Date debut, Date fin, String excludeId) {
        List<Tournee> conflicts = tourneeRepo.findConflictsByTravailleur(travailleur.getId(), debut, fin, excludeId);
        System.out.println("🔍 Checking travailleur conflicts: " + travailleur.getPrenom() + " - found: " + conflicts.size());
        if (!conflicts.isEmpty()) {
            Tournee c = conflicts.get(0);
            throw new IllegalStateException("Le travailleur « " + travailleur.getPrenom() + " " + travailleur.getNom()
                    + " » est déjà assigné du " + fmt(c.getDateDebut()) + " au " + fmt(c.getDateFin())
                    + " (tournée " + c.getCode() + ").");
        }
    }

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
        Verger v = t.getVerger();
        String vergerTypeOlive = null, vergerAgriculteurNom = null;
        Double vergerSuperficie = null;
        if (v != null) {
            vergerTypeOlive = v.getTypeOlive();
            vergerSuperficie = v.getSuperficie();
            if (v.getAgriculteur() != null) {
                vergerAgriculteurNom = v.getAgriculteur().getPrenom() + " " + v.getAgriculteur().getNom();
            }
        }
        Ressource benne = t.getBenne();
        Ressource tracteur = t.getTracteur();
        return TourneeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .statut(t.getStatut())
                .vergerId(v != null ? v.getId() : null)
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom)
                .vergerSuperficie(vergerSuperficie)
                .benneId(benne != null ? benne.getId() : null)
                .benneNom(benne != null ? benne.getNom() : null)
                .benneCapaciteKg(benne != null ? benne.getCapaciteKg() : null)
                .tracteurId(tracteur != null ? tracteur.getId() : null)
                .tracteurNom(tracteur != null ? tracteur.getNom() : null)
                .tracteurImmatriculation(tracteur != null ? tracteur.getImmatriculation() : null)
                .travailleurIds(java.util.Collections.emptyList())
                .travailleurNoms(java.util.Collections.emptyList())
                .nbreArbre(t.getNbreArbre())
                .distanceTotale(t.getDistanceTotale())
                .tempsTotal(t.getTempsTotal())
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .collecteFinalisee(t.getCollecteFinalisee())
                .efficacite(null)
                .observations(t.getObservations())
                .livraisonDestinationNom(t.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                .livraisonEstimeDebut(t.getLivraisonEstimeDebut())
                .livraisonEstimeFin(t.getLivraisonEstimeFin())
                .livraisonNotes(t.getLivraisonNotes())
                .responsablePressoirId(t.getResponsablePressoirId())
                .responsablePressoirNom(null)
                .pressoirNom(null)
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
    }

    private TourneeResponse toResponse(Tournee t) {
        Verger v = t.getVerger();
        String vergerTypeOlive = null, vergerAgriculteurNom = null;
        Double vergerSuperficie = null;

        if (v != null) {
            vergerTypeOlive = v.getTypeOlive();
            vergerSuperficie = v.getSuperficie();
            if (v.getAgriculteur() != null)
                vergerAgriculteurNom = v.getAgriculteur().getPrenom() + " " + v.getAgriculteur().getNom();
        }

        Ressource benne = t.getBenne();
        Ressource tracteur = t.getTracteur();
        Utilisateur responsablePressoir = t.getResponsablePressoir();

        List<String> travailleurIds = new ArrayList<>();
        List<String> travailleurNoms = new ArrayList<>();
        if (t.getTravailleurs() != null) {
            for (Utilisateur u : t.getTravailleurs()) {
                travailleurIds.add(u.getId());
                travailleurNoms.add(u.getPrenom() + " " + u.getNom());
            }
        }

        Double totalVerger = (v != null) ? getTotalCollecteParVergerInterne(v.getId()) : null;

        return TourneeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .statut(t.getStatut())
                .vergerId(v != null ? v.getId() : null)
                .vergerTypeOlive(vergerTypeOlive)
                .vergerAgriculteurNom(vergerAgriculteurNom)
                .vergerSuperficie(vergerSuperficie)
                .benneId(benne != null ? benne.getId() : null)
                .benneNom(benne != null ? benne.getNom() : null)
                .benneCapaciteKg(benne != null ? benne.getCapaciteKg() : null)
                .tracteurId(tracteur != null ? tracteur.getId() : null)
                .tracteurNom(tracteur != null ? tracteur.getNom() : null)
                .tracteurImmatriculation(tracteur != null ? tracteur.getImmatriculation() : null)
                .travailleurIds(travailleurIds)
                .travailleurNoms(travailleurNoms)
                .nbreArbre(t.getNbreArbre())
                .distanceTotale(t.getDistanceTotale())
                .tempsTotal(t.getTempsTotal())
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .collecteFinalisee(t.getCollecteFinalisee())
                .efficacite(calculerEfficacite(t))
                .observations(t.getObservations())
                .livraisonDestinationNom(t.getLivraisonDestinationNom())
                .livraisonDestinationAdresse(t.getLivraisonDestinationAdresse())
                .livraisonEstimeDebut(t.getLivraisonEstimeDebut())
                .livraisonEstimeFin(t.getLivraisonEstimeFin())
                .livraisonNotes(t.getLivraisonNotes())
                .responsablePressoirId(responsablePressoir != null ? responsablePressoir.getId() : t.getResponsablePressoirId())
                .responsablePressoirNom(responsablePressoir != null
                        ? ((responsablePressoir.getPrenom() != null ? responsablePressoir.getPrenom() : "") + " " + (responsablePressoir.getNom() != null ? responsablePressoir.getNom() : "")).trim()
                        : null)
                .pressoirNom(responsablePressoir != null && responsablePressoir.getPressoir() != null ? responsablePressoir.getPressoir().getNom() : null)
                .pressoirAdresse(responsablePressoir != null && responsablePressoir.getPressoir() != null ? responsablePressoir.getPressoir().getAdresse() : null)
                .livraisonStartedAt(t.getLivraisonStartedAt())
                .livraisonCompletedAt(t.getLivraisonCompletedAt())
                .livraisonEvidenceName(t.getLivraisonEvidenceName())
                .livraisonEvidenceUrl(t.getLivraisonEvidenceUrl())
                .dateDebut(t.getDateDebut())
                .dateFin(t.getDateFin())
                .dateCreation(t.getDateCreation())
                .totalCollecteVergerKg(totalVerger)
                .collecteId(t.getCollecte() != null ? t.getCollecte().getId() : null)
                .collecteCode(t.getCollecte() != null ? t.getCollecte().getCode() : null)
                .build();
    }
}