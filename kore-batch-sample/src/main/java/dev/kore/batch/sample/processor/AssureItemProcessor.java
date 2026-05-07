package dev.kore.batch.sample.processor;

import dev.kore.batch.dto.ISynthese;
import dev.kore.batch.error.FunctionalException;
import dev.kore.batch.sample.dto.AssureDto;
import dev.kore.batch.sample.dto.AssureResultDto;
import dev.kore.batch.sample.dto.IndividuSyntheseDto;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Applique les règles de validation sur chaque individu :
 * - NIR obligatoire, 13 caractères, commence par 1 ou 2
 * - Nom/prénom obligatoire
 * - Pays obligatoire
 */
@Slf4j
@Component
@StepScope
public class AssureItemProcessor implements ItemProcessor<AssureDto, AssureResultDto> {

    private IndividuSyntheseDto synthese;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        synthese = new IndividuSyntheseDto();
        stepExecution.getExecutionContext().put(ISynthese.SYNTHESE_KEY, synthese);
    }

    @Override
    public AssureResultDto process(AssureDto assure) {
        log.debug("Traitement individu {}", assure.getNumPension());
        try {
            valider(assure);
            synthese.incrementOK();
            return AssureResultDto.ok(assure);

        } catch (FunctionalException e) {
            log.warn("Erreur fonctionnelle [{}] : {}",
                StructuredArguments.value("numPension", assure.getNumPension()),
                e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addNumeroEnErreur(assure.getNumPension());
            return AssureResultDto.ko(assure, e.getMessage());
        }
    }

    private void valider(AssureDto assure) throws FunctionalException {
        if (assure.getNumPension() == null || assure.getNumPension().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Numero obligatoire");
        }
        if (assure.getNir() == null || assure.getNir().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "NIR absent - num=" + assure.getNumPension());
        }
        if (assure.getNir().length() != 13) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "NIR invalide (doit faire 13 caracteres) - NIR=" + assure.getNir());
        }
        char premierCaractere = assure.getNir().charAt(0);
        if (premierCaractere != '1' && premierCaractere != '2') {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "NIR invalide (doit commencer par 1 ou 2) - NIR=" + assure.getNir());
        }
        if (assure.getNomPrenom() == null || assure.getNomPrenom().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Nom absent - num=" + assure.getNumPension());
        }
        if (assure.getPays() == null || assure.getPays().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Pays absent - num=" + assure.getNumPension());
        }
    }
}
