package dev.kore.batch.sample.health;

import dev.kore.batch.health.AbstractWSHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Vérifie que le répertoire source des fichiers d'alimentation est accessible.
 * Exemple d'implémentation de AbstractWSHealthIndicator pour une ressource externe.
 *
 * En production : remplacer par un vrai ping HTTP/WS vers le service fournisseur.
 */
@Slf4j
@Component
public class FichierSourceHealthIndicator extends AbstractWSHealthIndicator {

    @Value("${batch.health.source-directory:${java.io.tmpdir}}")
    private String sourceDirectory;

    @Override
    protected String getName() {
        return "FICHIER_SOURCE";
    }

    @Override
    protected boolean ping() {
        File dir = new File(sourceDirectory);
        boolean accessible = dir.exists() && dir.isDirectory() && dir.canRead();
        if (!accessible) {
            log.warn("Repertoire source inaccessible : {}", sourceDirectory);
        }
        return accessible;
    }
}
