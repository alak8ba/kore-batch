package dev.kore.batch.validator;

import dev.kore.batch.error.TechnicalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.io.File;

/**
 * Valide que le parametre inputFile est present et que le fichier existe.
 * A enregistrer sur le Job via .validator(new InputFileValidator()).
 */
@Slf4j
public class InputFileValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        String inputFile = parameters.getString("inputFile");

        if (inputFile == null || inputFile.isBlank()) {
            throw new JobParametersInvalidException(
                "Parametre obligatoire manquant : inputFile"
            );
        }

        File file = new File(inputFile);
        if (!file.exists()) {
            throw new JobParametersInvalidException(
                "Fichier introuvable : " + inputFile
            );
        }

        if (!file.isFile()) {
            throw new JobParametersInvalidException(
                "Le chemin ne pointe pas vers un fichier : " + inputFile
            );
        }

        if (!file.canRead()) {
            throw new JobParametersInvalidException(
                "Fichier non lisible (droits insuffisants) : " + inputFile
            );
        }

        log.info("Validation OK - fichier : {} ({} octets)", inputFile, file.length());
    }
}
