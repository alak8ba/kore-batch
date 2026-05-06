package fr.kore.batch.sample.processor;

import fr.kore.batch.core.dto.ISynthese;
import fr.kore.batch.core.error.FunctionalException;
import fr.kore.batch.sample.dto.CommandeDto;
import fr.kore.batch.sample.dto.CommandeResultDto;
import fr.kore.batch.sample.dto.CommandeSyntheseDto;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Processor : applique les règles métier sur chaque commande.
 *
 * Analogie avec ConstitutionFluxAlimentationProcessor dans MCE.
 * Les erreurs fonctionnelles sont catchées et comptabilisées.
 * Les erreurs techniques remontent et font échouer le step.
 */
@Component
public class CommandeItemProcessor implements ItemProcessor<CommandeDto, CommandeResultDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandeItemProcessor.class);

    private CommandeSyntheseDto synthese;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.synthese = new CommandeSyntheseDto();
        stepExecution.getExecutionContext().put(ISynthese.SYNTHESE_KEY, synthese);
    }

    @Override
    public CommandeResultDto process(CommandeDto commande) {
        LOG.debug("Traitement de la commande {}", commande.getReference());

        try {
            valider(commande);
            enrichir(commande);
            synthese.incrementOK();
            return new CommandeResultDto(commande, true);

        } catch (FunctionalException e) {
            LOG.warn("Erreur fonctionnelle [{}] : {}",
                StructuredArguments.value("reference", commande.getReference()),
                e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addReferenceEnErreur(commande.getReference());
            return new CommandeResultDto(commande, e.getMessage());
        }
    }

    private void valider(CommandeDto commande) throws FunctionalException {
        if (commande.getClientId() == null || commande.getClientId().isBlank()) {
            throw new FunctionalException(
                getClass().getSimpleName(), "valider",
                "ClientId obligatoire - ref=" + commande.getReference()
            );
        }
        if (commande.getMontant() == null || commande.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FunctionalException(
                getClass().getSimpleName(), "valider",
                "Montant invalide (doit être > 0) - ref=" + commande.getReference()
            );
        }
    }

    private void enrichir(CommandeDto commande) {
        // Exemple d'enrichissement : appel service, lookup BDD, etc.
        commande.setStatut("TRAITE");
    }
}
