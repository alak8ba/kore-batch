package dev.kore.batch.sample.reader;

import dev.kore.batch.sample.dto.AssureDto;
import org.springframework.batch.item.file.LineMapper;

/**
 * Parse une ligne du fichier d'entrée au format largeur fixe.
 *
 * Champs extraits :
 * - reference      : valeur entre les deux marqueurs * de début de ligne
 * - identifiant    : 10 derniers caractères (identifiant national)
 * - typeFlux       : position 13-21
 * - civilite/nom   : après le second marqueur
 * - adresses/pays  : champs variables au milieu de la ligne
 */
public class AssureLineMapper implements LineMapper<AssureDto> {

    private static final int IDENTIFIANT_LENGTH = 10;

    @Override
    public AssureDto mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) return null;

        String reference   = extractReference(line);
        String identifiant = extractIdentifiant(line);
        String typeFlux    = extractField(line, 12, 21).trim();

        String afterRef  = extractAfterReference(line);
        String civilite  = extractCivilite(afterRef);
        String nomPrenom = extractNomPrenom(afterRef, civilite);
        String adresse1  = extractAdresse1(afterRef);
        String adresse2  = extractAdresse2(afterRef);
        String codePays  = extractCodePays(line);
        String pays      = extractPays(line);
        String dateRef   = extractDateReference(line);

        return AssureDto.builder()
                .reference(reference)
                .typeFlux(typeFlux)
                .civilite(civilite)
                .nomPrenom(nomPrenom)
                .adresseLigne1(adresse1)
                .adresseLigne2(adresse2)
                .codePays(codePays)
                .pays(pays)
                .dateReference(dateRef)
                .identifiant(identifiant)
                .build();
    }

    private String extractReference(String line) {
        int debut = line.indexOf('*');
        int fin   = line.indexOf('*', debut + 1);
        if (debut < 0 || fin < 0) return "";
        return line.substring(debut + 1, fin).trim();
    }

    private String extractAfterReference(String line) {
        int debut = line.indexOf('*');
        int fin   = line.indexOf('*', debut + 1);
        if (fin < 0 || fin + 1 >= line.length()) return "";
        return line.substring(fin + 1);
    }

    private String extractCivilite(String after) {
        String trimmed = after.trim();
        if (trimmed.startsWith("MME")) return "MME";
        if (trimmed.startsWith("MLE")) return "MLE";
        if (trimmed.startsWith("M "))  return "M";
        return "";
    }

    private String extractNomPrenom(String after, String civilite) {
        String trimmed = after.trim();
        int debut = civilite.isEmpty() ? 0 : civilite.length();
        int fin   = Math.min(debut + 32, trimmed.length());
        return trimmed.substring(debut, fin).trim();
    }

    private String extractAdresse1(String after) {
        String trimmed = after.trim();
        int debut = Math.min(32, trimmed.length());
        int fin   = Math.min(debut + 33, trimmed.length());
        return trimmed.substring(debut, fin).trim();
    }

    private String extractAdresse2(String after) {
        String trimmed = after.trim();
        int debut = Math.min(65, trimmed.length());
        int fin   = Math.min(debut + 33, trimmed.length());
        return trimmed.substring(debut, fin).trim();
    }

    private String extractCodePays(String line) {
        int idx = line.indexOf("99999");
        if (idx < 0) return "";
        return line.substring(idx, Math.min(idx + 5, line.length())).trim();
    }

    private String extractPays(String line) {
        int idx = line.indexOf("99999");
        if (idx < 0) return "";
        int debut = idx + 5;
        int fin   = Math.min(debut + 27, line.length());
        return line.substring(debut, fin).trim();
    }

    private String extractDateReference(String line) {
        int idxPays = line.indexOf("99999");
        if (idxPays < 0) return "";
        int debut = idxPays + 32;
        if (debut >= line.length()) return "";
        int fin = Math.min(debut + 17, line.length());
        return line.substring(debut, fin).trim();
    }

    private String extractIdentifiant(String line) {
        if (line.length() < IDENTIFIANT_LENGTH) return "";
        String val = line.substring(line.length() - IDENTIFIANT_LENGTH).trim();
        return val.isBlank() ? "" : val;
    }

    private String extractField(String line, int debut, int fin) {
        if (line.length() < fin) return line.substring(Math.min(debut, line.length())).trim();
        return line.substring(debut, fin);
    }
}
