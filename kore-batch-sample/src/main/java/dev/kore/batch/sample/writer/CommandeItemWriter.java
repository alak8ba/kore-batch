package dev.kore.batch.sample.writer;

import dev.kore.batch.sample.dto.CommandeResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandeItemWriter implements ItemWriter<CommandeResultDto> {

    @Override
    public void write(Chunk<? extends CommandeResultDto> chunk) {
        for (CommandeResultDto result : chunk) {
            if (result.succes()) {
                log.info("Commande persistée : {}", result.commande().getReference());
            } else {
                log.warn("Commande en erreur : {} - {}", result.commande().getReference(), result.messageErreur());
            }
        }
    }
}
