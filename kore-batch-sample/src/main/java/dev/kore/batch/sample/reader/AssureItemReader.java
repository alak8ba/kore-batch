package dev.kore.batch.sample.reader;

import dev.kore.batch.sample.dto.AssureDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * Reader thread-safe pour le partitioning via SynchronizedItemStreamReader.
 *
 * Pourquoi SynchronizedItemStreamReader et pas @StepScope ?
 * - @StepScope cree une instance PAR partition -> chaque worker lit le fichier entier
 *   -> avec gridSize=4 et 200 lignes : total = 800 (doublon x4)
 * - SynchronizedItemStreamReader = UN seul reader partage entre toutes les partitions
 *   -> synchronise les appels read() de facon thread-safe
 *   -> chaque item est lu UNE seule fois et dispatche a UN seul worker
 *   -> gridSize=4 + 200 lignes = 200 items traites, vrai parallelisme
 *
 * C'est la solution recommandee Spring Batch 5 pour remplacer synchronized.
 * (ADR-004 : @StepScope reste la solution pour les readers AVEC etat propre par partition)
 */
@Slf4j
@Component
public class AssureItemReader implements ItemReader<AssureDto> {

    private SynchronizedItemStreamReader<AssureDto> synchronizedReader;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws Exception {
        String inputFile = stepExecution.getJobParameters().getString("inputFile");
        log.info("Initialisation reader - fichier : {}", inputFile);

        FlatFileItemReader<AssureDto> delegate = new FlatFileItemReaderBuilder<AssureDto>()
                .name("assureItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(new AssureLineMapper())
                .encoding("ISO-8859-1")
                .build();

        synchronizedReader = new SynchronizedItemStreamReader<>();
        synchronizedReader.setDelegate(delegate);
        synchronizedReader.open(new ExecutionContext());

        log.info("SynchronizedItemStreamReader initialise sur : {}", inputFile);
    }

    @Override
    public AssureDto read() throws Exception {
        return synchronizedReader.read();
    }
}
