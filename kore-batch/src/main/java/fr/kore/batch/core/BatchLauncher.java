package fr.kore.batch.core;

import fr.kore.batch.core.dto.ISynthese;
import fr.kore.batch.core.error.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * Point d'entrée abstrait pour tous les batchs.
 *
 * Gestion des codes retour :
 *   0  = succès (ou erreurs fonctionnelles tolérées)
 *  -1  = erreur technique
 *   1  = erreurs fonctionnelles bloquantes (activable si besoin)
 */
public abstract class BatchLauncher implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BatchLauncher.class);

    protected static final int CODE_OK = 0;
    protected static final int CODE_ERREUR_TECHNIQUE = -1;
    protected static final int CODE_ERREUR_FONCTIONNELLE = 1;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job job;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        int codeRetour = CODE_ERREUR_TECHNIQUE;
        try {
            JobParametersBuilder builder = new JobParametersBuilder();
            // Timestamp pour garantir une nouvelle instance à chaque lancement
            builder.addLong("timestamp", System.currentTimeMillis());
            addJobParameters(args, builder);

            JobParameters jobParameters = builder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            codeRetour = resolveExitCode(jobExecution);

        } catch (TechnicalException e) {
            LOG.error("Erreur technique : {}", e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Erreur non gérée : {}", e.getMessage(), e);
        } finally {
            SpringApplication.exit(applicationContext);
            System.exit(codeRetour);
        }
    }

    /**
     * Surcharger pour ajouter des paramètres métier au job.
     */
    protected void addJobParameters(String[] args, JobParametersBuilder builder) throws TechnicalException {
        // par défaut : rien
    }

    private int resolveExitCode(JobExecution jobExecution) {
        ISynthese synthese = (ISynthese) jobExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        if (ExitStatus.FAILED.equals(jobExecution.getExitStatus())) {
            return CODE_ERREUR_TECHNIQUE;
        }
        if (synthese == null || synthese.hasTechnicalErrors()) {
            return CODE_ERREUR_TECHNIQUE;
        }
        // Les erreurs fonctionnelles sont loggées mais ne bloquent pas par défaut
        return CODE_OK;
    }
}
