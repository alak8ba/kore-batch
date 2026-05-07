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
 * - Identifiant obligatoire, 10 caractères, commence par 5 ou 6
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
        log.debug("Traitement individu ref={}", assure.getReference());
        try {
            valider(assure);
            synthese.incrementOK();
            return AssureResultDto.ok(assure);

        } catch (FunctionalException e) {
            log.warn("Erreur fonctionnelle [{}] : {}",
                StructuredArguments.value("reference", assure.getReference()),
                e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addReferenceEnErreur(assure.getReference());
            return AssureResultDto.ko(assure, e.getMessage());
        }
    }

    private void valider(AssureDto assure) throws FunctionalException {
        if (assure.getReference() == null || assure.getReference().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Reference obligatoire");
        }
        if (assure.getIdentifiant() == null || assure.getIdentifiant().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Identifiant absent - ref=" + assure.getReference());
        }
        if (assure.getIdentifiant().length() != 10) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Identifiant invalide (doit faire 10 caracteres) - val=" + assure.getIdentifiant());
        }
        char premier = assure.getIdentifiant().charAt(0);
        if (premier != '5' && premier != '6') {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Identifiant invalide (doit commencer par 5 ou 6) - val=" + assure.getIdentifiant());
        }
        if (assure.getNomPrenom() == null || assure.getNomPrenom().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Nom absent - ref=" + assure.getReference());
        }
        if (assure.getPays() == null || assure.getPays().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Pays absent - ref=" + assure.getReference());
        }
    }
}
