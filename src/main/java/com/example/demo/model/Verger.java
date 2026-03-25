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
@Document(collection = "vergers")
public class Verger {

    @Id
    private String id;

    private String nom;

    private Double superficie;

    private String typeOlive;

    private Integer niveauMaturite;

    private Localisation localisation;

    private String agriculteurId; // Référence à l'agriculteur propriétaire

    private Date dateCreation;
}