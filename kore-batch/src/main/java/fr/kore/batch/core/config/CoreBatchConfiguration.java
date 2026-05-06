package fr.kore.batch.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration infrastructure commune à tous les batchs.
 * Fournit le ThreadPoolTaskExecutor pour les steps partitionnés.
 */
@Configuration
public class CoreBatchConfiguration {

    @Value("${batch.partitioning.pool-size:4}")
    private int poolSize;

    @Value("${batch.partitioning.queue-capacity:50}")
    private int queueCapacity;

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-partition-");
        executor.initialize();
        return executor;
    }
}
