package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import com.tcc.pjb.backend.core.frontend.readiness.application.PjbBackendReadyForFrontendApplicationService;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadinessChecklistItem;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadyForFrontendBootstrapView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadyForFrontendSummary;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendAuthContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendDtoContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendErrorContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendValidationContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendHttpErrorCatalogEntry;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendHttpContractFreezeView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendEnvelopeContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicContractFreezeView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicContractSummary;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicRouteContractView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminFrontendReadinessControllerTest {

    private PjbBackendReadyForFrontendApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbBackendReadyForFrontendApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFrontendReadinessController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void summary_deveExporReadiness() throws Exception {
        when(applicationService.summary()).thenReturn(new PjbBackendReadyForFrontendSummary(false, true, true, true, true, true, false, true, 80, 20, 15, 3, Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildGateApproved").value(true))
                .andExpect(jsonPath("$.data.publicRouteCount").value(80));
    }

    @Test
    void checklist_deveExporItens() throws Exception {
        when(applicationService.checklist()).thenReturn(List.of(new PjbBackendReadinessChecklistItem("build-gate", "Build gate global", "READY", "CRITICO", "sem pendencias", "freeze")));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/checklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("build-gate"));
    }

    @Test
    void authErrorsERoutes_devemExporContratos() throws Exception {
        when(applicationService.authContract()).thenReturn(new PjbFrontendAuthContractView(true, true, true, true, true, true, true, List.of("jwt=true")));
        when(applicationService.errorContract()).thenReturn(new PjbFrontendErrorContractView(true, true, true, true, 0, 0, 0, List.of("apiQueryEnvelope=true")));
        when(applicationService.publicRoutes()).thenReturn(List.of(new PjbFrontendRouteView("GET", "/api/v1/ui/home", "UiController", "pkg", "ui", false, true)));
        when(applicationService.blockers()).thenReturn(List.of(new PjbFrontendDeliveryBlockerView("roadmap", "ALTO", "roadmap.pending", "macrobloco parcial")));
        when(applicationService.bootstrap()).thenReturn(new PjbBackendReadyForFrontendBootstrapView(
                new PjbBackendReadyForFrontendSummary(false, true, true, true, true, true, false, true, 80, 20, 15, 3, Instant.parse("2026-04-12T10:00:00Z")),
                List.of(new PjbBackendReadinessChecklistItem("build-gate", "Build gate global", "READY", "CRITICO", "sem pendencias", "freeze")),
                new PjbFrontendAuthContractView(true, true, true, true, true, true, true, List.of("jwt=true")),
                new PjbFrontendErrorContractView(true, true, true, true, 0, 0, 0, List.of("apiQueryEnvelope=true")),
                List.of(new PjbFrontendRouteView("GET", "/api/v1/ui/home", "UiController", "pkg", "ui", false, true)),
                List.of(new PjbFrontendDeliveryBlockerView("roadmap", "ALTO", "roadmap.pending", "macrobloco parcial")),
                List.of("freeze backend"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jwtEnabled").value(true));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exceptionAdvicePresent").value(true));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].path").value("/api/v1/ui/home"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("roadmap"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.publicRouteCount").value(80));
    }

    @Test
    void publicContractEndpoints_devemExporFreezeDaApiPublica() throws Exception {
        when(applicationService.publicContractSummary()).thenReturn(new PjbFrontendPublicContractSummary(true, true, true, true, 22, 4, 18, 60, Instant.parse("2026-04-12T10:00:00Z")));
        when(applicationService.publicContractRoutes()).thenReturn(List.of(new PjbFrontendPublicRouteContractView(
                "POST", "/api/v1/processual/protocolo", "ProcessualController", "processual", "AUTHENTICATED", "ProtocoloRequest", "ResponseEntity<ApiCommandResponse<?>>", true, List.of("auth=AUTHENTICATED"))));
        when(applicationService.dtoCatalog()).thenReturn(List.of(new PjbFrontendDtoContractView(
                "ApiQueryResponse", "com.tcc.pjb.backend.model.dto.api", "api-envelope", true, true, true)));
        when(applicationService.publicContractFreeze()).thenReturn(new PjbFrontendPublicContractFreezeView(
                new PjbFrontendPublicContractSummary(true, true, true, true, 22, 4, 18, 60, Instant.parse("2026-04-12T10:00:00Z")),
                List.of(new PjbFrontendPublicRouteContractView("POST", "/api/v1/processual/protocolo", "ProcessualController", "processual", "AUTHENTICATED", "ProtocoloRequest", "ResponseEntity<ApiCommandResponse<?>>", true, List.of("auth=AUTHENTICATED"))),
                List.of(new PjbFrontendDtoContractView("ApiQueryResponse", "com.tcc.pjb.backend.model.dto.api", "api-envelope", true, true, true)),
                new PjbFrontendAuthContractView(true, true, true, true, true, true, true, List.of("jwt=true")),
                new PjbFrontendErrorContractView(true, true, true, true, 0, 0, 0, List.of("apiQueryEnvelope=true")),
                List.of("freeze routes"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));
        when(applicationService.envelopeContract()).thenReturn(new PjbFrontendEnvelopeContractView(true, true, true, true, true, true, List.of("query-envelope=true")));
        when(applicationService.validationContract()).thenReturn(new PjbFrontendValidationContractView(true, true, true, true, true, 0, List.of("validation-gate=true")));
        when(applicationService.httpErrorCatalog()).thenReturn(List.of(new PjbFrontendHttpErrorCatalogEntry(422, "VALIDATION_ERROR", "validation", false, "ApiExceptionHandler")));
        when(applicationService.httpContractFreeze()).thenReturn(new PjbFrontendHttpContractFreezeView(
                new PjbFrontendPublicContractSummary(true, true, true, true, 22, 4, 18, 60, Instant.parse("2026-04-12T10:00:00Z")),
                new PjbFrontendAuthContractView(true, true, true, true, true, true, true, List.of("jwt=true")),
                new PjbFrontendErrorContractView(true, true, true, true, 0, 0, 0, List.of("apiQueryEnvelope=true")),
                new PjbFrontendEnvelopeContractView(true, true, true, true, true, true, List.of("query-envelope=true")),
                new PjbFrontendValidationContractView(true, true, true, true, true, 0, List.of("validation-gate=true")),
                List.of(new PjbFrontendHttpErrorCatalogEntry(422, "VALIDATION_ERROR", "validation", false, "ApiExceptionHandler")),
                List.of("freeze http"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.publicRouteCount").value(22));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].authMode").value("AUTHENTICATED"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/dtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].typeName").value("ApiQueryResponse"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.dtoCatalogCount").value(60));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/envelopes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.responseFactoryPresent").value(true));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.standardized422").value(true));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/error-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/public-contract/http-freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.envelopes.ready").value(true));
    }


    @Test
    void integrationPackEndpoints_devemExporOpenApiPostmanSeedsPerfilESmoke() throws Exception {
        when(applicationService.integrationPackSummary()).thenReturn(new com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendIntegrationPackSummary(true, true, true, true, true, true, true, 10, Instant.parse("2026-04-12T10:00:00Z")));
        when(applicationService.integrationArtifacts()).thenReturn(List.of(new com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendIntegrationArtifactView("openapi", "public-api", "docs/openapi/public-api.yaml", true, "ok")));
        when(applicationService.seedScenarios()).thenReturn(List.of(new com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendSeedScenarioView("users", "src/main/resources/frontend-dev/seed-users.json", true, 3, false, "ok")));
        when(applicationService.frontendDevProfile()).thenReturn(new com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendDevProfileView(true, "frontend-dev", true, true, true, true, true, true, List.of("h2=true")));
        when(applicationService.smokePack()).thenReturn(new com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendSmokePackView(true, true, true, true, true, true, true, true, List.of("FrontendPrimaryFlowsSmokeTest")));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/integration-pack/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.artifactCount").value(10));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/integration-pack/artifacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("openapi"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/integration-pack/seeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("users"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/integration-pack/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile").value("frontend-dev"));

        mockMvc.perform(get("/api/v1/admin/frontend-readiness/integration-pack/smoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.testClasses[0]").value("FrontendPrimaryFlowsSmokeTest"));
    }
}
