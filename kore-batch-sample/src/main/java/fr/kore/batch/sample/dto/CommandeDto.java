package fr.kore.batch.sample.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO représentant une ligne lue depuis le fichier d'entrée.
 * Analogie avec PensionDto dans MCE.
 */
public class CommandeDto {

    private String reference;
    private String clientId;
    private BigDecimal montant;
    private LocalDate dateCommande;
    private String statut;

    public CommandeDto() {}

    public CommandeDto(String reference, String clientId, BigDecimal montant, LocalDate dateCommande, String statut) {
        this.reference = reference;
        this.clientId = clientId;
        this.montant = montant;
        this.dateCommande = dateCommande;
        this.statut = statut;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate dateCommande) { this.dateCommande = dateCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return String.format("CommandeDto{ref='%s', client='%s', montant=%s}", reference, clientId, montant);
    }
}
