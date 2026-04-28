package com.example.demo.controller;

import com.example.demo.service.PredictionRendementAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class PredictionAIController {

    private final PredictionRendementAIService predictionService;
    
    @GetMapping("/prediction/verger/{vergerId}")
    public ResponseEntity<Map<String, Object>> predireVerger(@PathVariable String vergerId) {
        return ResponseEntity.ok(predictionService.predireRendement(vergerId));
    }
    
    @GetMapping("/prediction/tous")
    public ResponseEntity<List<Map<String, Object>>> predireTous() {
        return ResponseEntity.ok(predictionService.predireTousLesVergers());
    }
    
    @PostMapping("/reentrainer")
    public ResponseEntity<String> reentrainer() {
        predictionService.reentrainer();
        return ResponseEntity.ok("✅ Modèle IA ré-entraîné avec succès");
    }
}