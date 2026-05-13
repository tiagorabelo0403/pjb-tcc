package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.adapter.MniProcessoPayloadAssembler;
import com.tcc.pjb.backend.integration.mni.domain.MniConsultaRemessaCommand;
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

class MniRemessaServiceViewsTest {

    @Test
    void shouldExposeConsultaStatusTimelineAndHealth() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRemessaRepository remessaRepository = mock(MniRemessaRepository.class);
        MniRemessa remessa = MniRemessa.builder()
                .id(3L)
                .processoId(9L)
                .tribunalDestino("TJCE")
                .motivo("DECLINIO")
                .status(MniStatusRemessa.CONFIRMED)
                .mniPayloadHash("hash-1")
                .protocoloDestino("PROTO-9")
                .createdAt(Instant.parse("2026-04-11T10:00:00Z"))
                .sentAt(Instant.parse("2026-04-11T10:01:00Z"))
                .confirmedAt(Instant.parse("2026-04-11T10:02:00Z"))
                .tentativas(1)
                .maxTentativas(5)
                .build();
        when(remessaRepository.findById(3L)).thenReturn(Optional.of(remessa));
        MniRemessaService service = new MniRemessaService(
                processoRepository,
                remessaRepository,
                new MniProcessoPayloadAssembler(),
                mock(MniHttpClient.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new MniRemessaProperties(true, 5, 300000, 5));

        var consulta = service.consultar(new MniConsultaRemessaCommand(3L));
        var status = service.statusSnapshot(3L);
        var timeline = service.timeline(new MniRemessaTimelineQuery(3L));
        var health = service.health(new MniRemessaHealthQuery(3L));
        var window = service.windowSnapshot(3L);

        assertThat(consulta.remessa().status()).isEqualTo("CONFIRMED");
        assertThat(status.protocoloDestino()).isEqualTo("PROTO-9");
        assertThat(timeline.entries()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(health.confirmed()).isTrue();
        assertThat(window.maxTentativas()).isEqualTo(5);
    }
}
