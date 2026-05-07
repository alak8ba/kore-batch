package dev.kore.batch.sample.dto;

import dev.kore.batch.dto.SyntheseDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class IndividuSyntheseDto extends SyntheseDto {

    private final List<String> numerosEnErreur = new ArrayList<>();

    public void addNumeroEnErreur(String numero) {
        numerosEnErreur.add(numero);
    }

    public void merge(IndividuSyntheseDto other) {
        super.merge(other);
        if (other != null) {
            numerosEnErreur.addAll(other.numerosEnErreur);
        }
    }
}
