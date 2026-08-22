package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRecepcaoServiceFailureAndIdempotencyTest {

    @Test
    void shouldReuseExistingRecepcaoForSamePayloadHashWithoutWritingAgain() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        MniRecepcao existing = MniRecepcao.builder()
                .id(91L)
                .tribunalOrigem("TJSP")
                .numeroUnificado("000091")
                .processoIdLocal(55L)
                .motivo("CARTA")
                .mniPayloadHash(com.tcc.pjb.backend.core.util.Hashes.sha256Hex("<mni/>") )
                .status("PROCESSED")
                .receivedAt(Instant.now())
                .processedAt(Instant.now())
                .build();
        when(recepcaoRepository.findByMniPayloadHash(existing.getMniPayloadHash())).thenReturn(Optional.of(existing));
        MniRecepcaoService service = new MniRecepcaoService(
                processoRepository,
                recepcaoRepository,
                mock(MniXmlToProcessoAdapter.class),
                rawPolicy,
                mock(AuditLedgerService.class),
                new PoloCompositionPolicy(),
                mock(PoloProcessualApplicationService.class),
                new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(),
                mock(MovimentacaoProcessualRepository.class),
                mock(MniDocumentoIngestaoService.class));

        var result = service.receberAutos(new MniRecepcaoCommand("TJSP", "CARTA", "<mni/>"));

        assertThat(result.processoIdLocal()).isEqualTo(55L);
        verify(processoRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(rawPolicy, never()).markWrite();
    }

    @Test
    void shouldPropagateAdapterFailureWhenXmlCannotBeConverted() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        when(recepcaoRepository.findByMniPayloadHash(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        when(adapter.fromXml("<broken>", "TJSP", "CARTA")).thenThrow(new IllegalStateException("xml inválido"));
        MniRecepcaoService service = new MniRecepcaoService(
                processoRepository,
                recepcaoRepository,
                adapter,
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class),
                new PoloCompositionPolicy(),
                mock(PoloProcessualApplicationService.class),
                new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(),
                mock(MovimentacaoProcessualRepository.class),
                mock(MniDocumentoIngestaoService.class));

        assertThatThrownBy(() -> service.receberAutos(new MniRecepcaoCommand("TJSP", "CARTA", "<broken>")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("xml inválido");
    }

    private static ComarcaResolutionService comarcaResolutionServiceVazio() {
        ComarcaResolutionService service = mock(ComarcaResolutionService.class);
        when(service.resolver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        return service;
    }
}
