package com.tcc.pjb.backend.core.comunicacao.institucional.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalDeliveryJobTest {

    @Test
    void shouldAdvanceToFallbackChannelWhenAvailable() {
        Instant base = Instant.parse("2026-03-20T10:00:00Z");
        InstitutionalDeliveryJob job = new InstitutionalDeliveryJob(
                "job-1",
                "exp-1",
                1L,
                "0001",
                "MP-CE",
                "CAIXA-TRIAGEM",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                List.of(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO, CanalComunicacaoInstitucional.PJB_INBOX),
                0,
                StatusEntregaInstitucional.PENDENTE,
                0,
                4,
                base,
                base,
                base,
                null,
                null,
                "corr",
                null,
                null,
                null,
                List.of("seed"),
                null
        );

        InstitutionalDeliveryJob advanced = job.withAdvancedFallback(base.plusSeconds(5), base.plusSeconds(10), MotivoFalhaEntregaInstitucional.INTEGRACAO_INDISPONIVEL, "fallback");

        assertEquals(CanalComunicacaoInstitucional.PJB_INBOX, advanced.currentChannel());
        assertEquals(StatusEntregaInstitucional.AGUARDANDO_RETRY, advanced.status());
        assertEquals(1, advanced.attemptCount());
        assertTrue(advanced.justificativas().stream().anyMatch(v -> v.contains("fallback->PJB_INBOX")));
    }

    @Test
    void shouldMarkDeliveredAsTerminal() {
        Instant base = Instant.parse("2026-03-20T10:00:00Z");
        InstitutionalDeliveryJob job = new InstitutionalDeliveryJob(
                "job-2",
                "exp-2",
                1L,
                "0002",
                "DP-BA",
                "CAIXA-UNIDADE",
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                List.of(CanalComunicacaoInstitucional.PJB_INBOX),
                0,
                StatusEntregaInstitucional.PENDENTE,
                0,
                4,
                base,
                base,
                base,
                null,
                null,
                "corr-2",
                null,
                null,
                null,
                List.of(),
                null
        );

        InstitutionalDeliveryJob delivered = job.withEntregue(base.plusSeconds(1), "PROTO", "ok");

        assertEquals(StatusEntregaInstitucional.ENTREGUE, delivered.status());
        assertTrue(delivered.status().isTerminal());
        assertEquals("PROTO", delivered.providerReference());
    }
}
