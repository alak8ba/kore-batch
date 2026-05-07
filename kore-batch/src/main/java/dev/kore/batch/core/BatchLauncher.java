package dev.kore.batch.core;

import dev.kore.batch.core.dto.ISynthese;
import dev.kore.batch.core.error.TechnicalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * Point d'entrée abstrait pour tous les batchs.
 *
 * Codes retour :
 *   0  = succès
 *  -1  = erreur technique
 *   1  = erreurs fonctionnelles bloquantes (activable si besoin)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BatchLauncher implements CommandLineRunner {

    protected static final int CODE_OK                  = 0;
    protected static final int CODE_ERREUR_TECHNIQUE    = -1;
    protected static final int CODE_ERREUR_FONCTIONNELLE = 1;

    private final JobLauncher jobLauncher;
    private final Job job;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        int codeRetour = CODE_ERREUR_TECHNIQUE;
        try {
            JobParametersBuilder builder = new JobParametersBuilder();
            builder.addLong("timestamp", System.currentTimeMillis());
            addJobParameters(args, builder);

            JobParameters jobParameters = builder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            codeRetour = resolveExitCode(jobExecution);

        } catch (TechnicalException e) {
            log.error("Erreur technique : {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur non gérée : {}", e.getMessage(), e);
        } finally {
            SpringApplication.exit(applicationContext);
            System.exit(codeRetour);
        }
    }

    protected void addJobParameters(String[] args, JobParametersBuilder builder) throws TechnicalException {
        // surcharger pour ajouter des paramètres métier
    }

    private int resolveExitCode(JobExecution jobExecution) {
        ISynthese synthese = (ISynthese) jobExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);
        if (ExitStatus.FAILED.equals(jobExecution.getExitStatus())) return CODE_ERREUR_TECHNIQUE;
        if (synthese == null || synthese.hasTechnicalErrors())        return CODE_ERREUR_TECHNIQUE;
        return CODE_OK;
    }
}
