package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PressoirInfo {
    private String nomPressoir;
    private String adresse;
    private String telephone;
    private Double capaciteJournaliere; // en kg
}