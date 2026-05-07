package dev.kore.batch.sample;

import dev.kore.batch.dto.ISynthese;
import dev.kore.batch.sample.dto.CommandeSyntheseDto;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
class TraitementCommandesJobIT extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job traitementCommandesJob;

    @Test
    void jobDoitSeTerminerEnSucces() throws Exception {
        jobLauncherTestUtils.setJob(traitementCommandesJob);

        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("inputFile", "test-commandes.csv")
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void syntheseDoitComptabiliserLesErreursFonctionnelles() throws Exception {
        jobLauncherTestUtils.setJob(traitementCommandesJob);

        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("inputFile", "test-commandes.csv")
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        CommandeSyntheseDto synthese = (CommandeSyntheseDto)
                execution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        assertThat(synthese).isNotNull();
        assertThat(synthese.getNbErreursFonctionnelles()).isEqualTo(2);
        assertThat(synthese.getNbOK()).isEqualTo(3);
        assertThat(synthese.getReferencesEnErreur()).contains("CMD-003", "CMD-004");
    }
}
