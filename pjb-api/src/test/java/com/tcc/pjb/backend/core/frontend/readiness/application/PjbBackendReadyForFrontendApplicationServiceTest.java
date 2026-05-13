package com.tcc.pjb.backend.core.frontend.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.delivery.application.PjbFrontendDeliveryApplicationService;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliverySummary;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbBackendReadyForFrontendApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void summaryChecklistEPublicContract_devemMaterializarReadinessDoBackend() throws Exception {
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/api"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/security"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/publico"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/processual"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/platform/security/idempotency"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/service/api"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/model/dto/processual"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/delivery/domain"));
        Files.createDirectories(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/readiness/domain"));

        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/SecurityConfig.java"), "oauth2ResourceServer SessionCreationPolicy.STATELESS CorsConfigurationSource");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/api/OpenApiConfig.java"), "class OpenApiConfig {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/security/GovBrStepUpController.java"), "@GetMapping(\"/assurance-level\")");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyFilter.java"), "Idempotency-Key");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ApiExceptionHandler.java"), "@RestControllerAdvice MethodArgumentNotValidException.class ConstraintViolationException.class AccessDeniedException.class CapabilityRateLimitExceededException.class IdempotencyInProgressException.class HttpMessageNotReadableException.class ResponseStatusException.class Exception.class UNPROCESSABLE_ENTITY");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ProblemDetailResponseHardeningAdvice.java"), "class ProblemDetailResponseHardeningAdvice {} ");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/configs/api/SensitiveApiResponseHardeningAdvice.java"), "class SensitiveApiResponseHardeningAdvice {} ");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/service/api/ApiResponseFactory.java"), "package com.tcc.pjb.backend.service.api; public class ApiResponseFactory {} ");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiQueryResponse.java"), "package com.tcc.pjb.backend.model.dto.api; public record ApiQueryResponse() {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiCommandResponse.java"), "package com.tcc.pjb.backend.model.dto.api; public record ApiCommandResponse() {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/model/dto/processual/ProtocoloRequest.java"), "package com.tcc.pjb.backend.model.dto.processual; public record ProtocoloRequest(String valor) {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/delivery/domain/PjbFrontendRouteView.java"), "package com.tcc.pjb.backend.core.frontend.delivery.domain; public record PjbFrontendRouteView() {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/readiness/domain/PjbFrontendAuthContractView.java"), "package com.tcc.pjb.backend.core.frontend.readiness.domain; public record PjbFrontendAuthContractView() {}");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/publico/PublicStatusController.java"), """
package com.tcc.pjb.backend.controller.publico;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
@RequestMapping("/api/v1/publico/status")
public class PublicStatusController {
    @GetMapping("")
    public ResponseEntity<ApiQueryResponse<?>> status() { return null; }
}
""");
        Files.writeString(tempDir.resolve("src/main/java/com/tcc/pjb/backend/controller/processual/ProcessualProtocoloController.java"), """
package com.tcc.pjb.backend.controller.processual;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
 import com.tcc.pjb.backend.model.dto.processual.ProtocoloRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@RequestMapping("/api/v1/processual/protocolo")
