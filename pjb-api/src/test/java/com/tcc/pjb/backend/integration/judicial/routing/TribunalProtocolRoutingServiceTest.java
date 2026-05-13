package com.tcc.pjb.backend.integration.judicial.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialOAuthTokenService;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class TribunalProtocolRoutingServiceTest {

    @Test
    void resolvesCanonicalElectoralRoutingWithOperationalConnector() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        PerfilInstanciaTribunalService perfilService = new PerfilInstanciaTribunalService();
        perfilService.init();
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.PJE, true, true, "https://pje.tre-ce.jus.br"),
                connector(JudicialSystem.OUTRO, true, false, null)
        ));
        TribunalProtocolRoutingService service = routingService(registry, resolver, gate, perfilService);

        var decision = service.resolve(Map.of(
                "rito", "ELEITORAL",
                "uf", "CE"
        ), (String) null, null, null, false);

        assertEquals("TRE-CE", decision.tribunalCodigo());
        assertEquals(JudicialSystem.PJE, decision.judicialSystem());
        assertTrue(decision.metadata().containsKey("canonicalContext"));
        assertTrue(decision.metadata().containsKey("connectorHomologation"));
        assertTrue(decision.metadata().containsKey("connectorOperationalProfile"));
        assertEquals("OK", decision.metadata().get("sanityStatus"));
        assertFalse(decision.certificateRequired());
    }

    @Test
    void emitsExplicitWarningWhenOnlyContingencyConnectorIsAvailable() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        PerfilInstanciaTribunalService perfilService = new PerfilInstanciaTribunalService();
        perfilService.init();
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.OUTRO, true, false, null)
        ));
        TribunalProtocolRoutingService service = routingService(registry, resolver, gate, perfilService);

        var decision = service.resolve(Map.of(
                "rito", "ELEITORAL",
                "uf", "CE"
        ), (String) null, null, null, false);

        assertTrue(decision.warnings().stream().anyMatch(item -> item.contains("TRIBUNAL_NOT_HOMOLOGATED") || item.contains("Conector não registrado")));
        assertEquals(JudicialSystem.OUTRO, decision.judicialSystem());
    }

    @Test
    void resolvesRoutingFromNominalRitoWithoutLegacyEnum() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        PerfilInstanciaTribunalService perfilService = new PerfilInstanciaTribunalService();
        perfilService.init();
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.PJE, true, true, "https://pje.trt7.jus.br"),
                connector(JudicialSystem.OUTRO, true, false, null)
        ));
        TribunalProtocolRoutingService service = routingService(registry, resolver, gate, perfilService);

        var decision = service.resolve(Map.of(
                "uf", "CE"
        ), "TRABALHISTA_ORDINARIO", "TRABALHISTA", null, false);

        assertEquals(JudicialSystem.PJE, decision.judicialSystem());
        assertTrue(decision.metadata().containsKey("canonicalContext"));
        Map<?, ?> canonicalContext = (Map<?, ?>) decision.metadata().get("canonicalContext");
        assertEquals("TRABALHISTA_ORDINARIO", canonicalContext.get("rito"));
    }

    private static TribunalProtocolRoutingService routingService(JudicialConnectorRegistry registry,
                                                                ProceduralCanonicalResolver resolver,
                                                                CanonicalSanityGate gate,
                                                                PerfilInstanciaTribunalService perfilService) {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialConnectorHomologationService homologationService = JudicialConnectorHomologationService.withoutPolicy(properties);
        JudicialConnectorReadinessService readinessService = new JudicialConnectorReadinessService(
                properties,
                homologationService,
                new JudicialOAuthTokenService(new RestTemplateBuilder(), new ObjectMapper())
        );
        JudicialConnectorOperationalProfileService operationalProfileService = new JudicialConnectorOperationalProfileService(
                registry,
                homologationService,
                readinessService
        );
        return new TribunalProtocolRoutingService(registry, perfilService, resolver, gate, homologationService, operationalProfileService);
    }

    private static JudicialProcessConnector connector(JudicialSystem system,
                                                      boolean enabled,
                                                      boolean supportsProtocol,
                                                      String baseUrl) {
        return new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return system;
            }

            @Override
            public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return Optional.empty();
            }

            @Override
            public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
                return List.of();
            }

            @Override
            public JudicialSubmissionCapability capability() {
                return new JudicialSubmissionCapability(
                        system,
                        enabled,
                        supportsProtocol,
                        false,
                        true,
                        true,
                        false,
                        false,
                        false,
                        List.of("application/pdf"),
                        List.of(),
                        List.of(),
                        baseUrl
                );
            }
        };
    }
}
