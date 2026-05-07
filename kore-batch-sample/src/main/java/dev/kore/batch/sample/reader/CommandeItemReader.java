package dev.kore.batch.sample.reader;

import dev.kore.batch.sample.dto.CommandeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reader thread-safe pour le partitioning.
 * En production : remplacer par FlatFileItemReader ou JdbcPagingItemReader.
 */
@Slf4j
@Component
public class CommandeItemReader implements ItemReader<CommandeDto> {

    private List<CommandeDto> commandes;
    private final AtomicInteger index = new AtomicInteger(0);

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String inputFile = stepExecution.getJobParameters().getString("inputFile", "default");
        log.info("Initialisation reader - source : {}", inputFile);
        commandes = chargerCommandes(inputFile);
        index.set(0);
        log.info("{} commandes chargées", commandes.size());
    }

    @Override
    public CommandeDto read() {
        int i = index.getAndIncrement();
        return i < commandes.size() ? commandes.get(i) : null;
    }

    private List<CommandeDto> chargerCommandes(String source) {
        return new ArrayList<>(List.of(
            CommandeDto.builder().reference("CMD-001").clientId("CLIENT-A").montant(new BigDecimal("150.00")).dateCommande(LocalDate.now()).statut("VALIDE").build(),
            CommandeDto.builder().reference("CMD-002").clientId("CLIENT-B").montant(new BigDecimal("75.50")).dateCommande(LocalDate.now()).statut("VALIDE").build(),
            CommandeDto.builder().reference("CMD-003").clientId("").montant(new BigDecimal("200.00")).dateCommande(LocalDate.now()).statut("VALIDE").build(),
            CommandeDto.builder().reference("CMD-004").clientId("CLIENT-D").montant(new BigDecimal("-10.00")).dateCommande(LocalDate.now()).statut("VALIDE").build(),
            CommandeDto.builder().reference("CMD-005").clientId("CLIENT-E").montant(new BigDecimal("300.00")).dateCommande(LocalDate.now()).statut("VALIDE").build()
        ));
    }
}
