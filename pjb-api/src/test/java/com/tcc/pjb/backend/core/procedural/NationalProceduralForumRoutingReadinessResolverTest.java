package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralForumRoutingReadinessResolverTest {

    @Test
    void mustFailSafeWhenConnectorIsNotOperationalAndPreflightHasStructuralIssues() {
        TribunalProtocolRoutingService routingService = mock(TribunalProtocolRoutingService.class);
        ProceduralPreflightEngine preflightEngine = mock(ProceduralPreflightEngine.class);
        NationalProceduralPreflightPayloadFactory payloadFactory = mock(NationalProceduralPreflightPayloadFactory.class);
        NationalProceduralForumRoutingReadinessResolver resolver = new NationalProceduralForumRoutingReadinessResolver(
                routingService,
                preflightEngine,
                payloadFactory,
                new NationalProceduralForumAllocationMessages()
        );
        NationalProceduralForumAllocationContext context = context();
        NationalProceduralForumAllocationSeed seed = new NationalProceduralForumAllocationSeed(
                null,
                new NationalProceduralTerritorialAnchor("DOMICILIO_AUTOR", "Fortaleza", "CE", "fundamento"),
                new NationalProceduralLinkageAnalysis("NENHUM", "NENHUM", List.of(), List.of()),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "1a Vara",
                "CIVEL",
                88.0d,
                null
        );
        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(
                JudicialSystem.PJE,
                true,
                true,
                true,
                false,
                false,
                false,
                true,
                false,
                List.of(),
                List.of(),
                List.of(),
                null
        );
        TribunalProtocolRoutingService.RoutingDecision routingDecision = new TribunalProtocolRoutingService.RoutingDecision(
                "TJCE",
                "Tribunal de Justica do Ceara",
                JudicialSystem.PJE,
                capability,
                "competence-hint",
                false,
                true,
                List.of("warning externo"),
                Map.of("source", "routing"),
                Instant.now()
        );
        ProceduralPreflightEngine.PreflightIssue blocker = new ProceduralPreflightEngine.PreflightIssue(
                "BLOCK-1",
                new ProceduralPreflightEngine.Severity.BLOQUEANTE(),
                "Ausência de documento obrigatório.",
                "documentos",
                "Anexar documento mínimo."
        );
        ProceduralPreflightEngine.PreflightResult preflight = new ProceduralPreflightEngine.PreflightResult(
                "req-1",
                Instant.now(),
                false,
                "COMUM_ORDINARIO",
                "7",
                "TJCE",
                "PJE",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(blocker),
                Map.of("stage", "preflight")
        );

        when(payloadFactory.buildProtocolRoutingPayload(any(), isNull(), eq("TJCE"), eq("Fortaleza"), eq("CE"), eq("VARA-01"), eq("1a Vara"))).thenReturn(Map.of("tribunalCodigo", "TJCE"));
        when(payloadFactory.buildPreflightPayload(any(), isNull(), eq("COMUM_ORDINARIO"), any(), eq("TJCE"), eq("Fortaleza"), eq("CE"), eq("VARA-01"), eq("1a Vara"), eq(routingDecision))).thenReturn(Map.of("tribunalCodigo", "TJCE"));
        when(payloadFactory.isStructuralPreflightBlocker(blocker)).thenReturn(true);
        when(routingService.resolve(any(), eq("COMUM_ORDINARIO"), eq("CIVEL"), eq("ESTADUAL"), eq(false))).thenReturn(routingDecision);
        when(preflightEngine.evaluate(any())).thenReturn(preflight);

        NationalProceduralForumRoutingReadiness readiness = resolver.resolve(context, seed);

        assertFalse(readiness.connectorOperational());
        assertFalse(readiness.protocoloRealDisponivel());
        assertFalse(readiness.preProtocoloApto());
        assertEquals("STRUCTURAL_REVIEW_REQUIRED", readiness.preProtocoloStatus());
        assertTrue(readiness.warnings().contains("warning externo"));
        assertTrue(readiness.warnings().contains("Conector judicial ainda não homologado para protocolo completo no tribunal sugerido."));
        assertTrue(readiness.reviewChecklist().contains("Providenciar assinatura qualificada compatível com o conector do tribunal."));
        assertTrue(readiness.incompatibilities().contains("Ausência de documento obrigatório."));
        assertTrue(readiness.reviewChecklist().contains("Anexar documento mínimo."));
    }

    private static NationalProceduralForumAllocationContext context() {
        return new NationalProceduralForumAllocationContext(
                Map.of("classe", "indenizacao", "materia", "CIVEL"),
                "obrigacao de fazer civel",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVEL",
                        "7",
                        "Procedimento Comum",
                        "ESTADUAL",
                        "TJCE",
                        "TJCE",
                        "PJE",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.87d, List.of(), List.of(), Map.of()),
                TipoJustica.ESTADUAL,
                "COMUM_ORDINARIO",
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.84d, false),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "CIVEL",
                null
        );
    }
}
