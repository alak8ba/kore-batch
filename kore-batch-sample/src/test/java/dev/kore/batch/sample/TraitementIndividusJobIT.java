package dev.kore.batch.sample;

import dev.kore.batch.dto.ISynthese;
import dev.kore.batch.sample.dto.IndividuSyntheseDto;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
class TraitementIndividusJobIT extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job traitementIndividusJob;

    @Test
    void jobDoitSeTerminerEnSucces() throws Exception {
        jobLauncherTestUtils.setJob(traitementIndividusJob);

        JobExecution execution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("inputFile", getFichierTest())
                .toJobParameters()
        );

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void syntheseDoitComptabiliserLesErreursFonctionnelles() throws Exception {
        jobLauncherTestUtils.setJob(traitementIndividusJob);

        JobExecution execution = jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("inputFile", getFichierTest())
                .toJobParameters()
        );

        IndividuSyntheseDto synthese = (IndividuSyntheseDto)
            execution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);

        // Fichier de test : 5 lignes dont 1 sans NIR -> 1 erreur fonctionnelle
        assertThat(synthese).isNotNull();
        assertThat(synthese.getNbErreursFonctionnelles()).isEqualTo(1);
        assertThat(synthese.getNbOK()).isEqualTo(4);
    }

    private String getFichierTest() {
        return getClass().getClassLoader()
            .getResource("data/individus_test.txt").getPath();
    }
}
