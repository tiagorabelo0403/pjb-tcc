package com.tcc.pjb.backend.core.processo.conclusao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import com.tcc.pjb.backend.model.repository.ConclusaoProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class ConclusaoProcessualRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private ConclusaoProcessualRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;
    private Long magistradoId;
    private Long servidorId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        magistradoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'MAGISTRADO', 'hash', ?, true, 'MAGISTRADO') RETURNING id",
                Long.class, "Juiz " + s, "juiz.cp." + s + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s, 16)));
        String s2 = UUID.randomUUID().toString().substring(0, 8);
        servidorId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'SERVIDOR', 'hash', ?, true, 'SERVIDOR') RETURNING id",
                Long.class, "Srv " + s2, "srv.cp." + s2 + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s2, 16)));
        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'DISTRIBUIDO') RETURNING id",
                Long.class, "0001002-" + s + ".2025.8.06.0001");
    }

    private ConclusaoProcessual criarPendente(Instant dataLimite) {
        return transactionTemplate.execute(st -> repository.save(
                new ConclusaoProcessual(processoId, magistradoId, servidorId,
                        "PARA_DECISAO", null, dataLimite)));
    }

    @Test
    void it01_salvarPendenteRetornaNoFindByMagistrado() {
        criarPendente(Instant.now().plus(10, ChronoUnit.DAYS));

        List<ConclusaoProcessual> pendentes = repository.findByMagistradoIdAndStatus(magistradoId, "PENDENTE");
        assertThat(pendentes).isNotEmpty();
        assertThat(pendentes.get(0).getProcessoId()).isEqualTo(processoId);
    }

    @Test
    void it02_findExpiradasRetornaApenasDataLimitePassada() {
        criarPendente(Instant.now().minus(1, ChronoUnit.HOURS));
        criarPendente(Instant.now().plus(10, ChronoUnit.DAYS));

        List<ConclusaoProcessual> expiradas = repository.findExpiradas(Instant.now(), PageRequest.of(0, 50));
        assertThat(expiradas).isNotEmpty();
        expiradas.forEach(c -> assertThat(c.getDataLimite()).isBefore(Instant.now()));
    }

    @Test
    void it03_devolverAlteraStatusEPreencheDataDevolucao() {
        ConclusaoProcessual c = criarPendente(Instant.now().plus(5, ChronoUnit.DAYS));
        Long cId = c.getId();

        transactionTemplate.execute(st -> {
            ConclusaoProcessual carregada = repository.findById(cId).orElseThrow();
            carregada.devolver();
            return repository.save(carregada);
        });

        ConclusaoProcessual atualizada = repository.findById(cId).orElseThrow();
        assertThat(atualizada.getStatus()).isEqualTo("DEVOLVIDA");
        assertThat(atualizada.getDataDevolucao()).isNotNull();
    }

    @Test
    void it04_findExpiradasNaoRetornaDevolvidas() {
        ConclusaoProcessual c = criarPendente(Instant.now().minus(1, ChronoUnit.HOURS));
        transactionTemplate.execute(st -> {
            ConclusaoProcessual carregada = repository.findById(c.getId()).orElseThrow();
            carregada.devolver();
            return repository.save(carregada);
        });

        List<ConclusaoProcessual> expiradas = repository.findExpiradas(Instant.now(), PageRequest.of(0, 50));
        assertThat(expiradas).noneMatch(e -> e.getId().equals(c.getId()));
    }
}
