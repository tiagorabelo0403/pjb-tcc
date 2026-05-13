package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRemessaServiceTest {

    @Test
    void deveEnviarRemessaQuandoProcessoExiste() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        Processo processo = Processo.builder().id(1L).numeroUnificado("0001").tribunal("TJCE").classeProcessual("CIVEL").assunto("teste").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(1L, "TJSP", "DECLINIO")).thenReturn(Optional.empty());
        when(remessaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MniHttpClient client = (dest, xml) -> new com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse("PROTO-1");
        MniRemessaService service = new MniRemessaService(processoRepository, remessaRepository, new MniProcessoPayloadAssembler(), client, mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class), new MniRemessaProperties(true, 5, 300000, 10));
        var result = service.enviar(new MniRemessaRequest(1L, "TJSP", "DECLINIO"));
        assertThat(result.success()).isTrue();
        assertThat(result.protocoloDestino()).isEqualTo("PROTO-1");
    }

    @Test
    void deveReprocessarPendentesRespeitandoBatch() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        Processo processo = Processo.builder().id(1L).numeroUnificado("0001").tribunal("TJCE").classeProcessual("CIVEL").assunto("teste").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(remessaRepository.findRetryCandidates(any())).thenReturn(List.of(MniRemessa.builder()
                .processoId(1L)
                .tribunalDestino("TJSP")
                .motivo("DECLINIO")
                .status(MniStatusRemessa.FAILED)
                .tentativas(1)
                .maxTentativas(5)
                .createdAt(Instant.now())
                .build()));
        when(remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(1L, "TJSP", "DECLINIO")).thenReturn(Optional.empty());
        when(remessaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MniHttpClient client = (dest, xml) -> new com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse("PROTO-1");
        MniRemessaService service = new MniRemessaService(processoRepository, remessaRepository, new MniProcessoPayloadAssembler(), client, mock(AuditLedgerService.class), mock(ReadAfterWriteConsistencyPolicy.class), new MniRemessaProperties(true, 5, 300000, 1));
        assertThat(service.reprocessarPendentes()).isEqualTo(1);
    }
}
