package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void envoyerMotDePasse(String destinataire, String nom, String prenom, String motDePasse) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinataire);
            message.setSubject("Activation de votre compte - Coopérative Olives");
            message.setText(String.format(
                "Bonjour %s %s,\n\n" +
                "Votre compte a été activé avec succès.\n\n" +
                "Voici vos identifiants de connexion :\n" +
                "📧 Email : %s\n" +
                "🔑 Mot de passe : %s\n\n" +
                "Veuillez vous connecter sur : http://localhost:4200/login\n\n" +
                "Cordialement,\n" +
                "L'équipe Coopérative Olives",
                prenom, nom, destinataire, motDePasse
            ));
            
            mailSender.send(message);
            System.out.println("✅ Email envoyé avec succès à : " + destinataire);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
}