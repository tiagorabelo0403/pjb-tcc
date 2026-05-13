package com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain.ResolucaoDestinatarioProcessualRequest;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;

class DestinatarioProcessualResolverApplicationServiceTest {

    private final DestinatarioProcessualResolverApplicationService service = new DestinatarioProcessualResolverApplicationService();

    @Test
    void shouldResolveInstitutionalTrackForExplicitUnit() {
        var result = service.resolver(new ResolucaoDestinatarioProcessualRequest(
                1L,
                "0001",
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_DEFENSOR,
                NationalCommunicationRecipientKind.PESSOA_JURIDICA,
                DestinatarioProcessualKind.UNIDADE_INSTITUCIONAL,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                "MP-CE-FAM-001",
                "00.000.000/0001-00",
                "Promotoria de Família",
                "mp@example.com",
                null,
                null,
                null,
                "CE",
                "Fortaleza",
                "Foro Central",
                null,
                null,
                null,
                true,
                false,
                true
        ));
        assertEquals(TrilhoComunicacaoProcessual.INSTITUCIONAL_CAIXA, result.trilho());
        assertTrue(result.usaFluxoInstitucional());
        assertEquals(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, result.destinatario().destinatarioInstitucionalKind());
    }

    @Test
    void shouldResolveRepresentationTrackForAdvogado() {
        var result = service.resolver(new ResolucaoDestinatarioProcessualRequest(
                1L,
                "0001",
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                NationalCommunicationRecipientKind.ADVOGADO_OAB,
                null,
                null,
                null,
                null,
                "111.111.111-11",
                "Advogado",
                "adv@example.com",
                null,
                "12345",
                null,
                "CE",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true
        ));
        assertEquals(TrilhoComunicacaoProcessual.REPRESENTACAO_PROCESSUAL, result.trilho());
        assertEquals(DestinatarioProcessualKind.ADVOGADO, result.destinatario().kind());
    }
}
