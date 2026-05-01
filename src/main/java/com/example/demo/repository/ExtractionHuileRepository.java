package com.example.demo.repository;

import com.example.demo.model.ExtractionHuile;
import com.example.demo.model.enums.StatutExtractionHuile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtractionHuileRepository extends MongoRepository<ExtractionHuile, String> {
    Optional<ExtractionHuile> findByTourneeId(String tourneeId);
    boolean existsByTourneeId(String tourneeId);
    List<ExtractionHuile> findByCollecteId(String collecteId);

    @Query("{ 'responsablePressoir.id': ?0 }")
    List<ExtractionHuile> findByResponsablePressoirId(String responsablePressoirId);

    @Query("{ 'responsablePressoir.id': ?0, 'statut': ?1 }")
    List<ExtractionHuile> findByResponsablePressoirIdAndStatut(String responsablePressoirId, StatutExtractionHuile statut);
}
