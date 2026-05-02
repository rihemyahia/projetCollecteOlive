package com.example.demo.service;

import com.example.demo.dto.TourneeResponse;
import com.example.demo.model.Pressoir;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import com.example.demo.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Complète les champs pressoir sur les réponses « liste assignation transporteur »
 * sans modifier {@code TourneeServiceImpl#toResponseForTransporteurAssignList} :
 * charge les {@link Utilisateur} responsables pressoir par id et remplit nom / adresse.
 */
@Service
public class TourneeAssignListPressoirEnricher {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public void enrichPressoirDisplayFields(List<TourneeResponse> responses) {
        enrichPressoirDisplayFields(responses, null);
    }

    /**
     * @param tourneesParallelOrder même taille et ordre que {@code responses} (ex. page Mongo) :
     *                    si {@code responsablePressoirId} est vide mais la référence {@link Tournee#getResponsablePressoir()}
     *                    est présente en base, on récupère l’id (souvent absent du champ scalaire).
     */
    public void enrichPressoirDisplayFields(List<TourneeResponse> responses, List<Tournee> tourneesParallelOrder) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        if (tourneesParallelOrder != null && tourneesParallelOrder.size() == responses.size()) {
            for (int i = 0; i < responses.size(); i++) {
                TourneeResponse r = responses.get(i);
                if (r.getResponsablePressoirId() != null && !r.getResponsablePressoirId().isBlank()) {
                    continue;
                }
                Tournee t = tourneesParallelOrder.get(i);
                if (t == null || t.getResponsablePressoir() == null) {
                    continue;
                }
                String rid = t.getResponsablePressoir().getId();
                if (rid != null && !rid.isBlank()) {
                    r.setResponsablePressoirId(rid);
                }
            }
        }

        Set<String> rpIds = responses.stream()
                .map(TourneeResponse::getResponsablePressoirId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        if (rpIds.isEmpty()) {
            return;
        }

        Map<String, Utilisateur> byId = new HashMap<>();
        utilisateurRepository.findAllById(rpIds).forEach(u -> byId.put(u.getId(), u));

        for (TourneeResponse r : responses) {
            String pid = r.getResponsablePressoirId();
            if (pid == null || pid.isBlank()) {
                continue;
            }
            Utilisateur rp = byId.get(pid);
            if (rp == null) {
                continue;
            }
            String nomComplet = ((rp.getPrenom() != null ? rp.getPrenom() : "") + " "
                    + (rp.getNom() != null ? rp.getNom() : "")).trim();
            if (!nomComplet.isEmpty()) {
                r.setResponsablePressoirNom(nomComplet);
            }
            Pressoir p = rp.getPressoir();
            if (p != null) {
                if (p.getNom() != null && !p.getNom().isBlank()) {
                    r.setPressoirNom(p.getNom());
                }
                if (p.getAdresse() != null && !p.getAdresse().isBlank()) {
                    r.setPressoirAdresse(p.getAdresse());
                }
            }
        }
    }
}
