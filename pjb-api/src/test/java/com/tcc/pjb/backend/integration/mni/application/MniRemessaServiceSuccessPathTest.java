package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniHttpResponse;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaRequest;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRemessaServiceSuccessPathTest {

    @Test
    void shouldConfirmRemessaWhenHttpCallSucceeds() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniHttpClient httpClient = mock(MniHttpClient.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        Processo processo = Processo.builder().id(1L).numeroUnificado("0001").tribunal("TJCE").classeProcessual("CIVIL").assunto("tema").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(remessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(1L, "TJCE", "DECLINIO")).thenReturn(Optional.empty());
        when(remessaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(httpClient.enviarAutos(any(), any())).thenReturn(new MniHttpResponse("PROTO-1"));
        MniRemessaService service = new MniRemessaService(
                processoRepository,
                remessaRepository,
                new MniProcessoPayloadAssembler(),
                httpClient,
                mock(AuditLedgerService.class),
                rawPolicy,
                new MniRemessaProperties(true, 5, 300000, 5));

        var result = service.enviar(new MniRemessaRequest(1L, "TJCE", "DECLINIO"));

        assertThat(result.success()).isTrue();
        assertThat(result.protocoloDestino()).isEqualTo("PROTO-1");
        verify(rawPolicy).markWrite();
    }
}
