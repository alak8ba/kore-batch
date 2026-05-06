package fr.kore.batch.sample.dto;

/**
 * Résultat du traitement d'une commande par le processor.
 */
public class CommandeResultDto {

    private CommandeDto commande;
    private boolean succes;
    private String messageErreur;

    public CommandeResultDto(CommandeDto commande, boolean succes) {
        this.commande = commande;
        this.succes = succes;
    }

    public CommandeResultDto(CommandeDto commande, String messageErreur) {
        this.commande = commande;
        this.succes = false;
        this.messageErreur = messageErreur;
    }

    public CommandeDto getCommande() { return commande; }
    public boolean isSucces() { return succes; }
    public String getMessageErreur() { return messageErreur; }
}
