package com.example.demo.service;

/**
 * Service météo pour récupérer les données climatiques
 */
public interface MeteoService {
    
    /**
     * Récupère les précipitations pour des coordonnées GPS
     * @param latitude Latitude du verger
     * @param longitude Longitude du verger
     * @return Précipitations en mm
     */
    double getPrecipitationForCoordinates(double latitude, double longitude);
    
    /**
     * Récupère les précipitations historiques pour une année spécifique
     * @param latitude Latitude du verger
     * @param longitude Longitude du verger
     * @param annee Année de la collecte (ex: "2024")
     * @return Précipitations moyennes de l'année en mm
     */
    double getPrecipitationHistorique(double latitude, double longitude, String annee);
    
    /**
     * Récupère la température moyenne pour des coordonnées
     * @param latitude Latitude du verger
     * @param longitude Longitude du verger
     * @return Température en °C
     */
    double getTemperature(double latitude, double longitude);

	double getTemperatureMoyenneAnnuelle(double latitude, double longitude, String annee);
}