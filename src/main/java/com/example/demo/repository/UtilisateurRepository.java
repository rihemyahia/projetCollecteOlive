package com.example.demo.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Utilisateur;

import java.util.Optional;


public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    
    Optional<Utilisateur> findByEmail(String email);
    
    public Optional<Utilisateur> findByEmailAndRole(String email, String role) ;
		// TODO Auto-generated method stub
    
    boolean existsByEmail(String email);
    
    Optional<Utilisateur> findByRoleAndEstActif(String role, Boolean estActif);
}