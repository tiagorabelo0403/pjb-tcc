package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaHealthQuery;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaTimelineQuery;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.integration.mni.infra.MniHttpClient;
import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MniRemessaServiceHealthViewsTest {

    @Test
    void shouldExposeHealthEndpointPayloadAndWindowViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniRemessa remessa = MniRemessa.builder()
                .id(15L)
                .processoId(25L)
                .tribunalDestino("TJCE")
                .motivo("CARTA_PRECATORIA")
                .status(MniStatusRemessa.FAILED)
                .mniPayloadHash("hash-15")
                .protocoloDestino("PROTO-15")
                .createdAt(Instant.parse("2026-04-11T12:00:00Z"))
                .proximoRetryEm(Instant.parse("2026-04-11T12:30:00Z"))
                .tentativas(2)
                .maxTentativas(5)
                .failureReason("timeout")
                .build();
        when(remessaRepository.findById(15L)).thenReturn(Optional.of(remessa));
        MniRemessaProperties properties = new MniRemessaProperties(true, 5, 300000, 5);
        MniRemessaService service = new MniRemessaService(
                processoRepository,
                remessaRepository,
                new MniProcessoPayloadAssembler(),
                mock(MniHttpClient.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                properties);

        var health = service.health(new MniRemessaHealthQuery(15L));
        var snapshot = service.healthSnapshot(15L);
        var endpoint = service.endpointView("TJCE");
        var payload = service.payloadView(15L);
        var window = service.windowSnapshot(15L);
        var timeline = service.timeline(new MniRemessaTimelineQuery(15L));

        assertThat(health.healthy()).isFalse();
        assertThat(snapshot.failurePresent()).isTrue();
        assertThat(endpoint.tribunalCodigo()).isEqualTo("TJCE");
        assertThat(payload.payloadHash()).isEqualTo("hash-15");
        assertThat(window.tentativas()).isEqualTo(2);
        assertThat(timeline.entries()).extracting("evento").contains("CRIADA", "FALHA");
    }
}
