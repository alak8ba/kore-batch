package dev.kore.batch.sample.writer;

import dev.kore.batch.sample.dto.AssureResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Persiste les individus traités dans T_INDIVIDU.
 * Insère aussi bien les OK que les KO (avec statut et message d'erreur).
 * Utilise un upsert PostgreSQL pour eviter les doublons sur NUM_REFERENCE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssureItemWriter implements ItemWriter<AssureResultDto> {

    private final DataSource dataSource;

    private static final String UPSERT_SQL = """
            INSERT INTO T_INDIVIDU
                (NUM_REFERENCE, TYPE_REFERENCE, IDENTIFIANT, CIVILITE, NOM_PRENOM,
                 ADRESSE_LIGNE1, ADRESSE_LIGNE2, CODE_PAYS, PAYS, DATE_REFERENCE,
                 STATUT, DATE_TRAITEMENT, MESSAGE_ERREUR)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (NUM_REFERENCE) DO UPDATE SET
                STATUT          = EXCLUDED.STATUT,
                DATE_TRAITEMENT = EXCLUDED.DATE_TRAITEMENT,
                MESSAGE_ERREUR  = EXCLUDED.MESSAGE_ERREUR
            """;

    @Override
    public void write(Chunk<? extends AssureResultDto> chunk) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // batchUpdate avec ParameterizedPreparedStatementSetter retourne int[][]
        // (un int[] par batch de chunkSize items)
        int[][] counts = jdbc.batchUpdate(UPSERT_SQL, chunk.getItems(), chunk.size(),
            (ps, result) -> {
                var assure = result.assure();
                ps.setString(1,  assure.getReference());
                ps.setString(2,  assure.getTypeFlux());
                ps.setString(3,  assure.getIdentifiant());
                ps.setString(4,  assure.getCivilite());
                ps.setString(5,  assure.getNomPrenom());
                ps.setString(6,  assure.getAdresseLigne1());
                ps.setString(7,  assure.getAdresseLigne2());
                ps.setString(8,  assure.getCodePays());
                ps.setString(9,  assure.getPays());
                ps.setString(10, assure.getDateReference());
                ps.setString(11, result.succes() ? "OK" : "KO");
                ps.setTimestamp(12, now);
                ps.setString(13, result.messageErreur());
            });

        int total = java.util.Arrays.stream(counts).mapToInt(b -> b.length).sum();
        log.info("{} individus persistes en base", total);
    }
}
