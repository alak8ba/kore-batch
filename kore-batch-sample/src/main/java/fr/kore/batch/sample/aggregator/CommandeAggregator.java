package fr.kore.batch.sample.aggregator;

import fr.kore.batch.core.aggregator.AbstractBatchAggregator;
import fr.kore.batch.sample.dto.CommandeSyntheseDto;
import org.springframework.stereotype.Component;

/**
 * Agrège les synthèses des partitions en une synthèse globale.
 * Analogie avec ConstitutionFluxAlimentationAggregator dans MCE.
 */
@Component
public class CommandeAggregator extends AbstractBatchAggregator<CommandeSyntheseDto> {

    public CommandeAggregator() {
        super(CommandeSyntheseDto::new);
    }

    @Override
    protected void merge(CommandeSyntheseDto global, CommandeSyntheseDto partition) {
        global.merge(partition);
    }
}
