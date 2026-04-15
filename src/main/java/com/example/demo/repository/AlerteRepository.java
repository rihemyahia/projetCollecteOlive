package com.example.demo.repository;

import com.example.demo.model.AlerteTerrain;
import com.example.demo.model.enums.NiveauUrgence;
import com.example.demo.model.enums.StatutAlerte;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface AlerteRepository extends MongoRepository<AlerteTerrain, String> {

    List<AlerteTerrain> findByEstSupprimerFalse();

    @Query("{ 'statut': ?0, 'estSupprimer': false }")
    List<AlerteTerrain> findByStatutAndNotDeleted(StatutAlerte statut);

    @Query("{ 'niveauUrgence': ?0, 'estSupprimer': false }")
    List<AlerteTerrain> findByNiveauUrgenceAndNotDeleted(NiveauUrgence niveauUrgence);

    @Query("{ 'agriculteur': ?0, 'estSupprimer': false }")
    List<AlerteTerrain> findByAgriculteurId(ObjectId agriculteurId);

    @Query("{ 'verger': ?0, 'estSupprimer': false }")
    List<AlerteTerrain> findByVergerId(ObjectId vergerId);

    // Finds non-deleted alerts within 500m — requires 2dsphere index on 'location'
    // GeoJSON uses [longitude, latitude] order
    @Query("{ 'location': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: 500 } }, 'estSupprimer': false }")
    List<AlerteTerrain> findNearbyAlerts(Double longitude, Double latitude);

    // Count nearby alerts — used to decide if a cluster notification should fire
    @Query(value = "{ 'location': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: 500 } }, 'estSupprimer': false }", count = true)
    long countNearbyAlerts(Double longitude, Double latitude);
}