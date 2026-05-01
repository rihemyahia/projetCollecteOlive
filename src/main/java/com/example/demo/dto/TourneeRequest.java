package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TourneeRequest {

    @NotBlank(message = "L'identifiant du verger est obligatoire")
    private String vergerId;

    @NotBlank(message = "L'identifiant de la benne est obligatoire")
    private String benneId;

    @NotBlank(message = "L'identifiant du tracteur est obligatoire")
    private String tracteurId;

    @NotEmpty(message = "Au moins un travailleur doit être assigné")
    private List<String> travailleurIds;

    @Positive(message = "Le nombre d'arbres doit être positif")
    private Integer nbreArbre;

    /** Planned start — mandatory for availability check */
    @NotNull(message = "La date de début est obligatoire")
    private Date dateDebut;

    /** Planned end — mandatory for availability check */
    @NotNull(message = "La date de fin estimée est obligatoire")
    private Date dateFin;

    @PositiveOrZero(message = "La distance doit être positive ou nulle")
    private Double distanceTotale;
    private String collecteId;

    private String observations;
    private String responsablePressoirId;
    private String livraisonDestinationNom;
    private String livraisonDestinationAdresse;
}
