package com.example.demo.model;

import com.example.demo.model.enums.QualiteHuile;
import com.example.demo.model.enums.StatutExtractionHuile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "extractions_huile")
public class ExtractionHuile {

    @Id
    private String id;

    @DocumentReference(lazy = true)
    private Tournee tournee;

    @DocumentReference(lazy = true)
    private Collecte collecte;

    @DocumentReference(lazy = true)
    private Utilisateur responsablePressoir;

    private Pressoir pressoirSnapshot;

    private Double quantiteOlivesRecueKg;
    private Double quantiteHuileExtraiteL;
    private Double rendementPourcentage;

    private Date dateReception;
    private Date dateExtraction;

    private StatutExtractionHuile statut;
    private QualiteHuile qualiteHuile;
    private String observationsReception;
    private String observationsExtraction;

    @CreatedDate
    private Date dateCreation;
}
