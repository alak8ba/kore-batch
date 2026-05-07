package dev.kore.batch.sample.dto;

public record CommandeResultDto(CommandeDto commande, boolean succes, String messageErreur) {

    public static CommandeResultDto ok(CommandeDto commande) {
        return new CommandeResultDto(commande, true, null);
    }

    public static CommandeResultDto ko(CommandeDto commande, String messageErreur) {
        return new CommandeResultDto(commande, false, messageErreur);
    }
}
