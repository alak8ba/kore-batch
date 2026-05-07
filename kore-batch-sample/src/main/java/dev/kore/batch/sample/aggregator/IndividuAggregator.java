package dev.kore.batch.sample.aggregator;

import dev.kore.batch.aggregator.AbstractBatchAggregator;
import dev.kore.batch.sample.dto.IndividuSyntheseDto;
import org.springframework.stereotype.Component;

@Component
public class IndividuAggregator extends AbstractBatchAggregator<IndividuSyntheseDto> {

    public IndividuAggregator() {
        super(IndividuSyntheseDto::new);
    }

    @Override
    protected void merge(IndividuSyntheseDto global, IndividuSyntheseDto partition) {
        global.merge(partition);
    }
}
