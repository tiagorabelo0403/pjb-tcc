package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeNotificationScheduler;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeOverrideService;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenciaApplicationService;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.completude.OverrideCompletudeRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeEventoTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloCompletudeOutboxEntity;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloValidacaoHistorico;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloCompletudeOutboxRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloPendenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloValidacaoHistoricoRepository;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PjbProtocoloCompletudeFase3Test {

    private ProtocoloPendenciaRepository pendenciaRepository;
    private ProtocoloValidacaoHistoricoRepository historicoRepository;
    private ProtocoloCompletudeOutboxRepository outboxRepository;
    private LaianePeticaoInicialDraftSessionRepository draftSessionRepository;
    private UsuarioRepository usuarioRepository;
    private ObjectMapper objectMapper;

    private ProtocoloCompletudeOverrideService overrideService;
    private ProtocoloPendenciaApplicationService pendenciaService;
    private ProtocoloCompletudeNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        pendenciaRepository = mock(ProtocoloPendenciaRepository.class);
        historicoRepository = mock(ProtocoloValidacaoHistoricoRepository.class);
        outboxRepository = mock(ProtocoloCompletudeOutboxRepository.class);
        draftSessionRepository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        objectMapper = new ObjectMapper();

        overrideService = new ProtocoloCompletudeOverrideService(
                pendenciaRepository, historicoRepository, objectMapper);
        pendenciaService = new ProtocoloPendenciaApplicationService(
                pendenciaRepository, historicoRepository, outboxRepository,
                draftSessionRepository, objectMapper);
        scheduler = new ProtocoloCompletudeNotificationScheduler(
                outboxRepository, mock(com.tcc.pjb.backend.service.notification.NotificationService.class),
                usuarioRepository, objectMapper);
    }

    // ===== BLOQUEIO 3 — ArgumentCaptor duplo: histórico + pendência =====

    @Test
    void override_dispensa_audita_historico_e_pendencia_corretamente() {
        String justificativa = "Documento apresentado presencialmente na secretaria do fórum e verificado pelo servidor responsável.";
        ProtocoloPendencia pendencia = pendenciaDispensavel(1L);
        when(pendenciaRepository.findAtivaByProtocoloId(1L)).thenReturn(Optional.of(pendencia));
        when(pendenciaRepository.save(any())).thenReturn(pendencia);

        overrideService.dispensar(1L, justificativa, 99L);

        ArgumentCaptor<ProtocoloValidacaoHistorico> historicoCaptor =
                ArgumentCaptor.forClass(ProtocoloValidacaoHistorico.class);
        verify(historicoRepository).save(historicoCaptor.capture());
        ProtocoloValidacaoHistorico historico = historicoCaptor.getValue();
        assertThat(historico.getProtocoloId()).isEqualTo(1L);
        assertThat(historico.getOrigemValidacao()).isEqualTo(OrigemValidacao.OVERRIDE);
        assertThat(historico.getExecutadoPor()).isEqualTo(99L);
        assertThat(historico.getStatusResultante()).isEqualTo(ProtocoloCompletudeStatus.DISPENSADO);

        ArgumentCaptor<ProtocoloPendencia> pendenciaCaptor =
                ArgumentCaptor.forClass(ProtocoloPendencia.class);
        verify(pendenciaRepository).save(pendenciaCaptor.capture());
        ProtocoloPendencia salva = pendenciaCaptor.getValue();
        assertThat(salva.getDispensadoPor()).isEqualTo(99L);
        assertThat(salva.getDispensaJustificativa()).isEqualTo(justificativa);
        assertThat(salva.getStatus()).isEqualTo(ProtocoloCompletudeStatus.DISPENSADO);
    }

    @Test
    void override_rejeita_requisito_absoluto_sem_gravar_historico() {
        ProtocoloPendencia pendencia = pendenciaAbsoluta(2L);
        when(pendenciaRepository.findAtivaByProtocoloId(2L)).thenReturn(Optional.of(pendencia));

        assertThatThrownBy(() ->
                overrideService.dispensar(
                        2L,
                        "Justificativa suficientemente longa para passar na validação de tamanho mínimo.",
                        99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ABSOLUTA");

        verify(historicoRepository, never()).save(any());
        verify(pendenciaRepository, never()).save(any());
    }

    // ===== BLOQUEIO 1 — Scheduler resolve solicitante, não executor do histórico =====

    @Test
    void registrar_pendencia_grava_solicitante_da_peticao_no_outbox_nao_executor() {
        Usuario solicitante = mock(Usuario.class);
        when(solicitante.getId()).thenReturn(42L);
        LaianePeticaoInicialDraftSession draft = mock(LaianePeticaoInicialDraftSession.class);
        when(draft.getSolicitante()).thenReturn(solicitante);
        when(draftSessionRepository.findById(42L)).thenReturn(Optional.of(draft));

        ProtocoloPendencia salva = pendenciaBase(42L);
        when(pendenciaRepository.findAtivaByProtocoloId(42L)).thenReturn(Optional.empty());
        when(pendenciaRepository.save(any())).thenReturn(salva);

        pendenciaService.registrarPendencia(
                42L, resultadoBloqueado(), List.of("PROCURACAO"), OrigemValidacao.PROTOCOLO, 99L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("\"solicitanteId\":42");
        assertThat(payload).doesNotContain("\"solicitanteId\":99");
    }

    @Test
    void notifica_solicitante_da_peticao_nao_executor_do_historico() throws Exception {
        com.tcc.pjb.backend.service.notification.NotificationService notificationService =
                mock(com.tcc.pjb.backend.service.notification.NotificationService.class);
        scheduler = new ProtocoloCompletudeNotificationScheduler(
                outboxRepository, notificationService, usuarioRepository, objectMapper);

        ProtocoloCompletudeOutboxEntity entry = new ProtocoloCompletudeOutboxEntity();
        entry.setId(UUID.randomUUID());
        entry.setProtocoloId(10L);
        entry.setTipo(ProtocoloCompletudeEventoTipo.PROTOCOLO_PENDENTE_DOCUMENTACAO);
        entry.setPayload("{\"protocoloId\":10,\"tipo\":\"PROTOCOLO_PENDENTE_DOCUMENTACAO\","
                + "\"prazoRegularizacao\":\"2026-07-01\",\"solicitanteId\":42}");
        entry.setProcessado(false);
        entry.setTentativas(0);
        entry.setCriadoEm(Instant.now());

        Usuario solicitante = mock(Usuario.class);
        when(solicitante.getId()).thenReturn(42L);
        when(outboxRepository.findPendentes()).thenReturn(List.of(entry));
        when(outboxRepository.save(any())).thenReturn(entry);
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(solicitante));

        scheduler.processarOutbox();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(notificationService).notifyUser(
                usuarioCaptor.capture(), isNull(), anyString(), anyString(), contains("10"));
        assertThat(usuarioCaptor.getValue().getId()).isEqualTo(42L);
        verify(usuarioRepository, never()).findById(99L);
    }

    // ===== BLOQUEIO 2 — Schema test: constraint REGRA A presente em V286 =====

    @Test
    void regra_a_constraint_partial_unique_index_presente_em_v286() throws IOException {
        Path migrationPath = Paths.get("src", "main", "resources", "db", "migration",
                "V286__protocolo_completude_pendencia.sql");
        String sql = Files.readString(migrationPath);
        assertThat(sql)
                .as("V286 deve conter índice único parcial (REGRA A) sobre protocolo_id WHERE status IN")
                .containsPattern(
                        "(?is)CREATE\\s+UNIQUE\\s+INDEX[^;]+tb_protocolo_pendencia[^;]+"
                        + "\\(protocolo_id\\)[^;]+WHERE[^;]+status\\s+IN");
    }

    // ===== M1 — Enum no outbox =====

    @Test
    void outbox_grava_tipo_como_enum_nao_string_magica() {
        ProtocoloPendencia salva = pendenciaBase(55L);
        when(pendenciaRepository.findAtivaByProtocoloId(55L)).thenReturn(Optional.empty());
        when(pendenciaRepository.save(any())).thenReturn(salva);
        when(draftSessionRepository.findById(55L)).thenReturn(Optional.empty());

        pendenciaService.registrarPendencia(
                55L, resultadoBloqueado(), List.of("PROCURACAO"), OrigemValidacao.PROTOCOLO, 1L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo())
                .isEqualTo(ProtocoloCompletudeEventoTipo.PROTOCOLO_PENDENTE_DOCUMENTACAO);
    }

    // ===== M2 — @Size(min=80) =====

    @Test
    void override_request_rejeita_justificativa_de_30_chars() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new OverrideCompletudeRequest("Documento apresentado."));
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("justificativa");
    }

    @Test
    void override_request_rejeita_justificativa_curta() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new OverrideCompletudeRequest("curta demais"));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void override_request_aceita_justificativa_valida_com_80_chars() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new OverrideCompletudeRequest(
                "Documento apresentado presencialmente na secretaria do fórum e verificado pelo servidor."));
        assertThat(violations).isEmpty();
    }

    // ===== M3 — computeHash já era SHA-256 ordenado (confirmação) =====

    @Test
    void computeHash_ordem_dos_documentos_nao_altera_hash() {
        assertThat(ProtocoloPendenciaApplicationService.computeHash(List.of("A", "B", "C")))
                .isEqualTo(ProtocoloPendenciaApplicationService.computeHash(List.of("C", "B", "A")));
    }

    @Test
    void computeHash_documentos_diferentes_geram_hashes_diferentes() {
        assertThat(ProtocoloPendenciaApplicationService.computeHash(List.of("PROCURACAO")))
                .isNotEqualTo(ProtocoloPendenciaApplicationService.computeHash(List.of("CTPS")));
    }

    @Test
    void computeHash_determinista_mesma_lista_mesmo_hash() {
        List<String> docs = List.of("DOCUMENTO_IDENTIDADE", "PROCURACAO");
        assertThat(ProtocoloPendenciaApplicationService.computeHash(docs))
                .isEqualTo(ProtocoloPendenciaApplicationService.computeHash(docs));
    }

    // ===== Testes originais corrigidos / preservados =====

    @Test
    void registrar_pendencia_grava_no_outbox() {
        ProtocoloPendencia salva = pendenciaBase(42L);
        when(pendenciaRepository.findAtivaByProtocoloId(42L)).thenReturn(Optional.empty());
        when(pendenciaRepository.save(any())).thenReturn(salva);
        when(draftSessionRepository.findById(42L)).thenReturn(Optional.empty());

        pendenciaService.registrarPendencia(
                42L, resultadoBloqueado(), List.of("PROCURACAO"), OrigemValidacao.PROTOCOLO, 1L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo())
                .isEqualTo(ProtocoloCompletudeEventoTipo.PROTOCOLO_PENDENTE_DOCUMENTACAO);
        assertThat(captor.getValue().getProtocoloId()).isEqualTo(42L);
        assertThat(captor.getValue().isProcessado()).isFalse();
    }

    @Test
    void escalar_expirada_gera_evento_expiracao() {
        ProtocoloPendencia pendencia = ProtocoloPendencia.builder()
                .id(77L).uuid(UUID.randomUUID()).protocoloId(5L)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().minusDays(1))
                .violacoesJson("[]").requisitosVersao("v1.0")
                .documentosHash("hash").version(0L).build();
        when(pendenciaRepository.findById(77L)).thenReturn(Optional.of(pendencia));
        when(draftSessionRepository.findById(5L)).thenReturn(Optional.empty());

        pendenciaService.escalarExpirada(77L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(ProtocoloCompletudeEventoTipo.PROTOCOLO_EXPIRADO);
        assertThat(captor.getValue().getProtocoloId()).isEqualTo(5L);
    }

    @Test
    void escalar_expirada_nao_gera_evento_quando_status_diferente() {
        ProtocoloPendencia pendencia = ProtocoloPendencia.builder()
                .id(88L).uuid(UUID.randomUUID()).protocoloId(6L)
                .status(ProtocoloCompletudeStatus.DISPENSADO)
                .prazoRegularizacao(LocalDate.now().minusDays(1))
                .violacoesJson("[]").requisitosVersao("v1.0")
                .documentosHash("hash").version(0L).build();
        when(pendenciaRepository.findById(88L)).thenReturn(Optional.of(pendencia));

        pendenciaService.escalarExpirada(88L);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void registrar_pendencia_idempotente_mesmo_hash_nao_duplica_outbox() {
        List<String> docs = List.of("PROCURACAO");
        String hash = ProtocoloPendenciaApplicationService.computeHash(docs);
        ProtocoloPendencia existente = ProtocoloPendencia.builder()
                .id(9L).uuid(UUID.randomUUID()).protocoloId(30L)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().plusDays(5))
                .violacoesJson("[]").requisitosVersao("v1.0")
                .documentosHash(hash).version(0L).build();
        when(pendenciaRepository.findAtivaByProtocoloId(30L)).thenReturn(Optional.of(existente));

        pendenciaService.registrarPendencia(
                30L, resultadoBloqueado(), docs, OrigemValidacao.PROTOCOLO, 1L);

        verify(outboxRepository, never()).save(any());
        verify(pendenciaRepository, never()).save(any());
    }

    @Test
    void metrica_bloqueado_incrementa_counter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProtocoloCompletudeMetrics metrics = new ProtocoloCompletudeMetrics(registry);

        metrics.registrarBloqueado(RitoProcessual.COMUM_ORDINARIO);

        double count = registry.counter("pjb.protocolo.completude.bloqueados",
                "rito", RitoProcessual.COMUM_ORDINARIO.name()).count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void metrica_liberado_incrementa_counter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProtocoloCompletudeMetrics metrics = new ProtocoloCompletudeMetrics(registry);

        metrics.registrarLiberado(RitoProcessual.TRABALHISTA_ORDINARIO);

        double count = registry.counter("pjb.protocolo.completude.liberados",
                "rito", RitoProcessual.TRABALHISTA_ORDINARIO.name()).count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void metrica_violacao_tipo_doc_incrementa_counter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProtocoloCompletudeMetrics metrics = new ProtocoloCompletudeMetrics(registry);

        metrics.registrarViolacaoTipoDoc(TipoDocumentoProcessual.PROCURACAO);
        metrics.registrarViolacaoTipoDoc(TipoDocumentoProcessual.PROCURACAO);

        double count = registry.counter("pjb.protocolo.completude.pendencia_tipo_doc",
                "tipo_documento", TipoDocumentoProcessual.PROCURACAO.name()).count();
        assertThat(count).isEqualTo(2.0);
    }

    // ===== Fixtures =====

    private ProtocoloPendencia pendenciaDispensavel(Long protocoloId) {
        String json = "[{\"codigo\":\"DOC_OBRIGATORIO_AUSENTE\",\"severidade\":\"BLOQUEANTE\","
                + "\"campo\":\"PROCURACAO\",\"acaoCorretiva\":\"Anexe o documento\","
                + "\"fundamento\":{\"tipo\":\"LEI\",\"identificador\":\"CPC art. 287\","
                + "\"resumo\":\"Procuracao obrigatoria\","
                + "\"grau\":\"DISPENSAVEL_COM_JUSTIFICATIVA\"}}]";
        return ProtocoloPendencia.builder()
                .id(protocoloId).uuid(UUID.randomUUID()).protocoloId(protocoloId)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().plusDays(10))
                .violacoesJson(json).requisitosVersao("v1.0")
                .documentosHash("abc123").version(0L).build();
    }

    private ProtocoloPendencia pendenciaAbsoluta(Long protocoloId) {
        String json = "[{\"codigo\":\"DOC_OBRIGATORIO_AUSENTE\",\"severidade\":\"BLOQUEANTE\","
                + "\"campo\":\"DOCUMENTO_IDENTIDADE\",\"acaoCorretiva\":\"Anexe o documento\","
                + "\"fundamento\":{\"tipo\":\"LEI\",\"identificador\":\"CPC art. 319, II\","
                + "\"resumo\":\"Qualificacao das partes\","
                + "\"grau\":\"ABSOLUTO\"}}]";
        return ProtocoloPendencia.builder()
                .id(protocoloId).uuid(UUID.randomUUID()).protocoloId(protocoloId)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().plusDays(10))
                .violacoesJson(json).requisitosVersao("v1.0")
                .documentosHash("def456").version(0L).build();
    }

    private ProtocoloPendencia pendenciaBase(Long protocoloId) {
        return ProtocoloPendencia.builder()
                .id(protocoloId).uuid(UUID.randomUUID()).protocoloId(protocoloId)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().plusDays(10))
                .violacoesJson("[]").requisitosVersao("v1.0")
                .documentosHash("hash").version(0L).build();
    }

    private ResultadoValidacao resultadoBloqueado() {
        FundamentoNormativo fund = new FundamentoNormativo(
                FonteNormativaTipo.LEI, "CPC art. 287", "Procuracao obrigatoria",
                GrauExigibilidade.DISPENSAVEL_COM_JUSTIFICATIVA, null);
        ViolacaoCompletude violacao = new ViolacaoCompletude.DocumentoObrigatorioAusente(
                TipoDocumentoProcessual.PROCURACAO, fund);
        return new ResultadoValidacao(
                ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO, List.of(violacao), "v1.0");
    }
}
