package dev.kore.batch.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agrège tous les {@link BatchHealthIndicator} déclarés dans le contexte Spring
 * et détermine si l'environnement est prêt pour lancer le batch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchHealthAggregator {

    private final List<BatchHealthIndicator> indicators;

    /**
     * Vérifie tous les indicateurs et retourne true si tous sont UP.
     */
    public boolean checkAll() {
        log.info("=== DIAGNOSTIC ENVIRONNEMENT ===");

        boolean allUp = true;
        for (BatchHealthIndicator indicator : indicators) {
            HealthResult result = indicator.check();
            log.info("  {}", result);
            if (!result.isUp()) {
                allUp = false;
            }
        }

        if (allUp) {
            log.info("=== ENVIRONNEMENT OK - lancement du batch ===");
        } else {
            log.error("=== ENVIRONNEMENT KO - batch annule ===");
        }

        return allUp;
    }
}
