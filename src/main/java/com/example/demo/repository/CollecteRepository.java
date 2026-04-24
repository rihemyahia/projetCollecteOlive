package com.example.demo.repository;

import com.example.demo.model.Collecte;
import com.example.demo.model.enums.StatutCollecte;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteRepository extends MongoRepository<Collecte, String> {

    // ═══════════════════════════════════════════════════════════════
    // BASIC QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find collecte by verger ID and campaign year
     * Uses compound index: idx_verger_annee
     */
    Optional<Collecte> findByVergerIdAndAnnee(String vergerId, String annee);
    /**
     * Find all collectes for a specific verger, ordered by year (newest first)
     */
    List<Collecte> findByVergerIdOrderByAnneeDescNumeroDesc(String vergerId);
    /**
     * Find all collectes by status
     */
    List<Collecte> findByStatut(StatutCollecte statut);
    /**
     * Find all active collectes (PLANIFIEE or EN_COURS)
     */
    @Query("{ 'statut': { $in: ['PLANIFIEE', 'EN_COURS'] } }")
    List<Collecte> findActiveCollectes();
    /**
     * Find all closed collectes (TERMINEE)
     */
    List<Collecte> findByEstClotureeTrue();

    // ═══════════════════════════════════════════════════════════════
    // AGGREGATION QUERIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get the maximum numero for a verger in a specific year
     * Used when creating a new collecte to get the next number
     */
    @Query(value = "{ 'vergerId': ?0, 'annee': ?1 }", 
           fields = "{ 'numero': 1 }")
    Optional<Integer> findMaxNumeroByVergerIdAndAnnee(String vergerId, String annee);
    
    /**
     * Get all collectes for a specific year across all vergers
     */
    List<Collecte> findByAnnee(String annee);
    
    /**
     * Get collectes by year range
     */
    @Query("{ 'annee': { $regex: ?0, $options: 'i' } }")
    List<Collecte> findByAnneeContaining(String yearPattern);
    
    // ═══════════════════════════════════════════════════════════════
    // STATISTICS QUERIES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get total quantity collected for a verger in a specific year
     */
    @Aggregation(pipeline = {
        "{ $match: { 'vergerId': ?0, 'annee': ?1 } }",
        "{ $group: { _id: null, total: { $sum: '$quantiteTotaleKg' } } }"
    })
    Double getTotalQuantiteByVergerAndAnnee(String vergerId, String annee);
    
    /**
     * Get average rendement for all collectes of a verger
     */
    @Aggregation(pipeline = {
        "{ $match: { 'vergerId': ?0, 'rendementMoyenParArbre': { $ne: null } } }",
        "{ $group: { _id: null, avgRendement: { $avg: '$rendementMoyenParArbre' } } }"
    })
    Double getAverageRendementByVerger(String vergerId);
    
    /**
     * Count collectes by status for a specific verger
     */
    @Aggregation(pipeline = {
        "{ $match: { 'vergerId': ?0 } }",
        "{ $group: { _id: '$statut', count: { $sum: 1 } } }"
    })
    List<Object> countCollectesByStatutAndVerger(String vergerId);
    
    // ═══════════════════════════════════════════════════════════════
    // EXISTENCE CHECKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if a collecte exists for a verger in a specific year
     */
    boolean existsByVergerIdAndAnnee(String vergerId, String annee);
    
    /**
     * Check if a verger has any terminated collecte
     */
    boolean existsByVergerIdAndStatut(String vergerId, StatutCollecte statut);
    
    // ═══════════════════════════════════════════════════════════════
    // DELETE OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Delete all collectes for a specific verger (use with caution!)
     */
    void deleteByVergerId(String vergerId);
    
    /**
     * Delete all collectes with status PLANIFIEE for a verger
     */
    void deleteByVergerIdAndStatut(String vergerId, StatutCollecte statut);
	List<Collecte> findByVergerIdIn(List<String> vergerIds);
}