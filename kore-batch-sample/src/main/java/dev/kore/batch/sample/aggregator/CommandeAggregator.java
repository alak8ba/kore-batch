package dev.kore.batch.sample.aggregator;

import dev.kore.batch.aggregator.AbstractBatchAggregator;
import dev.kore.batch.sample.dto.CommandeSyntheseDto;
import org.springframework.stereotype.Component;

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
