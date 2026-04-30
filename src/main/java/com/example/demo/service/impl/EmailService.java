package com.example.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements com.example.demo.service.EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void envoyerMotDePasse(String destinataire, String nom, String prenom, String motDePasse) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(destinataire);
            helper.setSubject("Activation de votre compte - Cooperative Olives");
            
            String htmlContent = buildEmailHtml(nom, prenom, destinataire, motDePasse);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            System.out.println("Email envoye avec succes a : " + destinataire);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }
    
    private String buildEmailHtml(String nom, String prenom, String email, String motDePasse) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<title>Activation de compte</title>");
        sb.append("</head>");
        sb.append("<body style='font-family: Arial, sans-serif; margin:0; padding:20px; background-color:#f5f5f5;'>");
        sb.append("<div style='max-width:600px; margin:0 auto; background-color:#fff; border-radius:12px; overflow:hidden;'>");
        
        // Header
        sb.append("<div style='background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%); color:white; padding:30px 20px; text-align:center;'>");
        sb.append("<h1 style='margin:0;'>🌿 Cooperative Olives</h1>");
        sb.append("<p style='margin:10px 0 0;'>Gestion intelligente des collectes</p>");
        sb.append("</div>");
        
        // Content
        sb.append("<div style='padding:30px 25px;'>");
        sb.append("<p style='font-size:18px;'>Bonjour <strong>").append(prenom).append(" ").append(nom).append("</strong>,</p>");
        sb.append("<p>Votre compte a ete cree avec succes sur la plateforme de la <strong>Cooperative Olives</strong>.</p>");
        sb.append("<p>Voici vos identifiants de connexion :</p>");
        
        // Credentials
        sb.append("<div style='background:#f8f9fa; border-left:4px solid #2e7d32; padding:20px; margin:20px 0; border-radius:8px;'>");
        sb.append("<p><strong>📧 Email :</strong> ").append(email).append("</p>");
        sb.append("<p><strong>🔑 Mot de passe :</strong> <code style='background:#e9ecef; padding:4px 8px; border-radius:4px;'>").append(motDePasse).append("</code></p>");
        sb.append("</div>");
        
        // Button
        sb.append("<div style='text-align:center; margin:20px 0;'>");
        sb.append("<a href='http://localhost:4200/login' style='background:#2e7d32; color:white; padding:12px 30px; text-decoration:none; border-radius:30px; display:inline-block;'>🔐 Se connecter</a>");
        sb.append("</div>");
        
        // Warning
        sb.append("<div style='background:#fff3e0; border-left:4px solid #ff9800; padding:15px; margin:20px 0; border-radius:8px;'>");
        sb.append("<strong>⚠️ Important :</strong> Pour des raisons de securite, changez votre mot de passe apres votre premiere connexion.");
        sb.append("</div>");
        
        sb.append("</div>");
        
        // Footer
        sb.append("<div style='background:#f8f9fa; padding:20px; text-align:center; font-size:12px; color:#999;'>");
        sb.append("<p>© 2024 Cooperative Olives - Tous droits reserves</p>");
        sb.append("<p>Cet email est genere automatiquement, merci de ne pas y repondre.</p>");
        sb.append("</div>");
        
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");
        
        return sb.toString();
    }
}