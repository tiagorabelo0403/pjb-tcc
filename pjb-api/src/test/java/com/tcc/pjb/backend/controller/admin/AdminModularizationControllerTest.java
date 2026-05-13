package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.quality.modularization.application.PjbAggregatorActivationApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbCoreExtractionPlannerApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbCoreSeedExtractionApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleScaffoldApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorActivationChecklistView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorActivationSnapshot;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorPomPatchView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionCandidateView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionPomPreview;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedDriftIssueView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreSeedExtractionSnapshot;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryPhaseView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryReadinessSnapshot;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBuildOrderView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModulePomScaffoldView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleScaffoldSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminModularizationControllerTest {

    private PjbModuleBoundaryReadinessApplicationService applicationService;
    private PjbCoreExtractionPlannerApplicationService coreExtractionPlannerApplicationService;
    private PjbModuleScaffoldApplicationService moduleScaffoldApplicationService;
    private PjbCoreSeedExtractionApplicationService coreSeedExtractionApplicationService;
    private PjbAggregatorActivationApplicationService aggregatorActivationApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbModuleBoundaryReadinessApplicationService.class);
        coreExtractionPlannerApplicationService = mock(PjbCoreExtractionPlannerApplicationService.class);
        moduleScaffoldApplicationService = mock(PjbModuleScaffoldApplicationService.class);
        coreSeedExtractionApplicationService = mock(PjbCoreSeedExtractionApplicationService.class);
        aggregatorActivationApplicationService = mock(PjbAggregatorActivationApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminModularizationController(applicationService, coreExtractionPlannerApplicationService, moduleScaffoldApplicationService, coreSeedExtractionApplicationService, aggregatorActivationApplicationService, new ApiResponseFactory())).build();
    }

    @Test
    void snapshot_deveExporReadiness() throws Exception {
        when(applicationService.snapshot()).thenReturn(new PjbModuleBoundaryReadinessSnapshot(false, false, 3, 12, 4, 7, List.of(), List.of("extrair pjb-core"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aggregatorPomPresent").value(false))
                .andExpect(jsonPath("$.data.estimatedBoundaryViolations").value(3));
    }

    @Test
    void phases_deveExporPlanoIncremental() throws Exception {
        when(applicationService.phases()).thenReturn(List.of(new PjbModuleBoundaryPhaseView(1, "Extracao inicial pjb-core", "BLOCKED", "Isolar core", List.of("core depende de service"))));

        mockMvc.perform(get("/api/v1/admin/modularization/phases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].phase").value(1))
                .andExpect(jsonPath("$.data[0].status").value("BLOCKED"));
    }

    @Test
    void blockers_deveExporBloqueadores() throws Exception {
        when(applicationService.blockers()).thenReturn(List.of(new PjbModuleBoundaryIssue("root.without.modules", "ALTO", "pom.xml", "POM sem modules", List.of("fase 1"))));

        mockMvc.perform(get("/api/v1/admin/modularization/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("root.without.modules"));
    }

    @Test
    void coreExtractionCandidates_deveExporPacotesCandidatos() throws Exception {
        when(coreExtractionPlannerApplicationService.candidates()).thenReturn(List.of(new PjbCoreExtractionCandidateView("com.tcc.pjb.backend.core.audit", "pjb-core", 4, "BAIXO", List.of("candidato limpo"))));

        mockMvc.perform(get("/api/v1/admin/modularization/core-extraction/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].moduleTarget").value("pjb-core"))
                .andExpect(jsonPath("$.data[0].risk").value("BAIXO"));
    }

    @Test
    void coreExtractionPomPreview_deveExporPreviewDePom() throws Exception {
        when(coreExtractionPlannerApplicationService.pomPreview()).thenReturn(new PjbCoreExtractionPomPreview(true, List.of("pjb-core", "pjb-api"), List.of("<modules>", "<module>pjb-core</module>"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/core-extraction/pom-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aggregatorPomRequired").value(true))
                .andExpect(jsonPath("$.data.suggestedModules[0]").value("pjb-core"));
    }

    @Test
    void scaffoldSnapshot_deveExporEstadoDoScaffold() throws Exception {
        when(moduleScaffoldApplicationService.snapshot()).thenReturn(new PjbModuleScaffoldSnapshot(true, false, 2, 4, List.of("ligar agregador"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/scaffold/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scaffoldPresent").value(true))
                .andExpect(jsonPath("$.data.modulePomCount").value(2));
    }

    @Test
    void scaffoldModulePoms_deveExporPomsFisicos() throws Exception {
        when(moduleScaffoldApplicationService.modulePoms()).thenReturn(List.of(new PjbModulePomScaffoldView("pjb-core", "pjb-core/pom.xml", true, "jar", "pjb-core", "spring-boot-starter-parent")));

        mockMvc.perform(get("/api/v1/admin/modularization/scaffold/module-poms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].artifactId").value("pjb-core"));
    }

    @Test
    void scaffoldBuildOrder_deveExporSequenciaDeModulos() throws Exception {
        when(moduleScaffoldApplicationService.buildOrder()).thenReturn(List.of(new PjbModuleBuildOrderView(1, "pjb-core", "READY_FOR_EXTRACT_PREP", "Primeiro modulo", List.of("scaffold fisico"))));

        mockMvc.perform(get("/api/v1/admin/modularization/scaffold/build-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].moduleName").value("pjb-core"));
    }


    @Test
    void aggregatorSnapshot_deveExporReadinessDaFaseUm() throws Exception {
        when(aggregatorActivationApplicationService.snapshot()).thenReturn(new PjbAggregatorActivationSnapshot(true, false, false, 2, 5, List.of("POM raiz ainda nao usa packaging pom; ativacao direta de modules continua bloqueada."), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/aggregator/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phaseOneAggregatorFilePresent").value(true))
                .andExpect(jsonPath("$.data.activationReady").value(false));
    }

    @Test
    void aggregatorChecklist_deveExporChecklistDaAtivacao() throws Exception {
        when(aggregatorActivationApplicationService.checklist()).thenReturn(List.of(new PjbAggregatorActivationChecklistView("phase1.aggregator.file", "READY", "Arquivo de agregador da Fase 1 foi gerado.", List.of("pom.phase1-aggregator.xml"))));

        mockMvc.perform(get("/api/v1/admin/modularization/aggregator/checklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("phase1.aggregator.file"))
                .andExpect(jsonPath("$.data[0].status").value("READY"));
    }

    @Test
    void aggregatorPomPatch_deveExporPatchDaAtivacao() throws Exception {
        when(aggregatorActivationApplicationService.pomPatch()).thenReturn(new PjbAggregatorPomPatchView("pom.phase1-aggregator.xml", true, List.of("pjb-core", "pjb-api"), List.of("<modules>", "<module>pjb-core</module>"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/aggregator/pom-patch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetFile").value("pom.phase1-aggregator.xml"))
                .andExpect(jsonPath("$.data.modules[0]").value("pjb-core"));
    }


    @Test
    void coreSeedSnapshot_deveExporEstadoDoEspelhoInicial() throws Exception {
        when(coreSeedExtractionApplicationService.snapshot()).thenReturn(new PjbCoreSeedExtractionSnapshot(true, true, false, 6, 5, 5, List.of("espelho fisico ainda nao cobre a mesma quantidade de classes do pacote fonte"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/modularization/core-seed/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceFileCount").value(6))
                .andExpect(jsonPath("$.data.parityReady").value(false));
    }

    @Test
    void coreSeedDrift_deveExporIssuesDoEspelho() throws Exception {
        when(coreSeedExtractionApplicationService.drift()).thenReturn(List.of(new PjbCoreSeedDriftIssueView("ALTO", "PjbModuleId.java", "content.hash.mismatch", "divergiu")));

        mockMvc.perform(get("/api/v1/admin/modularization/core-seed/drift"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileName").value("PjbModuleId.java"))
                .andExpect(jsonPath("$.data[0].issueType").value("content.hash.mismatch"));
    }

}
