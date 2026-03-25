package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tournees")
public class Tournee {
    
    @Id
    private String id;
    
    private String collecteId; // Référence à la collecte
    
    private String equipeId;
    
    private String statut; // en_attente, en_cours, terminee
    
    private Date heureDebut;
    
    private Date heureFin;
    
    private List<PointControle> pointsControle;
    
    private Double quantiteRecoltee; // en kg
    
    private Integer nombreBennesRemplies;
    
    private String notes;
    
    private String misAJourPar;
    
    private Date dateMiseAJour;
}