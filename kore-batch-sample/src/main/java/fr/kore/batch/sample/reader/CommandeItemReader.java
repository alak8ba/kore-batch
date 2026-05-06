package fr.kore.batch.sample.reader;

import fr.kore.batch.sample.dto.CommandeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reader qui charge les données à traiter.
 *
 * En production : remplacer par FlatFileItemReader (CSV) ou JdbcPagingItemReader (BDD).
 * La logique thread-safe avec AtomicInteger est nécessaire pour le partitioning.
 */
@Component
public class CommandeItemReader implements ItemReader<CommandeDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandeItemReader.class);

    private List<CommandeDto> commandes;
    private final AtomicInteger index = new AtomicInteger(0);

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String inputFile = stepExecution.getJobParameters().getString("inputFile", "default");
        LOG.info("Initialisation du reader - fichier : {}", inputFile);
        this.commandes = chargerCommandes(inputFile);
        this.index.set(0);
        LOG.info("{} commandes chargées", commandes.size());
    }

    @Override
    public CommandeDto read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        int i = index.getAndIncrement();
        if (i < commandes.size()) {
            return commandes.get(i);
        }
        return null; // null = fin de lecture pour Spring Batch
    }

    /**
     * Simule le chargement depuis un fichier CSV ou une base de données.
     * Remplacer par FlatFileItemReader ou JdbcPagingItemReader en production.
     */
    private List<CommandeDto> chargerCommandes(String source) {
        List<CommandeDto> data = new ArrayList<>();
        data.add(new CommandeDto("CMD-001", "CLIENT-A", new BigDecimal("150.00"), LocalDate.now(), "VALIDE"));
        data.add(new CommandeDto("CMD-002", "CLIENT-B", new BigDecimal("75.50"),  LocalDate.now(), "VALIDE"));
        data.add(new CommandeDto("CMD-003", "",         new BigDecimal("200.00"), LocalDate.now(), "VALIDE")); // clientId manquant -> erreur fonctionnelle
        data.add(new CommandeDto("CMD-004", "CLIENT-D", new BigDecimal("-10.00"), LocalDate.now(), "VALIDE")); // montant négatif -> erreur fonctionnelle
        data.add(new CommandeDto("CMD-005", "CLIENT-E", new BigDecimal("300.00"), LocalDate.now(), "VALIDE"));
        return data;
    }
}
