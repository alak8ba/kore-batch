package fr.kore.batch.core.error;

/**
 * Exception fonctionnelle : erreur métier attendue, n'interrompt pas le batch.
 * Doit être catchée dans le processor et comptabilisée dans la synthèse.
 */
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

    public String getComponent() {
        return component;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return String.format("FunctionalException{component='%s', action='%s', message='%s'}",
            component, action, getMessage());
    }
}
