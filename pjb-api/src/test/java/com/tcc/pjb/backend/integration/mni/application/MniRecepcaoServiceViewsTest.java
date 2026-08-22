package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniAdapterResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MniRecepcaoServiceViewsTest {

    @Test
    void shouldReceiveBuildViewsAndMarkWrite() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        Processo processo = Processo.builder().id(55L).numeroUnificado("0001-22.2026.8.06.0001").build();
        AtomicReference<MniRecepcao> savedRecepcao = new AtomicReference<>();
        when(adapter.fromXml("<mni/>", "TJCE", "DECLINIO_COMPETENCIA")).thenReturn(new MniAdapterResult(processo, java.util.List.of(), java.util.List.of(), java.util.List.of()));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.findByMniPayloadHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(recepcaoRepository.save(org.mockito.ArgumentMatchers.any(MniRecepcao.class))).thenAnswer(invocation -> {
            MniRecepcao entity = invocation.getArgument(0);
            MniRecepcao persisted = MniRecepcao.builder()
                    .id(77L)
                    .tribunalOrigem(entity.getTribunalOrigem())
                    .numeroUnificado(entity.getNumeroUnificado())
                    .processoIdLocal(entity.getProcessoIdLocal())
                    .motivo(entity.getMotivo())
                    .mniPayloadHash(entity.getMniPayloadHash())
                    .receivedAt(entity.getReceivedAt())
                    .processedAt(entity.getProcessedAt())
                    .status(entity.getStatus())
                    .build();
            savedRecepcao.set(persisted);
            return persisted;
        });
        when(recepcaoRepository.findById(77L)).thenAnswer(invocation -> Optional.ofNullable(savedRecepcao.get()));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), mock(MovimentacaoProcessualRepository.class), mock(MniDocumentoIngestaoService.class));

        var result = service.receberAutos(new MniRecepcaoCommand("TJCE", "DECLINIO_COMPETENCIA", "<mni/>"));
        var query = service.consultar(new MniRecepcaoQuery(77L));
        var envelope = service.envelope(77L);
        var timeline = service.timeline(77L);
        var status = service.statusSnapshot(77L);

        assertThat(result.processoIdLocal()).isEqualTo(55L);
        assertThat(query.projection().status()).isEqualTo("PROCESSED");
        assertThat(envelope.ato().motivo()).isEqualTo("DECLINIO_COMPETENCIA");
        assertThat(timeline).hasSize(2);
        assertThat(status.status()).isEqualTo("PROCESSED");
        verify(rawPolicy).markWrite();
    }

    @Test
    void shouldReuseExistingReceptionWhenPayloadHashAlreadyExists() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        MniRecepcao existing = MniRecepcao.builder()
                .id(90L)
                .tribunalOrigem("TJCE")
                .numeroUnificado("0001-22.2026.8.06.0001")
                .processoIdLocal(55L)
                .motivo("CARTA_PRECATORIA")
                .mniPayloadHash("hash-1")
                .receivedAt(Instant.parse("2026-04-11T12:00:00Z"))
                .processedAt(Instant.parse("2026-04-11T12:01:00Z"))
                .status("PROCESSED")
                .build();
        when(recepcaoRepository.findByMniPayloadHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(existing));
        when(recepcaoRepository.findById(90L)).thenReturn(Optional.of(existing));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), mock(PoloProcessualApplicationService.class), new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), mock(MovimentacaoProcessualRepository.class), mock(MniDocumentoIngestaoService.class));

        var result = service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<same/>"));
        var audit = service.audit(90L);

        assertThat(result.processoIdLocal()).isEqualTo(55L);
        assertThat(audit.status()).isEqualTo("PROCESSED");
        verify(processoRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        verify(rawPolicy, org.mockito.Mockito.never()).markWrite();
    }

    private static ComarcaResolutionService comarcaResolutionServiceVazio() {
        ComarcaResolutionService service = mock(ComarcaResolutionService.class);
        when(service.resolver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        return service;
    }
}
