package com.tcc.pjb.backend.core.frontend.delivery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureBlockerView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbFrontendDeliveryApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void routesEDomains_devemSerCatalogadosParaFrontend() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/ui"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin"));
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/ui/UiAlphaController.java"), """
                package com.tcc.pjb.backend.controller.ui;
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping(\"/api/v1/ui/alpha\")
                public class UiAlphaController {
                    @GetMapping(\"/catalog\") void catalog() {}
                    @PostMapping(\"/save\") void save() {}
                }
                """);
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin/AdminBetaController.java"), """
                package com.tcc.pjb.backend.controller.admin;
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping(\"/api/v1/admin/runtime\")
                public class AdminBetaController {
                    @GetMapping(\"/health\") void health() {}
                }
                """);

        PjbFrontendDeliveryApplicationService service = new PjbFrontendDeliveryApplicationService(
                finalClosure(),
                roadmap(),
                apiSurface(),
                mock(AuditLedgerService.class),
                tempDir);

        var summary = service.summary();
        var routes = service.routes();
        var domains = service.domains();
        var bootstrap = service.bootstrap();

        assertThat(summary.totalRoutes()).isEqualTo(3);
        assertThat(summary.uiRoutes()).isEqualTo(2);
        assertThat(summary.adminRoutes()).isEqualTo(1);
        assertThat(routes).extracting("path").contains("/api/v1/ui/alpha/catalog", "/api/v1/admin/runtime/health");
        assertThat(domains).extracting("domain").contains("ui", "admin/runtime");
        assertThat(bootstrap.priorityRoutes()).isNotEmpty();
    }

    @Test
    void blockers_devemRefletirFechamentoGlobalParaConsumoDoFrontend() {
        PjbFrontendDeliveryApplicationService service = new PjbFrontendDeliveryApplicationService(
                finalClosure(),
                roadmap(),
                apiSurface(),
                mock(AuditLedgerService.class),
                tempDir);

        var blockers = service.blockers();

        assertThat(blockers).extracting("scope").contains("roadmap");
        assertThat(blockers).isNotEmpty();
    }

    private PjbFinalClosureApplicationService finalClosure() {
        PjbFinalClosureApplicationService service = mock(PjbFinalClosureApplicationService.class);
        when(service.summary()).thenReturn(new PjbFinalClosureSummary(
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                23,
                10,
                12,
                1,
                3,
                40,
                120,
                20,
                List.of("build pendente"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        when(service.blockers()).thenReturn(List.of(
                new PjbFinalClosureBlockerView("roadmap", "roadmap.pending", "ALTO", "roadmap", "macroblocos ainda parciais")
        ));
        return service;
    }

    private PjbRoadmapClosureApplicationService roadmap() {
        PjbRoadmapClosureApplicationService service = mock(PjbRoadmapClosureApplicationService.class);
        when(service.summary()).thenReturn(new PjbRoadmapClosureSummary(23, 10, 12, 1, 18, 20, 22, Instant.parse("2026-04-12T10:00:00Z")));
        return service;
    }

    private PjbApiSurfaceSanityApplicationService apiSurface() {
        PjbApiSurfaceSanityApplicationService service = mock(PjbApiSurfaceSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbApiSurfaceSanityAggregate(
                true,
                true,
                10,
                10,
                0,
                0,
                0,
                List.of(),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        return service;
    }
}
