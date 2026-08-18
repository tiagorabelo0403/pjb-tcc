package com.tcc.pjb.backend.core.processo.estado;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.processo.ProcessoEstadoLog;
import com.tcc.pjb.backend.model.repository.ProcessoEstadoLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class ProcessoEstadoLogRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private ProcessoEstadoLogRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        usuarioId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'SERVIDOR', 'hash', ?, true, 'SERVIDOR') RETURNING id",
                Long.class, "Servidor " + s, "srv.el." + s + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s, 16)));
        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'DISTRIBUIDO') RETURNING id",
                Long.class, "0001001-" + s + ".2025.8.06.0001");
    }

    @Test
    void it01_salvarLogRetornaComEstadoNovo() {
        transactionTemplate.execute(st -> repository.save(
                new ProcessoEstadoLog(processoId, StatusProcesso.DISTRIBUIDO,
                        StatusProcesso.CONCLUSO_JUIZ, usuarioId, "Conclusão IT")));

        List<ProcessoEstadoLog> logs = repository.findByProcessoIdOrderByCreatedAtAsc(processoId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEstadoNovo()).isEqualTo(StatusProcesso.CONCLUSO_JUIZ);
        assertThat(logs.get(0).getMotivo()).isEqualTo("Conclusão IT");
    }

    @Test
    void it02_doisLogsOrdenadosPorCreatedAtAscendente() {
        transactionTemplate.execute(st -> {
            repository.save(new ProcessoEstadoLog(processoId,
                    StatusProcesso.DISTRIBUIDO, StatusProcesso.CONCLUSO_JUIZ, usuarioId, "Passo 1"));
            repository.save(new ProcessoEstadoLog(processoId,
                    StatusProcesso.CONCLUSO_JUIZ, StatusProcesso.DISTRIBUIDO, usuarioId, "Passo 2"));
            return null;
        });

        List<ProcessoEstadoLog> logs = repository.findByProcessoIdOrderByCreatedAtAsc(processoId);
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getMotivo()).isEqualTo("Passo 1");
        assertThat(logs.get(1).getMotivo()).isEqualTo("Passo 2");
    }

    @Test
    void it03_logDeOutroProcessoNaoRetornaNaConsulta() {
        String s2 = UUID.randomUUID().toString().substring(0, 8);
        Long outroProcessoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) VALUES (?, 'DISTRIBUIDO') RETURNING id",
                Long.class, "9999999-" + s2 + ".2025.8.06.0001");

        transactionTemplate.execute(st -> repository.save(
                new ProcessoEstadoLog(outroProcessoId, null, StatusProcesso.DISTRIBUIDO, usuarioId, "Outro")));

        List<ProcessoEstadoLog> logs = repository.findByProcessoIdOrderByCreatedAtAsc(processoId);
        assertThat(logs).isEmpty();
    }

    @Test
    void it04_estadoAnteriorNullPermitidoNaPrimeiraTransicao() {
        transactionTemplate.execute(st -> repository.save(
                new ProcessoEstadoLog(processoId, null, StatusProcesso.AUTUADO, usuarioId, "Autuação inicial")));

        List<ProcessoEstadoLog> logs = repository.findByProcessoIdOrderByCreatedAtAsc(processoId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getEstadoAnterior()).isNull();
        assertThat(logs.get(0).getEstadoNovo()).isEqualTo(StatusProcesso.AUTUADO);
    }
}
