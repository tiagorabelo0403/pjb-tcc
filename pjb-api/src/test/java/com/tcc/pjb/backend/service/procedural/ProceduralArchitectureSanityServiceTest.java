package com.tcc.pjb.backend.service.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService.DivergenceReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProceduralArchitectureSanityServiceTest {

    @Test
    void marksReportUnhealthyWhenCatalogAndRoutingCoverageDiverge() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        RitoPackService ritoPackService = mock(RitoPackService.class);
        when(ritoPackService.definitions()).thenReturn(Map.of());

        CnjTpuSyncService cnjTpuSyncService = mock(CnjTpuSyncService.class);
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("snapshotFresh", false);
        when(cnjTpuSyncService.health()).thenReturn(Map.copyOf(health));
        when(cnjTpuSyncService.checkDivergence()).thenReturn(new DivergenceReport(
                Instant.now(),
                10,
                12,
                List.of("LOCAL_ONLY"),
                List.of("CNJ_ONLY"),
                List.of("DESC"),
                false
        ));

        JudicialProcessConnector outro = mock(JudicialProcessConnector.class);
        when(outro.system()).thenReturn(JudicialSystem.OUTRO);
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(outro));
        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setStrictConnectorRegistry(true);

        ProceduralArchitectureSanityService service = new ProceduralArchitectureSanityService(
                catalogService,
                ritoPackService,
                cnjTpuSyncService,
                registry,
                properties,
                new MockEnvironment()
        );

        var report = service.report();

        assertFalse(report.healthy());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.contains("Rito pack carregado")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.contains("Conector preferido")));
    }

    @Test
    void keepsMissingPreferredConnectorsNonBlockingOutsideStrictProfiles() {
        ProceduralCatalogService catalogService = mock(ProceduralCatalogService.class);
        when(catalogService.coverage()).thenReturn(Map.of(
                "totalRitos", 0,
                "withStages", 0,
                "withRequiredParties", 0,
                "withRequiredDocuments", 0,
                "withExternalActor", 0
        ));
        when(catalogService.catalogDrivenRitos()).thenReturn(List.of());
        when(catalogService.listNationalTribunals()).thenReturn(List.of(Map.of(
                "codigo", "TJCE",
                "connectorPreferido", "PJE"
        )));

        RitoPackService ritoPackService = mock(RitoPackService.class);
        when(ritoPackService.definitions()).thenReturn(Map.of());

        CnjTpuSyncService cnjTpuSyncService = mock(CnjTpuSyncService.class);
        when(cnjTpuSyncService.health()).thenReturn(Map.of("snapshotFresh", true));
        when(cnjTpuSyncService.checkDivergence()).thenReturn(new DivergenceReport(
                Instant.now(),
                10,
                10,
                List.of(),
                List.of(),
                List.of(),
                true
        ));

        JudicialProcessConnector outro = mock(JudicialProcessConnector.class);
        when(outro.system()).thenReturn(JudicialSystem.OUTRO);

        ProceduralArchitectureSanityService service = new ProceduralArchitectureSanityService(
                catalogService,
                ritoPackService,
                cnjTpuSyncService,
                new JudicialConnectorRegistry(List.of(outro)),
                new ProceduralBootstrapGovernanceProperties(),
                new MockEnvironment().withProperty("spring.profiles.active", "docker")
        );

        var report = service.report();

        assertTrue(report.healthy());
        assertFalse(report.routingCoverage().get("missingPreferredConnectors").toString().isBlank());
        assertFalse((Boolean) report.routingCoverage().get("strictConnectorRegistry"));
    }
}
