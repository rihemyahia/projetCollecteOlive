package com.example.demo.repository;

import com.example.demo.model.Verger;
import com.example.demo.model.enums.StatutVerger;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VergerRepository extends MongoRepository<Verger, String> {

    List<Verger> findByProprietaireId(String proprietaireId);

    List<Verger> findByProprietaireIdAndEstActifTrue(String proprietaireId);

    List<Verger> findByStatut(StatutVerger statut);

    List<Verger> findByEstActifTrue();

    boolean existsByNomAndProprietaireId(String nom, String proprietaireId);
    List<Verger> findByEstActifFalse();
    List<Verger> findByEstActifTrueAndSupprimerFalse();
    List<Verger> findByEstActifFalseAndSupprimerFalse();
    List<Verger> findByProprietaireIdAndEstActifTrueAndSupprimerFalse(String proprietaireId);
}