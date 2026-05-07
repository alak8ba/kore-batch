package dev.kore.batch.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Vérifie que la base de données est accessible avant le lancement du batch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements BatchHealthIndicator {

    private final DataSource dataSource;

    @Override
    public HealthResult check() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            if (valid) {
                log.info("Health [DATABASE] : UP");
                return HealthResult.up("DATABASE");
            }
            log.warn("Health [DATABASE] : DOWN - connexion invalide");
            return HealthResult.down("DATABASE", "connexion invalide");
        } catch (Exception e) {
            log.error("Health [DATABASE] : DOWN - {}", e.getMessage());
            return HealthResult.down("DATABASE", e.getMessage());
        }
    }
}
