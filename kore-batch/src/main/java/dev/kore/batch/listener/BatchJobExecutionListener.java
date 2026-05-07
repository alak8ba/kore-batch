package dev.kore.batch.listener;

import dev.kore.batch.dto.ISynthese;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class BatchJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=== DEBUT DU BATCH [{}] - JobId={} ===",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duree = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        ISynthese synthese = (ISynthese) jobExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        log.info("=== FIN DU BATCH [{}] - Statut={} - Duree={}s ===",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getStatus(),
            duree.toSeconds());

        if (synthese != null) {
            log.info("SYNTHESE : total={}, OK={}, KO={}, errTech={}, errFonc={}, doublons={}",
                synthese.getTotal(), synthese.getNbOK(), synthese.getNbKO(),
                synthese.getNbErreursTechniques(), synthese.getNbErreursFonctionnelles(),
                synthese.getNbDoublons());
        }
    }
}
