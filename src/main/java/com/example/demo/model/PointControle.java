package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointControle {
    private Date horodatage;
    private String statut; // debutee, pause, reprise, terminee
    private Localisation localisation;
    private String commentaire;
}