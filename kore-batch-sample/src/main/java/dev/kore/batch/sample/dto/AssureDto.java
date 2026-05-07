package dev.kore.batch.sample.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Représente un individu lu depuis le fichier d'entrée (format largeur fixe).
 */
@Data
@Builder
public class AssureDto {

    /** Identifiant unique de l'individu dans le fichier source */
    private String reference;

    /** Type de flux */
    private String typeFlux;

    private String civilite;
    private String nomPrenom;
    private String adresseLigne1;
    private String adresseLigne2;
    private String codePays;
    private String pays;

    /** Identifiant  de l'individu (10 caractères, commence par 5 ou 6) */
    private String identifiant;

    private String dateReference;
}
