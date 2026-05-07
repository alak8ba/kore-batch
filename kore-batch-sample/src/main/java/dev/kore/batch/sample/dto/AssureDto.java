package dev.kore.batch.sample.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Représente un assuré lu depuis le fichier d'alimentation (format largeur fixe).
 */
@Data
@Builder
public class AssureDto {

    private String numPension;
    private String civilite;
    private String nomPrenom;
    private String adresseLigne1;
    private String adresseLigne2;
    private String codePays;
    private String pays;
    private String nir;
    private String typePension;
    private String dateReference;
}
