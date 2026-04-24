package com.example.demo.repository;

import com.example.demo.model.Verger;
import com.example.demo.model.Utilisateur;
import com.example.demo.model.enums.StatutVerger;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface VergerRepository extends MongoRepository<Verger, String> {

    List<Verger> findByEstSupprimerFalse();

    List<Verger> findByStatut(StatutVerger statut);

    List<Verger> findByAgriculteur(Utilisateur agriculteur);

    @Query("{ 'agriculteur' : ?0, 'estSupprimer' : false }")
    List<Verger> findActiveByAgriculteurId(ObjectId agriculteurId);

    @Query(value = "{ 'agriculteur' : ?0 }", exists = true)
    boolean existsByAgriculteurId(ObjectId agriculteurId);

    // ── Geolocation queries ─────────────────────────────────────────────────

    /**
     * Find all non-deleted vergers that have GPS coordinates set.
     * Used by the responsable/admin map view to show all georeferenced vergers.
     */
    @Query("{ 'location': { $exists: true, $ne: null }, 'estSupprimer': false }")
    List<Verger> findAllWithLocation();

    /**
     * Find all non-deleted vergers belonging to a given agriculteur that have GPS coordinates.
     * Used by the agriculteur map view to show only their own vergers.
     */
    List<Verger> findByAgriculteurId(String id);
    @Query("{ 'agriculteur': ?0, 'location': { $exists: true, $ne: null }, 'estSupprimer': false }")
    List<Verger> findByAgriculteurWithLocation(ObjectId agriculteurId);

    /**
     * Find vergers within a given radius (in metres) of a point.
     * GeoJSON coordinates are [longitude, latitude].
     * Requires the 2dsphere index declared on Verger.location.
     */
    @Query("{ 'location': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } }, 'estSupprimer': false }")
    List<Verger> findNearby(Double longitude, Double latitude, Double maxDistanceMetres);

    @Query("{ 'responsable' : ?0, 'estSupprimer' : false }")
    List<Verger> findByResponsableIdAndEstSupprimerFalse(ObjectId responsableId);

	List<Verger> findByResponsableId(String responsableId);

    @Query("{ 'responsable' : ?0, 'estSupprimer' : false }")
    List<Verger> findByResponsableIdAndEstSupprimerFalse(String responsableId);

    @Query("{ 'agriculteur': ?0, 'estSupprimer': false }")
    List<Verger> findByAgriculteurIdAndEstSupprimerFalse(String agriculteurId);
}