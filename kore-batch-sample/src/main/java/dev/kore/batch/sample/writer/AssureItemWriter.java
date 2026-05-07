package dev.kore.batch.sample.writer;

import dev.kore.batch.sample.dto.AssureResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste les individus traités.
 * En production : utiliser JpaItemWriter ou JdbcBatchItemWriter.
 */
@Slf4j
@Component
public class AssureItemWriter implements ItemWriter<AssureResultDto> {

    @Override
    public void write(Chunk<? extends AssureResultDto> chunk) {
        for (AssureResultDto result : chunk) {
            if (result.succes()) {
                log.info("Individu traite : num={} NIR={} pays={}",
                    result.assure().getNumPension(),
                    result.assure().getNir(),
                    result.assure().getPays());
            } else {
                log.warn("Individu en erreur : num={} - {}",
                    result.assure().getNumPension(),
                    result.messageErreur());
            }
        }
    }
}
