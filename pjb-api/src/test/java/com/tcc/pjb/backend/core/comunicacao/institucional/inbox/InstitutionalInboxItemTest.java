package com.tcc.pjb.backend.core.comunicacao.institucional.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalInboxItemTest {

    @Test
    void shouldAdvanceInboxLifecycle() {
        Instant now = Instant.parse("2026-03-20T12:00:00Z");
        InstitutionalInboxItem item = new InstitutionalInboxItem(
                "item-1",
                "exp-1",
                10L,
                "0001",
                "MP-CE-01",
                "MPCE",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_PORTAL,
                "CAIXA-MP-01",
                "CAIXA-MP-01",
                "PJB_INBOX",
                StatusComunicacaoInstitucional.DISPONIBILIZADA,
                "GATE-MP",
                true,
                null,
                null,
                now,
                null,
                null,
                null,
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                now,
                List.of("inicial"),
                "hash-1"
        );
        InstitutionalInboxItem received = item.withRecebimento(5L, now.plusSeconds(30), "hash-2", List.of("recebido"));
        InstitutionalInboxItem aware = received.withCiencia(5L, now.plusSeconds(60), "hash-3", List.of("ciencia"));
        InstitutionalInboxItem done = aware.withCumprimento(5L, now.plusSeconds(90), "hash-4", List.of("cumprido"));

        assertEquals(StatusComunicacaoInstitucional.RECEBIDA, received.status());
        assertEquals(StatusComunicacaoInstitucional.CIENTIFICADA, aware.status());
        assertEquals(StatusComunicacaoInstitucional.CUMPRIDA, done.status());
    }
}
