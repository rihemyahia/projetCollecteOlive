package com.example.demo;

import com.example.demo.model.Role;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@PropertySource("classpath:application.properties")
public class ProjetCollecteOliveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjetCollecteOliveApplication.class, args);
        System.out.println("========================================");
        System.out.println("🌿 Projet Collecte Olives démarré !");
        System.out.println("📍 http://localhost:8080");
        System.out.println("========================================");
    }

    @Bean
    public CommandLineRunner initData(UtilisateurRepository utilisateurRepository) {
        return args -> {
            System.out.println("\n🚀 Initialisation des données de démarrage...");

            // ===== CREATE ADMIN USER =====
            if (utilisateurRepository.findByEmail("admin@cooperative.com").isEmpty()) {
                Utilisateur admin = new Utilisateur();
                admin.setEmail("admin@cooperative.com");
                admin.setPrenom("Admin");
                admin.setNom("Coopérative");
                admin.setTelephone("+216 98 765 432");
                admin.setRole(Role.ADMIN);
                admin.setAdresse("Sfax, Tunisie");
                admin.setEstActif(true);
                admin.setDateCreation(new Date());
                admin.setCompteActif(true);
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                admin.setMotDePasse(encoder.encode("admin123"));

                utilisateurRepository.save(admin);
                System.out.println("✅ Utilisateur ADMIN créé avec succès !");
                System.out.println("   📧 Email: admin@cooperative.com");
                System.out.println("   🔑 Mot de passe: admin123");
                System.out.println("   👤 Rôle: ADMIN");
            } else {
                System.out.println("ℹ️ Utilisateur ADMIN existe déjà");
            }

            // ===== CREATE RESPONSABLE USER =====
            if (utilisateurRepository.findByEmail("responsable1@cooperative.com").isEmpty()) {
                Utilisateur responsable = new Utilisateur();
                responsable.setEmail("responsable1@cooperative.com");
                responsable.setPrenom("Faiza");  // Keeping your specific name
                responsable.setNom("Ghozzi");    // Keeping your specific name
                responsable.setTelephone("+216 98 765 432");
                responsable.setRole(Role.RESPONSABLE);
                responsable.setAdresse("Sfax, Tunisie");
                responsable.setEstActif(true);
                responsable.setDateCreation(new Date());
                responsable.setCompteActif(true);
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                responsable.setMotDePasse(encoder.encode("responsable123")); // Using teammate's password

                utilisateurRepository.save(responsable);

                System.out.println("✅ Utilisateur RESPONSABLE créé avec succès !");
                System.out.println("   📧 Email: responsable@cooperative.com");
                System.out.println("   🔑 Mot de passe: responsable123");
                System.out.println("   👤 Rôle: RESPONSABLE");
                System.out.println("   👤 Nom: Faiza Ghozzi");
            } else {
                System.out.println("ℹ️ Utilisateur RESPONSABLE existe déjà");
            }

            // ===== OPTIONAL: Create a test AGRICULTEUR user =====
            if (utilisateurRepository.findByEmail("agriculteur2@test.com").isEmpty()) {
                Utilisateur agriculteur = new Utilisateur();
                agriculteur.setEmail("agriculteur2@test.com");
                agriculteur.setPrenom("Mohamed");
                agriculteur.setNom("Ben Ali");
                agriculteur.setTelephone("+216 55 555 555");
                agriculteur.setRole(Role.AGRICULTEUR);
                agriculteur.setAdresse("Mahdia, Tunisie");
                agriculteur.setEstActif(true);
                agriculteur.setDateCreation(new Date());

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                agriculteur.setMotDePasse(encoder.encode("agriculteur123"));

                utilisateurRepository.save(agriculteur);

                System.out.println("✅ Utilisateur AGRICULTEUR créé avec succès !");
                System.out.println("   📧 Email: agriculteur@test.com");
                System.out.println("   🔑 Mot de passe: agriculteur123");
                System.out.println("   👤 Rôle: AGRICULTEUR");
            }

            // ===== OPTIONAL: Create a test EQUIPE_RECOLTE user =====
            if (utilisateurRepository.findByEmail("equipe@recolte.com").isEmpty()) {
                Utilisateur equipeRecolte = new Utilisateur();
                equipeRecolte.setEmail("equipe@recolte.com");
                equipeRecolte.setPrenom("Karim");
                equipeRecolte.setNom("Said");
                equipeRecolte.setTelephone("+216 77 777 777");
                equipeRecolte.setRole(Role.TRAVAILLEUR);
                equipeRecolte.setAdresse("Sousse, Tunisie");
                equipeRecolte.setEstActif(true);
                List<String> specialites = new ArrayList<>();
                specialites.add("cueillette");
                specialites.add("ramassage");
                equipeRecolte.setSpecialites(specialites);  // ✅ Avec valeurs                equipeRecolte.setDateCreation(new Date());

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                equipeRecolte.setMotDePasse(encoder.encode("equipe123"));

                utilisateurRepository.save(equipeRecolte);

                System.out.println("✅ Utilisateur EQUIPE_RECOLTE créé avec succès !");
                System.out.println("   📧 Email: equipe@recolte.com");
                System.out.println("   🔑 Mot de passe: equipe123");
                System.out.println("   👤 Rôle: TRAVAILLEUR");
            }

            System.out.println("\n🎉 Initialisation des données terminée !");
            System.out.println("========================================");
            System.out.println("📋 Résumé des utilisateurs disponibles:");
            System.out.println("   👑 ADMIN: admin@cooperative.com / admin123");
            System.out.println("   👤 RESPONSABLE:   / responsable123");
            System.out.println("   👨‍🌾 AGRICULTEUR: agriculteur@test.com / agriculteur123");
            System.out.println("   🚜 EQUIPE_RECOLTE: equipe@recolte.com / equipe123");
            System.out.println("========================================\n");
        };
    }
}