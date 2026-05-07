package dev.kore.batch.sample.listener;

import dev.kore.batch.dto.ISynthese;
import dev.kore.batch.sample.dto.IndividuSyntheseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TraitementAssuresStepListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("=== DEBUT DU STEP [{}] ===", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        IndividuSyntheseDto synthese = (IndividuSyntheseDto)
            stepExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        if (synthese != null) {
            log.info("=== FIN DU STEP [{}] - lu={}, OK={}, KO={}, errFonc={} ===",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                synthese.getNbOK(),
                synthese.getNbKO(),
                synthese.getNbErreursFonctionnelles());

            if (!synthese.getNumerosEnErreur().isEmpty()) {
                log.warn("Individus en erreur : {}", synthese.getNumerosEnErreur());
            }
        }
        return stepExecution.getExitStatus();
    }
}
