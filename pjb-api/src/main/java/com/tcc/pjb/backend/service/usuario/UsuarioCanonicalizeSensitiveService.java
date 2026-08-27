package com.tcc.pjb.backend.service.usuario;

import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import com.tcc.pjb.backend.core.security.crypto.UsuarioBlindIndexService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfill de {@code tb_usuario.cpf}/{@code email}: cifra (AES-GCM) e calcula o índice cego
 * ({@code cpf_hash}/{@code email_hash}) das linhas gravadas antes de {@code SensitiveDataConverter}
 * ser ligado nesses campos (V344). Linhas gravadas DEPOIS já saem corretas automaticamente pelo
 * {@code @PrePersist}/{@code @PreUpdate} de {@code Usuario} — este backfill só processa o passivo.
 *
 * <p>Lê e escreve por SQL nativo ({@link JdbcTemplate}), nunca via JPA/{@code UsuarioRepository}:
 * carregar uma linha legada (ainda em texto puro) através do {@code @Convert} já ativo estouraria
 * {@code SecurityException} — o valor bruto não decodifica como o ciphertext AES-GCM esperado. O
 * valor original (formato exato) é preservado; só é cifrado, nunca reformatado — a normalização
 * (dígitos/minúsculas) existe apenas para o hash, igual ao restante do índice cego.</p>
 */
@Service
public class UsuarioCanonicalizeSensitiveService {

    private final JdbcTemplate jdbc;
    private final CryptoVaultService cryptoVaultService;
    private final UsuarioBlindIndexService blindIndex;

    public UsuarioCanonicalizeSensitiveService(JdbcTemplate jdbc,
                                               CryptoVaultService cryptoVaultService,
                                               UsuarioBlindIndexService blindIndex) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.cryptoVaultService = Objects.requireNonNull(cryptoVaultService, "cryptoVaultService");
        this.blindIndex = Objects.requireNonNull(blindIndex, "blindIndex");
    }

    public record BatchResult(long processed, long updated, long lastId, boolean done) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchResult canonicalizeBatch(long afterId, Long untilId, int batchSize, boolean dryRun) {
        int size = Math.max(1, Math.min(batchSize, 2000));
        List<Map<String, Object>> rows = untilId != null
                ? jdbc.queryForList("""
                        SELECT id, cpf, email FROM tb_usuario
                        WHERE id > ? AND id <= ?
                          AND ((cpf IS NOT NULL AND cpf_hash IS NULL) OR (email IS NOT NULL AND email_hash IS NULL))
                        ORDER BY id ASC LIMIT ?
                        """, afterId, untilId, size)
                : jdbc.queryForList("""
                        SELECT id, cpf, email FROM tb_usuario
                        WHERE id > ?
                          AND ((cpf IS NOT NULL AND cpf_hash IS NULL) OR (email IS NOT NULL AND email_hash IS NULL))
                        ORDER BY id ASC LIMIT ?
                        """, afterId, size);

        if (rows.isEmpty()) {
            return new BatchResult(0, 0, afterId, true);
        }

        long processed = 0;
        long updated = 0;
        long lastId = afterId;

        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            lastId = Math.max(lastId, id);
            processed++;

            String rawCpf = (String) row.get("cpf");
            String rawEmail = (String) row.get("email");

            String cpfCifrado = rawCpf != null ? cryptoVaultService.blindarDado(rawCpf) : null;
            String cpfHash = rawCpf != null ? blindIndex.hashCpf(rawCpf) : null;
            String emailCifrado = rawEmail != null ? cryptoVaultService.blindarDado(rawEmail) : null;
            String emailHash = rawEmail != null ? blindIndex.hashEmail(rawEmail) : null;

            if (!dryRun) {
                jdbc.update("UPDATE tb_usuario SET cpf = ?, cpf_hash = ?, email = ?, email_hash = ? WHERE id = ?",
                        cpfCifrado, cpfHash, emailCifrado, emailHash, id);
            }
            updated++;
        }

        return new BatchResult(processed, updated, lastId, false);
    }

    @Transactional(readOnly = true)
    public long countTotal(long afterId, Long untilId) {
        return untilId != null
                ? jdbc.queryForObject("""
                        SELECT COUNT(*) FROM tb_usuario
                        WHERE id > ? AND id <= ?
                          AND ((cpf IS NOT NULL AND cpf_hash IS NULL) OR (email IS NOT NULL AND email_hash IS NULL))
                        """, Long.class, afterId, untilId)
                : jdbc.queryForObject("""
                        SELECT COUNT(*) FROM tb_usuario
                        WHERE id > ?
                          AND ((cpf IS NOT NULL AND cpf_hash IS NULL) OR (email IS NOT NULL AND email_hash IS NULL))
                        """, Long.class, afterId);
    }
}
