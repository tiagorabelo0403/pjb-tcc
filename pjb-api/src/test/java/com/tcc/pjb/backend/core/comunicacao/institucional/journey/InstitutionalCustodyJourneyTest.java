package com.tcc.pjb.backend.core.comunicacao.institucional.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

class InstitutionalCustodyJourneyTest {

    @Test
    void shouldResolveCustodyPresentationFlowWithNationalFallbackChain() {
        AtoCanonicoProcessualResolver atoResolver = new AtoCanonicoProcessualResolver(new AtoCanonicoComunicacaoMapper());
        var ato = atoResolver.resolver(new ResolucaoAtoCanonicoRequest(
                202L,
                "0000202-77.2026.8.06.0001",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                FaseProcessual.INSTRUTORIA,
                "Ação penal",
                "réu preso com audiência designada",
                "requisição de apresentação do preso",
                "escolta e apresentação do custodiado em audiência",
                "CE",
                "Fortaleza",
                null,
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
        assertEquals(AtoCanonicoProcessual.REQUISITAR_APRESENTACAO_REU_PRESO, ato.atoCanonico());

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
                202L,
                "0000202-77.2026.8.06.0001",
                ato.politica().destinatarioKind(),
                ato.politica().papelProcessual(),
                TipoComunicacaoJudicial.NOTIFICACAO_JUDICIAL,
                ato.atoCanonico(),
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "CE",
                "Fortaleza",
                null,
                null,
                null,
                ato.politica().fundamentoLegal(),
                false,
                null,
                true,
                true
        ));

        assertEquals(DestinatarioInstitucionalKind.POLICIA_PENAL, rota.alvo().destinatarioKind());
        assertTrue(rota.bloqueiaFluxo());
        assertEquals(CanalComunicacaoInstitucional.PJB_INBOX, rota.planoEntrega().canalPrincipal().canal());
        assertTrue(rota.planoEntrega().canaisFallback().stream().anyMatch(c -> c.canal() == CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL));
    }
}
