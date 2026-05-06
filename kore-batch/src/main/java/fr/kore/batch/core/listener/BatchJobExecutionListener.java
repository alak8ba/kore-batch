package fr.kore.batch.core.listener;

import fr.kore.batch.core.dto.ISynthese;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BatchJobExecutionListener implements JobExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(BatchJobExecutionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOG.info("=== DEBUT DU BATCH [{}] - JobId={} ===",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duree = Duration.between(
            jobExecution.getStartTime(),
            jobExecution.getEndTime()
        );

        ISynthese synthese = (ISynthese) jobExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        LOG.info("=== FIN DU BATCH [{}] - Statut={} - Durée={}s ===",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getStatus(),
            duree.toSeconds());

        if (synthese != null) {
            LOG.info("SYNTHESE : total={}, OK={}, KO={}, errTech={}, errFonc={}, doublons={}",
                synthese.getTotal(),
                synthese.getNbOK(),
                synthese.getNbKO(),
                synthese.getNbErreursTechniques(),
                synthese.getNbErreursFonctionnelles(),
                synthese.getNbDoublons());
        }
    }
}
