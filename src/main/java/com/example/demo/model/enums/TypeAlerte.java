package com.example.demo.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TypeAlerte {
    MALADIE,
    NUISIBLE,
    IRRIGATION,
    METEO,
    AUTRE,
    MATURITE_ACCELEREE,
    CHUTE_PREMATUREE,
    QUALITE_HUILE,
    LOGISTIQUE_MOULIN,
    SECURITE_RECOLTE,
    RENDEMENT_ANORMAL,
    MATURITE,
    RECOLTE;

    @JsonCreator
    public static TypeAlerte fromString(String value) {
        if (value == null) return null;
        try {
            // Converts "recolte" or "RECOLTE " to "RECOLTE"
            return TypeAlerte.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // 🛡️ THE SAFETY NET:
            // If the DB has a value we haven't added here yet,
            // return AUTRE instead of crashing with a 400 error.
            return AUTRE;
        }
    }
}