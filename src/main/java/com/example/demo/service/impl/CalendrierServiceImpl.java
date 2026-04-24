package com.example.demo.service.impl;

import com.example.demo.dto.EvenementCalendrierDTO;
import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.Verger;
import com.example.demo.repository.TourneeRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.example.demo.repository.VergerRepository;
import com.example.demo.service.CalendrierService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendrierServiceImpl implements CalendrierService {

    private final TourneeRepository tourneeRepo;
    private final TourneeServiceImpl tourneeService;
    private final UtilisateurRepository utilisateurRepo;
    private final VergerRepository vergerRepo;

    @Override
    public List<EvenementCalendrierDTO> getEvenementsByUserEmail(String email, Date debut, Date fin) {
        Utilisateur user = utilisateurRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));
        
        List<Tournee> tournees = new ArrayList<>();
        
        switch (user.getRole()) {
            case ADMIN:
            case RESPONSABLE:
                tournees = tourneeRepo.findByDateDebutBetween(debut, fin);
                break;
                
            case AGRICULTEUR:
                List<Verger> vergers = vergerRepo.findByAgriculteurIdAndEstSupprimerFalse(user.getId());
                // ✅ FIX: Convert to ObjectId
                List<ObjectId> objectIds = vergers.stream()
                    .map(v -> new ObjectId(v.getId()))
                    .collect(Collectors.toList());
                if (!objectIds.isEmpty()) {
                    tournees = tourneeRepo.findByVergerIdInAndDateDebutBetween(objectIds, debut, fin);
                }
                break;
                
            case TRAVAILLEUR:
                tournees = tourneeRepo.findByTravailleursIdAndDateDebutBetween(user.getId(), debut, fin);
                break;
                
            default:
                tournees = new ArrayList<>();
        }
        
        return tournees.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<EvenementCalendrierDTO> getEvenementsByUserConnected(Date debut, Date fin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        Utilisateur user = utilisateurRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));
        
        List<Tournee> tournees = new ArrayList<>();
        
        switch (user.getRole()) {
            case ADMIN:
            case RESPONSABLE:
                tournees = tourneeRepo.findByDateDebutBetween(debut, fin);
                System.out.println("👑 " + user.getRole() + " voit " + tournees.size() + " tournée(s)");
                break;
                
            case AGRICULTEUR:
                System.out.println("========== AGRICULTEUR DEBUG ==========");
                System.out.println("User ID: " + user.getId());
                System.out.println("User Email: " + user.getEmail());
                
                List<Verger> vergers = vergerRepo.findByAgriculteurIdAndEstSupprimerFalse(user.getId());
                System.out.println("Number of vergers found: " + vergers.size());
                
                for (Verger v : vergers) {
                    System.out.println("  Verger ID: " + v.getId() + " | Type: " + v.getTypeOlive());
                }
                
                // ✅ Convert String IDs to ObjectId - filter out any nulls
                List<ObjectId> objectIds = vergers.stream()
                    .filter(v -> v.getId() != null && !v.getId().isEmpty())
                    .map(v -> new ObjectId(v.getId()))
                    .collect(Collectors.toList());
                
                System.out.println("ObjectIds: " + objectIds);
                System.out.println("Date range - Debut: " + debut + " | Fin: " + fin);
                
                if (!objectIds.isEmpty()) {
                    // ✅ Remove the problematic debug loop
                    tournees = tourneeRepo.findByVergerIdInAndDateDebutBetween(objectIds, debut, fin);
                    System.out.println("Tournées found by filter: " + tournees.size());
                } else {
                    System.out.println("⚠️ No valid vergers found for this agriculteur!");
                }
                System.out.println("=====================================");
                break;
            case TRAVAILLEUR:
                tournees = tourneeRepo.findByTravailleursIdAndDateDebutBetween(user.getId(), debut, fin);
                System.out.println("👤 TRAVAILLEUR " + user.getPrenom() + " " + user.getNom() + " voit " + tournees.size() + " tournée(s)");
                break;
                
            default:
                tournees = new ArrayList<>();
        }
        
        return tournees.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<EvenementCalendrierDTO> getEvenements(Date debut, Date fin) {
        return getEvenementsByUserConnected(debut, fin);
    }

    @Override
    public List<EvenementCalendrierDTO> getEvenementsByVerger(String vergerId, Date debut, Date fin) {
        List<Tournee> tournees = tourneeRepo.findByVergerIdAndDateDebutBetween(vergerId, debut, fin);
        return tournees.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<EvenementCalendrierDTO> getEvenementsByTravailleur(String travailleurId, Date debut, Date fin) {
        System.out.println("🔍 getEvenementsByTravailleur called with id: " + travailleurId);
        List<Tournee> tournees = tourneeRepo.findByTravailleursIdAndDateDebutBetween(travailleurId, debut, fin);
        System.out.println("📊 Found " + tournees.size() + " tournees for travailleur " + travailleurId);
        return tournees.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<EvenementCalendrierDTO> getEvenementsByVergerAndTravailleur(
            String vergerId, String travailleurId, Date debut, Date fin) {
        
        System.out.println("========== DEBUG INFO ==========");
        System.out.println("Searching for tournées with:");
        System.out.println("  Verger ID: " + vergerId);
        System.out.println("  Travailleur ID: " + travailleurId);
        System.out.println("  Date range: " + debut + " to " + fin);
        System.out.println("=================================");
        
        List<Tournee> allForTravailleur = tourneeRepo.findByTravailleurIdAndDateDebutBetween(
            travailleurId, debut, fin);
        System.out.println("Total tournées for travailleur (any verger): " + allForTravailleur.size());
        
        List<Tournee> tournees = tourneeRepo.findByVergerIdAndTravailleurIdAndDateDebutBetween(
            vergerId, travailleurId, debut, fin);
        
        System.out.println("Tournées matching both verger AND travailleur: " + tournees.size());
        
        if (tournees.isEmpty()) {
            System.out.println("⚠️ No tournées found! Possible reasons:");
            System.out.println("  1. Verger ID doesn't match any tournée");
            System.out.println("  2. Travailleur not assigned to any tournée in this verger");
            System.out.println("  3. No tournées in the date range");
            System.out.println("  4. The tournées are outside the date range");
            
            Set<String> vergerIds = allForTravailleur.stream()
                .map(t -> t.getVerger().getId())
                .collect(Collectors.toSet());
            System.out.println("Vergers this travailleur is assigned to: " + vergerIds);
        }
        
        return tournees.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    @Override
    public EvenementCalendrierDTO reprogrammerEvenement(String tourneeId, Date nouvelleDate, String raison) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Utilisateur user = utilisateurRepo.findByEmail(email).orElse(null);
        
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.RESPONSABLE)) {
            throw new RuntimeException("Seuls les ADMIN et RESPONSABLE peuvent reprogrammer des tournées");
        }
        
        Tournee tournee = tourneeService.getTourneeById(tourneeId);
        
        Date ancienneDate = tournee.getDateDebut();
        tournee.setDateDebut(nouvelleDate);
        
        if (tournee.getDateFin() != null) {
            long duree = tournee.getDateFin().getTime() - ancienneDate.getTime();
            tournee.setDateFin(new Date(nouvelleDate.getTime() + duree));
        }
        
        Tournee saved = tourneeRepo.save(tournee);
        
        System.out.println("📅 Événement reprogrammé par " + user.getEmail() + ": " + tournee.getCode() + 
                           " (" + ancienneDate + " → " + nouvelleDate + ")" +
                           (raison != null ? " Raison: " + raison : ""));
        
        return toDTO(saved);
    }

    private EvenementCalendrierDTO toDTO(Tournee t) {
        String couleur = getCouleurParStatut(t.getStatut());
        
        Verger verger = t.getVerger();
        String agriculteurNom = null;
        String agriculteurPrenom = null;
        String agriculteurEmail = null;
        String agriculteurTelephone = null;
        String agriculteurId = null;
        
        if (verger != null && verger.getAgriculteur() != null) {
            Utilisateur agriculteur = verger.getAgriculteur();
            agriculteurId = agriculteur.getId();
            agriculteurNom = agriculteur.getNom();
            agriculteurPrenom = agriculteur.getPrenom();
            agriculteurEmail = agriculteur.getEmail();
            agriculteurTelephone = agriculteur.getTelephone();
        }
        
        return EvenementCalendrierDTO.builder()
                .id(t.getId())
                .titre("Collecte - " + (verger != null ? verger.getTypeOlive() : "Inconnu"))
                .debut(t.getDateDebut())
                .fin(t.getDateFin())
                .vergerId(verger != null ? verger.getId() : null)
                .vergerNom(verger != null ? verger.getTypeOlive() : "Inconnu")
                .vergerTypeOlive(verger != null ? verger.getTypeOlive() : null)
                .vergerSuperficie(verger != null ? verger.getSuperficie() : null)
                .vergerNbArbre(verger != null ? verger.getNbArbre() : null)
                .vergerStatut(verger != null && verger.getStatut() != null ? verger.getStatut().toString() : null)
                .agriculteurId(agriculteurId)
                .agriculteurNom(agriculteurNom)
                .agriculteurPrenom(agriculteurPrenom)
                .agriculteurEmail(agriculteurEmail)
                .agriculteurTelephone(agriculteurTelephone)
                .travailleursNoms(
                    t.getTravailleurs() != null ? 
                        t.getTravailleurs().stream()
                            .map(u -> u.getPrenom() + " " + u.getNom())
                            .collect(Collectors.toList()) : 
                        new ArrayList<>()
                )
                .travailleurIds(
                    t.getTravailleurs() != null ?
                        t.getTravailleurs().stream()
                            .map(Utilisateur::getId)
                            .collect(Collectors.toList()) :
                        new ArrayList<>()
                )
                .statut(t.getStatut() != null ? t.getStatut().name() : "UNKNOWN")
                .couleur(couleur)
                .quantiteCollecteeKg(t.getQuantiteCollecteeKg())
                .nbreArbre(t.getNbreArbre())
                .distanceTotale(t.getDistanceTotale())
                .collecteId(t.getCollecte() != null ? t.getCollecte().getId() : null)
                .collecteCode(t.getCollecte() != null ? t.getCollecte().getCode() : null)
                .observations(t.getObservations())
                .dateCreation(t.getDateCreation())
                .build();
    }
    
    private String getCouleurParStatut(StatutTournee statut) {
        switch (statut) {
            case PLANIFIEE: return "#3498db";
            case EN_COURS: return "#f39c12";
            case TERMINEE: return "#27ae60";
            case ANNULEE: return "#e74c3c";
            default: return "#95a5a6";
        }
    }
}