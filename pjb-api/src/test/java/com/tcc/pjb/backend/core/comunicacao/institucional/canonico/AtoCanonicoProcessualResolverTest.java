package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

class AtoCanonicoProcessualResolverTest {

    private final AtoCanonicoProcessualResolver resolver = new AtoCanonicoProcessualResolver(new AtoCanonicoComunicacaoMapper());

    @Test
    void shouldResolveMpInteresseIncapazForFamilyCase() {
        ResolucaoAtoCanonicoResult result = resolver.resolver(new ResolucaoAtoCanonicoRequest(
                10L,
                "0001234-56.2026.8.06.0001",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                FaseProcessual.CONHECIMENTO,
                "Divórcio litigioso",
                "Guarda e alimentos",
                "Regulamentação de convivência de criança menor",
                "Fixação de alimentos para menor",
                "CE",
                "Fortaleza",
                "Foro de Fortaleza",
                true,
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals(AtoCanonicoProcessual.ABRIR_VISTA_MP_INTERESSE_INCAPAZ, result.atoCanonico());
        assertEquals("MINISTERIO_PUBLICO", result.politica().destinatarioKind().name());
        assertTrue(result.politica().bloqueiaFluxo());
    }

    @Test
    void shouldResolvePrisonerProductionForCustodyHearing() {
        ResolucaoAtoCanonicoResult result = resolver.resolver(new ResolucaoAtoCanonicoRequest(
                11L,
                "0009876-00.2026.8.26.0001",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                FaseProcessual.AUDIENCIA_CUSTODIA,
                "Ação penal",
                "Audiência de custódia",
                "Réu preso recolhido em unidade prisional",
                "Requisição de apresentação do custodiado",
                "SP",
                "São Paulo",
                "Foro Central Criminal",
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        ));

        assertEquals(AtoCanonicoProcessual.REQUISITAR_APRESENTACAO_REU_PRESO, result.atoCanonico());
        assertTrue(result.justificativas().stream().anyMatch(v -> v.contains("réu preso") || v.contains("apresentação")));
    }
}