public class ProcessualProtocoloController {
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<ApiCommandResponse<?>> protocolar(
            @RequestBody ProtocoloRequest request) { return null; }
}
""");

        PjbBackendReadyForFrontendApplicationService service = new PjbBackendReadyForFrontendApplicationService(
                frontendDelivery(),
                finalClosure(),
                apiSurface(),
                buildGate(),
                matrix(),
                mock(AuditLedgerService.class),
                tempDir);

        var summary = service.summary();
        var checklist = service.checklist();
        var auth = service.authContract();
        var errors = service.errorContract();
        var routes = service.publicRoutes();
        var publicContract = service.publicContractSummary();
        var contractRoutes = service.publicContractRoutes();
        var dtoCatalog = service.dtoCatalog();
        var freeze = service.publicContractFreeze();
        var envelopes = service.envelopeContract();
        var validation = service.validationContract();
        var errorCatalog = service.httpErrorCatalog();
        var httpFreeze = service.httpContractFreeze();

        assertThat(summary.publicRouteCount()).isEqualTo(2);
        assertThat(summary.authContractReady()).isTrue();
        assertThat(auth.ready()).isTrue();
        assertThat(errors.ready()).isTrue();
        assertThat(routes).extracting("path").contains("/api/v1/ui/home", "/api/v1/publico/status");
        assertThat(checklist).extracting("code").contains("build-gate", "auth-contract", "error-contract", "public-contract-freeze");
        assertThat(publicContract.ready()).isTrue();
        assertThat(publicContract.publicRouteCount()).isEqualTo(2);
        assertThat(contractRoutes).extracting("path").contains("/api/v1/publico/status", "/api/v1/processual/protocolo");
        assertThat(contractRoutes).extracting("authMode").contains("PUBLIC", "AUTHENTICATED");
        assertThat(dtoCatalog).extracting("typeName").contains("ApiQueryResponse", "ApiCommandResponse", "ProtocoloRequest");
        assertThat(freeze.routes()).hasSize(2);
        assertThat(freeze.dtos()).isNotEmpty();
        assertThat(envelopes.ready()).isTrue();
        assertThat(validation.ready()).isTrue();
        assertThat(errorCatalog).extracting("code").contains("VALIDATION_ERROR", "FORBIDDEN");
        assertThat(httpFreeze.envelopes().ready()).isTrue();
        assertThat(httpFreeze.validation().standardized422()).isTrue();
    }

    @Test
    void integrationPack_devemExporOpenApiPostmanSeedsPerfilESmoke() throws Exception {
        Files.createDirectories(tempDir.resolve("docs/openapi"));
        Files.createDirectories(tempDir.resolve("docs/postman"));
        Files.createDirectories(tempDir.resolve("docs/reports"));
        Files.createDirectories(tempDir.resolve("src/main/resources/frontend-dev"));
        Files.createDirectories(tempDir.resolve("src/test/java/com/tcc/pjb/backend/smoke"));

        Files.writeString(tempDir.resolve("docs/openapi/public-api.yaml"), "openapi: 3.0.3");
        Files.writeString(tempDir.resolve("docs/openapi/admin-api.yaml"), "openapi: 3.0.3");
        Files.writeString(tempDir.resolve("docs/postman/PJB_Frontend_Integration.postman_collection.json"), "{}");
        Files.writeString(tempDir.resolve("docs/postman/PJB_Frontend_Environment.postman_environment.json"), "{}");
        Files.writeString(tempDir.resolve("docs/ERROR_CODES.md"), "# errors");
        Files.writeString(tempDir.resolve("docs/reports/error_code_catalog.json"), "[]");
        Files.writeString(tempDir.resolve("docs/FRONTEND_DEV_PROFILE.md"), "# frontend-dev");
        Files.writeString(tempDir.resolve("docs/reports/frontend_dev_seed_pack.json"), "{}");
        Files.writeString(tempDir.resolve("src/main/resources/application-frontend-dev.yml"), """
jdbc:h2:mem:pjb-frontend-dev
cors-allowed-origins:
- http://localhost:3000
- http://localhost:5173
mni:
 mock-enabled: true
eleitoral:
 tse:
  enabled: false
""");
        Files.writeString(tempDir.resolve("src/main/resources/frontend-dev/seed-users.json"), """
[{"code":"u1"},{"code":"u2"},{"code":"u3"}]
""");
        Files.writeString(tempDir.resolve("src/main/resources/frontend-dev/seed-processos.json"), """
[{"code":"p1"},{"code":"p2"}]
""");
        Files.writeString(tempDir.resolve("src/main/resources/frontend-dev/seed-custas.json"), """
[{"code":"c1"}]
""");
        Files.writeString(tempDir.resolve("src/main/resources/frontend-dev/seed-integrations.json"), """
