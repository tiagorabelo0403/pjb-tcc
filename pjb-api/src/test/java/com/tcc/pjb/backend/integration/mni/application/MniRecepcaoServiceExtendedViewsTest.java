package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniAdapterResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRecepcaoServiceExtendedViewsTest {

    @Test
    void shouldExposeFailureHealthEnvelopeAndPayloadViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        Processo processo = Processo.builder().id(55L).numeroUnificado("0001-22.2026.8.06.0001").build();
        when(adapter.fromXml("<mni/>", "TJCE", "CARTA_PRECATORIA")).thenReturn(new MniAdapterResult(processo, java.util.List.of()));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.findByMniPayloadHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(recepcaoRepository.save(org.mockito.ArgumentMatchers.any(MniRecepcao.class))).thenAnswer(invocation -> {
            MniRecepcao entity = invocation.getArgument(0);
            return MniRecepcao.builder()
                    .id(101L)
                    .tribunalOrigem(entity.getTribunalOrigem())
                    .numeroUnificado(entity.getNumeroUnificado())
                    .processoIdLocal(entity.getProcessoIdLocal())
                    .motivo(entity.getMotivo())
                    .mniPayloadHash(entity.getMniPayloadHash())
                    .receivedAt(entity.getReceivedAt())
                    .processedAt(entity.getProcessedAt())
                    .status("FAIL_TEMPORARIO")
                    .build();
        });

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator());
        var result = service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));
        when(recepcaoRepository.findById(101L)).thenReturn(Optional.of(MniRecepcao.builder()
                .id(101L)
                .tribunalOrigem("TJCE")
                .numeroUnificado(result.numeroUnificado())
                .processoIdLocal(result.processoIdLocal())
                .motivo("CARTA_PRECATORIA")
                .mniPayloadHash(result.payloadHash())
                .receivedAt(Instant.parse("2026-04-11T12:00:00Z"))
                .processedAt(Instant.parse("2026-04-11T12:01:00Z"))
                .status("FAIL_TEMPORARIO")
                .build()));

        var failure = service.failureResult(101L);
        var health = service.healthSnapshot(101L);
        var envelopeView = service.envelopeView(101L);
        var payload = service.payloadSnapshot(101L);

        assertThat(failure.status()).isEqualTo("FAIL_TEMPORARIO");
        assertThat(failure.failureReason()).isEqualTo("FAIL_TEMPORARIO");
        assertThat(health.status()).isEqualTo("FAIL_TEMPORARIO");
        assertThat(health.payloadHashPresent()).isTrue();
        assertThat(envelopeView.motivo()).isEqualTo("CARTA_PRECATORIA");
        assertThat(envelopeView.status()).isEqualTo("FAIL_TEMPORARIO");
        assertThat(payload.payloadHash()).isNotBlank();
    }
}
