package com.example.demo.controller;

import com.example.demo.model.Verger;
import com.example.demo.service.VergerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vergers")
@CrossOrigin(origins = "http://localhost:4200")
public class VergerController {

    @Autowired
    private VergerService vergerService;

    @PostMapping
    public ResponseEntity<Verger> creerVerger(@RequestBody Verger verger) {
        Verger nouveauVerger = vergerService.creerVerger(verger);
        return ResponseEntity.ok(nouveauVerger);
    }

    @GetMapping
    public ResponseEntity<List<Verger>> listerVergers() {
        List<Verger> vergers = vergerService.listerVergers();
        return ResponseEntity.ok(vergers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Verger> trouverVergerParId(@PathVariable String id) {
        Verger verger = vergerService.trouverVergerParId(id).orElseThrow();
        return ResponseEntity.ok(verger);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Verger> mettreAJourVerger(@PathVariable String id, @RequestBody Verger verger) {
        Verger vergerMisAJour = vergerService.mettreAJourVerger(id, verger);
        return ResponseEntity.ok(vergerMisAJour);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerVerger(@PathVariable String id) {
        vergerService.supprimerVerger(id);
        return ResponseEntity.noContent().build();
    }
}
