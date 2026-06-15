package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRemessaServiceEdgeCasesTest {

    @Test
    void shouldReturnAlreadyConfirmedWithoutCallingHttpClient() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniHttpClient httpClient = mock(MniHttpClient.class);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(Processo.builder().id(1L).build()));
        when(remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(1L, "TJCE", "CARTA_PRECATORIA"))
                .thenReturn(Optional.of(MniRemessa.builder().processoId(1L).tribunalDestino("TJCE").motivo("CARTA_PRECATORIA").status(MniStatusRemessa.CONFIRMED).protocoloDestino("P-1").build()));
        MniRemessaService service = new MniRemessaService(processoRepository, remessaRepository, new MniProcessoPayloadAssembler(), httpClient, mock(AuditLedgerService.class), new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5)), new MniRemessaProperties(true,5,300000,5));
        var result = service.enviar(new MniRemessaRequest(1L, "TJCE", "CARTA_PRECATORIA"));
        assertThat(result.alreadyConfirmed()).isTrue();
        verifyNoInteractions(httpClient);
    }

    @Test
    void shouldSupersedeWhenProcessMissingOnRetry() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniRemessa remessa = MniRemessa.builder().id(1L).processoId(99L).tribunalDestino("TJCE").motivo("DECLINIO").status(MniStatusRemessa.FAILED).proximoRetryEm(Instant.now()).build();
        when(remessaRepository.findRetryCandidates(any())).thenReturn(List.of(remessa));
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());
        when(remessaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MniRemessaService service = new MniRemessaService(processoRepository, remessaRepository, new MniProcessoPayloadAssembler(), mock(MniHttpClient.class), mock(AuditLedgerService.class), new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5)), new MniRemessaProperties(true,5,300000,5));
        var result = service.reprocessarPendentes(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaBatchCommand(1));
        assertThat(result.superseded()).isEqualTo(1);
        assertThat(remessa.getStatus()).isEqualTo(MniStatusRemessa.SUPERSEDED);
    }
}
