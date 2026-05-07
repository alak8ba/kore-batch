package dev.kore.batch.sample.dto;

public record AssureResultDto(AssureDto assure, boolean succes, String messageErreur) {

    public static AssureResultDto ok(AssureDto assure) {
        return new AssureResultDto(assure, true, null);
    }

    public static AssureResultDto ko(AssureDto assure, String messageErreur) {
        return new AssureResultDto(assure, false, messageErreur);
    }
}
