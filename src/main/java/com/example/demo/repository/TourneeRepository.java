package com.example.demo.repository;

import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Verger;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourneeRepository extends MongoRepository<Tournee, String> {

    Optional<Tournee> findByCode(String code);
    List<Tournee> findByStatut(StatutTournee statut);
    List<Tournee> findByVergerId(String vergerId);
    List<Tournee> findByCollecteId(String collecteId);

    @Query("{ 'statut': { $in: ['PLANIFIEE', 'EN_COURS'] } }")
    List<Tournee> findActive();
    // Ajoutez cette méthode
    @Query("{ 'verger._id': { $in: ?0 } }")
    List<Tournee> findByVergerIdIn(List<String> vergerIds);
    boolean existsByCode(String code);

    @Query("{ 'vergerId': ?0, 'statut': 'TERMINEE' }")
    List<Tournee> findTermineesByVergerId(String vergerId);

    // ✅ FIXED CONFLICT QUERY FOR BENNE
    @Query("{ " +
            "  'benne.id': ?0, " +
            "  'statut': { $in: ['PLANIFIEE', 'EN_COURS'] }, " +
            "  '_id': { $ne: ?3 }, " +
            "  $or: [ " +
            "    { $and: [ { 'dateDebut': { $lt: ?2 } }, { 'dateFin': { $gt: ?1 } } ] }, " +
            "    { $and: [ { 'dateDebut': { $gte: ?1 } }, { 'dateDebut': { $lt: ?2 } } ] }, " +
            "    { $and: [ { 'dateFin': { $gt: ?1 } }, { 'dateFin': { $lte: ?2 } } ] } " +
            "  ] " +
            "}")
    List<Tournee> findConflictsByBenne(String benneId, Date debut, Date fin, String excludeId);
    @Query("{ 'statut': ?0, 'transporteur': null }")
    Page<Tournee> findByStatutAndTransporteurIsNull(StatutTournee statut, Pageable pageable);
    @Query("{ 'statut': { $in: ?0 }, 'transporteur': null }")
    Page<Tournee> findByStatutInAndTransporteurIsNull(List<StatutTournee> statuses, Pageable pageable);

    /**
     * Filtrage par vergers du responsable : Spring peut persister la référence comme {@code verger._id}
     * ou {@code verger.$id} selon le document — les deux sont couverts.
     */
    @Query("{ 'statut': { $in: ?0 }, 'transporteur': null, '$or': [ "
            + "{ 'verger._id': { $in: ?1 } }, "
            + "{ 'verger.$id': { $in: ?1 } } "
            + "] }")
    Page<Tournee> findByStatutInAndTransporteurIsNullAndVergerIdIn(
            List<StatutTournee> statuses,
            List<String> vergerIds,
            Pageable pageable);

    @Query("{ 'transporteur.id': ?0 }")
    List<Tournee> findByTransporteurId(String transporteurId, Sort sort);
    @Query("{ 'responsablePressoir.id': ?0 }")
    List<Tournee> findByResponsablePressoirId(String responsablePressoirId, Sort sort);
    @Query("{ 'responsablePressoir.id': ?0, 'statut': ?1 }")
    List<Tournee> findByResponsablePressoirIdAndStatut(String responsablePressoirId, StatutTournee statut, Sort sort);
    @Query("{ 'transporteur.id': ?0, 'statut': ?1 }")
    List<Tournee> findByTransporteurIdAndStatut(String transporteurId, StatutTournee statut, Sort sort);
    // ✅ FIXED CONFLICT QUERY FOR TRACTEUR
    @Query("{ " +
            "  'tracteur.id': ?0, " +
            "  'statut': { $in: ['PLANIFIEE', 'EN_COURS','TERMINEE'] }, " +
            "  '_id': { $ne: ?3 }, " +
            "  $or: [ " +
            "    { $and: [ { 'dateDebut': { $lt: ?2 } }, { 'dateFin': { $gt: ?1 } } ] }, " +
            "    { $and: [ { 'dateDebut': { $gte: ?1 } }, { 'dateDebut': { $lt: ?2 } } ] }, " +
            "    { $and: [ { 'dateFin': { $gt: ?1 } }, { 'dateFin': { $lte: ?2 } } ] } " +
            "  ] " +
            "}")


    List<Tournee> findConflictsByTracteur(String tracteurId, Date debut, Date fin, String excludeId);
    List<Tournee> findByDateDebutBetween(Date debut, Date fin);
    @Query("{ 'travailleurs': { $in: [ObjectId(?0)] }, 'dateDebut': { $gte: ?1, $lte: ?2 } }")
    List<Tournee> findByTravailleursIdAndDateDebutBetween(String travailleurId, Date debut, Date fin);
    @Query("{ 'verger': { $in: ?0 }, 'dateDebut': { $gte: ?1, $lte: ?2 } }")
    List<Tournee> findByVergerIdInAndDateDebutBetween(List<ObjectId> vergerIds, Date debut, Date fin);
    List<Tournee> findByVergerIdAndDateDebutBetween(String vergerId, Date debut, Date fin);
    @Query("{ 'verger': { $oid: ?0 }, 'travailleurs': { $in: [ObjectId(?1)] }, 'dateDebut': { $gte: ?2, $lte: ?3 } }")
    List<Tournee> findByVergerIdAndTravailleurIdAndDateDebutBetween(
            String vergerId,
            String travailleurId,
            Date debut,
            Date fin
    );
    @Query("{ 'travailleurs': { $in: [ObjectId(?0)] }, 'dateDebut': { $gte: ?1, $lte: ?2 } }")
    List<Tournee> findByTravailleurIdAndDateDebutBetween(String travailleurId, Date debut, Date fin);
    @Query(value = "{}", fields = "{'code': 1, 'statut': 1, 'dateDebut': 1, 'dateFin': 1, 'dateCreation': 1, 'quantiteCollecteeKg': 1, 'distanceTotale': 1, 'observations': 1, 'livraisonDestinationNom': 1, 'livraisonDestinationAdresse': 1}")
    List<Tournee> findAllMinimal();
    // ✅ FIXED CONFLICT QUERY FOR TRAVAILLEUR
    @Query("{ " +
            "  'travailleurs': { $in: [ObjectId(?0)] }, " +
            "  'statut': { $in: ['PLANIFIEE', 'EN_COURS','TERMINEE'] }, " +
            "  '_id': { $ne: ?3 }, " +
            "  $or: [ " +
            "    { $and: [ { 'dateDebut': { $lt: ?2 } }, { 'dateFin': { $gt: ?1 } } ] }, " +
            "    { $and: [ { 'dateDebut': { $gte: ?1 } }, { 'dateDebut': { $lt: ?2 } } ] }, " +
            "    { $and: [ { 'dateFin': { $gt: ?1 } }, { 'dateFin': { $lte: ?2 } } ] } " +
            "  ] " +
            "}")
    List<Tournee> findConflictsByTravailleur(String travailleurId, Date debut, Date fin, String excludeId);

    // CONFLICT QUERY FOR TRANSPORTEUR (used when assigning tournees to a transporteur)
    @Query("{ " +
            "  'transporteur.id': ?0, " +
            "  'statut': { $in: ['PLANIFIEE', 'EN_COURS','TERMINEE','EN_LIVRAISON'] }, " +
            "  '_id': { $ne: ?3 }, " +
            "  $or: [ " +
            "    { $and: [ { 'dateDebut': { $lt: ?2 } }, { 'dateFin': { $gt: ?1 } } ] }, " +
            "    { $and: [ { 'dateDebut': { $gte: ?1 } }, { 'dateDebut': { $lt: ?2 } } ] }, " +
            "    { $and: [ { 'dateFin': { $gt: ?1 } }, { 'dateFin': { $lte: ?2 } } ] } " +
            "  ] " +
            "}")
    List<Tournee> findConflictsByTransporteur(String transporteurId, Date debut, Date fin, String excludeId);
}