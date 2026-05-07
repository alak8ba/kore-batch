package dev.kore.batch.core.error;

import lombok.Getter;

/**
 * Exception fonctionnelle : erreur métier attendue, n'interrompt pas le batch.
 * Doit être catchée dans le processor et comptabilisée dans la synthèse.
 */
@Getter
public class FunctionalException extends Exception {

    private final String component;
    private final String action;

    public FunctionalException(String component, String action, String message) {
        super(message);
        this.component = component;
        this.action = action;
    }

    public FunctionalException(String message) {
        super(message);
        this.component = "unknown";
        this.action = "unknown";
    }

    public FunctionalException(String message, Throwable cause) {
        super(message, cause);
        this.component = "unknown";
        this.action = "unknown";
    }

    @Override
    public String toString() {
        return "FunctionalException{component='%s', action='%s', message='%s'}"
            .formatted(component, action, getMessage());
    }
}
