package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalDelegatedAndSecurityRouteHardeningTest {

    @Test
    void delegatedOnboardingAndClosureControllersMustUseInstitutionalRouteRegistryConstants() {
        String onboarding = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalDelegatedOnboardingController.java"));
        String closure = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalDelegatedClosureController.java"));
        assertTrue(onboarding.contains("@PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATIONS)"));
        assertTrue(onboarding.contains("@PostMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_HOMOLOGATE)"));
        assertTrue(onboarding.contains("@GetMapping(InstitutionalApiRoutes.PATH_TRUST_MATRIX)"));
        assertTrue(onboarding.contains("@GetMapping(InstitutionalApiRoutes.PATH_PANEL_BLUEPRINTS)"));
        assertTrue(onboarding.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER)"));
        assertTrue(closure.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_CLOSURE)"));
        assertTrue(closure.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_CURRENT_ENTRY)"));
        assertFalse(onboarding.contains("@PostMapping(\"/adesoes-delegadas\")"));
        assertFalse(closure.contains("@GetMapping(\"/fechamento-delegado\")"));
    }

    @Test
    void institutionalSecurityControllerMustUseInstitutionalRouteRegistryConstants() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/security/NationalCommunicationInstitutionalSecurityGovernanceController.java"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_VALIDATION)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_APPROVAL_TRAIL)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_REMOTE_CERTIFICATE_AUTHORIZATION_REVOKE)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_SESSION_RISK)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_SENSITIVE_ACT_AUTHORIZE)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIALS)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_ROTATE)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_REVOKE)"));
        assertTrue(controller.contains("@PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_REGISTER_CALL)"));
        assertTrue(controller.contains("@GetMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_TRAIL)"));
        assertFalse(controller.contains("@PostMapping(\"/credenciais-integracao\")"));
        assertFalse(controller.contains("@GetMapping(\"/risco-sessao\")"));
    }
}
