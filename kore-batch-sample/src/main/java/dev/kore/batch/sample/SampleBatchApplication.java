package dev.kore.batch.sample;

import dev.kore.batch.core.BatchLauncher;
import dev.kore.batch.core.error.TechnicalException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Lancement : java -jar kore-batch-sample.jar --inputFile=/data/commandes.csv
 */
@SpringBootApplication(scanBasePackages = {"dev.kore.batch"})
public class SampleBatchApplication extends BatchLauncher {

    public SampleBatchApplication(JobLauncher jobLauncher, Job job, ApplicationContext applicationContext) {
        super(jobLauncher, job, applicationContext);
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleBatchApplication.class, args);
    }

    @Override
    protected void addJobParameters(String[] args, JobParametersBuilder builder) throws TechnicalException {
        for (String arg : args) {
            if (arg.startsWith("--inputFile=")) {
                builder.addString("inputFile", arg.substring("--inputFile=".length()));
                return;
            }
        }
        builder.addString("inputFile", "default-input.csv");
    }
}
