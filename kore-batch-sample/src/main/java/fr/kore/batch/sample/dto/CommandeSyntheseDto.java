package fr.kore.batch.sample.dto;

import fr.kore.batch.core.dto.SyntheseDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Synthèse métier spécifique au batch commandes.
 * Étend SyntheseDto du socle et ajoute les données métier.
 */
public class CommandeSyntheseDto extends SyntheseDto {

    private final List<String> referencesEnErreur = new ArrayList<>();

    public void addReferenceEnErreur(String reference) {
        referencesEnErreur.add(reference);
    }

    public List<String> getReferencesEnErreur() {
        return referencesEnErreur;
    }

    public void merge(CommandeSyntheseDto other) {
        super.merge(other);
        if (other != null) {
            this.referencesEnErreur.addAll(other.referencesEnErreur);
        }
    }
}
