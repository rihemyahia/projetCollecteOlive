package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ReceptionOlivesRequest {
    @NotNull(message = "La quantite d'olives recue est obligatoire")
    @Positive(message = "La quantite d'olives recue doit etre positive")
    private Double quantiteOlivesRecueKg;

    private String observations;
}
