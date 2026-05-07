package dev.kore.batch.sample.dto;

import dev.kore.batch.dto.SyntheseDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandeSyntheseDto extends SyntheseDto {

    private final List<String> referencesEnErreur = new ArrayList<>();

    public void addReferenceEnErreur(String reference) {
        referencesEnErreur.add(reference);
    }

    public void merge(CommandeSyntheseDto other) {
        super.merge(other);
        if (other != null) {
            referencesEnErreur.addAll(other.referencesEnErreur);
        }
    }
}
