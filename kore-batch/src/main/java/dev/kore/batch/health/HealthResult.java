package dev.kore.batch.health;

import lombok.Getter;

@Getter
public class HealthResult {

    private final String name;
    private final HealthStatus status;
    private final String detail;

    private HealthResult(String name, HealthStatus status, String detail) {
        this.name = name;
        this.status = status;
        this.detail = detail;
    }

    public static HealthResult up(String name) {
        return new HealthResult(name, HealthStatus.UP, null);
    }

    public static HealthResult down(String name, String detail) {
        return new HealthResult(name, HealthStatus.DOWN, detail);
    }

    public boolean isUp() {
        return HealthStatus.UP.equals(status);
    }

    @Override
    public String toString() {
        return detail != null
            ? "[%s] %s - %s".formatted(status, name, detail)
            : "[%s] %s".formatted(status, name);
    }
}
