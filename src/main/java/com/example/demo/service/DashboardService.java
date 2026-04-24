package com.example.demo.service;

import com.example.demo.dto.dashboard.AdminDashboardDTO;
import com.example.demo.dto.dashboard.AgriculteurDashboardDTO;
import com.example.demo.dto.dashboard.ResponsableDashboardDTO;
import org.springframework.security.core.userdetails.UserDetails;

public interface DashboardService {
    AdminDashboardDTO getAdminDashboard();
    ResponsableDashboardDTO getResponsableDashboard(UserDetails userDetails);
    AgriculteurDashboardDTO getAgriculteurDashboard(UserDetails userDetails);
}