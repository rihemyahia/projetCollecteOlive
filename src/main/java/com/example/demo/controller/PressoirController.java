package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.Tournee;
import com.example.demo.service.ResponsablePressoirService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pressoir")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('RESPONSABLE_PRESSOIR')")
public class PressoirController {
@Autowired
    private final ResponsablePressoirService responsablePressoirService;

    @GetMapping("/tournees-livrees")
    public ResponseEntity<List<Tournee>> getTourneesLivreesEnAttente(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.getTourneesLivreesEnAttente(currentUser));
    }

    @PostMapping("/tournees/{tourneeId}/reception")
    public ResponseEntity<ExtractionHuileResponse> receptionnerTournee(
            @PathVariable String tourneeId,
            @Valid @RequestBody ReceptionOlivesRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.receptionnerTournee(tourneeId, request, currentUser));
    }

    @PatchMapping("/extractions/{extractionId}/extraire")
    public ResponseEntity<ExtractionHuileResponse> extraireHuile(
            @PathVariable String extractionId,
            @Valid @RequestBody ExtractionHuileRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.extraireHuile(extractionId, request, currentUser));
    }

    @PatchMapping("/extractions/{extractionId}/valider")
    public ResponseEntity<ExtractionHuileResponse> validerExtraction(
            @PathVariable String extractionId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.validerExtraction(extractionId, currentUser));
    }

    @GetMapping("/extractions")
    public ResponseEntity<List<ExtractionHuileResponse>> getExtractions(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.getExtractions(currentUser));
    }

    @GetMapping("/collectes-huile")
    public ResponseEntity<List<CollecteHuileResponse>> getCollectesHuile(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.getCollectesHuile(currentUser));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<PressoirDashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.getDashboard(currentUser));
    }

    @GetMapping("/profile")
    public ResponseEntity<PressoirProfileResponse> getProfile(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.getProfile(currentUser));
    }

    @PatchMapping("/profile")
    public ResponseEntity<PressoirProfileResponse> updateProfile(
            @RequestBody PressoirProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(responsablePressoirService.updateProfile(request, currentUser));
    }
}
