package dev.kore.batch.sample.config;

import dev.kore.batch.listener.BatchJobExecutionListener;
import dev.kore.batch.sample.aggregator.CommandeAggregator;
import dev.kore.batch.sample.dto.CommandeDto;
import dev.kore.batch.sample.dto.CommandeResultDto;
import dev.kore.batch.sample.processor.CommandeItemProcessor;
import dev.kore.batch.sample.reader.CommandeItemReader;
import dev.kore.batch.sample.writer.CommandeItemWriter;
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
    private final CommandeAggregator aggregator;
    private final CommandeItemReader reader;
    private final CommandeItemProcessor processor;
    private final CommandeItemWriter writer;

    @Value("${batch.partitioning.grid-size:4}")
    private int gridSize;

    @Bean
    public Job traitementCommandesJob(Step partitionStep) {
        return new JobBuilder("traitementCommandesJob", jobRepository)
                .listener(jobExecutionListener)
                .start(partitionStep)
                .build();
    }

    @Bean
    public Step partitionStep(Step workerStep) {
        return new StepBuilder("traitementCommandes-partition", jobRepository)
                .partitioner("traitementCommandes-worker", new SimplePartitioner())
                .step(workerStep)
                .aggregator(aggregator)
                .taskExecutor(taskExecutor)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step workerStep() {
        return new StepBuilder("traitementCommandes-worker", jobRepository)
                .<CommandeDto, CommandeResultDto>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
