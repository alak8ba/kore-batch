package dev.kore.batch.sample.reader;

import dev.kore.batch.sample.dto.AssureDto;
import org.springframework.batch.item.file.LineMapper;

/**
 * Parse une ligne du fichier d'alimentation au format largeur fixe.
 *
 * Format de chaque ligne :
 * - pos  1-12  : code service (ex: LVP021)
 * - pos 13-21  : type pension (ex: ER4-NR)
 * - *numPension* : numero pension entre asterisques
 * - civilite + nom/prenom (champ variable apres le second *)
 * - adresse ligne 1 et 2
 * - code pays (5 chiffres) + libelle pays
 * - date de reference
 * - NIR : 13 derniers caracteres (peut etre vide)
 */
public class AssureLineMapper implements LineMapper<AssureDto> {

    private static final int NIR_LENGTH = 13;

    @Override
    public AssureDto mapLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) return null;

        String numPension = extractNumPension(line);
        String nir        = extractNir(line);
        String typePension = extractField(line, 12, 21).trim();

        // Tout ce qui est apres le second * jusqu'au bloc pays
        String afterPension = extractAfterNumPension(line);

        // Civilite : MME, M, MLE - premier mot
        String civilite  = extractCivilite(afterPension);
        String nomPrenom = extractNomPrenom(afterPension, civilite);

        // Adresse : colonnes fixes apres nom/prenom (largeur 33 chacune)
        String adresse1 = extractAdresse1(afterPension);
        String adresse2 = extractAdresse2(afterPension);

        // Pays : "99999SUISSE" ou "99999ESPAGNE"
        String codePays = extractCodePays(line);
        String pays     = extractPays(line);

        // Date de reference
        String dateRef = extractDateReference(line);

        return AssureDto.builder()
                .numPension(numPension)
                .typePension(typePension)
                .civilite(civilite)
                .nomPrenom(nomPrenom)
                .adresseLigne1(adresse1)
                .adresseLigne2(adresse2)
                .codePays(codePays)
                .pays(pays)
                .dateReference(dateRef)
                .nir(nir)
                .build();
    }

    private String extractNumPension(String line) {
        int debut = line.indexOf('*');
        int fin   = line.indexOf('*', debut + 1);
        if (debut < 0 || fin < 0) return "";
        return line.substring(debut + 1, fin).trim();
    }

    private String extractAfterNumPension(String line) {
        int debut = line.indexOf('*');
        int fin   = line.indexOf('*', debut + 1);
        if (fin < 0 || fin + 1 >= line.length()) return "";
        return line.substring(fin + 1);
    }

    private String extractCivilite(String after) {
        String trimmed = after.trim();
        if (trimmed.startsWith("MME")) return "MME";
        if (trimmed.startsWith("MLE")) return "MLE";
        if (trimmed.startsWith("M ")) return "M";
        return "";
    }

    private String extractNomPrenom(String after, String civilite) {
        String trimmed = after.trim();
        int debut = civilite.isEmpty() ? 0 : civilite.length();
        // Le nom/prenom occupe ~32 chars
        int fin = Math.min(debut + 32, trimmed.length());
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
        // Cherche le pattern 5 chiffres (ex: 99999)
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
        // La date est apres le bloc pays, format "01 DECEMBRE 2018"
        int idxPays = line.indexOf("99999");
        if (idxPays < 0) return "";
        int debut = idxPays + 32;
        if (debut >= line.length()) return "";
        int fin = Math.min(debut + 17, line.length());
        return line.substring(debut, fin).trim();
    }

    private String extractNir(String line) {
        if (line.length() < NIR_LENGTH) return "";
        String nir = line.substring(line.length() - NIR_LENGTH).trim();
        return nir.isBlank() ? "" : nir;
    }

    private String extractField(String line, int debut, int fin) {
        if (line.length() < fin) return line.substring(Math.min(debut, line.length())).trim();
        return line.substring(debut, fin);
    }
}
