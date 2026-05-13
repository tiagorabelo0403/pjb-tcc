package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalNavigationRouteHardeningTest {

    @Test
    void institutionalEntryControllerMustUseRouteRegistryConstants() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/entry/NationalCommunicationInstitutionalEntryController.java"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_ENTRADA_INTELIGENTE)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_CONTEXTOS_ENTRADA)"));
        assertFalse(controller.contains("@GetMapping(\"/entrada-inteligente\")"));
        assertFalse(controller.contains("@GetMapping(\"/contextos-entrada\")"));
    }


    @Test
    void institutionalAffiliationControllerMustUseRouteRegistryConstants() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalAffiliationController.java"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATIONS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_HOMOLOGATE)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATIONS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ONBOARDING_PLAN)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_AUTHENTICATION_POLICY)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_PROVISIONING)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_PROVISION)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIALS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIALS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_MANAGED_CREDENTIAL_REVOKE)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ROOT_ADMIN_APPROVAL)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ROOT_ADMIN_APPROVAL)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_STRONG_SIGNATURE_POLICY)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_COVERAGE_DELEGATIONS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_COVERAGE_DELEGATIONS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_API_EDGE_PROFILE)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_NOMINATIONS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_NOMINATIONS)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_SECURE_ENTRY)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_ACCESS_CATALOG)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_BLUEPRINTS)"));
        assertFalse(controller.contains("@PostMapping(\"/afiliacoes\")"));
        assertFalse(controller.contains("@PostMapping(\"/afiliacoes/{affiliationId}/homologar\")"));
        assertFalse(controller.contains("@GetMapping(\"/afiliacoes\")"));
        assertFalse(controller.contains("@PostMapping(\"/nomeacoes\")"));
        assertFalse(controller.contains("@GetMapping(\"/nomeacoes\")"));
        assertFalse(controller.contains("@GetMapping(\"/entrada-segura\")"));
        assertFalse(controller.contains("@GetMapping(\"/catalogo-acessos\")"));
        assertFalse(controller.contains("@GetMapping(\"/blueprints\")"));
    }


    @Test
    void institutionalGovernanceAndClosureControllersMustUseRouteRegistryConstants() {
        String governance = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalGovernanceHardeningController.java"));
        String closure = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalClosureController.java"));
        assertTrue(governance.contains("@GetMapping(InstitutionalApiRoutes.PATH_RECERTIFICATIONS)"));
        assertTrue(governance.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_RECERTIFY)"));
        assertTrue(governance.contains("@PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_REVOKE_ACCESS)"));
        assertTrue(governance.contains("@GetMapping(InstitutionalApiRoutes.PATH_GOVERNANCE_INTEGRATIONS)"));
        assertTrue(closure.contains("@GetMapping(InstitutionalApiRoutes.PATH_FOUR_LEVELS)"));
        assertTrue(closure.contains("@GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_CASES)"));
        assertTrue(closure.contains("@GetMapping(InstitutionalApiRoutes.PATH_STRUCTURAL_DIAGNOSTIC)"));
        assertFalse(governance.contains("@GetMapping(\"/recertificacoes\")"));
        assertFalse(governance.contains("@PostMapping(\"/afiliacoes/{affiliationId}/recertificar\")"));
        assertFalse(governance.contains("@PostMapping(\"/afiliacoes/{affiliationId}/revogar-acessos\")"));
        assertFalse(governance.contains("@GetMapping(\"/integracoes-governanca\")"));
        assertFalse(closure.contains("@GetMapping(\"/quatro-niveis\")"));
        assertFalse(closure.contains("@GetMapping(\"/afiliacoes/{affiliationId}/casos-operacionais\")"));
        assertFalse(closure.contains("@GetMapping(\"/diagnostico-estrutural\")"));
    }

    @Test
    void landingPathBuildersMustUseInstitutionalRouteHelpersInsteadOfLiterals() {
        String governance = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalTrustGovernanceOrchestrationApplicationService.java"));
        String dataPlane = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalHorizontalDataPlaneApplicationService.java"));
        assertTrue(governance.contains("InstitutionalApiRoutes.painelPessoal()"));
        assertTrue(governance.contains("InstitutionalApiRoutes.painelExecutivoComUnidade"));
        assertTrue(dataPlane.contains("InstitutionalApiRoutes.painelPessoal()"));
        assertTrue(dataPlane.contains("InstitutionalApiRoutes.painelExecutivoComUnidade"));
        assertFalse(governance.contains("\"/api/v1/painel/pessoal\""));
        assertFalse(governance.contains("\"/api/v1/institucional/painel-executivo"));
        assertFalse(dataPlane.contains("\"/api/v1/painel/pessoal\""));
        assertFalse(dataPlane.contains("\"/api/v1/institucional/painel-executivo"));
    }

    @Test
    void routeRegistryMustEncodeQueryParametersForInstitutionalNavigation() {
        String routes = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/InstitutionalApiRoutes.java"));
        assertTrue(routes.contains("URLEncoder.encode(value, StandardCharsets.UTF_8)"));
    }
}
