package fr.kore.batch.core.aggregator;

import fr.kore.batch.core.dto.ISynthese;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Agrégateur générique pour les steps partitionnés.
 *
 * Utilisation : étendre cette classe, passer un Supplier<T> au constructeur,
 * implémenter {@link #merge(Object, Object)}.
 *
 * @param <T> type de la synthèse (doit implémenter ISynthese)
 */
public abstract class AbstractBatchAggregator<T extends ISynthese> implements StepExecutionAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBatchAggregator.class);

    private final Supplier<T> syntheseFactory;

    protected AbstractBatchAggregator(Supplier<T> syntheseFactory) {
        this.syntheseFactory = syntheseFactory;
    }

    @Override
    public void aggregate(StepExecution result, Collection<StepExecution> executions) {
        LOG.debug("Agrégation de {} partitions", executions.size());

        T syntheseGlobale = syntheseFactory.get();

        for (StepExecution stepExecution : executions) {
            @SuppressWarnings("unchecked")
            T synthesePartition = (T) stepExecution.getExecutionContext().get(ISynthese.SYNTHESE_KEY);
            if (synthesePartition != null) {
                merge(syntheseGlobale, synthesePartition);
            }
        }

        result.getJobExecution().getExecutionContext().put(ISynthese.SYNTHESE_KEY, syntheseGlobale);
        LOG.info("Synthèse globale après agrégation : {}", syntheseGlobale);
    }

    /**
     * Fusionne la synthèse d'une partition dans la synthèse globale.
     *
     * @param global    synthèse globale à enrichir
     * @param partition synthèse de la partition à fusionner
     */
    protected abstract void merge(T global, T partition);
}
