package dev.kore.batch.sample;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static {
        // Fix Docker Desktop WSL2 sur Windows : force le socket TCP
        if (System.getenv("DOCKER_HOST") == null) {
            System.setProperty("DOCKER_HOST", "tcp://localhost:2375");
        }
        System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
    }

    @MockBean
    IndividuBatchApplication individuBatchApplication;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("batchdb_test")
            .withUsername("batch_user")
            .withPassword("batch_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
