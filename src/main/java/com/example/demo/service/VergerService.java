package com.example.demo.service;

import com.example.demo.model.Verger;
import com.example.demo.repository.VergerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VergerService {

    @Autowired
    private VergerRepository vergerRepository;

    public Verger creerVerger(Verger verger) {
        verger.setDateCreation(new Date());
        return vergerRepository.save(verger);
    }

    public Optional<Verger> trouverVergerParId(String id) {
        return vergerRepository.findById(id);
    }

    public List<Verger> listerVergers() {
        return vergerRepository.findAll();
    }

    public Verger mettreAJourVerger(String id, Verger verger) {
        Verger vergerExistant = vergerRepository.findById(id).orElseThrow();
        vergerExistant.setNom(verger.getNom());
        vergerExistant.setSuperficie(verger.getSuperficie());
        vergerExistant.setTypeOlive(verger.getTypeOlive());
        vergerExistant.setNiveauMaturite(verger.getNiveauMaturite());
        vergerExistant.setLocalisation(verger.getLocalisation());
        return vergerRepository.save(vergerExistant);
    }

    public void supprimerVerger(String id) {
        vergerRepository.deleteById(id);
    }
}
