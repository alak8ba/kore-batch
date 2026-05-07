package dev.kore.batch.health;

/**
 * Contrat pour un indicateur de santé.
 * Chaque dépendance à vérifier implémente cette interface.
 */
public interface BatchHealthIndicator {

    HealthResult check();
}
