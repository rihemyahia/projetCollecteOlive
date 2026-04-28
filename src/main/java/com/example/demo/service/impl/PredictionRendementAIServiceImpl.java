package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.model.enums.StatutCollecte;
import com.example.demo.repository.*;
import com.example.demo.service.PredictionRendementAIService;
import com.example.demo.service.MeteoService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PredictionRendementAIServiceImpl implements PredictionRendementAIService {

    @Autowired
    private VergerRepository vergerRepository;
    
    @Autowired
    private CollecteRepository collecteRepository;
    
    @Autowired
    private MeteoService meteoService;
    
    // Stocke les coefficients du modèle pour chaque type d'olive
    private Map<String, double[]> modeles = new HashMap<>();
    
    @PostConstruct
    public void entrainerIA() {
        log.info("========== 🧠 DÉMARRAGE DE L'ENTRAÎNEMENT IA ==========");
        
        // 1. Récupérer toutes les collectes
        List<Collecte> toutesCollectes = collecteRepository.findAll();
        log.info("📊 [1] Total des collectes dans la base: {}", toutesCollectes.size());
        
        // 2. Filtrer les TERMINEE
        List<Collecte> toutesCollectesTerminees = toutesCollectes.stream()
                .filter(c -> c.getStatut() == StatutCollecte.TERMINEE)
                .collect(Collectors.toList());
        
        log.info("📊 [2] Collectes TERMINÉES trouvées: {}", toutesCollectesTerminees.size());
        
        // 3. Afficher chaque collecte en détail
        log.info("========== DÉTAIL DES COLLECTES ==========");
        for (Collecte c : toutesCollectesTerminees) {
            log.info("   📍 Code: {} | Statut: {} | VergerId: {} | Quantité: {} kg | Année: {}", 
                c.getCode(), c.getStatut(), c.getVergerId(), c.getQuantiteTotaleKg(), c.getAnnee());
        }
        
        if (toutesCollectesTerminees.isEmpty()) {
            log.warn("⚠️ [ERREUR] Aucune collecte TERMINEE trouvée ! L'IA ne peut pas s'entraîner.");
            log.warn("   Solution: Ajoutez des collectes avec statut='TERMINEE' dans MongoDB");
            return;
        }
        
        // 4. Récupérer tous les vergers
        List<Verger> tousVergers = vergerRepository.findAll();
        log.info("📊 [3] Vergers trouvés: {}", tousVergers.size());
        
        // 5. Afficher les vergers
        log.info("========== DÉTAIL DES VERGERS ==========");
        for (Verger v : tousVergers) {
            log.info("   🌳 ID: {} | Type: {} | Superficie: {} | NbArbres: {} | Maturité: {}", 
                v.getId(), v.getTypeOlive(), v.getSuperficie(), v.getNbArbre(), v.getMaturiteActuelle());
        }
        
        // 6. Grouper par type d'olive
        Map<String, List<Verger>> vergersParType = tousVergers.stream()
                .collect(Collectors.groupingBy(Verger::getTypeOlive));
        
        log.info("========== ENTRAÎNEMENT PAR TYPE D'OLIVE ==========");
        
        for (Map.Entry<String, List<Verger>> entry : vergersParType.entrySet()) {
            String typeOlive = entry.getKey();
            List<Verger> vergers = entry.getValue();
            
            log.info("\n--- Traitement du type: {} ---", typeOlive);
            log.info("   Vergers de ce type: {}", vergers.size());
            
            // Afficher les IDs des vergers de ce type
            List<String> vergerIds = vergers.stream()
                    .map(Verger::getId)
                    .collect(Collectors.toList());
            log.info("   IDs des vergers: {}", vergerIds);
            
            // Filtrer les collectes de ce type
            List<Collecte> collectesType = toutesCollectesTerminees.stream()
                    .filter(c -> {
                        boolean contains = vergerIds.contains(c.getVergerId());
                        log.debug("      Collecte {} (vergerId: {}) -> match: {}", 
                            c.getCode(), c.getVergerId(), contains);
                        return contains;
                    })
                    .collect(Collectors.toList());
            
            log.info("   Collectes trouvées pour {}: {}", typeOlive, collectesType.size());
            
            if (!collectesType.isEmpty()) {
                // Afficher les détails des collectes
                for (Collecte c : collectesType) {
                    log.info("      - {}: {} kg (année {})", c.getCode(), c.getQuantiteTotaleKg(), c.getAnnee());
                }
                
                double[] coeffs = entrainerModele(vergers, collectesType);
                modeles.put(typeOlive, coeffs);
                log.info("   ✅ Modèle entraîné pour {} ({} collectes)", typeOlive, collectesType.size());
                log.info("   📐 Coefficients: a={:.2f}, b={:.2f}, c={:.2f}", coeffs[0], coeffs[1], coeffs[2]);
            } else {
                log.warn("   ⚠️ Aucune collecte pour {} - modèle non entraîné", typeOlive);
            }
        }
        
        log.info("========== FIN DE L'ENTRAÎNEMENT ==========");
        log.info("📊 Modèles disponibles: {}", modeles.keySet());
    }
    
    private double[] entrainerModele(List<Verger> vergers, List<Collecte> collectes) {
        log.info("   >>> Début de l'entraînement pour {} collectes", collectes.size());
        
        // Normalisation : ramener les valeurs entre 0 et 1
        double maxSuperficie = 0;
        double maxNbArbres = 0;
        double maxPrecipitations = 0;
        double maxTemperature = 0;
        double maxQuantite = 0;
        
        for (Collecte c : collectes) {
            Verger v = vergers.stream()
                    .filter(verg -> verg.getId().equals(c.getVergerId()))
                    .findFirst().orElse(null);
            if (v != null && c.getQuantiteTotaleKg() != null) {
                maxSuperficie = Math.max(maxSuperficie, v.getSuperficie());
                maxNbArbres = Math.max(maxNbArbres, v.getNbArbre());
                maxQuantite = Math.max(maxQuantite, c.getQuantiteTotaleKg());
                
                // Récupérer les données météo de la collecte
                if (c.getPrecipitations() != null) {
                    maxPrecipitations = Math.max(maxPrecipitations, c.getPrecipitations());
                }
                if (c.getTemperature() != null) {
                    maxTemperature = Math.max(maxTemperature, c.getTemperature());
                }
            }
        }
        
        log.info("   Normalisation: maxSuperficie={}, maxNbArbres={}, maxPrecipitations={}, maxTemperature={}, maxQuantite={}", 
            maxSuperficie, maxNbArbres, maxPrecipitations, maxTemperature, maxQuantite);
        
        // Éviter division par zéro
        if (maxSuperficie == 0) maxSuperficie = 1;
        if (maxNbArbres == 0) maxNbArbres = 1;
        if (maxPrecipitations == 0) maxPrecipitations = 1;
        if (maxTemperature == 0) maxTemperature = 1;
        if (maxQuantite == 0) maxQuantite = 1;
        
        // Coefficients: [superficie, nbArbres, precipitations, temperature, intercept]
        double[] coeffs = {0.0, 0.0, 0.0, 0.0, 0.0};
        double learningRate = 0.1;
        
        for (int iteration = 0; iteration < 2000; iteration++) {
            double grad0 = 0, grad1 = 0, grad2 = 0, grad3 = 0, grad4 = 0;
            int validCount = 0;
            
            for (Collecte c : collectes) {
                Verger v = vergers.stream()
                        .filter(verg -> verg.getId().equals(c.getVergerId()))
                        .findFirst().orElse(null);
                
                if (v != null && c.getQuantiteTotaleKg() != null) {
                    validCount++;
                    
                    // Normaliser les données
                    double superficieNorm = v.getSuperficie() / maxSuperficie;
                    double nbArbresNorm = v.getNbArbre() / maxNbArbres;
                    
                    // Valeurs par défaut si météo manquante
                    double precipitations = c.getPrecipitations() != null ? c.getPrecipitations() : 500;
                    double temperature = c.getTemperature() != null ? c.getTemperature() : 20;
                    
                    double precipitationsNorm = precipitations / maxPrecipitations;
                    double temperatureNorm = temperature / maxTemperature;
                    double quantiteNorm = c.getQuantiteTotaleKg() / maxQuantite;
                    
                    double prediction = coeffs[0] * superficieNorm 
                                      + coeffs[1] * nbArbresNorm
                                      + coeffs[2] * precipitationsNorm
                                      + coeffs[3] * temperatureNorm
                                      + coeffs[4];
                    double erreur = prediction - quantiteNorm;
                    
                    grad0 += erreur * superficieNorm;
                    grad1 += erreur * nbArbresNorm;
                    grad2 += erreur * precipitationsNorm;
                    grad3 += erreur * temperatureNorm;
                    grad4 += erreur;
                }
            }
            
            if (validCount > 0) {
                coeffs[0] -= learningRate * (grad0 / validCount);
                coeffs[1] -= learningRate * (grad1 / validCount);
                coeffs[2] -= learningRate * (grad2 / validCount);
                coeffs[3] -= learningRate * (grad3 / validCount);
                coeffs[4] -= learningRate * (grad4 / validCount);
            }
            
            if (iteration % 500 == 0) {
                log.info("   Iteration {}: coeffs = [{:.4f}, {:.4f}, {:.4f}, {:.4f}, {:.4f}]", 
                    iteration, coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4]);
            }
        }
        
        // Stocker les coefficients ET les max pour dénormalisation
        double[] result = new double[10];
        System.arraycopy(coeffs, 0, result, 0, 5);
        result[5] = maxSuperficie;
        result[6] = maxNbArbres;
        result[7] = maxPrecipitations;
        result[8] = maxTemperature;
        result[9] = maxQuantite;
        
        log.info("   >>> Coefficients finaux: [{:.4f}, {:.4f}, {:.4f}, {:.4f}, {:.4f}]", 
            coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4]);
        
        return result;
    }
    
    public Map<String, Object> predireRendement(String vergerId) {
        log.info("========== PRÉDICTION POUR VERGER: {} ==========", vergerId);
        
        Verger verger = vergerRepository.findById(vergerId)
                .orElseThrow(() -> new RuntimeException("Verger non trouvé"));
        
        log.info("🌳 Verger trouvé: Type={}, Superficie={}, NbArbres={}, Maturité={}", 
            verger.getTypeOlive(), verger.getSuperficie(), verger.getNbArbre(), verger.getMaturiteActuelle());
        
        Map<String, Object> resultat = new HashMap<>();
        resultat.put("vergerId", verger.getId());
        resultat.put("nom", verger.getAgriculteur() != null ? verger.getAgriculteur().getNom() : null);
        resultat.put("typeOlive", verger.getTypeOlive());
        resultat.put("superficie", verger.getSuperficie());
        resultat.put("nbArbres", verger.getNbArbre());
        resultat.put("maturiteActuelle", verger.getMaturiteActuelle());
        resultat.put("statut", verger.getStatut());
        
        double[] modele = modeles.get(verger.getTypeOlive());
        
        if (modele != null && modele.length >= 10) {
            double[] coeffs = Arrays.copyOfRange(modele, 0, 5);
            double maxSuperficie = modele[5];
            double maxNbArbres = modele[6];
            double maxPrecipitations = modele[7];
            double maxTemperature = modele[8];
            double maxQuantite = modele[9];
            
            log.info("📐 Modèle trouvé pour {}: coeffs = [{:.4f}, {:.4f}, {:.4f}, {:.4f}, {:.4f}]", 
                verger.getTypeOlive(), coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4]);
            
            // Normaliser les données du verger
            double superficieNorm = verger.getSuperficie() / maxSuperficie;
            double nbArbresNorm = verger.getNbArbre() / maxNbArbres;
            
            // 🔥 RÉCUPÉRATION DES DONNÉES CLIMATIQUES RÉELLES (PAS DE VALEURS PAR DÉFAUT)
            double precipitations;
            double temperature;
            
            if (verger.getGeolocalisation() != null && verger.getGeolocalisation().getLatitude() != null) {
                double lat = verger.getGeolocalisation().getLatitude();
                double lon = verger.getGeolocalisation().getLongitude();
                
                // Récupération de l'année actuelle
                String anneeActuelle = String.valueOf(java.time.Year.now().getValue());
                
                // Appel aux APIs réelles - Si ça plante, l'exception remonte
                precipitations = meteoService.getPrecipitationHistorique(lat, lon, anneeActuelle);
                temperature = meteoService.getTemperatureMoyenneAnnuelle(lat, lon, anneeActuelle);
                
                log.info("🌤️ Climat RÉEL pour {}: {} mm/an, {}°C moyenne", 
                    verger.getTypeOlive(), precipitations, temperature);
            } else {
                throw new RuntimeException("Le verger " + verger.getId() + " n'a pas de géolocalisation");
            }
            
            double precipitationsNorm = precipitations / maxPrecipitations;
            double temperatureNorm = temperature / maxTemperature;
            
            // Prédiction normalisée
            double predictionNorm = coeffs[0] * superficieNorm
                                  + coeffs[1] * nbArbresNorm
                                  + coeffs[2] * precipitationsNorm
                                  + coeffs[3] * temperatureNorm
                                  + coeffs[4];
            
            // Dénormalisation
            double prediction = predictionNorm * maxQuantite;
            
            log.info("🔢 Calcul: {} * {:.2f} + {} * {} + {} * {:.2f} + {} * {:.2f} + {} = {:.0f} kg", 
                coeffs[0], superficieNorm, coeffs[1], nbArbresNorm, 
                coeffs[2], precipitationsNorm, coeffs[3], temperatureNorm, coeffs[4], prediction);
            
            // Limitation à des valeurs réalistes
            double kgParArbreMax = 100;
            double kgParArbreMin = 3;
            double predictionMax = verger.getNbArbre() * kgParArbreMax;
            double predictionMin = verger.getNbArbre() * kgParArbreMin;
            
            if (prediction > predictionMax) {
                log.warn("⚠️ Prédiction trop élevée ({:.0f} kg), limitée à {:.0f} kg", prediction, predictionMax);
                prediction = predictionMax;
            }
            
            if (prediction < predictionMin) {
                log.warn("⚠️ Prédiction trop faible ({:.0f} kg), augmentée à {:.0f} kg", prediction, predictionMin);
                prediction = predictionMin;
            }
            
            log.info("📊 Prédiction finale: {:.0f} kg ({:.1f} kg/arbre)", prediction, prediction / verger.getNbArbre());
            
            resultat.put("rendementPredictedKg", Math.round(prediction));
            resultat.put("precipitationsUtilisees", precipitations);
            resultat.put("temperatureUtilisee", temperature);
            resultat.put("niveauConfiance", "MOYEN");
            
            // Recommandation basée sur la météo
            if (precipitations < 400) {
                resultat.put("recommandationMeteo", "⚠️ Sécheresse: rendement pourrait être inférieur");
            } else if (precipitations > 700) {
                resultat.put("recommandationMeteo", "⚠️ Excès d'eau: risque pour les olives");
            } else {
                resultat.put("recommandationMeteo", "✅ Conditions météo favorables");
            }
            
            // Recommandation basée sur maturité
            if (verger.getMaturiteActuelle() >= 80) {
                resultat.put("recommandation", "⚠️ URGENT : Maturité élevée, récoltez rapidement!");
                resultat.put("urgence", "ELEVEE");
            } else if (verger.getMaturiteActuelle() >= 60) {
                resultat.put("recommandation", "📅 Planifier la récolte dans les 2 semaines");
                resultat.put("urgence", "MOYENNE");
            } else {
                resultat.put("recommandation", "🌱 En attente de maturité");
                resultat.put("urgence", "FAIBLE");
            }
            
            int nbBennes = (int) Math.ceil(prediction / 1000);
            resultat.put("nbBennesNecessaires", nbBennes);
            log.info("✅ Prédiction finale: {} kg, soit {} bennes", Math.round(prediction), nbBennes);
            
        } else {
            log.warn("⚠️ Aucun modèle trouvé pour le type: {}", verger.getTypeOlive());
            log.warn("   Modèles disponibles: {}", modeles.keySet());
            
            double estimation = verger.getNbArbre() * 5.0;
            resultat.put("rendementPredictedKg", Math.round(estimation));
            resultat.put("niveauConfiance", "FAIBLE (modèle non entraîné)");
            resultat.put("recommandation", "Ajoutez des collectes terminées pour améliorer les prédictions");
            resultat.put("nbBennesNecessaires", (int) Math.ceil(estimation / 1000));
            log.info("📊 Fallback utilisé: {} kg ({} arbres x 5 kg)", estimation, verger.getNbArbre());
        }
        
        log.info("========== FIN PRÉDICTION ==========\n");
        return resultat;
    }
    public List<Map<String, Object>> predireTousLesVergers() {
        log.info("📊 Prédiction pour tous les vergers...");
        List<Verger> vergers = vergerRepository.findByEstSupprimerFalse();
        log.info("   Vergers actifs: {}", vergers.size());
        
        List<Map<String, Object>> resultats = new ArrayList<>();
        
        for (Verger v : vergers) {
            resultats.add(predireRendement(v.getId()));
        }
        
        resultats.sort((a, b) -> {
            Integer matA = (Integer) a.get("maturiteActuelle");
            Integer matB = (Integer) b.get("maturiteActuelle");
            return matB.compareTo(matA);
        });
        
        return resultats;
    }
    
    public void reentrainer() {
        log.info("🔄 Ré-entraînement manuel demandé...");
        entrainerIA();
        log.info("✅ Ré-entraînement terminé");
    }
}