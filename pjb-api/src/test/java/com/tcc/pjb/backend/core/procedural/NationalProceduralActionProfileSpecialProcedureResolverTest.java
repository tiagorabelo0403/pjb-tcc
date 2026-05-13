package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileSpecialProcedureResolverTest {

    @Test
    void mustPreferMandadoDeSegurancaByExplicitKindBeforeGenericCivilSignals() {
        NationalProceduralActionProfileSpecialProcedureResolver resolver = new NationalProceduralActionProfileSpecialProcedureResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of("kind", "mandado seguranca"),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "discussao administrativa com rito civil comum",
                null
        )).orElseThrow();

        assertEquals("MANDADO_SEGURANCA", profile.actionNature());
        assertTrue(profile.reasons().contains("Vara com competência para mandado de segurança"));
    }

    @Test
    void mustDetectRcedBeforeGenericElectoralFallback() {
        NationalProceduralActionProfileSpecialProcedureResolver resolver = new NationalProceduralActionProfileSpecialProcedureResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "recurso contra expedicao do diploma por abuso eleitoral",
                null
        )).orElseThrow();

        assertEquals("ELEITORAL_RCED", profile.defaultRito());
        assertTrue(profile.reviewChecklist().contains("Conferir diplomação impugnada, legitimidade ativa e substrato fático-eleitoral específico do RCED."));
    }

    @Test
    void mustDetectSpecialDescumprimentoObrigacaoWhenExplicitKindExists() {
        NationalProceduralActionProfileSpecialProcedureResolver resolver = new NationalProceduralActionProfileSpecialProcedureResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of("kind", "acao descumprimento obrigacao"),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de tutela para descumprimento de obrigacao especifica",
                null
        )).orElseThrow();

        assertEquals("ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO", profile.defaultRito());
        assertTrue(profile.reviewChecklist().contains("Conferir obrigação específica, inadimplemento, prova documental mínima e adequação da tutela pretendida."));
    }
}
