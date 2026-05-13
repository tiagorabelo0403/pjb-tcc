package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

class MotorRoteamentoComunicacaoInstitucionalTest {

    private final com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService governanceOverlay =
            new com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService(
                    new com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceStateRepository(),
                    new com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCompetenceRuleStateRepository(),
                    Clock.systemUTC());

    private final MotorRoteamentoComunicacaoInstitucional resolver = new MotorRoteamentoComunicacaoInstitucional(
            new UnitResolutionService(
                    new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class)),
                    governanceOverlay),
            new PoliticaEntregaInstitucionalService(),
            new PrazoEntregaInstitucionalResolver()
    );

    @Test
    void shouldPreferDomicilioForInstitutionalCitation() {
        ResolucaoRoteamentoInstitucionalResult result = resolver.resolver(new ResolucaoRoteamentoInstitucionalRequest(
                22L,
                "0001111-22.2026.8.01.0001",
                DestinatarioInstitucionalKind.FAZENDA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoComunicacaoJudicial.CITACAO_INICIAL,
                AtoCanonicoProcessual.INTIMAR_FAZENDA_PUBLICA_REPRESENTACAO,
                RamoDireito.TRIBUTARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "AC",
                "Rio Branco",
                null,
                null,
                null,
                "CPC art. 246 e art. 183",
                true,
                null,
                false,
                true
        ));

        assertEquals(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO, result.planoEntrega().canalPrincipal().canal());
        assertTrue(result.planoEntrega().forcarDigital());
        assertTrue(result.bloqueiaFluxo());
    }

    @Test
    void shouldPreferDjenWhenPersonalScienceIsNotRequired() {
        ResolucaoRoteamentoInstitucionalResult result = resolver.resolver(new ResolucaoRoteamentoInstitucionalRequest(
                23L,
                "0001111-23.2026.8.26.0001",
                DestinatarioInstitucionalKind.CEJUSC,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoComunicacaoJudicial.INTIMACAO_PUBLICA_DJE,
                AtoCanonicoProcessual.ENCAMINHAR_CEJUSC,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "SP",
                "São Paulo",
                null,
                null,
                null,
                "Lei 11.419/2006 art. 4º",
                false,
                CanalComunicacaoInstitucional.DJEN,
                false,
                false
        ));

        assertEquals(CanalComunicacaoInstitucional.DJEN, result.planoEntrega().canalPrincipal().canal());
        assertTrue(result.planoEntrega().canaisFallback().stream().anyMatch(c -> c.canal() == CanalComunicacaoInstitucional.PJB_INBOX));
    }
}
