package com.tcc.pjb.backend.core.processo.carga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.processo.CargaProcesso;
import com.tcc.pjb.backend.model.repository.CargaProcessoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class CargaProcessoRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private CargaProcessoRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;
    private Long responsavelId;
    private Long unidadeId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        responsavelId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'SERVIDOR', 'hash', ?, true, 'SERVIDOR') RETURNING id",
                Long.class, "Srv " + s, "srv.cg." + s + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s, 16)));
        unidadeId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_codigo, tipo_vara) " +
                "VALUES (?, 'TJCE', 'CRIMINAL') RETURNING id",
                Long.class, "VARA-CG-" + s);
        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'DISTRIBUIDO') RETURNING id",
                Long.class, "0001004-" + s + ".2025.8.06.0001");
    }

    private CargaProcesso criarCargaAtiva() {
        return transactionTemplate.execute(st ->
                repository.save(new CargaProcesso(processoId, responsavelId, unidadeId,
                        "CONCLUSO_GABINETE", null, 1, null)));
    }

    @Test
    void it01_salvarCargaAtivaRetornaNoFindCargaAtiva() {
        criarCargaAtiva();

        Optional<CargaProcesso> result = repository.findCargaAtivaByProcessoId(processoId);
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("ATIVA");
        assertThat(result.get().getTipoCarga()).isEqualTo("CONCLUSO_GABINETE");
    }

    @Test
    void it02_registrarRetornoPermiteNovaCargaAtiva() {
        CargaProcesso carga = criarCargaAtiva();
        transactionTemplate.execute(st -> {
            CargaProcesso c = repository.findById(carga.getId()).orElseThrow();
            c.registrarRetorno();
            return repository.save(c);
        });

        assertThat(repository.findCargaAtivaByProcessoId(processoId)).isEmpty();

        transactionTemplate.execute(st ->
                repository.save(new CargaProcesso(processoId, responsavelId, unidadeId,
                        "VISTA_PARTE", null, 2, null)));
        assertThat(repository.findCargaAtivaByProcessoId(processoId)).isPresent();
    }

    @Test
    void it03_segundaCargaAtivaLancaConstraintViolation() {
        criarCargaAtiva();

        assertThatThrownBy(() -> transactionTemplate.execute(st -> {
            repository.save(new CargaProcesso(processoId, responsavelId, unidadeId,
                    "VISTA_MP", null, 1, null));
            repository.flush();
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void it04_registrarExtravioAlteraStatusParaExtraviada() {
        CargaProcesso carga = criarCargaAtiva();
        Long cargaId = carga.getId();

        transactionTemplate.execute(st -> {
            CargaProcesso c = repository.findById(cargaId).orElseThrow();
            c.registrarExtravio("Processo não localizado no arquivo");
            return repository.save(c);
        });

        CargaProcesso atualizada = repository.findById(cargaId).orElseThrow();
        assertThat(atualizada.getStatus()).isEqualTo("EXTRAVIADA");
        assertThat(atualizada.getObservacao()).isEqualTo("Processo não localizado no arquivo");
    }

    @Test
    void it05_findByProcessoIdOrdenadoPorDataSaida() {
        criarCargaAtiva();
        CargaProcesso c1 = repository.findCargaAtivaByProcessoId(processoId).orElseThrow();
        transactionTemplate.execute(st -> {
            CargaProcesso c = repository.findById(c1.getId()).orElseThrow();
            c.registrarRetorno();
            return repository.save(c);
        });
        criarCargaAtiva();

        List<CargaProcesso> todas = repository.findByProcessoIdOrderByDataSaidaAsc(processoId);
        assertThat(todas).hasSize(2);
        assertThat(todas.get(0).getDataSaida()).isBeforeOrEqualTo(todas.get(1).getDataSaida());
    }
}
