package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaBatchCommand;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRemessaServiceBatchReprocessTest {

    @Test
    void shouldRespectCommandLimitWhenReprocessingPendentes() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniHttpClient httpClient = mock(MniHttpClient.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);

        Processo processo = Processo.builder().id(1L).numeroUnificado("0001").tribunal("TJCE").classeProcessual("CIVIL").assunto("tema").build();
        MniRemessa first = MniRemessa.builder().id(10L).processoId(1L).tribunalDestino("TJCE").motivo("DECLINIO").status(MniStatusRemessa.FAILED).createdAt(Instant.now()).maxTentativas(5).build();
        MniRemessa second = MniRemessa.builder().id(11L).processoId(1L).tribunalDestino("TJCE").motivo("DECLINIO").status(MniStatusRemessa.FAILED).createdAt(Instant.now()).maxTentativas(5).build();
        when(remessaRepository.findRetryCandidates(any())).thenReturn(List.of(first, second));
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(1L, "TJCE", "DECLINIO")).thenReturn(Optional.of(first));
        when(httpClient.enviarAutos(any(), any())).thenReturn(new MniHttpResponse("PROTO-1"));
        when(remessaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRemessaService service = new MniRemessaService(
                processoRepository,
                remessaRepository,
                new MniProcessoPayloadAssembler(),
                httpClient,
                mock(AuditLedgerService.class),
                rawPolicy,
                new MniRemessaProperties(true, 5, 300000, 5));

        var summary = service.reprocessarPendentes(new MniRemessaBatchCommand(1));

        assertThat(summary.processadas()).isEqualTo(1);
        assertThat(summary.ignoradas()).isZero();
        verify(httpClient).enviarAutos(any(), any());
    }

    @Test
    void shouldReturnZeroSummaryWhenFeatureDisabled() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniHttpClient httpClient = mock(MniHttpClient.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        MniRemessaService service = new MniRemessaService(
                processoRepository,
                remessaRepository,
                new MniProcessoPayloadAssembler(),
                httpClient,
                mock(AuditLedgerService.class),
                rawPolicy,
                new MniRemessaProperties(false, 5, 300000, 5));

        var summary = service.reprocessarPendentes(new MniRemessaBatchCommand(3));

        assertThat(summary.processadas()).isZero();
        assertThat(summary.superseded()).isZero();
        assertThat(summary.ignoradas()).isZero();
        verify(remessaRepository, never()).findRetryCandidates(any());
    }
}