[{"code":"i1"},{"code":"i2"}]
""");
        Files.writeString(tempDir.resolve("src/test/java/com/tcc/pjb/backend/smoke/FrontendPrimaryFlowsSmokeTest.java"), "MockMvcBuilders.standaloneSetup andExpect(status().isOk()) /api/v1/auth/passkey /api/v1/public/consultas-publicas /api/v1/timeline/processo/ /api/v1/peticionamento /api/v1/admin/custas /api/v1/admin/dje /api/v1/admin/frontend-readiness");

        PjbBackendReadyForFrontendApplicationService service = new PjbBackendReadyForFrontendApplicationService(
                frontendDelivery(),
                finalClosure(),
                apiSurface(),
                buildGate(),
                matrix(),
                mock(AuditLedgerService.class),
                tempDir);

        var summary = service.integrationPackSummary();
        var artifacts = service.integrationArtifacts();
        var seeds = service.seedScenarios();
        var profile = service.frontendDevProfile();
        var smoke = service.smokePack();

        assertThat(summary.ready()).isTrue();
        assertThat(summary.openApiExported()).isTrue();
        assertThat(summary.postmanExported()).isTrue();
        assertThat(artifacts).extracting("path").contains("docs/openapi/public-api.yaml", "docs/postman/PJB_Frontend_Integration.postman_collection.json");
        assertThat(seeds).extracting("code").contains("users", "processos", "custas", "integrations");
        assertThat(seeds).extracting("sampleCount").contains(3, 2, 1);
        assertThat(profile.ready()).isTrue();
        assertThat(profile.profile()).isEqualTo("frontend-dev");
        assertThat(smoke.ready()).isTrue();
        assertThat(smoke.testClasses()).contains("FrontendPrimaryFlowsSmokeTest");
    }

    private PjbFrontendDeliveryApplicationService frontendDelivery() {
        PjbFrontendDeliveryApplicationService service = mock(PjbFrontendDeliveryApplicationService.class);
        when(service.summary()).thenReturn(new PjbFrontendDeliverySummary(true, true, true, true, 4, 2, 1, 2, 3, 20, 1, Instant.parse("2026-04-12T10:00:00Z")));
        when(service.routes()).thenReturn(List.of(
                new PjbFrontendRouteView("GET", "/api/v1/ui/home", "UiController", "pkg", "ui", false, true),
                new PjbFrontendRouteView("GET", "/api/v1/publico/status", "PublicController", "pkg", "publico", false, false),
                new PjbFrontendRouteView("GET", "/api/v1/admin/runtime/health", "AdminRuntimeController", "pkg", "admin/runtime", true, false)
        ));
        when(service.blockers()).thenReturn(List.of(new PjbFrontendDeliveryBlockerView("roadmap", "ALTO", "roadmap.pending", "macrobloco parcial")));
        return service;
    }

    private PjbFinalClosureApplicationService finalClosure() {
        PjbFinalClosureApplicationService service = mock(PjbFinalClosureApplicationService.class);
        when(service.summary()).thenReturn(new PjbFinalClosureSummary(false, true, true, true, false, false, false, 25, 17, 8, 0, 1, 40, 120, 25, List.of("build pendente"), Instant.parse("2026-04-12T10:00:00Z")));
        return service;
    }

    private PjbApiSurfaceSanityApplicationService apiSurface() {
        PjbApiSurfaceSanityApplicationService service = mock(PjbApiSurfaceSanityApplicationService.class);
        when(service.auditar()).thenReturn(new PjbApiSurfaceSanityAggregate(true, true, 10, 12, 0, 0, 0, List.of(), Instant.parse("2026-04-12T10:00:00Z")));
        return service;
    }

    private BuildGateGovernanceService buildGate() {
        BuildGateGovernanceService service = mock(BuildGateGovernanceService.class);
        when(service.evaluate()).thenReturn(new BuildGateEvaluationResponse(true, true, true, true, true, true, true, 0, List.of(), List.of("freeze backend")));
        return service;
    }

    private TestQualityMatrixService matrix() {
        TestQualityMatrixService service = mock(TestQualityMatrixService.class);
        when(service.verify()).thenReturn(new TestQualityMatrixResponse(10, 5, 10, 5, 1, List.of("PrazoProcessualNacionalService"), List.of(), List.of("manter contratos")));
        return service;
    }
}
