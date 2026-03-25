package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Localisation {
    
    private String type = "Point";
    
    @GeoSpatialIndexed
    private double[] coordonnees; // [longitude, latitude]
    
    private String adresse;
}