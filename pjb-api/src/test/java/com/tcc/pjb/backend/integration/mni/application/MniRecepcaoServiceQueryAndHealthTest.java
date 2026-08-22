package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniAdapterResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRecepcaoCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoQuery;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRecepcaoServiceQueryAndHealthTest {

    @Test
    void shouldExposeQueryHealthStatusAndPayloadAudit() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        MniRecepcao recepcao = MniRecepcao.builder()
                .id(44L)
                .tribunalOrigem("TJCE")
                .numeroUnificado("0000123-45.2026.8.06.0001")
                .processoIdLocal(77L)
                .motivo("CARTA_ORDENATORIA")
                .mniPayloadHash("hash-abc")
                .receivedAt(Instant.parse("2026-04-11T12:00:00Z"))
                .processedAt(Instant.parse("2026-04-11T12:01:00Z"))
                .status("PROCESSED")
                .build();
        when(recepcaoRepository.findById(44L)).thenReturn(Optional.of(recepcao));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), mock(MovimentacaoProcessualRepository.class), mock(MniDocumentoIngestaoService.class));

        var consulta = service.consultar(new MniConsultaRecepcaoCommand(44L));
        var query = service.consultar(new MniRecepcaoQuery(44L));
        var health = service.healthSnapshot(44L);
        var status = service.statusSnapshot(44L);
        var envelopeView = service.envelopeView(44L);
        var payloadAudit = service.payloadAudit(44L);

        assertThat(query.envelope().numeroUnificado()).isEqualTo("0000123-45.2026.8.06.0001");
        assertThat(query.envelope().payloadHash()).isEqualTo("hash-abc");
        assertThat(health.processed()).isTrue();
        assertThat(status.status()).isEqualTo("PROCESSED");
        assertThat(envelopeView.status()).isEqualTo("PROCESSED");
        assertThat(payloadAudit.payloadHash()).isEqualTo("hash-abc");
    }

    @Test
    void shouldThrowWhenReceptionDoesNotExist() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        when(recepcaoRepository.findById(999L)).thenReturn(Optional.empty());

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), mock(MovimentacaoProcessualRepository.class), mock(MniDocumentoIngestaoService.class));

        assertThatThrownBy(() -> service.envelope(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MNI recepção não encontrada");
    }

    @Test
    void shouldReceiveAndThenExposeTimelineWithTwoEntries() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        Processo processo = Processo.builder().id(81L).numeroUnificado("0001-11.2026.8.06.0001").build();
        when(adapter.fromXml("<xml/>", "TJCE", "COOPERACAO")).thenReturn(new MniAdapterResult(processo, java.util.List.of(), java.util.List.of(), java.util.List.of()));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.findByMniPayloadHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(recepcaoRepository.save(org.mockito.ArgumentMatchers.any(MniRecepcao.class))).thenAnswer(invocation -> {
            MniRecepcao entity = invocation.getArgument(0);
            return MniRecepcao.builder()
                    .id(82L)
                    .tribunalOrigem(entity.getTribunalOrigem())
                    .numeroUnificado(entity.getNumeroUnificado())
                    .processoIdLocal(entity.getProcessoIdLocal())
                    .motivo(entity.getMotivo())
                    .mniPayloadHash(entity.getMniPayloadHash())
                    .receivedAt(entity.getReceivedAt())
                    .processedAt(entity.getProcessedAt())
                    .status(entity.getStatus())
                    .build();
        });
        when(recepcaoRepository.findById(82L)).thenReturn(Optional.of(MniRecepcao.builder()
                .id(82L)
                .tribunalOrigem("TJCE")
                .numeroUnificado("0001-11.2026.8.06.0001")
                .processoIdLocal(81L)
                .motivo("COOPERACAO")
                .mniPayloadHash("h")
                .receivedAt(Instant.parse("2026-04-11T12:00:00Z"))
                .processedAt(Instant.parse("2026-04-11T12:00:30Z"))
                .status("PROCESSED")
                .build()));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), mock(MovimentacaoProcessualRepository.class), mock(MniDocumentoIngestaoService.class));
        var result = service.receberAutos(new MniRecepcaoCommand("TJCE", "COOPERACAO", "<xml/>"));
        var timeline = service.timeline(82L);

        assertThat(result.processoIdLocal()).isEqualTo(81L);
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).evento()).isEqualTo("RECEBIDO");
        assertThat(timeline.get(1).evento()).isEqualTo("PROCESSADO");
    }

    private static ComarcaResolutionService comarcaResolutionServiceVazio() {
        ComarcaResolutionService service = mock(ComarcaResolutionService.class);
        when(service.resolver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        return service;
    }
}
