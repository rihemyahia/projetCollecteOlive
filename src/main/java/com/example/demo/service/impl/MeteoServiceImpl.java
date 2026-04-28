package com.example.demo.service.impl;

import com.example.demo.service.MeteoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Year;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MeteoServiceImpl implements MeteoService {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public double getPrecipitationForCoordinates(double latitude, double longitude) {
        String url = String.format("%s?lat=%f&lon=%f&appid=%s&units=metric", 
            apiUrl, latitude, longitude, apiKey);
        
        System.out.println("🔗 Appel OpenWeatherMap: " + url);
        
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (root.has("cod") && root.get("cod").asInt() != 200) {
                throw new RuntimeException("API Error: " + root.path("message").asText());
            }
            
            JsonNode rainNode = root.path("rain").path("1h");
            JsonNode snowNode = root.path("snow").path("1h");

            double total = 0.0;
            if (!rainNode.isMissingNode()) total += rainNode.asDouble();
            if (!snowNode.isMissingNode()) total += snowNode.asDouble();
            
            System.out.println("🌧️ Précipitations actuelles: " + total + " mm");
            return total;
            
        } catch (Exception e) {
            throw new RuntimeException("❌ Erreur météo OpenWeatherMap: " + e.getMessage());
        }
    }

    @Override
    public double getPrecipitationHistorique(double latitude, double longitude, String annee) {
        // 🔥 CORRECTION : Utiliser l'année précédente si l'année demandée n'est pas terminée
        String anneeUtilisee = getAnneeComplete(annee);
        
        String latFormatted = String.format(Locale.US, "%.6f", latitude);
        String lonFormatted = String.format(Locale.US, "%.6f", longitude);
        
        String dateDebut = anneeUtilisee + "-01-01";
        String dateFin = anneeUtilisee + "-12-31";
        
        String url = String.format(
            "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=precipitation_sum",
            latFormatted, lonFormatted, dateDebut, dateFin
        );
        
        System.out.println("🌍 Année utilisée: " + anneeUtilisee);
        System.out.println("🔗 Open-Meteo URL: " + url);
        
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (root.has("reason")) {
                throw new RuntimeException("API Error: " + root.path("reason").asText());
            }
            
            JsonNode precipitations = root.path("daily").path("precipitation_sum");
            
            if (precipitations.isMissingNode() || precipitations.size() == 0) {
                throw new RuntimeException("Aucune donnée de précipitations pour " + anneeUtilisee);
            }
            
            double totalAnnuel = 0;
            for (JsonNode jour : precipitations) {
                totalAnnuel += jour.asDouble();
            }
            
            System.out.println("🌧️ Précipitations annuelles " + anneeUtilisee + ": " + totalAnnuel + " mm");
            return totalAnnuel;
            
        } catch (Exception e) {
            throw new RuntimeException("❌ Erreur Open-Meteo précipitations: " + e.getMessage());
        }
    }
    
    @Override
    public double getTemperatureMoyenneAnnuelle(double latitude, double longitude, String annee) {
        // 🔥 CORRECTION : Utiliser l'année précédente si l'année demandée n'est pas terminée
        String anneeUtilisee = getAnneeComplete(annee);
        
        String latFormatted = String.format(Locale.US, "%.6f", latitude);
        String lonFormatted = String.format(Locale.US, "%.6f", longitude);
        
        String dateDebut = anneeUtilisee + "-01-01";
        String dateFin = anneeUtilisee + "-12-31";
        
        String url = String.format(
            "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=temperature_2m_mean",
            latFormatted, lonFormatted, dateDebut, dateFin
        );
        
        System.out.println("🌍 Année utilisée: " + anneeUtilisee);
        System.out.println("🔗 Open-Meteo URL: " + url);
        
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (root.has("reason")) {
                throw new RuntimeException("API Error: " + root.path("reason").asText());
            }
            
            JsonNode temperatures = root.path("daily").path("temperature_2m_mean");
            
            if (temperatures.isMissingNode() || temperatures.size() == 0) {
                throw new RuntimeException("Aucune donnée de température pour " + anneeUtilisee);
            }
            
            double totalTemp = 0;
            int jours = 0;
            for (JsonNode temp : temperatures) {
                totalTemp += temp.asDouble();
                jours++;
            }
            
            double moyenneAnnuelle = totalTemp / jours;
            System.out.println("🌡️ Température moyenne annuelle " + anneeUtilisee + ": " + String.format(Locale.US, "%.2f", moyenneAnnuelle) + "°C");
            return moyenneAnnuelle;
            
        } catch (Exception e) {
            throw new RuntimeException("❌ Erreur Open-Meteo température: " + e.getMessage());
        }
    }
    
    /**
     * 🔥 Méthode utilitaire : Retourne une année complète (terminée)
     * Si l'année demandée est l'année en cours, retourne l'année précédente
     */
    private String getAnneeComplete(String anneeDemandee) {
        int anneeInt = Integer.parseInt(anneeDemandee);
        int anneeActuelle = Year.now().getValue();
        
        if (anneeInt == anneeActuelle) {
            String anneePrecedente = String.valueOf(anneeActuelle - 1);
            System.out.println("⚠️ Année " + anneeDemandee + " non terminée, utilisation de " + anneePrecedente);
            return anneePrecedente;
        }
        
        return anneeDemandee;
    }

    @Override
    public double getTemperature(double latitude, double longitude) {
        String url = String.format("%s?lat=%f&lon=%f&appid=%s&units=metric", 
            apiUrl, latitude, longitude, apiKey);
        
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (root.has("cod") && root.get("cod").asInt() != 200) {
                throw new RuntimeException("API Error: " + root.path("message").asText());
            }
            
            JsonNode tempNode = root.path("main").path("temp");
            if (tempNode.isMissingNode()) {
                throw new RuntimeException("Température non trouvée dans la réponse");
            }
            
            double temperature = tempNode.asDouble();
            System.out.println("🌡️ Température actuelle: " + temperature + "°C");
            return temperature;
            
        } catch (Exception e) {
            throw new RuntimeException("❌ Erreur température: " + e.getMessage());
        }
    }
}