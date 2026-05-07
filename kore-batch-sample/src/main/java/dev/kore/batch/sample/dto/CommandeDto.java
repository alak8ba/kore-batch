package dev.kore.batch.sample.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandeDto {
    private String reference;
    private String clientId;
    private BigDecimal montant;
    private LocalDate dateCommande;
    private String statut;
}
