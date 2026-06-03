package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeOverrideService;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenciaApplicationService;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.completude.OverrideCompletudeRequest;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloCompletudeOutboxEntity;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloCompletudeOutboxRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloPendenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloValidacaoHistoricoRepository;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
    private ObjectMapper objectMapper;

    private ProtocoloCompletudeOverrideService overrideService;
    private ProtocoloPendenciaApplicationService pendenciaService;

    @BeforeEach
    void setUp() {
        pendenciaRepository = mock(ProtocoloPendenciaRepository.class);
        historicoRepository = mock(ProtocoloValidacaoHistoricoRepository.class);
        outboxRepository = mock(ProtocoloCompletudeOutboxRepository.class);
        objectMapper = new ObjectMapper();

        overrideService = new ProtocoloCompletudeOverrideService(
                pendenciaRepository, historicoRepository, objectMapper);
        pendenciaService = new ProtocoloPendenciaApplicationService(
                pendenciaRepository, historicoRepository, outboxRepository, objectMapper);
    }

    @Test
    void override_dispensa_requisito_dispensavel_e_audita() {
        ProtocoloPendencia pendencia = pendenciaDispensavel(1L);
        when(pendenciaRepository.findAtivaByProtocoloId(1L)).thenReturn(Optional.of(pendencia));
        when(pendenciaRepository.save(any())).thenReturn(pendencia);

        ProtocoloPendencia dispensada = overrideService.dispensar(
                1L,
                "Partes identificadas por certidão judicial. Documentos apresentados presencialmente.",
                99L);

        assertThat(dispensada.getStatus()).isEqualTo(ProtocoloCompletudeStatus.DISPENSADO);
        assertThat(dispensada.getDispensadoPor()).isEqualTo(99L);
        verify(historicoRepository).save(any());
    }

    @Test
    void override_rejeita_requisito_absoluto() {
        ProtocoloPendencia pendencia = pendenciaAbsoluta(2L);
        when(pendenciaRepository.findAtivaByProtocoloId(2L)).thenReturn(Optional.of(pendencia));

        assertThatThrownBy(() ->
                overrideService.dispensar(
                        2L,
                        "Justificativa suficientemente longa para passar na validação bean.",
                        99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ABSOLUTA");
    }

    @Test
    void override_request_rejeita_justificativa_curta() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new OverrideCompletudeRequest("curta demais"));

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("justificativa");
    }

    @Test
    void override_request_aceita_justificativa_valida() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new OverrideCompletudeRequest(
                "Documento apresentado presencialmente na secretaria e conferido."));

        assertThat(violations).isEmpty();
    }

    @Test
    void registrar_pendencia_grava_no_outbox() {
        ProtocoloPendencia salva = ProtocoloPendencia.builder()
                .id(10L).uuid(UUID.randomUUID()).protocoloId(42L)
                .status(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO)
                .prazoRegularizacao(LocalDate.now().plusDays(10))
                .violacoesJson("[]").requisitosVersao("v1.0")
                .documentosHash("hash").version(0L).build();
        when(pendenciaRepository.findAtivaByProtocoloId(42L)).thenReturn(Optional.empty());
        when(pendenciaRepository.save(any())).thenReturn(salva);

        pendenciaService.registrarPendencia(
                42L, resultadoBloqueado(), List.of("PROCURACAO"), OrigemValidacao.PROTOCOLO, 1L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("PROTOCOLO_PENDENTE_DOCUMENTACAO");
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

        pendenciaService.escalarExpirada(77L);

        ArgumentCaptor<ProtocoloCompletudeOutboxEntity> captor =
                ArgumentCaptor.forClass(ProtocoloCompletudeOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("PROTOCOLO_EXPIRADO");
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

    // ---- fixtures ----

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
