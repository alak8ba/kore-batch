package dev.kore.batch.health;

import lombok.extern.slf4j.Slf4j;

/**
 * Template pour vérifier la disponibilité d'un service web externe.
 * Le projet métier étend cette classe et implémente {@link #getName()} et {@link #ping()}.
 *
 * Exemple :
 * <pre>
 * {@literal @}Component
 * public class MonServiceHealthIndicator extends AbstractWSHealthIndicator {
 *
 *     private final MonServiceClient client;
 *
 *     {@literal @}Override
 *     protected String getName() { return "MON_SERVICE"; }
 *
 *     {@literal @}Override
 *     protected boolean ping() {
 *         return client.ping();
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractWSHealthIndicator implements BatchHealthIndicator {

    protected abstract String getName();

    protected abstract boolean ping();

    @Override
    public HealthResult check() {
        try {
            boolean up = ping();
            if (up) {
                log.info("Health [{}] : UP", getName());
                return HealthResult.up(getName());
            }
            log.warn("Health [{}] : DOWN - ping KO", getName());
            return HealthResult.down(getName(), "ping KO");
        } catch (Exception e) {
            log.error("Health [{}] : DOWN - {}", getName(), e.getMessage());
            return HealthResult.down(getName(), e.getMessage());
        }
    }
}
