package com.example.demo;

import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Date;

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
            // Créer un admin
            if (utilisateurRepository.findByEmail("admin@cooperative.com").isEmpty()) {
                Utilisateur admin = new Utilisateur();
                admin.setEmail("admin@cooperative.com");
                admin.setPrenom("Admin");
                admin.setNom("Coopérative");
                admin.setTelephone("+216 98 765 432");
                admin.setRole("admin");
                admin.setAdresse("Sfax, Tunisie");
                admin.setEstActif(true);
                admin.setDateCreation(new Date());

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                admin.setMotDePasse(encoder.encode("admin123"));

                utilisateurRepository.save(admin);
                System.out.println("✅ Utilisateur admin créé avec succès !");
                System.out.println("   Email: admin@cooperative.com");
                System.out.println("   Mot de passe: admin123");
            } else {
                System.out.println("ℹ️ Utilisateur admin existe déjà");
            }

            // Créer un responsable
            if (utilisateurRepository.findByEmail("responsable@cooperative.com").isEmpty()) {
                Utilisateur responsable = new Utilisateur();
                responsable.setEmail("responsable@cooperative.com");
                responsable.setPrenom("Responsable");
                responsable.setNom("Coopérative");
                responsable.setTelephone("+216 98 765 432");
                responsable.setRole("responsable");
                responsable.setAdresse("Sfax, Tunisie");
                responsable.setEstActif(true);
                responsable.setDateCreation(new Date());

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                responsable.setMotDePasse(encoder.encode("responsable123"));

                utilisateurRepository.save(responsable);
                System.out.println("✅ Utilisateur responsable créé avec succès !");
                System.out.println("   Email: responsable@cooperative.com");
                System.out.println("   Mot de passe: responsable123");
            } else {
                System.out.println("ℹ️ Utilisateur responsable existe déjà");
            }
        };
    }
}