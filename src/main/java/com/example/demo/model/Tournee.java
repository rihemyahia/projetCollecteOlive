package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tournees")
public class Tournee {

	// 200 trees × ~5 kg/tree avg yield = 1 000 kg → fills one standard 1-tonne
	// benne
	public static final int NB_ARBRES_PAR_TOURNEE = 200;
	private Verger vergerSnapshot;  // Complete copy at creation time

	@Id
	private String id;

	private String code; // e.g. T-20251012-001

	private StatutTournee statut; // PLANIFIEE | EN_COURS | TERMINEE | ANNULEE

	@DocumentReference(lazy = true)
	private Verger verger;

	@DocumentReference(lazy = true)
	private Ressource benne;

	@DocumentReference(lazy = true)
	private Ressource tracteur;

	@DocumentReference(lazy = true)
	@Builder.Default
	private List<Utilisateur> travailleurs = new ArrayList<>();

	@DocumentReference(lazy = true)
	private Utilisateur transporteur;

	@DocumentReference(lazy = true)
	private Utilisateur responsablePressoir;

	/**
	 * Planned start date (set at creation, updated when demarrer() is called). Used
	 * for availability overlap checks.
	 */
	private Date dateDebut;

	/**
	 * Planned / actual end date. Set at creation as the estimated end; overwritten
	 * with the real time on terminer(). Used for availability overlap checks.
	 */
	private Date dateFin;

	@Builder.Default
	private Integer nbreArbre = NB_ARBRES_PAR_TOURNEE;

	private Double distanceTotale; // km
	private Integer tempsTotal; // minutes (actual dateDebut → actual dateFin)

	private Double quantiteCollecteeKg;
	private Boolean collecteFinalisee;

	private String observations;
	private String responsablePressoirId;
	private String livraisonDestinationNom;
	private String livraisonDestinationAdresse;
	@DocumentReference(lazy = true)
	private Collecte collecte;
	@CreatedDate
	private Date dateCreation;

	// ========== LIVRAISON (proof & timestamps) ==========
	/** Créneau estimé (admin) pour la livraison au pressoir — permet plusieurs livraisons le même jour sans chevauchement. */
	private Date livraisonEstimeDebut;
	private Date livraisonEstimeFin;
	/** Consigne courte pour le transporteur (optionnel). */
	private String livraisonNotes;
	private Date livraisonStartedAt;
	private Date livraisonCompletedAt;
	private String livraisonEvidenceName;
	private String livraisonEvidenceUrl;
	private String livraisonEvidenceBase64;
}
