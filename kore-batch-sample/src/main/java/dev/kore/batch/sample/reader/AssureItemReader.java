package dev.kore.batch.sample.reader;

import dev.kore.batch.sample.dto.AssureDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/**
 * Lit le fichier d'alimentation au format largeur fixe.
 * Le chemin du fichier est passé en paramètre du job (--inputFile=...).
 */
@Slf4j
@Component
@StepScope
public class AssureItemReader implements ItemReader<AssureDto> {

    private FlatFileItemReader<AssureDto> delegate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws Exception {
        String inputFile = stepExecution.getJobParameters().getString("inputFile");
        log.info("Initialisation reader - fichier : {}", inputFile);

        delegate = new FlatFileItemReaderBuilder<AssureDto>()
                .name("assureItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(new AssureLineMapper())
                .encoding("ISO-8859-1")
                .build();

        delegate.open(new ExecutionContext());
        log.info("Reader initialise sur : {}", inputFile);
    }

    @Override
    public AssureDto read() throws Exception {
        return delegate.read();
    }
}
