package com.example.demo.service;

import java.util.List;
import java.util.Map;

public interface PredictionRendementAIService {
    
    /**
     * Prédire le rendement d'un verger spécifique
     * @param vergerId L'ID du verger
     * @return Map contenant la prédiction et les recommandations
     */
    Map<String, Object> predireRendement(String vergerId);
    
    /**
     * Prédire le rendement de tous les vergers
     * @return Liste des prédictions pour tous les vergers
     */
    List<Map<String, Object>> predireTousLesVergers();
    
    /**
     * Forcer le ré-entraînement du modèle IA
     */
    void reentrainer();
}