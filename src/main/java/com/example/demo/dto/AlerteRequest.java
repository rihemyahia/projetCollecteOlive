package com.example.demo.dto;

import com.example.demo.model.enums.TypeAlerte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlerteRequest {

    @NotBlank
    private String agriculteurId;
    @NotBlank(message = "Le verger est obligatoire")
    private String vergerId;

    @NotNull
    private TypeAlerte type;

    @NotBlank
    private String description;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String adresseIndicative;
}