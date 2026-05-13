package com.tcc.pjb.backend.core.quality.roadmap.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryReadinessSnapshot;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbRoadmapClosureApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void summary_deveLerMacroblocosDoPlacar() throws Exception {
        Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(
                tempDir.resolve("docs/ROADMAP_23_MACROBLOCOS_STATUS.md"),
                "## Parte 1\n1. Prazo — **Parcial**\n2. Custodia — **Fechado**\n## Parte 2\n12. Eleitoral — **Não iniciado**\n",
                StandardCharsets.UTF_8);
        PjbRoadmapClosureApplicationService service = PjbRoadmapClosureApplicationService.forProjectRoot(
                buildGate(),
                testMatrix(),
                codebase(),
                apiSurface(),
                modularization(),
                tempDir);

        var summary = service.summary();

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.closed()).isEqualTo(1);
        assertThat(summary.partial()).isEqualTo(1);
        assertThat(summary.notStarted()).isEqualTo(1);
    }

    @Test
    void quality_deveCombinarGatesEstruturais() throws Exception {
        Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(tempDir.resolve("docs/ROADMAP_23_MACROBLOCOS_STATUS.md"), "## Parte 1\n1. Prazo — **Parcial**\n", StandardCharsets.UTF_8);
        PjbRoadmapClosureApplicationService service = PjbRoadmapClosureApplicationService.forProjectRoot(
                buildGate(),
                testMatrix(),
                codebase(),
                apiSurface(),
                modularization(),
                tempDir);

        var quality = service.quality();

        assertThat(quality.buildApproved()).isTrue();
        assertThat(quality.modularizationReady()).isFalse();
        assertThat(quality.criticalModules()).contains("PrazoProcessualNacionalService");
    }

    private BuildGateGovernanceService buildGate() {
        BuildGateGovernanceService service = mock(BuildGateGovernanceService.class);
        when(service.evaluate()).thenReturn(new BuildGateEvaluationResponse(true, true, true, true, true, true, true, 0, List.of(), List.of("manter gate")));
        return service;
    }

    private TestQualityMatrixService testMatrix() {
        TestQualityMatrixService service = mock(TestQualityMatrixService.class);
        when(service.verify()).thenReturn(new TestQualityMatrixResponse(10, 8, 10, 8, 2, List.of("PrazoProcessualNacionalService"), List.of(), List.of("continuar integração")));
        return service;
    }

    private PjbCodebaseSanityApplicationService codebase() {
        PjbCodebaseSanityApplicationService service = mock(PjbCodebaseSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbCodebaseSanityAggregate(true, true, 20, 0, 0, 0, List.of(), List.of(), List.of(), Instant.parse("2026-04-12T10:00:00Z")));
        return service;
    }

    private PjbApiSurfaceSanityApplicationService apiSurface() {
        PjbApiSurfaceSanityApplicationService service = mock(PjbApiSurfaceSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbApiSurfaceSanityAggregate(true, true, 5, 10, 0, 0, 0, List.of(), Instant.parse("2026-04-12T10:00:00Z")));
        return service;
    }

    private PjbModuleBoundaryReadinessApplicationService modularization() {
        PjbModuleBoundaryReadinessApplicationService service = mock(PjbModuleBoundaryReadinessApplicationService.class);
        when(service.snapshot()).thenReturn(new PjbModuleBoundaryReadinessSnapshot(
                false,
                false,
                1,
                10,
                5,
                7,
                List.of(new PjbModuleBoundaryIssue("root.without.modules", "ALTO", "pom.xml", "POM sem modules", List.of("iniciar fase 1"))),
                List.of("extrair pjb-core"),
                Instant.parse("2026-04-12T10:00:00Z")));
        when(service.blockers()).thenReturn(List.of(new PjbModuleBoundaryIssue("root.without.modules", "ALTO", "pom.xml", "POM sem modules", List.of("iniciar fase 1"))));
        return service;
    }
}
