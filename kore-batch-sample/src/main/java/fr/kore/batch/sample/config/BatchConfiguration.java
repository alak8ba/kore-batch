package fr.kore.batch.sample.config;

import fr.kore.batch.core.listener.BatchJobExecutionListener;
import fr.kore.batch.sample.aggregator.CommandeAggregator;
import fr.kore.batch.sample.dto.CommandeDto;
import fr.kore.batch.sample.dto.CommandeResultDto;
import fr.kore.batch.sample.processor.CommandeItemProcessor;
import fr.kore.batch.sample.reader.CommandeItemReader;
import fr.kore.batch.sample.writer.CommandeItemWriter;
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

/**
 * Configuration du job Spring Batch 5.
 *
 * Changements vs Spring Batch 4 :
 *  - Plus de JobBuilderFactory / StepBuilderFactory (dépréciés et supprimés)
 *  - JobBuilder et StepBuilder prennent directement le JobRepository
 *  - @EnableBatchProcessing n'est plus nécessaire avec Spring Boot 3 (auto-configuré)
 */
@Configuration
public class BatchConfiguration {

    @Value("${batch.partitioning.grid-size:4}")
    private int gridSize;

    @Bean
    public Job traitementCommandesJob(
            JobRepository jobRepository,
            BatchJobExecutionListener jobExecutionListener,
            Step partitionStep) {

        return new JobBuilder("traitementCommandesJob", jobRepository)
                .listener(jobExecutionListener)
                .start(partitionStep)
                .build();
    }

    @Bean
    public Step partitionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ThreadPoolTaskExecutor taskExecutor,
            CommandeAggregator aggregator,
            Step workerStep) {

        return new StepBuilder("traitementCommandes-partition", jobRepository)
                .partitioner("traitementCommandes-worker", new SimplePartitioner())
                .step(workerStep)
                .aggregator(aggregator)
                .taskExecutor(taskExecutor)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step workerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            CommandeItemReader reader,
            CommandeItemProcessor processor,
            CommandeItemWriter writer) {

        return new StepBuilder("traitementCommandes-worker", jobRepository)
                .<CommandeDto, CommandeResultDto>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
