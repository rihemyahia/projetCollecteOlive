package com.example.demo.model;

import com.example.demo.model.enums.StatutVerger;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vergers")
public class Verger {

    @Id
    private String id;
    @DocumentReference(lazy = true)
    private Utilisateur agriculteur; //reference agriculteur

    private Double superficie;             // hectares
    private String typeOlive;             // Chemlali, Chétoui, Picholine…
    private Double rendementEstime;        // kg
    private Integer maturiteActuelle;     // 0-100 %
    private int nbArbre;

    private StatutVerger statut;

    private Date dateDerniereRecolte;

    @Builder.Default
    private Boolean estSupprimer= false;

    @CreatedDate
    private Date dateCreation;
}