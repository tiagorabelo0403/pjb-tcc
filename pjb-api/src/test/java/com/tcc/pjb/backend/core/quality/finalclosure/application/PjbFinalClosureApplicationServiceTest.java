package com.tcc.pjb.backend.core.quality.finalclosure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryReadinessSnapshot;
import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapBlockingView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapMacroblockView;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbFinalClosureApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void summary_deveRefletirQueAindaNaoHaFechamentoGlobal() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/sample"));
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/admin/AdminAlphaController.java"), "class AdminAlphaController {}\n");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/sample/AlphaApplicationService.java"), "class AlphaApplicationService {}\n");
        PjbFinalClosureApplicationService service = new PjbFinalClosureApplicationService(
                buildGate(false),
                codebase(false),
                apiSurface(false),
                modularization(false),
                roadmap(),
                audit(),
                tempDir);

        var summary = service.summary();

        assertThat(summary.overallReady()).isFalse();
        assertThat(summary.endToEndValidated()).isFalse();
        assertThat(summary.adminControllers()).isEqualTo(1);
        assertThat(summary.applicationServices()).isEqualTo(1);
        assertThat(summary.criticalBlockers()).isNotEmpty();
    }

    @Test
    void blockers_deveConsolidarRoadmapCodebaseApiEBuildGate() {
        PjbFinalClosureApplicationService service = new PjbFinalClosureApplicationService(
                buildGate(false),
                codebase(false),
                apiSurface(false),
                modularization(false),
                roadmap(),
                audit(),
                tempDir);

        var blockers = service.blockers();

        assertThat(blockers).extracting("scope").contains("roadmap", "codebase", "api-surface", "build-gate");
    }

    @Test
    void readiness_deveManterEndToEndComoBlockedComHonestidade() {
        PjbFinalClosureApplicationService service = new PjbFinalClosureApplicationService(
                buildGate(true),
                codebase(true),
                apiSurface(true),
                modularization(true),
                roadmap(),
                audit(),
                tempDir);

        var readiness = service.readiness();

        assertThat(readiness).extracting("dimension").contains("end-to-end-validation");
        assertThat(readiness.stream().filter(item -> item.dimension().equals("end-to-end-validation")).findFirst().orElseThrow().status())
                .isEqualTo("BLOCKED");
    }

    private BuildGateGovernanceService buildGate(boolean approved) {
        BuildGateGovernanceService service = mock(BuildGateGovernanceService.class);
        when(service.evaluate()).thenReturn(new BuildGateEvaluationResponse(
                approved,
                approved,
                approved,
                approved,
                approved,
                approved,
                approved,
                approved ? 0 : 2,
                approved ? List.of() : List.of("gate pendente"),
                List.of("executar validacao global")
        ));
        return service;
    }

    private PjbCodebaseSanityApplicationService codebase(boolean clean) {
        PjbCodebaseSanityApplicationService service = mock(PjbCodebaseSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbCodebaseSanityAggregate(
                true,
                clean,
                100,
                0,
                clean ? 0 : 1,
                0,
                List.of(),
                List.of("src/main/java"),
                clean ? List.of() : List.of(new PjbCodebaseSanityIssue("imports.quebrados", "ALTO", "Alpha.java", List.of(10), "import quebrado")),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        return service;
    }

    private PjbApiSurfaceSanityApplicationService apiSurface(boolean clean) {
        PjbApiSurfaceSanityApplicationService service = mock(PjbApiSurfaceSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbApiSurfaceSanityAggregate(
                true,
                clean,
                20,
                40,
                clean ? 0 : 1,
                0,
                0,
                clean ? List.of() : List.of(new PjbApiSurfaceIssue("route.duplicada", "CRITICO", "AlphaController", "GET", "/alpha", List.of("rota repetida"))),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        return service;
    }

    private PjbModuleBoundaryReadinessApplicationService modularization(boolean ready) {
        PjbModuleBoundaryReadinessApplicationService service = mock(PjbModuleBoundaryReadinessApplicationService.class);
        when(service.snapshot()).thenReturn(new PjbModuleBoundaryReadinessSnapshot(
                ready,
                ready,
                ready ? 0 : 1,
                10,
                5,
                15,
                ready ? List.of() : List.of(new PjbModuleBoundaryIssue("root.without.modules", "ALTO", "pom.xml", "sem modules", List.of("iniciar fase 1"))),
                List.of("extrair pjb-core"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        return service;
    }

    private PjbRoadmapClosureApplicationService roadmap() {
        PjbRoadmapClosureApplicationService service = mock(PjbRoadmapClosureApplicationService.class);
        when(service.summary()).thenReturn(new PjbRoadmapClosureSummary(23, 1, 21, 1, 4, 20, 22, Instant.parse("2026-04-12T10:00:00Z")));
        when(service.blockers()).thenReturn(List.of(new PjbRoadmapBlockingView("modularization", "root.without.modules", "ALTO", "POM sem modules")));
        when(service.macroblocks()).thenReturn(List.of(
                new PjbRoadmapMacroblockView(1, "Parte 1", "Prazo", "Parcial", "MATERIALIZADO", true),
                new PjbRoadmapMacroblockView(8, "Parte 1", "Testcontainers reais", "Parcial", "ESTRUTURAL", false)
        ));
        return service;
    }

    private AuditLedgerService audit() {
        return mock(AuditLedgerService.class);
    }
}
