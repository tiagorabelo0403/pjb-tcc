package com.tcc.pjb.backend.core.comunicacao.institucional.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoComunicacaoMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.AtoCanonicoProcessualResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.ResolucaoAtoCanonicoRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCompetenceRuleStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.PoliticaEntregaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.PrazoEntregaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.UnitResolutionService;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

class InstitutionalFamilyWithIncapazJourneyTest {

    @Test
    void shouldResolveMandatoryMpViewAndFlowGateForFamilyCaseWithChild() {
        AtoCanonicoProcessualResolver atoResolver = new AtoCanonicoProcessualResolver(new AtoCanonicoComunicacaoMapper());
        var ato = atoResolver.resolver(new ResolucaoAtoCanonicoRequest(
                101L,
                "0000101-22.2026.8.26.0001",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                FaseProcessual.CONHECIMENTO,
                "Ação de divórcio consensual",
                "guarda e alimentos de menor",
                "homologação de acordo com criança",
                "homologação de guarda compartilhada e alimentos",
                "SP",
                "São Paulo",
                null,
                true,
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
                false,
                false
        ));
        assertEquals(AtoCanonicoProcessual.ABRIR_VISTA_MP_INTERESSE_INCAPAZ, ato.atoCanonico());

        InstitutionalCatalogGovernanceOverlayService overlay = new InstitutionalCatalogGovernanceOverlayService(
                new InstitutionalCatalogGovernanceStateRepository(),
                new InstitutionalCompetenceRuleStateRepository(),
                Clock.systemUTC());
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(
                new StaticListableBeanFactory().getBeanProvider(InstitutionalCatalogGovernanceOverlayService.class));
        MotorRoteamentoComunicacaoInstitucional roteador = new MotorRoteamentoComunicacaoInstitucional(
                new UnitResolutionService(catalogo, overlay),
                new PoliticaEntregaInstitucionalService(),
                new PrazoEntregaInstitucionalResolver());

        var rota = roteador.resolver(new ResolucaoRoteamentoInstitucionalRequest(
                101L,
                "0000101-22.2026.8.26.0001",
                ato.politica().destinatarioKind(),
                ato.politica().papelProcessual(),
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_DEFENSOR,
                ato.atoCanonico(),
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "SP",
                "São Paulo",
                null,
                null,
                null,
                ato.politica().fundamentoLegal(),
                true,
                null,
                false,
                true
        ));

        assertEquals(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, rota.alvo().destinatarioKind());
        assertEquals(PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA, rota.alvo().papelProcessual());
        assertTrue(rota.bloqueiaFluxo());
        assertNotNull(rota.gateCode());
        assertTrue(rota.gateCode().contains("MP_INTERESSE_INCAPAZ"));
        assertTrue(rota.justificativas().stream().anyMatch(item -> item.contains("Ministério Público") || item.contains("incapaz") || item.contains("criança")));
    }
}
