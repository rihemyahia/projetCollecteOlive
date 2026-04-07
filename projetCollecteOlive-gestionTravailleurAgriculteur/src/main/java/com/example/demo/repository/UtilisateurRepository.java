package com.example.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByEmailAndRole(String email, Role role);

    // ========== MÉTHODES AVEC @Query UTILISANT LE BON NOM DE CHAMP ==========
    
    @Query("{ 'compteActif': false }")
    List<Utilisateur> findUsersWithAccountFalse();

    @Query("{ 'role': ?0, 'compteActif': false }")
    List<Utilisateur> findUsersByRoleAndAccountFalse(Role role);

    @Query(value = "{ 'role': ?0, 'compteActif': false }", count = true)
    long countByRoleAndAccountFalse(Role role);

    @Query("{ 'compteActif': false }")
    List<Utilisateur> findByCompteActifFalse();  // Changé de findByACompteFalse

    // ========== MÉTHODES POUR AUTH SERVICE ==========
    
    @Query("{ 'role': ?0, 'compteActif': false }")
    List<Utilisateur> findByRoleAndCompteActifFalse(Role role);
    
    @Query(value = "{ 'role': ?0, 'compteActif': false }", count = true)
    long countByRoleAndCompteActifFalse(Role role);
    
    @Query(value = "{ 'compteActif': false }", count = true)
    long countByCompteActifFalse();

    // ========== MÉTHODES SIMPLES ==========
    
    List<Utilisateur> findByRole(Role role);

    List<Utilisateur> findByRoleAndDisponibleTrue(Role role);

    boolean existsByEmail(String email);

    Optional<Utilisateur> findByRoleAndEstActif(Role role, Boolean estActif);

    List<Utilisateur> findByRoleAndSpecialitesContaining(Role role, String specialite);

    long countByRole(Role role);

    List<Utilisateur> findByRoleAndNomExploitationContaining(Role role, String nomExploitation);

    // ⚠️ SUPPRIMEZ CES DEUX LIGNES (elles causent l'erreur) :
    // List<Utilisateur> findByRoleAndACompteIsFalse(Role role);
    // long countByRoleAndACompteIsFalse(Role role);
}