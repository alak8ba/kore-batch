package fr.kore.batch.sample;

import fr.kore.batch.core.BatchLauncher;
import fr.kore.batch.core.error.TechnicalException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Point d'entrée du batch traitement des commandes.
 *
 * Lancement : java -jar kore-batch-sample.jar --inputFile=/data/commandes.csv
 */
@SpringBootApplication
@ComponentScan(basePackages = {"fr.kore.batch.sample", "fr.kore.batch.core"})
public class SampleBatchApplication extends BatchLauncher {

    public static void main(String[] args) {
        SpringApplication.run(SampleBatchApplication.class, args);
    }

    @Override
    protected void addJobParameters(String[] args, JobParametersBuilder builder) throws TechnicalException {
        String inputFile = extractArg(args, "--inputFile");
        if (inputFile == null) {
            inputFile = "default-input.csv";
        }
        builder.addString("inputFile", inputFile);
    }

    private String extractArg(String[] args, String key) {
        for (String arg : args) {
            if (arg.startsWith(key + "=")) {
                return arg.substring(key.length() + 1);
            }
        }
        return null;
    }
}
