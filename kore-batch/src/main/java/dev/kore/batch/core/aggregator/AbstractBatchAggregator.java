package dev.kore.batch.core.aggregator;

import dev.kore.batch.core.dto.ISynthese;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Agrégateur générique pour les steps partitionnés.
 * Étendre cette classe, passer un Supplier<T> au constructeur,
 * implémenter {@link #merge(Object, Object)}.
 *
 * @param <T> type de la synthèse (doit implémenter ISynthese)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBatchAggregator<T extends ISynthese> implements StepExecutionAggregator {

    private final Supplier<T> syntheseFactory;

    @Override
    public void aggregate(StepExecution result, Collection<StepExecution> executions) {
        log.debug("Agrégation de {} partitions", executions.size());

        T syntheseGlobale = syntheseFactory.get();

        for (StepExecution stepExecution : executions) {
            @SuppressWarnings("unchecked")
            T synthesePartition = (T) stepExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);
            if (synthesePartition != null) {
                merge(syntheseGlobale, synthesePartition);
            }
        }

        result.getJobExecution().getExecutionContext().put(ISynthese.SYNTHESE_KEY, syntheseGlobale);
        log.info("Synthèse globale : {}", syntheseGlobale);
    }

    protected abstract void merge(T global, T partition);
}
