package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ressources")
public class Ressource {
    
    @Id
    private String id;
    
    private String type; // tracteur, benne, vibrateur, filet, equipe
    
    private String code;
    
    private String nom;
    
    private String statut; // disponible, en_utilisation, en_maintenance
    
    private Double capacite; // en kg (pour les bennes)
    
    private String collecteActuelleId; // Référence à la collecte en cours
    
    private Date dateMaintenance;
    
    private Boolean estActif;
    
    private Date dateCreation;
    
    // Pour les équipes (si type = equipe)
    private String chefId;
    
    private java.util.List<String> membresIds;
    
    private String niveauExpertise; // debutant, intermediaire, expert
}