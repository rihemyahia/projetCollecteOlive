package com.example.demo.dto;

import com.example.demo.model.Role;
import com.example.demo.model.StatutTournee;
import com.example.demo.model.Tournee;
import com.example.demo.model.Utilisateur;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.demo.repository.UtilisateurRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AssignTourneesRequest {
    private List<String> tourneesIds;
    public List<String> getTourneesIds() { return tourneesIds; }
    public void setTourneesIds(List<String> tourneesIds) { this.tourneesIds = tourneesIds; }
}


