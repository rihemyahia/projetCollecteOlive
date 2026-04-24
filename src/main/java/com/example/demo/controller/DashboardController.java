package com.example.demo.controller;

import com.example.demo.dto.dashboard.AdminDashboardDTO;
import com.example.demo.dto.dashboard.AgriculteurDashboardDTO;
import com.example.demo.dto.dashboard.ResponsableDashboardDTO;
import com.example.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/admin
     * Returns global statistics for the ADMIN role.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    /**
     * GET /api/dashboard/responsable
     * Returns statistics scoped to the vergers managed by the logged-in RESPONSABLE.
     */
    @GetMapping("/responsable")
    @PreAuthorize("hasRole('RESPONSABLE')")
    public ResponseEntity<ResponsableDashboardDTO> getResponsableDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getResponsableDashboard(userDetails));
    }

    /**
     * GET /api/dashboard/agriculteur
     * Returns statistics for the logged-in AGRICULTEUR's own vergers.
     */
    @GetMapping("/agriculteur")
    @PreAuthorize("hasRole('AGRICULTEUR')")
    public ResponseEntity<AgriculteurDashboardDTO> getAgriculteurDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getAgriculteurDashboard(userDetails));
    }
}