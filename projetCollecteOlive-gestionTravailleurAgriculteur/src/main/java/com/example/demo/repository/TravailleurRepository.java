package com.example.demo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.demo.model.Ressource;
import com.example.demo.model.TypeRessource;
import com.example.demo.model.Utilisateur;

public interface TravailleurRepository extends MongoRepository<Ressource, String>{

	List<Ressource> findByType(TypeRessource travailleur);

	List<Ressource> findByTypeAndStatutAndTourneeActuelleIdIsNull(TypeRessource travailleur, String string);

	List<Ressource> findByTypeAndSpecialite(TypeRessource travailleur, String specialite);


}
