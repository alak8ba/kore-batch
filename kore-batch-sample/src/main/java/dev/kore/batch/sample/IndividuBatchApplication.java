package dev.kore.batch.sample;

import dev.kore.batch.BatchLauncher;
import dev.kore.batch.error.TechnicalException;
import dev.kore.batch.health.BatchHealthAggregator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Batch de traitement d'individus depuis un fichier à largeur fixe.
 *
 * Lancement :
 *   java -jar kore-batch-sample.jar --inputFile=/chemin/vers/fichier
 */
@SpringBootApplication(scanBasePackages = {"dev.kore.batch"})
public class IndividuBatchApplication extends BatchLauncher {

    public IndividuBatchApplication(JobLauncher jobLauncher, Job job,
                                     ApplicationContext applicationContext,
                                     BatchHealthAggregator healthAggregator) {
        super(jobLauncher, job, applicationContext, healthAggregator);
    }

    public static void main(String[] args) {
        SpringApplication.run(IndividuBatchApplication.class, args);
    }

    @Override
    protected void addJobParameters(String[] args, JobParametersBuilder builder) throws TechnicalException {
        for (String arg : args) {
            if (arg.startsWith("--inputFile=")) {
                builder.addString("inputFile", arg.substring("--inputFile=".length()));
                return;
            }
        }
        throw new TechnicalException("Parametre obligatoire manquant : --inputFile=/chemin/vers/fichier");
    }
}
