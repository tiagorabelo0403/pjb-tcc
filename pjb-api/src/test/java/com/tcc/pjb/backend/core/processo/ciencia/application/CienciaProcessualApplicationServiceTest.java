package com.tcc.pjb.backend.core.processo.ciencia.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.ciencia.domain.CienciaDiesAQuoCalculator;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.notification.IntimacaoMulticanalService;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CienciaProcessualApplicationServiceTest {

    @Mock private CienciaProcessualRepository cienciaRepository;
    @Mock private ProcessoRepository processoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private IntimacaoMulticanalService intimacaoService;
    @Mock private AuditLedgerService auditLedgerService;

    private CienciaProcessualApplicationService service;
    private CalendarioUteisService calendarioUteisService;

    private static final Long PROCESSO_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
        calendarioUteisService = new CalendarioUteisService();
        service = new CienciaProcessualApplicationService(
                cienciaRepository, processoRepository, usuarioRepository,
                calendarioUteisService, intimacaoService, auditLedgerService);
    }

    private Processo processoFixture() {
        Processo p = new Processo();
        p.setId(PROCESSO_ID);
        p.setNumeroProcesso("0001234-56.2024.8.26.0100");
        return p;
    }

    private Usuario usuarioFixture() {
        Usuario u = new Usuario();
        u.setId(USUARIO_ID);
        return u;
    }

    private CienciaProcessual criarCiencia(TipoCienciaProcessual tipo,
                                            CanalCiencia canal,
                                            Instant dataDisponibilizacao,
                                            Instant diesAQuo) {
        return new CienciaProcessual(
                processoFixture(), usuarioFixture(), tipo, canal,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                "COMUM_ORDINARIO", null, "ATIVO", null, 42L,
                "0001234-56.2024.8.26.0100", null, "hashtest",
                dataDisponibilizacao, null, diesAQuo);
    }

    private CienciaProcessual registrarViaService(TipoCienciaProcessual tipo, CanalCiencia canal) {
        Instant agora = Instant.now();
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processoFixture()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioFixture()));
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        return service.registrar(
                PROCESSO_ID, USUARIO_ID, tipo, canal,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU, null, "COMUM_ORDINARIO",
                "ATIVO", null, 99L, "hashconteudo", agora, null, null);
    }

    // ─── REGISTRO ────────────────────────────────────────────────────────

    @Test
    void test01_registrar_statusPendente() {
        CienciaProcessual c = registrarViaService(TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB);
        assertThat(c.getStatus()).isEqualTo(StatusCiencia.PENDENTE);
    }

    @Test
    void test02_registrar_canalDje_diesAQuoProximoDiaUtil() {
        Instant disponibilizacao = ZonedDateTime.of(2025, 1, 10, 8, 0, 0, 0, FUSO_BR).toInstant();
        Instant publicacaoDje = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, FUSO_BR).toInstant();

        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processoFixture()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioFixture()));
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CienciaProcessual c = service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.INTIMACAO_SENTENCA,
                CanalCiencia.DJE_TRIBUNAL, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 100L,
                "hash", disponibilizacao, publicacaoDje, null);

        assertThat(c.getDiesAQuo()).isAfter(publicacaoDje);
        ZonedDateTime dq = c.getDiesAQuo().atZone(FUSO_BR);
        assertThat(dq.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void test03_registrar_canalPortal_diesAQuoIgualDisponibilizacao() {
        Instant disponibilizacao = Instant.now();
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processoFixture()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioFixture()));
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CienciaProcessual c = service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 101L,
                "hash", disponibilizacao, null, null);

        assertThat(c.getDiesAQuo()).isEqualTo(disponibilizacao);
    }

    @Test
    void test04_registrar_transitoJulgado_dataExpiracaoNull() {
        CienciaProcessual c = registrarViaService(
                TipoCienciaProcessual.INTIMACAO_TRANSITO_JULGADO, CanalCiencia.PORTAL_PJB);
        assertThat(c.getDataExpiracao()).isNull();
    }

    @Test
    void test05_registrar_atoProcessualIdExistente_retornaExistente() {
        CienciaProcessual existente = criarCiencia(
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                Instant.now(), Instant.now());

        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(42L, USUARIO_ID))
                .thenReturn(Optional.of(existente));

        CienciaProcessual resultado = service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 42L,
                "hash", Instant.now(), null, null);

        assertThat(resultado).isSameAs(existente);
        verify(processoRepository, times(0)).findById(any());
    }

    @Test
    void test06_registrar_processoIdInvalido_lancaExcecao() {
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 200L,
                "hash", Instant.now(), null, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void test07_registrar_usuarioIdInvalido_lancaExcecao() {
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processoFixture()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 201L,
                "hash", Instant.now(), null, null))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── CONFIRMAÇÃO ──────────────────────────────────────────────────────

    @Test
    void test08_confirmar_statusConfirmadaDataCienciaPreenchida() {
        CienciaProcessual ciencia = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        when(cienciaRepository.findById(1L)).thenReturn(Optional.of(ciencia));
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CienciaProcessual c = service.confirmar(1L, USUARIO_ID, "192.168.0.1", "agentHash", "sessHash");

        assertThat(c.getStatus()).isEqualTo(StatusCiencia.CONFIRMADA);
        assertThat(c.getDataCiencia()).isNotNull();
    }

    @Test
    void test09_confirmar_comprovanteHashNaoNulo() {
        CienciaProcessual ciencia = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        when(cienciaRepository.findById(1L)).thenReturn(Optional.of(ciencia));
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CienciaProcessual c = service.confirmar(1L, USUARIO_ID, "192.168.0.1", "agentHash", "sessHash");

        assertThat(c.getComprovanteHash()).isNotBlank();
    }

    @Test
    void test10_confirmar_usuarioErrado_lancaSecurityException() {
        CienciaProcessual ciencia = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        when(cienciaRepository.findById(1L)).thenReturn(Optional.of(ciencia));

        assertThatThrownBy(() -> service.confirmar(1L, 999L, "ip", "uh", "sh"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void test11_confirmar_statusConfirmada_lancaIllegalStateException() {
        CienciaProcessual ciencia = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        ciencia.confirmar("ip", "uh", "sh", Instant.now());
        when(cienciaRepository.findById(1L)).thenReturn(Optional.of(ciencia));

        assertThatThrownBy(() -> service.confirmar(1L, USUARIO_ID, "ip", "uh", "sh"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void test12_confirmar_registraIpESessionTokenHash() {
        CienciaProcessual ciencia = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        when(cienciaRepository.findById(1L)).thenReturn(Optional.of(ciencia));
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmar(1L, USUARIO_ID, "10.0.0.1", "hashAgent", "hashSession");

        assertThat(ciencia.getIpConfirmacao()).isEqualTo("10.0.0.1");
        assertThat(ciencia.getSessionTokenHash()).isEqualTo("hashSession");
    }

    // ─── RITOS CÍVEIS ─────────────────────────────────────────────────────

    @Test
    void test13_citacaoInicial_prazo15du() {
        assertThat(TipoCienciaProcessual.CITACAO_INICIAL.prazoRespostaDias()).isEqualTo(15);
        assertThat(TipoCienciaProcessual.CITACAO_INICIAL.emDiasUteis()).isTrue();
    }

    @Test
    void test14_intimacaoEmbargosDeclaracao_prazo5du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_EMBARGOS_DECLARACAO.prazoRespostaDias()).isEqualTo(5);
        assertThat(TipoCienciaProcessual.INTIMACAO_EMBARGOS_DECLARACAO.emDiasUteis()).isTrue();
    }

    @Test
    void test15_intimacaoSentenca_prazo15du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA.prazoRespostaDias()).isEqualTo(15);
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA.emDiasUteis()).isTrue();
    }

    @Test
    void test16_intimacaoJuizadoCivel_prazo10du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_JUIZADO_CIVEL.prazoRespostaDias()).isEqualTo(10);
        assertThat(TipoCienciaProcessual.INTIMACAO_JUIZADO_CIVEL.emDiasUteis()).isTrue();
    }

    @Test
    void test17_intimacaoJuizadoFederal_prazo10du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_JUIZADO_FEDERAL.prazoRespostaDias()).isEqualTo(10);
        assertThat(TipoCienciaProcessual.INTIMACAO_JUIZADO_FEDERAL.emDiasUteis()).isTrue();
    }

    // ─── RITOS PENAIS ─────────────────────────────────────────────────────

    @Test
    void test18_citacaoPenalOrdinario_prazo10diasCorridos() {
        assertThat(TipoCienciaProcessual.CITACAO_PENAL_ORDINARIO.prazoRespostaDias()).isEqualTo(10);
        assertThat(TipoCienciaProcessual.CITACAO_PENAL_ORDINARIO.emDiasUteis()).isFalse();
    }

    @Test
    void test19_intimacaoSentencaPenal_prazo5diasCorridos() {
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA_PENAL.prazoRespostaDias()).isEqualTo(5);
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA_PENAL.emDiasUteis()).isFalse();
    }

    @Test
    void test20_intimacaoPronuncia_prazo20diasCorridos() {
        assertThat(TipoCienciaProcessual.INTIMACAO_PRONUNCIA.prazoRespostaDias()).isEqualTo(20);
        assertThat(TipoCienciaProcessual.INTIMACAO_PRONUNCIA.emDiasUteis()).isFalse();
    }

    @Test
    void test21_intimacaoMariaDaPenha_prazo48horas() {
        assertThat(TipoCienciaProcessual.INTIMACAO_MARIA_DA_PENHA.prazoRespostaDias()).isEqualTo(48);
        assertThat(TipoCienciaProcessual.INTIMACAO_MARIA_DA_PENHA.prazoEmHoras()).isTrue();
    }

    // ─── RITOS TRABALHISTAS ────────────────────────────────────────────────

    @Test
    void test22_notificacaoReclamado_prazo5dias() {
        assertThat(TipoCienciaProcessual.NOTIFICACAO_RECLAMADO_TRABALHISTA.prazoRespostaDias()).isEqualTo(5);
    }

    @Test
    void test23_intimacaoROTrabalhista_prazo8dias() {
        assertThat(TipoCienciaProcessual.INTIMACAO_RO_TRABALHISTA.prazoRespostaDias()).isEqualTo(8);
    }

    // ─── RITOS ELEITORAIS ──────────────────────────────────────────────────

    @Test
    void test24_intimacaoAIME_prazo3du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_AIME.prazoRespostaDias()).isEqualTo(3);
        assertThat(TipoCienciaProcessual.INTIMACAO_AIME.emDiasUteis()).isTrue();
    }

    @Test
    void test25_intimacaoAIJE_prazo5du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_AIJE.prazoRespostaDias()).isEqualTo(5);
        assertThat(TipoCienciaProcessual.INTIMACAO_AIJE.emDiasUteis()).isTrue();
    }

    @Test
    void test26_intimacaoRCED_prazo3du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_RCED.prazoRespostaDias()).isEqualTo(3);
        assertThat(TipoCienciaProcessual.INTIMACAO_RCED.emDiasUteis()).isTrue();
    }

    // ─── RITOS MILITARES ──────────────────────────────────────────────────

    @Test
    void test27_citacaoMilitar_prazo10dias() {
        assertThat(TipoCienciaProcessual.CITACAO_MILITAR.prazoRespostaDias()).isEqualTo(10);
        assertThat(TipoCienciaProcessual.CITACAO_MILITAR.emDiasUteis()).isFalse();
    }

    @Test
    void test28_intimacaoConselhoJustica_prazo5dias() {
        assertThat(TipoCienciaProcessual.INTIMACAO_CONSELHO_JUSTICA.prazoRespostaDias()).isEqualTo(5);
        assertThat(TipoCienciaProcessual.INTIMACAO_CONSELHO_JUSTICA.emDiasUteis()).isFalse();
    }

    // ─── CADEIA RECURSAL ──────────────────────────────────────────────────

    @Test
    void test29_grauPrimeiroGrau_intimacaoSentenca_prazo15du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA.prazoRespostaDias()).isEqualTo(15);
        assertThat(GrauJurisdicaoCiencia.PRIMEIRO_GRAU.ordem()).isEqualTo(1);
    }

    @Test
    void test30_grauSTF_intimacaoRE_prazo15du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_RE.prazoRespostaDias()).isEqualTo(15);
        assertThat(GrauJurisdicaoCiencia.STF.isUltimaInstancia()).isTrue();
    }

    @Test
    void test31_grauSegundoGrau_intimacaoED_prazo5du() {
        assertThat(TipoCienciaProcessual.INTIMACAO_EMBARGOS_DECLARACAO.prazoRespostaDias()).isEqualTo(5);
        assertThat(GrauJurisdicaoCiencia.SEGUNDO_GRAU.ordem()).isEqualTo(2);
    }

    @Test
    void test32_grauSTF_isUltimaInstancia_true() {
        assertThat(GrauJurisdicaoCiencia.STF.isUltimaInstancia()).isTrue();
    }

    @Test
    void test33_grauPrimeiroGrau_proximoGrau_segundoGrau() {
        assertThat(GrauJurisdicaoCiencia.PRIMEIRO_GRAU.proximoGrau())
                .contains(GrauJurisdicaoCiencia.SEGUNDO_GRAU);
    }

    @Test
    void test34_grauSTF_proximoGrau_empty() {
        assertThat(GrauJurisdicaoCiencia.STF.proximoGrau()).isEmpty();
    }

    // ─── CIÊNCIA FICTA ────────────────────────────────────────────────────

    @Test
    void test35_processarExpirados_pendentesExpiradas_fictoConfirmada() {
        Instant vencida = Instant.now().minus(5, ChronoUnit.DAYS);
        CienciaProcessual ciencia = new CienciaProcessual(
                processoFixture(), usuarioFixture(),
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                "COMUM_ORDINARIO", null, "ATIVO", null, null,
                "0001", null, "hash",
                Instant.now().minus(30, ChronoUnit.DAYS), null,
                Instant.now().minus(25, ChronoUnit.DAYS));

        when(cienciaRepository.findPendentesExpirados(any(), any()))
                .thenReturn(List.of(ciencia))
                .thenReturn(List.of());
        when(cienciaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int total = service.processarExpirados();

        assertThat(total).isEqualTo(1);
        assertThat(ciencia.getStatus()).isEqualTo(StatusCiencia.FICTA_CONFIRMADA);
    }

    @Test
    void test36_processarExpirados_confirmadaNaoAlterada() {
        CienciaProcessual ciencia = new CienciaProcessual(
                processoFixture(), usuarioFixture(),
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                "COMUM_ORDINARIO", null, "ATIVO", null, null,
                "0001", null, "hash",
                Instant.now().minus(30, ChronoUnit.DAYS), null,
                Instant.now().minus(25, ChronoUnit.DAYS));
        ciencia.confirmar("ip", "uh", "sh", Instant.now());

        when(cienciaRepository.findPendentesExpirados(any(), any()))
                .thenReturn(List.of());

        service.processarExpirados();

        assertThat(ciencia.getStatus()).isEqualTo(StatusCiencia.CONFIRMADA);
    }

    @Test
    void test37_processarExpirados_processaEmLoteDe50() {
        when(cienciaRepository.findPendentesExpirados(any(), any()))
                .thenReturn(List.of());

        service.processarExpirados();

        verify(cienciaRepository).findPendentesExpirados(any(Instant.class),
                eq(org.springframework.data.domain.PageRequest.of(0, 50)));
    }

    @Test
    void test38_processarExpirados_retornaContagemCorreta() {
        CienciaProcessual c1 = new CienciaProcessual(
                processoFixture(), usuarioFixture(),
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                "COMUM_ORDINARIO", null, "ATIVO", null, null,
                "0001", null, "hash",
                Instant.now().minus(30, ChronoUnit.DAYS), null,
                Instant.now().minus(20, ChronoUnit.DAYS));
        CienciaProcessual c2 = new CienciaProcessual(
                processoFixture(), usuarioFixture(),
                TipoCienciaProcessual.CITACAO_INICIAL, CanalCiencia.PORTAL_PJB,
                GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                "COMUM_ORDINARIO", null, "ATIVO", null, null,
                "0002", null, "hash2",
                Instant.now().minus(30, ChronoUnit.DAYS), null,
                Instant.now().minus(20, ChronoUnit.DAYS));

        when(cienciaRepository.findPendentesExpirados(any(), any()))
                .thenReturn(List.of(c1, c2))
                .thenReturn(List.of());
        when(cienciaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int total = service.processarExpirados();

        assertThat(total).isEqualTo(2);
    }

    // ─── ENUMS — STATUS ───────────────────────────────────────────────────

    @Test
    void test39_statusConfirmada_isTerminal_true() {
        assertThat(StatusCiencia.CONFIRMADA.isTerminal()).isTrue();
    }

    @Test
    void test40_statusPendente_isTerminal_false() {
        assertThat(StatusCiencia.PENDENTE.isTerminal()).isFalse();
    }

    @Test
    void test41_statusPendente_permiteTransicaoPara_confirmada_true() {
        assertThat(StatusCiencia.PENDENTE.permiteTransicaoPara(StatusCiencia.CONFIRMADA)).isTrue();
    }

    @Test
    void test42_statusConfirmada_permiteTransicaoPara_cancelada_false() {
        assertThat(StatusCiencia.CONFIRMADA.permiteTransicaoPara(StatusCiencia.CANCELADA)).isFalse();
    }

    // ─── ENUMS — TIPO CIÊNCIA ─────────────────────────────────────────────

    @Test
    void test43_intimacaoEmbargosDeclaracao_isRecursal_true() {
        assertThat(TipoCienciaProcessual.INTIMACAO_EMBARGOS_DECLARACAO.isRecursal()).isTrue();
    }

    @Test
    void test44_intimacaoSentencaPenal_isPenal_true() {
        assertThat(TipoCienciaProcessual.INTIMACAO_SENTENCA_PENAL.isPenal()).isTrue();
    }

    @Test
    void test45_notificacaoReclamado_isTrabalhista_true() {
        assertThat(TipoCienciaProcessual.NOTIFICACAO_RECLAMADO_TRABALHISTA.isTrabalhista()).isTrue();
    }

    @Test
    void test46_intimacaoAIME_isEleitoral_true() {
        assertThat(TipoCienciaProcessual.INTIMACAO_AIME.isEleitoral()).isTrue();
    }

    @Test
    void test47_citacaoMilitar_isMilitar_true() {
        assertThat(TipoCienciaProcessual.CITACAO_MILITAR.isMilitar()).isTrue();
    }

    @Test
    void test48_intimacaoMariaDaPenha_prazoEmHoras_true() {
        assertThat(TipoCienciaProcessual.INTIMACAO_MARIA_DA_PENHA.prazoEmHoras()).isTrue();
    }

    // ─── ENUMS — CANAL ────────────────────────────────────────────────────

    @Test
    void test49_djeSSTF_isDje_true() {
        assertThat(CanalCiencia.DJE_STF.isDje()).isTrue();
    }

    @Test
    void test50_notificacaoTrabalhista_isPostal_true() {
        assertThat(CanalCiencia.NOTIFICACAO_TRABALHISTA.isPostal()).isTrue();
    }

    @Test
    void test51_djeSTF_diesAQuoNoDiaUtilSeguinte_true() {
        assertThat(CanalCiencia.DJE_STF.diesAQuoNoDiaUtilSeguinte()).isTrue();
    }

    @Test
    void test52_portalPJB_diesAQuoNoDiaUtilSeguinte_false() {
        assertThat(CanalCiencia.PORTAL_PJB.diesAQuoNoDiaUtilSeguinte()).isFalse();
    }

    // ─── DIES A QUO ───────────────────────────────────────────────────────

    @Test
    void test53_calculatorDjeSabado_diesAQuoNaSegunda() {
        CienciaDiesAQuoCalculator calc = new CienciaDiesAQuoCalculator(calendarioUteisService);
        Instant sabado = ZonedDateTime.of(2025, 1, 11, 10, 0, 0, 0, FUSO_BR).toInstant();
        assertThat(ZonedDateTime.of(2025, 1, 11, 10, 0, 0, 0, FUSO_BR).getDayOfWeek())
                .isEqualTo(DayOfWeek.SATURDAY);

        Instant diesAQuo = calc.calcularDiesAQuo(CanalCiencia.DJE_TRIBUNAL, sabado, sabado);

        ZonedDateTime resultado = diesAQuo.atZone(FUSO_BR);
        assertThat(resultado.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void test54_calculatorDjeDomingo_diesAQuoNaSegunda() {
        CienciaDiesAQuoCalculator calc = new CienciaDiesAQuoCalculator(calendarioUteisService);
        Instant domingo = ZonedDateTime.of(2025, 1, 12, 10, 0, 0, 0, FUSO_BR).toInstant();
        assertThat(ZonedDateTime.of(2025, 1, 12, 10, 0, 0, 0, FUSO_BR).getDayOfWeek())
                .isEqualTo(DayOfWeek.SUNDAY);

        Instant diesAQuo = calc.calcularDiesAQuo(CanalCiencia.DJE_TRIBUNAL, domingo, domingo);

        ZonedDateTime resultado = diesAQuo.atZone(FUSO_BR);
        assertThat(resultado.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void test55_calculatorPortal_diesAQuoIgualDisponibilizacao() {
        CienciaDiesAQuoCalculator calc = new CienciaDiesAQuoCalculator(calendarioUteisService);
        Instant disponibilizacao = Instant.now();

        Instant diesAQuo = calc.calcularDiesAQuo(CanalCiencia.PORTAL_PJB, disponibilizacao, null);

        assertThat(diesAQuo).isEqualTo(disponibilizacao);
    }

    // ─── POLO PROCESSUAL ──────────────────────────────────────────────────

    @Test
    void test56_buscarPorPolo_retornaApenasPoloInformado() {
        CienciaProcessual c = criarCiencia(TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, Instant.now(), Instant.now());
        when(cienciaRepository.findPendentesByProcessoAndPolo(PROCESSO_ID, "ATIVO"))
                .thenReturn(List.of(c));

        List<CienciaProcessual> resultado = service.buscarPorPolo(PROCESSO_ID, "ATIVO");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPoloProcessual()).isEqualTo("ATIVO");
    }

    @Test
    void test57_registrarPolosDistintos_cienciasIndependentes() {
        when(processoRepository.findById(PROCESSO_ID)).thenReturn(Optional.of(processoFixture()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioFixture()));
        when(cienciaRepository.findByAtoProcessualIdAndUsuarioId(anyLong(), eq(USUARIO_ID)))
                .thenReturn(Optional.empty());
        when(cienciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant agora = Instant.now();

        CienciaProcessual ativo = service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "ATIVO", null, 300L,
                "hash", agora, null, null);

        CienciaProcessual passivo = service.registrar(
                PROCESSO_ID, USUARIO_ID, TipoCienciaProcessual.CITACAO_INICIAL,
                CanalCiencia.PORTAL_PJB, GrauJurisdicaoCiencia.PRIMEIRO_GRAU,
                null, "COMUM_ORDINARIO", "PASSIVO", null, 301L,
                "hash", agora, null, null);

        assertThat(ativo.getPoloProcessual()).isEqualTo("ATIVO");
        assertThat(passivo.getPoloProcessual()).isEqualTo("PASSIVO");
    }
}
