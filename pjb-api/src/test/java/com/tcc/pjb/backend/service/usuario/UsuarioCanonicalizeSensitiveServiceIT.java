package com.tcc.pjb.backend.service.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Prova o backfill (V344) contra uma linha "legada" real: semeada via SQL nativo com cpf/email em
 * texto puro e cpf_hash/email_hash nulos — exatamente o estado de qualquer linha gravada antes desta
 * migração — nunca via JPA/{@code UsuarioRepository} (carregar essa linha pelo {@code @Convert} já
 * ativo estouraria {@code SecurityException}, que é precisamente o motivo do backfill ler/escrever
 * por SQL nativo).
 */
@TestPropertySource(properties = {
        "pjb.security.master-key=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=",
        "pjb.security.crypto.allow-plaintext-fallback=false"
})
class UsuarioCanonicalizeSensitiveServiceIT extends PjbIntegrationTestBase {

    @Autowired
    private UsuarioCanonicalizeSensitiveService service;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private long seedLinhaLegadaEmTextoPuro(String sufixo, String cpf, String email) {
        jdbc.update(
                "INSERT INTO tb_usuario (nome, email, cpf, senha, tipo_usuario, perfil, ativo) "
                        + "VALUES (?, ?, ?, 'hash-fake', 'ADVOGADO', 'ADVOGADO', true)",
                "Legado " + sufixo, email, cpf);
        return jdbc.queryForObject("SELECT id FROM tb_usuario WHERE cpf = ?", Long.class, cpf);
    }

    @Test
    void backfillCifraEHasheiaLinhaLegadaSemPassarPorJpa() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "66677788" + sufixo.replaceAll("\\D", "5").substring(0, 3);
        String email = "legado." + sufixo + "@teste.local";
        long id = seedLinhaLegadaEmTextoPuro(sufixo, cpf, email);

        UsuarioCanonicalizeSensitiveService.BatchResult resultado =
                service.canonicalizeBatch(id - 1, id, 10, false);

        assertThat(resultado.updated()).isEqualTo(1);

        var raw = jdbc.queryForMap("SELECT cpf, email, cpf_hash, email_hash FROM tb_usuario WHERE id = ?", id);
        assertThat((String) raw.get("cpf")).isNotEqualTo(cpf).doesNotContain(cpf);
        assertThat((String) raw.get("email")).isNotEqualTo(email);
        assertThat((String) raw.get("cpf_hash")).hasSize(64);
        assertThat((String) raw.get("email_hash")).hasSize(64);

        assertThat(usuarioRepository.findByCpf(cpf)).isPresent().get()
                .extracting(u -> u.getId()).isEqualTo(id);
        assertThat(usuarioRepository.findById(id).orElseThrow().getCpf()).isEqualTo(cpf);
    }

    @Test
    void batchVazioNaoAlteraNadaEIndicaConcluido() {
        UsuarioCanonicalizeSensitiveService.BatchResult resultado =
                service.canonicalizeBatch(Long.MAX_VALUE - 2, null, 10, false);

        assertThat(resultado.processed()).isZero();
        assertThat(resultado.done()).isTrue();
    }

    @Test
    void linhaJaProcessadaNaoEReprocessada() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String cpf = "77788899" + sufixo.replaceAll("\\D", "6").substring(0, 3);
        long id = seedLinhaLegadaEmTextoPuro(sufixo, cpf, "reprocessa." + sufixo + "@teste.local");

        service.canonicalizeBatch(id - 1, id, 10, false);
        long countAntes = service.countTotal(id - 1, id);

        assertThat(countAntes).isZero();
    }
}
