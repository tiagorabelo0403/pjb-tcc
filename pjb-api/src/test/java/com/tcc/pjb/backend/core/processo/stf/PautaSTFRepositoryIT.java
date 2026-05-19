package com.tcc.pjb.backend.core.processo.stf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorSTF;
import com.tcc.pjb.backend.model.entity.enums.ResultadoVotacaoSTF;
import com.tcc.pjb.backend.model.entity.enums.TipoSessaoSTF;
import com.tcc.pjb.backend.model.entity.processo.ImpedimentoMinistro;
import com.tcc.pjb.backend.model.entity.processo.PautaSTF;
import com.tcc.pjb.backend.model.entity.processo.PedidoVistaSTF;
import com.tcc.pjb.backend.model.repository.ImpedimentoMinistroRepository;
import com.tcc.pjb.backend.model.repository.PautaSTFRepository;
import com.tcc.pjb.backend.model.repository.PedidoVistaSTFRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class PautaSTFRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private PautaSTFRepository pautaRepository;
    @Autowired private PedidoVistaSTFRepository vistaRepository;
    @Autowired private ImpedimentoMinistroRepository impedimentoRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long processoId;
    private Long relatorId;
    private Long ministroId;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        relatorId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'MAGISTRADO', 'hash', ?, true, 'MAGISTRADO') RETURNING id",
                Long.class, "Relator " + s, "rel.stf." + s + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s, 16)));
        String s2 = UUID.randomUUID().toString().substring(0, 8);
        ministroId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) " +
                "VALUES (?, ?, 'MAGISTRADO', 'hash', ?, true, 'MAGISTRADO') RETURNING id",
                Long.class, "Ministro " + s2, "min.stf." + s2 + "@pjb.test",
                String.format("%011d", Long.parseUnsignedLong(s2, 16)));
        processoId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_processo (numero_processo, status_processo) " +
                "VALUES (?, 'PAUTADO') RETURNING id",
                Long.class, "0001003-" + s + ".2025.1.01.0000");
    }

    @Test
    void it01_salvarPautaRetornaComCamposCorretos() {
        PautaSTF pauta = transactionTemplate.execute(st ->
                pautaRepository.save(new PautaSTF(processoId, OrgaoJulgadorSTF.PLENARIO,
                        TipoSessaoSTF.VIRTUAL, LocalDate.now().plusDays(30), relatorId)));

        PautaSTF carregada = pautaRepository.findById(pauta.getId()).orElseThrow();
        assertThat(carregada.getProcessoId()).isEqualTo(processoId);
        assertThat(carregada.getOrgaoJulgador()).isEqualTo(OrgaoJulgadorSTF.PLENARIO);
        assertThat(carregada.getTipoSessao()).isEqualTo(TipoSessaoSTF.VIRTUAL);
    }

    @Test
    void it02_registrarVotacaoPersistePlacarEResultado() {
        PautaSTF pauta = transactionTemplate.execute(st ->
                pautaRepository.save(new PautaSTF(processoId, OrgaoJulgadorSTF.PRIMEIRA_TURMA,
                        TipoSessaoSTF.PRESENCIAL, LocalDate.now(), relatorId)));
        Long pautaId = pauta.getId();

        transactionTemplate.execute(st -> {
            PautaSTF p = pautaRepository.findById(pautaId).orElseThrow();
            p.registrarVotacao(3, 2, 0, 0, ResultadoVotacaoSTF.APROVADO);
            return pautaRepository.save(p);
        });

        PautaSTF atualizada = pautaRepository.findById(pautaId).orElseThrow();
        assertThat(atualizada.getResultado()).isEqualTo(ResultadoVotacaoSTF.APROVADO);
        assertThat(atualizada.getVotosFavoraveis()).isEqualTo(3);
        assertThat(atualizada.getDataJulgamento()).isNotNull();
    }

    @Test
    void it03_pedidoVistaSalvaComStatusPendente() {
        PautaSTF pauta = transactionTemplate.execute(st ->
                pautaRepository.save(new PautaSTF(processoId, OrgaoJulgadorSTF.PLENARIO,
                        TipoSessaoSTF.VIRTUAL, LocalDate.now(), relatorId)));

        PedidoVistaSTF vista = transactionTemplate.execute(st ->
                vistaRepository.save(new PedidoVistaSTF(
                        pauta.getId(), ministroId, Instant.now().plus(90, ChronoUnit.DAYS))));

        assertThat(vista.getStatus()).isEqualTo("PENDENTE");
        PedidoVistaSTF carregado = vistaRepository.findById(vista.getId()).orElseThrow();
        assertThat(carregado.getPautaId()).isEqualTo(pauta.getId());
        assertThat(carregado.getMinistroId()).isEqualTo(ministroId);
    }

    @Test
    void it04_impedimentoDuplicadoProcessoMinistroLancaConstraint() {
        transactionTemplate.execute(st ->
                impedimentoRepository.save(new ImpedimentoMinistro(
                        processoId, ministroId, "PARENTESCO", "Familiar direto")));

        assertThatThrownBy(() -> transactionTemplate.execute(st -> {
            impedimentoRepository.save(new ImpedimentoMinistro(
                    processoId, ministroId, "INTERESSE_DIRETO", "Investimento declarado"));
            impedimentoRepository.flush();
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
    }
}
