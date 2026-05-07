package dev.kore.batch.sample.processor;

import dev.kore.batch.dto.ISynthese;
import dev.kore.batch.error.FunctionalException;
import dev.kore.batch.sample.dto.CommandeDto;
import dev.kore.batch.sample.dto.CommandeResultDto;
import dev.kore.batch.sample.dto.CommandeSyntheseDto;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@StepScope
public class CommandeItemProcessor implements ItemProcessor<CommandeDto, CommandeResultDto> {

    private CommandeSyntheseDto synthese;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        synthese = new CommandeSyntheseDto();
        stepExecution.getExecutionContext().put(ISynthese.SYNTHESE_KEY, synthese);
    }

    @Override
    public CommandeResultDto process(CommandeDto commande) {
        log.debug("Traitement commande {}", commande.getReference());
        try {
            valider(commande);
            enrichir(commande);
            synthese.incrementOK();
            return CommandeResultDto.ok(commande);

        } catch (FunctionalException e) {
            log.warn("Erreur fonctionnelle [{}] : {}",
                StructuredArguments.value("reference", commande.getReference()),
                e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addReferenceEnErreur(commande.getReference());
            return CommandeResultDto.ko(commande, e.getMessage());
        }
    }

    private void valider(CommandeDto commande) throws FunctionalException {
        if (commande.getClientId() == null || commande.getClientId().isBlank()) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "ClientId obligatoire - ref=" + commande.getReference());
        }
        if (commande.getMontant() == null || commande.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FunctionalException(getClass().getSimpleName(), "valider",
                "Montant invalide (doit etre > 0) - ref=" + commande.getReference());
        }
    }

    private void enrichir(CommandeDto commande) {
        commande.setStatut("TRAITE");
    }
}
