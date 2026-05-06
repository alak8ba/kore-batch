package fr.kore.batch.sample.writer;

import fr.kore.batch.sample.dto.CommandeResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer : persiste les résultats (BDD, fichier, appel WS...).
 * Ici on simule avec un simple log.
 */
@Component
public class CommandeItemWriter implements ItemWriter<CommandeResultDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandeItemWriter.class);

    @Override
    public void write(Chunk<? extends CommandeResultDto> chunk) {
        for (CommandeResultDto result : chunk) {
            if (result.isSucces()) {
                LOG.info("Commande persistée : {}", result.getCommande().getReference());
                // En production : entityManager.persist(...) ou jdbcTemplate.update(...)
            } else {
                LOG.warn("Commande en erreur ignorée : {} - {}",
                    result.getCommande().getReference(),
                    result.getMessageErreur());
            }
        }
    }
}
