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
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
class TraitementIndividusJobIT extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job traitementIndividusJob;

    @Autowired
    private DataSource dataSource;

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

        // 200 individus dont 8 sans identifiant (1 ligne sur 25)
        assertThat(synthese).isNotNull();
        assertThat(synthese.getNbErreursFonctionnelles()).isEqualTo(8);
        assertThat(synthese.getNbOK()).isEqualTo(192);
    }

    @Test
    void tousLesIndividusDoiventEtrePersisteesEnBase() throws Exception {
        jobLauncherTestUtils.setJob(traitementIndividusJob);

        jobLauncherTestUtils.launchJob(
            new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("inputFile", getFichierTest())
                .toJobParameters()
        );

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Tous les individus (OK + KO) sont persistes
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM T_INDIVIDU", Integer.class);
        assertThat(total).isEqualTo(200);

        // Les OK ont le statut OK
        int nbOk = jdbc.queryForObject(
            "SELECT COUNT(*) FROM T_INDIVIDU WHERE STATUT = 'OK'", Integer.class);
        assertThat(nbOk).isEqualTo(192);

        // Les KO ont le statut KO avec un message d'erreur
        int nbKo = jdbc.queryForObject(
            "SELECT COUNT(*) FROM T_INDIVIDU WHERE STATUT = 'KO'", Integer.class);
        assertThat(nbKo).isEqualTo(8);

        int nbKoAvecMessage = jdbc.queryForObject(
            "SELECT COUNT(*) FROM T_INDIVIDU WHERE STATUT = 'KO' AND MESSAGE_ERREUR IS NOT NULL",
            Integer.class);
        assertThat(nbKoAvecMessage).isEqualTo(8);
    }

    private String getFichierTest() {
        return getClass().getClassLoader()
            .getResource("data/individus_test.txt").getPath();
    }
}
