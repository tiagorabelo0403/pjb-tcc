package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalWorkloadIdentityPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityBinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalApiEdgeSecurityProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalIntegrationCredentialStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalApiEdgeSecurityProfileApplicationServiceTest {

    @Test
    void profileMustRequireFapiAndBackendTlsForActiveAffiliation() {
        InstitutionalAffiliationStateRepository affiliationRepository = mock(InstitutionalAffiliationStateRepository.class);
        InstitutionalWorkloadIdentityPlanApplicationService workloadService = mock(InstitutionalWorkloadIdentityPlanApplicationService.class);
        InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService = mock(InstitutionalIntegrationCredentialApplicationService.class);
        InstitutionalAffiliation affiliation = new InstitutionalAffiliation(
                "aff-1",
                DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "FORUM-LN",
                "Fórum de Limoeiro do Norte",
                InstitutionalOrganizationScope.FORUM,
                "forum-default",
                "CE",
                "Limoeiro do Norte",
                null,
                "ESTADUAL",
                List.of("CIVEL"),
                List.of("LIMOEIRO_DO_NORTE"),
                "tjce.jus.br",
                "Diretor",
                1L,
                null,
                "seguranca@tjce.jus.br",
                List.of("API"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                true,
                true,
                true,
                true,
                InstitutionalAffiliationStatus.HOMOLOGADA,
                List.of(),
                Instant.now(),
                Instant.now(),
                null);
        InstitutionalWorkloadIdentityPlan plan = new InstitutionalWorkloadIdentityPlan(
                "aff-1",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "spiffe://pjb.jus.br/tjce",
                "pjb",
                true,
                true,
                true,
                List.of(new InstitutionalWorkloadIdentityBinding("api", "API", "spiffe://pjb.jus.br/tjce/workload/api", "pjb-backend", "pjb", "pjb-api", true, true, List.of("postgres:rw"), List.of())),
                List.of(),
                List.of(),
                Instant.now());
        InstitutionalIntegrationCredential credential = new InstitutionalIntegrationCredential(
                "cred-1",
                "aff-1",
                "TJCE API",
                List.of("processual"),
                List.of("https://tjce.jus.br"),
                true,
                true,
                true,
                true,
                21,
                InstitutionalIntegrationCredentialStatus.ATIVA,
                "kid_1",
                "hash",
                "preview",
                Instant.now(),
                null,
                Instant.now().plusSeconds(86400),
                null,
                List.of(),
                null);
        when(affiliationRepository.findByAffiliationId("aff-1")).thenReturn(Optional.of(affiliation));
        when(workloadService.avaliar("aff-1")).thenReturn(plan);
        when(integrationCredentialApplicationService.list("aff-1")).thenReturn(List.of(credential));
        InstitutionalApiEdgeSecurityProfileApplicationService service = new InstitutionalApiEdgeSecurityProfileApplicationService(
                affiliationRepository,
                workloadService,
                integrationCredentialApplicationService);

        InstitutionalApiEdgeSecurityProfile profile = service.avaliar("aff-1");

        assertTrue(profile.fapi2SecurityProfileRequired());
        assertTrue(profile.fapi2MessageSigningRequired());
        assertTrue(profile.mutualTlsRequired());
        assertTrue(profile.backendTlsPolicyRequired());
        assertTrue(profile.spiffeBindingRequired());
        assertFalse(profile.dpopAllowed());
    }
}
