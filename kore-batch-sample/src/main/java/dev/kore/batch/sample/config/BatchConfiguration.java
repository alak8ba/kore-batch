package dev.kore.batch.sample.config;

import dev.kore.batch.error.TechnicalException;
import dev.kore.batch.listener.BatchJobExecutionListener;
import dev.kore.batch.validator.InputFileValidator;
import dev.kore.batch.sample.aggregator.IndividuAggregator;
import dev.kore.batch.sample.dto.AssureDto;
import dev.kore.batch.sample.dto.AssureResultDto;
import dev.kore.batch.sample.listener.TraitementAssuresStepListener;
import dev.kore.batch.sample.processor.AssureItemProcessor;
import dev.kore.batch.sample.reader.AssureItemReader;
import dev.kore.batch.sample.writer.AssureItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final BatchJobExecutionListener jobExecutionListener;
    private final IndividuAggregator aggregator;
    private final AssureItemReader reader;
    private final AssureItemProcessor processor;
    private final AssureItemWriter writer;
    private final TraitementAssuresStepListener stepListener;

    @Value("${batch.partitioning.grid-size:4}")
    private int gridSize;

    /**
     * Taille du chunk : 10 items par commit.
     * Choix justifié : items légers (validation + log), fichier de taille moyenne.
     * Augmenter si gros volume sans traitement complexe (ex: 100-500).
     * Réduire à 1 si rollback précis par item requis.
     */
    @Value("${batch.chunk-size:10}")
    private int chunkSize;

    @Bean
    public Job traitementIndividusJob(Step partitionStep) {
        return new JobBuilder("traitementIndividusJob", jobRepository)
                .listener(jobExecutionListener)
                .validator(new InputFileValidator())
                .start(partitionStep)
                .build();
    }

    @Bean
    public Step partitionStep(Step workerStep) {
        return new StepBuilder("traitementIndividus-partition", jobRepository)
                .partitioner("traitementIndividus-worker", new SimplePartitioner())
                .step(workerStep)
                .aggregator(aggregator)
                .taskExecutor(taskExecutor)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("traitementIndividus-worker", jobRepository)
                .<AssureDto, AssureResultDto>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(stepListener)
                // Pattern fonctionnel : FunctionalException catchee dans le processor (ADR-006)
                // TechnicalException non catchee -> arret immediat du batch
                .faultTolerant()
                .noSkip(TechnicalException.class)
                .build();
    }
}
