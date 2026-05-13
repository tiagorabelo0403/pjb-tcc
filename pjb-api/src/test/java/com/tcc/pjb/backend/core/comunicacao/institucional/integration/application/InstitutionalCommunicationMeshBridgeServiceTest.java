package com.tcc.pjb.backend.core.comunicacao.institucional.integration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalMeshDispatchService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionalCommunicationMeshBridgeServiceTest {

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private DiligenceInstitutionalMeshDispatchService diligenceService;


    @Test
    void deveEspelharComunicacaoNaMalhaInstitucional() {
        Instant now = Instant.now();
        InstitutionalDeliveryJob job = new InstitutionalDeliveryJob(
                "job-1",
                "exp-1",
                10L,
                "0001",
                "MPCE-FAM-001",
                "CAIXA-PRINCIPAL",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                List.of(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL),
                0,
                StatusEntregaInstitucional.PENDENTE,
                0,
                3,
                now,
                now,
                now,
                null,
                null,
                "corr-1",
                null,
                null,
                null,
                List.of("teste"),
                null
        );
        when(diligenceService.routingKeyForProcessualCommunication(anyString(), anyString(), anyString())).thenReturn("MESH:PROCESSUAL:COMMUNICATION:MINISTERIO_PUBLICO:MPCE-FAM-001");
        when(diligenceService.signMeshDigest(anyString())).thenReturn("signature");
        when(outboxPublisher.enqueueTracked(anyString(), anyString(), any(), anyMap(), anyString(), anyString(), anyString())).thenReturn(UUID.randomUUID());

        InstitutionalCommunicationMeshBridgeService service = new InstitutionalCommunicationMeshBridgeService(outboxPublisher, new ObjectMapper(), diligenceService);
        var result = service.espelhar(job);

        assertNotNull(result.outboxEventId());
        assertEquals("WEBHOOK_INSTITUCIONAL", result.channel());
        assertEquals("MPCE-FAM-001", result.meshUnitKey());
    }
}
