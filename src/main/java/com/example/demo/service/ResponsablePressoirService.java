package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.Tournee;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface ResponsablePressoirService {
    List<Tournee> getTourneesLivreesEnAttente(UserDetails currentUser);
    ExtractionHuileResponse receptionnerTournee(String tourneeId, ReceptionOlivesRequest request, UserDetails currentUser);
    ExtractionHuileResponse extraireHuile(String extractionId, ExtractionHuileRequest request, UserDetails currentUser);
    ExtractionHuileResponse validerExtraction(String extractionId, UserDetails currentUser);
    List<ExtractionHuileResponse> getExtractions(UserDetails currentUser);
    List<CollecteHuileResponse> getCollectesHuile(UserDetails currentUser);
    PressoirDashboardResponse getDashboard(UserDetails currentUser);
    PressoirProfileResponse getProfile(UserDetails currentUser);
    PressoirProfileResponse updateProfile(PressoirProfileUpdateRequest request, UserDetails currentUser);
}
