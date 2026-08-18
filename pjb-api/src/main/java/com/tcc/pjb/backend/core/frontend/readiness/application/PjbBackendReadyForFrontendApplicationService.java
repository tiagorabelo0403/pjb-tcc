package com.tcc.pjb.backend.core.frontend.readiness.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.delivery.application.PjbFrontendDeliveryApplicationService;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliverySummary;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadinessChecklistItem;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadyForFrontendBootstrapView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadyForFrontendSummary;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendAuthContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendDtoContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendDevProfileView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendIntegrationArtifactView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendIntegrationPackSummary;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendSeedScenarioView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendSmokePackView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendErrorContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendEnvelopeContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendHttpContractFreezeView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendHttpErrorCatalogEntry;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendValidationContractView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicContractFreezeView;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicContractSummary;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbFrontendPublicRouteContractView;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbBackendReadyForFrontendApplicationService {

    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:public\\s+)?(?:record|class|interface|enum)\\s+(\\w+)");
    private static final Pattern REQUEST_MAPPING = Pattern.compile("@RequestMapping\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern HTTP_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern METHOD_SIGNATURE = Pattern.compile("public\\s+(.+?)\\s+(\\w+)\\s*\\(([^)]*)\\)", Pattern.DOTALL);
    private static final Pattern DECLARED_TYPE = Pattern.compile("\\b(?:public\\s+)?(record|class|enum)\\s+(\\w+)");

    private final PjbFrontendDeliveryApplicationService frontendDeliveryApplicationService;
    private final PjbFinalClosureApplicationService finalClosureApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final BuildGateGovernanceService buildGateGovernanceService;
    private final TestQualityMatrixService testQualityMatrixService;
    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    public PjbBackendReadyForFrontendApplicationService(PjbFrontendDeliveryApplicationService frontendDeliveryApplicationService,
                                                        PjbFinalClosureApplicationService finalClosureApplicationService,
                                                        PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                        BuildGateGovernanceService buildGateGovernanceService,
                                                        TestQualityMatrixService testQualityMatrixService,
                                                        AuditLedgerService auditLedgerService) {
        this(frontendDeliveryApplicationService, finalClosureApplicationService, apiSurfaceSanityApplicationService, buildGateGovernanceService, testQualityMatrixService, auditLedgerService, Path.of(""));
    }

    PjbBackendReadyForFrontendApplicationService(PjbFrontendDeliveryApplicationService frontendDeliveryApplicationService,
                                                 PjbFinalClosureApplicationService finalClosureApplicationService,
                                                 PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                 BuildGateGovernanceService buildGateGovernanceService,
                                                 TestQualityMatrixService testQualityMatrixService,
                                                 AuditLedgerService auditLedgerService,
                                                 Path projectRoot) {
        this.frontendDeliveryApplicationService = Objects.requireNonNull(frontendDeliveryApplicationService);
        this.finalClosureApplicationService = Objects.requireNonNull(finalClosureApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.testQualityMatrixService = Objects.requireNonNull(testQualityMatrixService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    @Transactional(readOnly = true)
    public PjbBackendReadyForFrontendSummary summary() {
        PjbFrontendDeliverySummary frontend = frontendDeliveryApplicationService.summary();
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        PjbApiSurfaceSanityAggregate apiSurface = apiSurfaceSanityApplicationService.auditar();
        PjbFrontendAuthContractView auth = authContract();
        PjbFrontendErrorContractView errors = errorContract();
        List<PjbFrontendRouteView> publicRoutes = publicRoutes();
        int adminRoutes = frontend.adminRoutes();
        int uiRoutes = frontend.uiRoutes();
        boolean publicRouteCatalogReady = !publicRoutes.isEmpty();
        boolean controllerCoverageReady = build.controllerCoverageGateApproved() && build.qualityMatrixGateApproved();
        boolean ready = build.approved()
                && apiSurface.limpo()
                && auth.ready()
                && errors.ready()
                && publicRouteCatalogReady
                && frontend.readyForFrontend();
        PjbBackendReadyForFrontendSummary summary = new PjbBackendReadyForFrontendSummary(
                ready,
                build.approved(),
                apiSurface.limpo(),
                auth.ready(),
                errors.ready(),
                publicRouteCatalogReady,
                controllerCoverageReady,
                frontend.readyForFrontend(),
                publicRoutes.size(),
                adminRoutes,
                uiRoutes,
                blockers().size(),
                Instant.now());
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_SUMMARY_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + summary.readyForFrontend() + " blockers=" + summary.blockerCount());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<PjbBackendReadinessChecklistItem> checklist() {
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        PjbApiSurfaceSanityAggregate apiSurface = apiSurfaceSanityApplicationService.auditar();
        PjbFrontendAuthContractView auth = authContract();
        PjbFrontendErrorContractView errors = errorContract();
        PjbFinalClosureSummary closure = finalClosureApplicationService.summary();
        TestQualityMatrixResponse matrix = testQualityMatrixService.verify();
        List<PjbFrontendRouteView> publicRoutes = publicRoutes();
        PjbFrontendPublicContractSummary publicContract = publicContractSummary();
        List<PjbBackendReadinessChecklistItem> items = List.of(
                item("build-gate", "Build gate global", build.approved(), "CRITICO", first(build.outstandingIssues(), "Build gate ainda tem pendências estruturais."), first(build.nextActions(), "Rodar correções estruturais e revalidar build gate.")),
                item("api-surface", "Superfície HTTP sem drift crítico", apiSurface.limpo(), "ALTO", "rotasDuplicadas=" + apiSurface.rotasDuplicadas() + ", dtoIssues=" + apiSurface.dtoForaDoPadrao() + ", entitiesExposed=" + apiSurface.entidadesExpostasDiretamente(), "Padronizar DTOs e remover wildcard/entity exposure."),
                item("auth-contract", "Contrato de autenticação e autorização", auth.ready(), "CRITICO", String.join(" | ", auth.notes()), "Congelar JWT, step-up Gov.br, CORS e idempotência nas escritas críticas."),
                item("error-contract", "Envelope e erros consistentes", errors.ready(), "ALTO", String.join(" | ", errors.notes()), "Padronizar 401/403/409/422 e reduzir wildcard responses."),
                item("public-routes", "Catálogo público para o frontend", !publicRoutes.isEmpty(), "ALTO", "publicRoutes=" + publicRoutes.size(), "Congelar rotas públicas e DTOs consumidos pelo frontend."),
                item("public-contract-freeze", "Freeze do contrato público", publicContract.ready(), "CRITICO", "routes=" + publicContract.publicRouteCount() + ", dtos=" + publicContract.dtoCatalogCount() + ", openApi=" + publicContract.openApiAvailable(), "Validar e congelar catálogo de rotas públicas, DTOs e modos de autenticação do frontend."),
                item("controller-coverage", "Cobertura mínima de controllers críticos", build.controllerCoverageGateApproved() && build.qualityMatrixGateApproved(), "ALTO", "targets=" + matrix.controllerContractSuitesTarget() + "/controllers=" + matrix.totalControllers(), "Elevar contratos dedicados nos controllers críticos antes do freeze de frontend."),
                item("closure", "Fechamento institucional do backend", closure.buildApproved() && closure.apiSurfaceClean(), "MEDIO", "macroblocks=" + closure.closedMacroblocks() + "/" + closure.totalMacroblocks() + ", blockers=" + closure.blockerCount(), "Manter bloqueadores residuais explícitos até o freeze de integração.")
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_CHECKLIST_QUERY", "FRONTEND", "BACKEND_READY", "items=" + items.size());
        return items;
    }

    @Transactional(readOnly = true)
    public PjbFrontendAuthContractView authContract() {
        Path securityConfig = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/SecurityConfig.java");
        Path govBrController = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/controller/security/GovBrStepUpController.java");
        Path idempotencyFilter = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyFilter.java");
        Path apiExceptionHandler = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ApiExceptionHandler.java");
        String securitySource = read(securityConfig);
        String govBrSource = read(govBrController);
        boolean jwtEnabled = securitySource.contains("oauth2ResourceServer");
        boolean statelessSession = securitySource.contains("SessionCreationPolicy.STATELESS");
        boolean corsConfigured = securitySource.contains(".cors(") || securitySource.contains("CorsConfigurationSource");
        boolean govBrAssuranceSurface = govBrSource.contains("/assurance-level") || govBrSource.contains("assurance-level");
        boolean idempotencyProtection = Files.exists(idempotencyFilter) && read(idempotencyFilter).contains("Idempotency-Key");
        boolean apiExceptionHandlerPresent = Files.exists(apiExceptionHandler);
        List<String> notes = List.of(
                flag("jwt", jwtEnabled),
                flag("stateless", statelessSession),
                flag("cors", corsConfigured),
                flag("govbr-assurance", govBrAssuranceSurface),
                flag("idempotency", idempotencyProtection),
                flag("api-exception-handler", apiExceptionHandlerPresent)
        );
        PjbFrontendAuthContractView view = new PjbFrontendAuthContractView(
                jwtEnabled && statelessSession && corsConfigured && govBrAssuranceSurface && idempotencyProtection && apiExceptionHandlerPresent,
                jwtEnabled,
                statelessSession,
                corsConfigured,
                govBrAssuranceSurface,
                idempotencyProtection,
                apiExceptionHandlerPresent,
                notes
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_AUTH_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendErrorContractView errorContract() {
        boolean apiQueryEnvelopePresent = Files.exists(projectRoot.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiQueryResponse.java"));
        boolean apiCommandEnvelopePresent = Files.exists(projectRoot.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiCommandResponse.java"));
        boolean exceptionAdvicePresent = Files.exists(projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ApiExceptionHandler.java"));
        PjbApiSurfaceSanityAggregate aggregate = apiSurfaceSanityApplicationService.auditar();
        int wildcardResponses = countIssues(aggregate.issues(), "controller.wildcard.response");
        List<String> notes = new ArrayList<>();
        notes.add("apiQueryEnvelope=" + apiQueryEnvelopePresent);
        notes.add("apiCommandEnvelope=" + apiCommandEnvelopePresent);
        notes.add("exceptionAdvice=" + exceptionAdvicePresent);
        notes.add("duplicateRoutes=" + aggregate.rotasDuplicadas());
        notes.add("wildcardResponses=" + wildcardResponses);
        notes.add("dtoSurfaceIssues=" + aggregate.dtoForaDoPadrao());
        PjbFrontendErrorContractView view = new PjbFrontendErrorContractView(
                apiQueryEnvelopePresent && apiCommandEnvelopePresent && exceptionAdvicePresent && aggregate.rotasDuplicadas() == 0 && wildcardResponses == 0,
                apiQueryEnvelopePresent,
                apiCommandEnvelopePresent,
                exceptionAdvicePresent,
                aggregate.rotasDuplicadas(),
                wildcardResponses,
                aggregate.dtoForaDoPadrao(),
                List.copyOf(notes)
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_ERRORS_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendRouteView> publicRoutes() {
        List<PjbFrontendRouteView> routes = frontendDeliveryApplicationService.routes().stream()
                .filter(route -> !route.adminSurface())
                .limit(120)
                .toList();
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_ROUTES_QUERY", "FRONTEND", "BACKEND_READY", "routes=" + routes.size());
        return routes;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendDeliveryBlockerView> blockers() {
        List<PjbFrontendDeliveryBlockerView> blockers = frontendDeliveryApplicationService.blockers().stream().limit(20).toList();
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_BLOCKERS_QUERY", "FRONTEND", "BACKEND_READY", "blockers=" + blockers.size());
        return blockers;
    }

    @Transactional(readOnly = true)
    public PjbFrontendIntegrationPackSummary integrationPackSummary() {
        List<PjbFrontendIntegrationArtifactView> artifacts = integrationArtifacts();
        boolean openApiExported = artifacts.stream().filter(a -> "openapi".equals(a.category())).anyMatch(PjbFrontendIntegrationArtifactView::present);
        boolean postmanExported = artifacts.stream().filter(a -> "postman".equals(a.category())).anyMatch(PjbFrontendIntegrationArtifactView::present);
        boolean seedPackPresent = !seedScenarios().isEmpty() && seedScenarios().stream().allMatch(PjbFrontendSeedScenarioView::present);
        boolean errorCatalogPresent = artifacts.stream().filter(a -> "errors".equals(a.category())).allMatch(PjbFrontendIntegrationArtifactView::present);
        boolean frontendDevProfilePresent = Files.exists(projectRoot.resolve("src/main/resources/application-frontend-dev.yml"));
        boolean smokePackPresent = smokePack().ready();
        PjbFrontendIntegrationPackSummary summary = new PjbFrontendIntegrationPackSummary(
                openApiExported && postmanExported && seedPackPresent && frontendDevProfilePresent && smokePackPresent,
                openApiExported,
                postmanExported,
                seedPackPresent,
                errorCatalogPresent,
                frontendDevProfilePresent,
                smokePackPresent,
                artifacts.size(),
                Instant.now());
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_INTEGRATION_PACK_SUMMARY_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + summary.ready() + " artifacts=" + summary.artifactCount());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendIntegrationArtifactView> integrationArtifacts() {
        List<PjbFrontendIntegrationArtifactView> artifacts = List.of(
                artifact("openapi", "public-api", "docs/openapi/public-api.yaml", "Contrato OpenAPI público exportado para integração."),
                artifact("openapi", "admin-api", "docs/openapi/admin-api.yaml", "Contrato OpenAPI administrativo exportado para integração."),
                artifact("postman", "frontend-collection", "docs/postman/PJB_Frontend_Integration.postman_collection.json", "Coleção oficial Postman para frontend."),
                artifact("postman", "frontend-environment", "docs/postman/PJB_Frontend_Environment.postman_environment.json", "Environment oficial para integração local."),
                artifact("errors", "error-codes-doc", "docs/ERROR_CODES.md", "Catálogo oficial de erros HTTP."),
                artifact("errors", "error-codes-json", "docs/reports/error_code_catalog.json", "Catálogo serializado de erros HTTP."),
                artifact("profile", "frontend-dev-profile", "src/main/resources/application-frontend-dev.yml", "Perfil dedicado para integração frontend-dev."),
                artifact("smoke", "frontend-primary-flows-smoke", "src/test/java/com/tcc/pjb/backend/smoke/FrontendPrimaryFlowsSmokeTest.java", "Smoke pack dos fluxos primários do frontend."),
                artifact("seed", "frontend-dev-profile-doc", "docs/FRONTEND_DEV_PROFILE.md", "Guia de seed pack para frontend-dev."),
                artifact("seed", "frontend-dev-seed-report", "docs/reports/frontend_dev_seed_pack.json", "Relatório serializado do seed pack do frontend-dev.")
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_INTEGRATION_ARTIFACTS_QUERY", "FRONTEND", "BACKEND_READY", "artifacts=" + artifacts.size());
        return artifacts;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendSeedScenarioView> seedScenarios() {
        List<PjbFrontendSeedScenarioView> seeds = List.of(
                seed("users", "src/main/resources/frontend-dev/seed-users.json", false, "Perfis básicos para autenticação e navegação."),
                seed("processos", "src/main/resources/frontend-dev/seed-processos.json", false, "Processos base para listagem, overview e timeline."),
                seed("custas", "src/main/resources/frontend-dev/seed-custas.json", false, "Custas e depósitos para fluxos financeiros."),
                seed("integrations", "src/main/resources/frontend-dev/seed-integrations.json", true, "Integrações mockadas para homologação visual.")
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_SEEDS_QUERY", "FRONTEND", "BACKEND_READY", "seeds=" + seeds.size());
        return seeds;
    }

    @Transactional(readOnly = true)
    public PjbFrontendDevProfileView frontendDevProfile() {
        Path profile = projectRoot.resolve("src/main/resources/application-frontend-dev.yml");
        String source = read(profile);
        boolean h2Enabled = source.contains("jdbc:h2:mem:pjb-frontend-dev") && source.contains("h2:");
        boolean corsReady = source.contains("cors-allowed-origins") && source.contains("localhost:3000") && source.contains("localhost:5173");
        boolean mockIntegrations = source.contains("mock-enabled: true") && source.contains("mni:") && source.contains("eleitoral:");
        boolean openApiDocsPresent = Files.exists(projectRoot.resolve("docs/openapi/public-api.yaml")) && Files.exists(projectRoot.resolve("docs/openapi/admin-api.yaml"));
        boolean postmanPresent = Files.exists(projectRoot.resolve("docs/postman/PJB_Frontend_Integration.postman_collection.json"))
                && Files.exists(projectRoot.resolve("docs/postman/PJB_Frontend_Environment.postman_environment.json"));
        boolean seedPackPresent = seedScenarios().stream().allMatch(PjbFrontendSeedScenarioView::present);
        List<String> notes = List.of(
                flag("h2", h2Enabled),
                flag("cors", corsReady),
                flag("mockIntegrations", mockIntegrations),
                flag("openApiDocs", openApiDocsPresent),
                flag("postman", postmanPresent),
                flag("seedPack", seedPackPresent)
        );
        PjbFrontendDevProfileView view = new PjbFrontendDevProfileView(
                h2Enabled && corsReady && mockIntegrations && openApiDocsPresent && postmanPresent && seedPackPresent,
                "frontend-dev",
                h2Enabled,
                corsReady,
                mockIntegrations,
                openApiDocsPresent,
                postmanPresent,
                seedPackPresent,
                notes
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_DEV_PROFILE_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendSmokePackView smokePack() {
        Path smoke = projectRoot.resolve("src/test/java/com/tcc/pjb/backend/smoke/FrontendPrimaryFlowsSmokeTest.java");
        String source = read(smoke);
        boolean executableSmoke = source.contains("MockMvcBuilders.standaloneSetup") && source.contains("andExpect(status().isOk())");
        boolean loginFlow = source.contains("/api/v1/auth/passkey") || source.contains("/api/v1/auth/govbr");
        boolean queryFlow = source.contains("/api/v1/public/consultas-publicas") || source.contains("/api/v1/cidadao/processos");
        boolean timelineFlow = source.contains("/api/v1/timeline/processo/");
        boolean protocolFlow = source.contains("/api/v1/peticionamento") || source.contains("processual/protocolo");
        boolean custasFlow = source.contains("/api/v1/admin/custas");
        boolean djeFlow = source.contains("/api/v1/admin/dje");
        boolean adminDashboardsFlow = source.contains("/api/v1/admin/frontend-readiness");
        PjbFrontendSmokePackView view = new PjbFrontendSmokePackView(
                Files.exists(smoke) && executableSmoke && loginFlow && queryFlow && timelineFlow && protocolFlow && custasFlow && djeFlow && adminDashboardsFlow,
                loginFlow,
                queryFlow,
                timelineFlow,
                protocolFlow,
                custasFlow,
                djeFlow,
                adminDashboardsFlow,
                Files.exists(smoke) ? List.of("FrontendPrimaryFlowsSmokeTest") : List.of()
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_SMOKE_PACK_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendPublicContractSummary publicContractSummary() {
        PjbFrontendAuthContractView auth = authContract();
        PjbFrontendErrorContractView errors = errorContract();
        List<PjbFrontendPublicRouteContractView> routes = publicContractRoutes();
        List<PjbFrontendDtoContractView> dtos = dtoCatalog();
        boolean openApiAvailable = Files.exists(projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/OpenApiConfig.java"));
        int anonymousCount = (int) routes.stream().filter(route -> "PUBLIC".equals(route.authMode())).count();
        int authenticatedCount = routes.size() - anonymousCount;
        PjbFrontendPublicContractSummary summary = new PjbFrontendPublicContractSummary(
                auth.ready() && errors.ready() && openApiAvailable && !routes.isEmpty() && !dtos.isEmpty(),
                auth.ready(),
                errors.ready(),
                openApiAvailable,
                routes.size(),
                anonymousCount,
                authenticatedCount,
                dtos.size(),
                Instant.now()
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_PUBLIC_CONTRACT_SUMMARY_QUERY", "FRONTEND", "BACKEND_READY", "routes=" + summary.publicRouteCount() + " dtos=" + summary.dtoCatalogCount());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendPublicRouteContractView> publicContractRoutes() {
        Path root = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/controller");
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<PjbFrontendPublicRouteContractView> routes = walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .flatMap(path -> parsePublicContractRoutes(path).stream())
                    .sorted(Comparator.comparing(PjbFrontendPublicRouteContractView::path)
                            .thenComparing(PjbFrontendPublicRouteContractView::method)
                            .thenComparing(PjbFrontendPublicRouteContractView::controller))
                    .limit(180)
                    .toList();
            auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_PUBLIC_CONTRACT_ROUTES_QUERY", "FRONTEND", "BACKEND_READY", "routes=" + routes.size());
            return routes;
        } catch (IOException ex) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendDtoContractView> dtoCatalog() {
        List<Path> roots = List.of(
                projectRoot.resolve("src/main/java/com/tcc/pjb/backend/model/dto"),
                projectRoot.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/delivery/domain"),
                projectRoot.resolve("src/main/java/com/tcc/pjb/backend/core/frontend/readiness/domain")
        );
        ArrayList<PjbFrontendDtoContractView> dtos = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .sorted()
                        .forEach(path -> parseDto(path).ifPresent(dtos::add));
            } catch (IOException ignored) {
            }
        }
        List<PjbFrontendDtoContractView> result = dtos.stream()
                .sorted(Comparator.comparing(PjbFrontendDtoContractView::packageName).thenComparing(PjbFrontendDtoContractView::typeName))
                .limit(220)
                .toList();
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_DTO_CATALOG_QUERY", "FRONTEND", "BACKEND_READY", "dtos=" + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public PjbFrontendPublicContractFreezeView publicContractFreeze() {
        PjbFrontendPublicContractFreezeView view = new PjbFrontendPublicContractFreezeView(
                publicContractSummary(),
                publicContractRoutes().stream().limit(60).toList(),
                dtoCatalog().stream().limit(80).toList(),
                authContract(),
                errorContract(),
                List.of(
                        "Congelar o catálogo de rotas públicas antes do início das telas.",
                        "Validar authMode por rota no frontend antes do freeze de integração.",
                        "Usar apenas DTOs deste catálogo no contrato público inicial.",
                        "Cruzar este freeze com /v3/api-docs e com a OpenApiConfig do projeto.",
                        "Executar a validação end-to-end fora deste ambiente antes de prometer estabilidade final."
                ),
                Instant.now()
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_PUBLIC_CONTRACT_FREEZE_QUERY", "FRONTEND", "BACKEND_READY", "routes=" + view.routes().size() + " dtos=" + view.dtos().size());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendEnvelopeContractView envelopeContract() {
        Path apiQueryResponse = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiQueryResponse.java");
        Path apiCommandResponse = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/model/dto/api/ApiCommandResponse.java");
        Path apiResponseFactory = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/service/api/ApiResponseFactory.java");
        Path problemDetailAdvice = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ProblemDetailResponseHardeningAdvice.java");
        Path sensitiveAdvice = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/SensitiveApiResponseHardeningAdvice.java");
        boolean queryEnvelopePresent = Files.exists(apiQueryResponse);
        boolean commandEnvelopePresent = Files.exists(apiCommandResponse);
        boolean responseFactoryPresent = Files.exists(apiResponseFactory);
        boolean problemDetailHardeningPresent = Files.exists(problemDetailAdvice);
        boolean sensitiveResponseHardeningPresent = Files.exists(sensitiveAdvice);
        PjbFrontendEnvelopeContractView view = new PjbFrontendEnvelopeContractView(
                queryEnvelopePresent && commandEnvelopePresent && responseFactoryPresent && problemDetailHardeningPresent && sensitiveResponseHardeningPresent,
                queryEnvelopePresent,
                commandEnvelopePresent,
                responseFactoryPresent,
                problemDetailHardeningPresent,
                sensitiveResponseHardeningPresent,
                List.of(
                        flag("query-envelope", queryEnvelopePresent),
                        flag("command-envelope", commandEnvelopePresent),
                        flag("response-factory", responseFactoryPresent),
                        flag("problem-detail-hardening", problemDetailHardeningPresent),
                        flag("sensitive-response-hardening", sensitiveResponseHardeningPresent)
                )
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_ENVELOPES_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbFrontendValidationContractView validationContract() {
        Path apiExceptionHandler = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ApiExceptionHandler.java");
        String handler = read(apiExceptionHandler);
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        int outstandingValidationIssues = (int) build.outstandingIssues().stream()
                .filter(issue -> issue != null && issue.toLowerCase(Locale.ROOT).contains("validation"))
                .count();
        boolean methodArgumentValidationHandled = handler.contains("MethodArgumentNotValidException.class");
        boolean constraintViolationHandled = handler.contains("ConstraintViolationException.class");
        boolean standardized422 = handler.contains("UNPROCESSABLE_ENTITY") || handler.contains("422");
        PjbFrontendValidationContractView view = new PjbFrontendValidationContractView(
                build.validationGateApproved() && methodArgumentValidationHandled && constraintViolationHandled && standardized422,
                build.validationGateApproved(),
                methodArgumentValidationHandled,
                constraintViolationHandled,
                standardized422,
                outstandingValidationIssues,
                List.of(
                        flag("validation-gate", build.validationGateApproved()),
                        flag("method-argument-validation", methodArgumentValidationHandled),
                        flag("constraint-violation", constraintViolationHandled),
                        flag("standardized-422", standardized422),
                        "outstanding-validation-issues=" + outstandingValidationIssues
                )
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_VALIDATION_QUERY", "FRONTEND", "BACKEND_READY", "ready=" + view.ready());
        return view;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendHttpErrorCatalogEntry> httpErrorCatalog() {
        Path apiExceptionHandler = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/configs/api/ApiExceptionHandler.java");
        String handler = read(apiExceptionHandler);
        ArrayList<PjbFrontendHttpErrorCatalogEntry> entries = new ArrayList<>();
        if (!handler.isBlank()) {
            if (handler.contains("MethodArgumentNotValidException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(422, "VALIDATION_ERROR", "validation", false, "ApiExceptionHandler"));
            }
            if (handler.contains("ConstraintViolationException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(422, "CONSTRAINT_VIOLATION", "validation", false, "ApiExceptionHandler"));
            }
            if (handler.contains("AccessDeniedException.class") || handler.contains("AccessDeniedPjbException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(403, "FORBIDDEN", "forbidden", false, "ApiExceptionHandler"));
            }
            if (handler.contains("CapabilityRateLimitExceededException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(429, "RATE_LIMIT_EXCEEDED", "rate_limit", true, "ApiExceptionHandler"));
            }
            if (handler.contains("IdempotencyInProgressException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(409, "IDEMPOTENCY_IN_PROGRESS", "idempotency", true, "ApiExceptionHandler"));
            }
            if (handler.contains("HttpMessageNotReadableException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(400, "MESSAGE_NOT_READABLE", "request_body", false, "ApiExceptionHandler"));
            }
            if (handler.contains("ResponseStatusException.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(409, "RESPONSE_STATUS", "generic_status", false, "ApiExceptionHandler"));
            }
            if (handler.contains("Exception.class")) {
                entries.add(new PjbFrontendHttpErrorCatalogEntry(500, "INTERNAL_ERROR", "generic", true, "ApiExceptionHandler"));
            }
        }
        List<PjbFrontendHttpErrorCatalogEntry> result = entries.stream()
                .sorted(Comparator.comparingInt(PjbFrontendHttpErrorCatalogEntry::status).thenComparing(PjbFrontendHttpErrorCatalogEntry::code))
                .toList();
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_ERROR_CATALOG_QUERY", "FRONTEND", "BACKEND_READY", "errors=" + result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public PjbFrontendHttpContractFreezeView httpContractFreeze() {
        PjbFrontendHttpContractFreezeView view = new PjbFrontendHttpContractFreezeView(
                publicContractSummary(),
                authContract(),
                errorContract(),
                envelopeContract(),
                validationContract(),
                httpErrorCatalog(),
                List.of(
                        "Congelar 401/403/409/422 antes do início das telas críticas.",
                        "Usar ApiQueryResponse e ApiCommandResponse como envelopes padrão do frontend.",
                        "Tratar erros de validação com prioridade no front antes dos fluxos de escrita.",
                        "Mapear códigos retriáveis do catálogo HTTP no client do frontend.",
                        "Executar build/test global fora deste ambiente antes do freeze final da integração."
                ),
                Instant.now()
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_HTTP_FREEZE_QUERY", "FRONTEND", "BACKEND_READY", "errors=" + view.errorCatalog().size());
        return view;
    }

    @Transactional(readOnly = true)
    public PjbBackendReadyForFrontendBootstrapView bootstrap() {
        PjbBackendReadyForFrontendBootstrapView view = new PjbBackendReadyForFrontendBootstrapView(
                summary(),
                checklist(),
                authContract(),
                errorContract(),
                publicRoutes().stream().limit(40).toList(),
                blockers().stream().limit(12).toList(),
                List.of(
                        "Congelar a lista de rotas públicas consumidas pelo frontend.",
                        "Executar build/test global fora deste ambiente antes do freeze de integração.",
                        "Padronizar 401/403/409/422 com base na ApiExceptionHandler antes da implementação das telas.",
                        "Subir primeiro os fluxos autenticados centrais sobre JWT + step-up Gov.br quando necessário.",
                        "Usar esta surface como checklist de readiness antes de prometer fechamento para o frontend."
                ),
                Instant.now()
        );
        auditLedgerService.appendSafely("BACKEND_READY_FRONTEND_BOOTSTRAP_QUERY", "FRONTEND", "BACKEND_READY", "routes=" + view.publicRoutes().size() + " blockers=" + view.blockers().size());
        return view;
    }

    private List<PjbFrontendPublicRouteContractView> parsePublicContractRoutes(Path file) {
        String source = read(file);
        String packageName = match(PACKAGE, source);
        String controller = match(TYPE, source);
        String prefix = normalizePath(match(REQUEST_MAPPING, source));
        ArrayList<PjbFrontendPublicRouteContractView> routes = new ArrayList<>();
        Matcher matcher = HTTP_MAPPING.matcher(source);
        while (matcher.find()) {
            String path = normalizePath(prefix + '/' + Objects.toString(matcher.group(2), ""));
            if (path.startsWith("/api/v1/admin/")) {
                continue;
            }
            int publicIndex = source.indexOf("public ", matcher.end());
            if (publicIndex < 0) {
                continue;
            }
            int braceIndex = source.indexOf('{', publicIndex);
            if (braceIndex < 0) {
                continue;
            }
            String declaration = source.substring(publicIndex, braceIndex);
            Matcher signature = METHOD_SIGNATURE.matcher(declaration.replace('\n', ' ').replace('\r', ' '));
            String returnType = "ResponseEntity<?>";
            String parameters = "";
            if (signature.find()) {
                returnType = normalizeSpaces(signature.group(1));
                parameters = signature.group(3);
            }
            String authMode = resolveAuthMode(path, source.substring(matcher.start(), braceIndex));
            String requestBodyType = extractRequestBodyType(parameters);
            List<String> notes = new ArrayList<>();
            notes.add("auth=" + authMode);
            if (path.contains("{")) {
                notes.add("path-params");
            }
            if (requestBodyType != null) {
                notes.add("requestBody=" + requestBodyType);
            }
            if (path.startsWith("/api/v1/ui/")) {
                notes.add("ui-surface");
            }
            routes.add(new PjbFrontendPublicRouteContractView(
                    httpVerb(matcher.group(1)),
                    path,
                    controller,
                    routeDomain(path, packageName),
                    authMode,
                    requestBodyType,
                    simplifyType(returnType),
                    isStableForFrontend(path, returnType),
                    List.copyOf(notes)
            ));
        }
        return routes;
    }

    private java.util.Optional<PjbFrontendDtoContractView> parseDto(Path file) {
        String source = read(file);
        Matcher matcher = DECLARED_TYPE.matcher(source);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        String packageName = match(PACKAGE, source);
        String declarationKind = matcher.group(1);
        String typeName = matcher.group(2);
        boolean apiEnvelope = typeName.startsWith("Api") || packageName.contains(".dto.api");
        boolean recordType = "record".equals(declarationKind);
        boolean stableForFrontend = apiEnvelope || packageName.contains(".frontend.") || typeName.endsWith("Response") || typeName.endsWith("View");
        return java.util.Optional.of(new PjbFrontendDtoContractView(
                typeName,
                packageName,
                dtoCategory(packageName, typeName),
                apiEnvelope,
                recordType,
                stableForFrontend
        ));
    }

    private static String dtoCategory(String packageName, String typeName) {
        if (packageName.contains(".dto.api")) {
            return "api-envelope";
        }
        if (packageName.contains(".frontend.readiness.")) {
            return "frontend-readiness";
        }
        if (packageName.contains(".frontend.delivery.")) {
            return "frontend-delivery";
        }
        if (typeName.endsWith("Response")) {
            return "response";
        }
        if (typeName.endsWith("Request") || typeName.endsWith("Command")) {
            return "request";
        }
        return "dto";
    }

    private static boolean isStableForFrontend(String path, String returnType) {
        if (!(returnType.contains("ApiQueryResponse") || returnType.contains("ApiCommandResponse") || returnType.contains("ResponseEntity"))) {
            return false;
        }
        return path.startsWith("/api/v1/publico/")
                || path.startsWith("/api/v1/ui/")
                || path.startsWith("/api/v1/processual/")
                || path.startsWith("/api/v1/offline/")
                || path.startsWith("/api/v1/frontend/");
    }

    private static String resolveAuthMode(String path, String methodSegment) {
        String normalized = methodSegment.toLowerCase(Locale.ROOT);
        if (path.startsWith("/api/v1/publico/") || path.startsWith("/api/v1/frontend/delivery")) {
            return "PUBLIC";
        }
        if (normalized.contains("assurance") || normalized.contains("step-up")) {
            return "STEP_UP";
        }
        if (normalized.contains("preauthorize") || normalized.contains("authentication ") || path.startsWith("/api/v1/ui/") || path.startsWith("/api/v1/processual/") || path.startsWith("/api/v1/offline/")) {
            return "AUTHENTICATED";
        }
        return "AUTHENTICATED";
    }

    private static String extractRequestBodyType(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return null;
        }
        for (String rawParam : parameters.split(",")) {
            String parameter = rawParam.trim();
            if (parameter.isBlank()) {
                continue;
            }
            boolean looksLikeBody = parameter.contains("@RequestBody") || parameter.contains("Request ") || parameter.contains("Command ");
            String clean = parameter.replaceAll("@\\w+(\\([^)]*\\))?", " ")
                    .replace("final ", " ")
                    .replace("@Valid", " ")
                    .trim();
            String[] tokens = clean.split("\\s+");
            if (tokens.length >= 2 && looksLikeBody) {
                return simplifyType(tokens[tokens.length - 2]);
            }
        }
        return null;
    }

    private PjbFrontendIntegrationArtifactView artifact(String category, String name, String relativePath, String notes) {
        Path path = projectRoot.resolve(relativePath);
        return new PjbFrontendIntegrationArtifactView(category, name, relativePath, Files.exists(path), notes);
    }

    private PjbFrontendSeedScenarioView seed(String code, String relativePath, boolean mockExternalIntegrations, String notes) {
        Path path = projectRoot.resolve(relativePath);
        String source = read(path);
        int sampleCount = countOccurrences(source, "\"code\"");
        return new PjbFrontendSeedScenarioView(code, relativePath, Files.exists(path), sampleCount, mockExternalIntegrations, notes);
    }

    private static int countOccurrences(String source, String token) {
        if (source == null || source.isBlank() || token == null || token.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static int countIssues(List<PjbApiSurfaceIssue> issues, String code) {
        return (int) issues.stream().filter(issue -> code.equals(issue.codigo())).count();
    }

    private static PjbBackendReadinessChecklistItem item(String code,
                                                         String title,
                                                         boolean ready,
                                                         String severity,
                                                         String detail,
                                                         String nextAction) {
        return new PjbBackendReadinessChecklistItem(
                code,
                title,
                ready ? "READY" : "BLOCKED",
                severity,
                detail,
                nextAction
        );
    }

    private static String first(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : values.getFirst();
    }

    private static String flag(String key, boolean enabled) {
        return key + '=' + enabled;
    }

    private static String match(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String httpVerb(String annotation) {
        return switch (annotation) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            default -> annotation.toUpperCase(Locale.ROOT);
        };
    }

    private static String routeDomain(String path, String packageName) {
        if (path.startsWith("/api/v1/")) {
            String remainder = path.substring("/api/v1/".length());
            int slash = remainder.indexOf('/');
            return slash < 0 ? remainder : remainder.substring(0, slash);
        }
        return packageName;
    }

    private static String normalizePath(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.isEmpty()) {
            return "/";
        }
        normalized = normalized.replaceAll("//+", "/");
        if (!normalized.startsWith("/")) {
            normalized = '/' + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replaceAll("//+", "/");
    }

    private static String normalizeSpaces(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static String simplifyType(String value) {
        String normalized = normalizeSpaces(value);
        normalized = normalized.replace("org.springframework.http.ResponseEntity", "ResponseEntity");
        normalized = normalized.replace("java.util.List", "List");
        return normalized;
    }

    private String read(Path path) {
        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        } catch (IOException ex) {
            return "";
        }
    }
}
