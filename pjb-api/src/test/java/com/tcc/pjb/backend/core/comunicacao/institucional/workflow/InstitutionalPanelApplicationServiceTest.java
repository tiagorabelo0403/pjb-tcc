package com.tcc.pjb.backend.core.comunicacao.institucional.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalPanelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalPanelApplicationServiceTest {

    @Test
    void mustAggregatePanelAndQueueUsingCanonicalTopologyKeys() {
        InstitutionalInboxStateRepository repository = mock(InstitutionalInboxStateRepository.class);
        when(repository.findByUnidadeCodigo("UNI-1")).thenReturn(List.of(
                item("exp-1", " UNI-1 ", "MPCE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, " CX-TRIAGEM ", StatusComunicacaoInstitucional.DISPONIBILIZADA, Instant.parse("2026-04-03T10:00:00Z")),
                item("exp-2", "UNI-1", "MPCE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CX-TRIAGEM", StatusComunicacaoInstitucional.RECEBIDA, Instant.parse("2026-04-03T09:00:00Z")),
                item("exp-3", "UNI-1", "MPCE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CX-ANALISE", StatusComunicacaoInstitucional.CUMPRIDA, Instant.parse("2026-04-04T08:00:00Z"))
        ));
        InstitutionalPanelApplicationService service = new InstitutionalPanelApplicationService(repository);

        var painel = service.painelOrgao("UNI-1");
        var filas = service.filasUnidade("UNI-1");

        assertEquals(1, painel.size());
        assertEquals(3L, painel.get(0).totalExpedientes());
        assertEquals(2, painel.get(0).caixasVisiveis().size());
        assertEquals(2, filas.size());
        assertEquals("CX-ANALISE", filas.get(0).caixaCodigo());
        assertEquals("CX-TRIAGEM", filas.get(1).caixaCodigo());
        assertEquals(2L, filas.get(1).total());
    }


    @Test
    void mustReuseShortCacheForRepeatedUnitPanelReads() {
        InstitutionalInboxStateRepository repository = mock(InstitutionalInboxStateRepository.class);
        when(repository.findByUnidadeCodigo("UNI-1")).thenReturn(List.of(
                item("exp-1", "UNI-1", "MPCE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "CX-TRIAGEM", StatusComunicacaoInstitucional.DISPONIBILIZADA, Instant.parse("2026-04-03T10:00:00Z"))
        ));
        InstitutionalPanelApplicationService service = new InstitutionalPanelApplicationService(repository);

        service.painelOrgao("UNI-1");
        service.filasUnidade("uni-1");

        verify(repository, times(1)).findByUnidadeCodigo("UNI-1");
    }

    private InstitutionalInboxItem item(String expedicaoUuid,
                                        String unidadeCodigo,
                                        String unidadeSigla,
                                        DestinatarioInstitucionalKind destinatarioKind,
                                        String caixaCodigo,
                                        StatusComunicacaoInstitucional status,
                                        Instant prazoResposta) {
        Instant now = Instant.parse("2026-04-03T08:00:00Z");
        return new InstitutionalInboxItem(
                expedicaoUuid + "-item",
                expedicaoUuid,
                10L,
                "0001",
                unidadeCodigo,
                unidadeSigla,
                destinatarioKind,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_PORTAL,
                caixaCodigo,
                caixaCodigo,
                "PJB_INBOX",
                status,
                "GATE-1",
                false,
                null,
                null,
                now,
                null,
                null,
                null,
                now.plusSeconds(3600),
                prazoResposta,
                now,
                List.of("seed"),
                "hash-" + expedicaoUuid
        );
    }
}
