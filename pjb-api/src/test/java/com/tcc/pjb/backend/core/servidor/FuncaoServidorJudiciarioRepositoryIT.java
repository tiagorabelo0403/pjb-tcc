package com.tcc.pjb.backend.core.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbTransactionalRepositoryItBase;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Repo IT — rollback automático herdado de PjbTransactionalRepositoryItBase.
 * Padrão válido para ITs de repositório que não commitam (flush() dispara constraint dentro do TX).
 * Não aplicável a flow ITs com requisição HTTP real (thread separada).
 */
class FuncaoServidorJudiciarioRepositoryIT extends PjbTransactionalRepositoryItBase {

    @Autowired private FuncaoServidorJudiciarioRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long usuarioId;
    private Long unidadeId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        usuarioId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'SERVIDOR', 'hash', ?, true, 'SERVIDOR') RETURNING id",
                Long.class, "Srv " + s, "srv.fn." + s + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s, 16)));
        unidadeId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_codigo, tipo_vara) " +
                "VALUES (?, 'TJCE', 'CIVEL_GERAL') RETURNING id",
                Long.class, "VARA-" + s);
    }

    @Test
    void it01_designarFuncaoRetornaComoAtiva() {
        transactionTemplate.execute(st -> repository.save(
                new FuncaoServidorJudiciarioEntity(usuarioId, unidadeId,
                        FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, LocalDate.now(), null, "Port. 1/2026")));

        List<FuncaoServidorJudiciarioEntity> ativos = repository.findByUnidadeIdAndAtivo(unidadeId, true);
        assertThat(ativos).hasSize(1);
        assertThat(ativos.get(0).getFuncao()).isEqualTo(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL);
    }

    @Test
    void it02_encerrarFuncaoNaoRetornaComoAtiva() {
        transactionTemplate.execute(st -> {
            FuncaoServidorJudiciarioEntity e = repository.save(
                    new FuncaoServidorJudiciarioEntity(usuarioId, unidadeId,
                            FuncaoServidorJudiciario.TECNICO_JUDICIARIO, LocalDate.now().minusDays(1), null, null));
            e.encerrar(LocalDate.now());
            return repository.save(e);
        });

        List<FuncaoServidorJudiciarioEntity> ativos = repository.findByUnidadeIdAndAtivo(unidadeId, true);
        assertThat(ativos).isEmpty();
    }

    @Test
    void it03_unicidadeParcialProibeSegundaAtivaMesmaFuncao() {
        transactionTemplate.execute(st -> repository.save(
                new FuncaoServidorJudiciarioEntity(usuarioId, unidadeId,
                        FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, LocalDate.now(), null, null)));

        assertThatThrownBy(() -> transactionTemplate.execute(st -> {
            repository.save(new FuncaoServidorJudiciarioEntity(usuarioId, unidadeId,
                    FuncaoServidorJudiciario.ANALISTA_JUDICIARIO, LocalDate.now(), null, null));
            repository.flush();
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void it04_findByUsuarioUnidadeFuncaoAtivoRetornaOptionalPresente() {
        transactionTemplate.execute(st -> repository.save(
                new FuncaoServidorJudiciarioEntity(usuarioId, unidadeId,
                        FuncaoServidorJudiciario.DIRETOR_SECRETARIA, LocalDate.now(), null, "Portaria Teste")));

        Optional<FuncaoServidorJudiciarioEntity> result = repository
                .findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(
                        usuarioId, unidadeId, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true);
        assertThat(result).isPresent();
        assertThat(result.get().getPortariaReferencia()).isEqualTo("Portaria Teste");
    }
}
