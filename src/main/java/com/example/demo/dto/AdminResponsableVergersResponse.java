package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminResponsableVergersResponse {
    private String responsableId;
    private String responsableNom;
    private String responsableEmail;
    private String fonction;
    private List<VergerResponse> vergers;
}

