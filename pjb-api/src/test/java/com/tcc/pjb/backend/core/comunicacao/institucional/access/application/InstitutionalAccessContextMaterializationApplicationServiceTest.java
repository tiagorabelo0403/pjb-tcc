package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStrongSignaturePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalHorizontalDataPlaneApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOperationalProfileProjectionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalCoverageDelegationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalUnitGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalAccessContextMaterializationApplicationServiceTest {

    @Test
    void materializarMustAggregateUnitBoxAndRlsScope() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        InstitutionalHorizontalDataPlaneApplicationService horizontal = mock(InstitutionalHorizontalDataPlaneApplicationService.class);
        InstitutionalOperationalProfileProjectionApplicationService profileService = mock(InstitutionalOperationalProfileProjectionApplicationService.class);
        InstitutionalTrustGovernanceOrchestrationApplicationService trustService = mock(InstitutionalTrustGovernanceOrchestrationApplicationService.class);
        InstitutionalStrongSignaturePolicyApplicationService signaturePolicyService = mock(InstitutionalStrongSignaturePolicyApplicationService.class);
        InstitutionalCoverageDelegationApplicationService coverageService = mock(InstitutionalCoverageDelegationApplicationService.class);
        InstitutionalUnitGovernanceApplicationService unitService = mock(InstitutionalUnitGovernanceApplicationService.class);
        InstitutionalNominationStateRepository nominationRepository = mock(InstitutionalNominationStateRepository.class);
        InstitutionalAccessContextMaterializationApplicationService service = new InstitutionalAccessContextMaterializationApplicationService(
                currentUserService,
                horizontal,
                profileService,
                trustService,
                signaturePolicyService,
                coverageService,
                unitService,
                nominationRepository);

        when(currentUserService.currentUserIdOrZero()).thenReturn(10L);
        when(horizontal.avaliarAtual("aff-1", "nom-1")).thenReturn(new InstitutionalHorizontalDataPlanePlan(
                "profile-1", "aff-1", "nom-1", "ESTADUAL", "FORUM", "Limoeiro do Norte", "CE", "TJCE", "UNI-1", "Forum Limoeiro", "Limoeiro do Norte", "CAIXA-1",
                "PAINEL_FORUM", "/painel/forum", true, false, true, "SEDE_COMPETENTE", "dp-ce-forum", "write-ce-forum", "rr-ce-forum", 1, 32,
                "warm-2026", List.of("UF", "COMARCA"), Map.of("X-PJB-Data-Plane", "dp-ce-forum"), List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(profileService.materializar("aff-1", "nom-1")).thenReturn(new InstitutionalOperationalProfileProjection(
                "profile-1", "ATIVO", true, "aff-1", "nom-1", 10L, "Servidor Limoeiro", "SERVIDOR", "ESTADUAL", "FORUM", "TJCE", "Forum Limoeiro",
                "UNI-1", "Forum Limoeiro", "CAIXA-1", "LANE_FORUM", "SERVIDOR_FORUM", "PROCESSUAL", "COMUM", "PAINEL_FORUM", "/painel/forum",
                "blue", "CIVEL", "NIVEL_3_CERTIFICADO_QUALIFICADO", true, true, true, false, false, true, "SEDE_COMPETENTE", "TJCE", "UNI-1", "Forum Limoeiro", "Limoeiro do Norte",
                "dp-ce-forum", "write-ce-forum", "rr-ce-forum", List.of("PETICIONAR", "ASSINAR"), List.of(), List.of("TRUST_OK"), List.of(), List.of(), List.of("perfil_ok"), Instant.now()));
        when(trustService.avaliarAtual("aff-1", "nom-1")).thenReturn(new InstitutionalTrustGovernanceProfile(
                "profile-1", "aff-1", "nom-1", 10L, "Servidor Limoeiro", "SERVIDOR", "ESTADUAL", "FORUM", "UNI-1", "CAIXA-1", "PAINEL_FORUM", "/painel/forum",
                "blue", "CIVEL", "NIVEL_3_CERTIFICADO_QUALIFICADO", true, true, true, false, true,
                List.of("A1"), List.of("A1"), List.of(), true, true, false, "dp-ce-forum", List.of(), List.of("trust_ok"), Instant.now()));
        when(signaturePolicyService.avaliar("aff-1", "nom-1")).thenReturn(new InstitutionalStrongSignaturePolicy(
                "aff-1", "nom-1", 10L, "Servidor Limoeiro", "LANE_FORUM", true, true, true, true, true, true, true, true, true, true, true, true, false, true, true,
                List.of(), List.of("signature_ok"), Instant.now()));
        when(unitService.consolidar("aff-1")).thenReturn(new InstitutionalUnitGovernanceSnapshot(
                "snap-1", "aff-1", "TJCE", "Forum Limoeiro", "ESTADUAL", "ATIVO", 1, 2, 1,
                List.of(new InstitutionalManagedUnitEntry("UNI-1", "Forum Limoeiro", null, "COMARCA", "Limoeiro do Norte", "CAIXA-1", "write-ce-forum", "rr-ce-forum", true, true, List.of("CAIXA-1", "CAIXA-2"), List.of("LANE_FORUM"), List.of())),
                List.of(new InstitutionalLotationGovernanceEntry("lot-1", "nom-1", 10L, "Servidor Limoeiro", "UNI-1", "CAIXA-1", "LANE_FORUM", "SERVIDOR_FORUM", "PROCESSUAL", "NIVEL_3_CERTIFICADO_QUALIFICADO", true, Instant.now(), null, List.of())),
                List.of(), List.of("unit_ok"), Instant.now()));
        when(coverageService.consolidar("aff-1")).thenReturn(new InstitutionalCoverageDelegationSnapshot(
                "cov-1", "aff-1", "ATIVA", 1, 1,
                List.of(new InstitutionalCoverageDelegationEntry("deleg-1", "lot-1", 10L, "Servidor Limoeiro", "lot-2", 11L, "Cobertura", "UNI-1", "CAIXA-2", "LANE_FORUM", "SUBSTITUICAO_TEMPORARIA", Instant.now(), null, true, false, List.of())),
                List.of(), List.of("coverage_ok"), Instant.now()));
        when(nominationRepository.findByNominationId("nom-1")).thenReturn(Optional.of(new InstitutionalNomination(
                "nom-1", "aff-1", 10L, "Servidor Limoeiro", null, InstitutionalAccessLaneKind.SECRETARIA,
                InstitutionalNominationRole.SECRETARIA_FORUM, FuncaoOperacionalInstitucional.GESTOR_CAIXA, InstitutionalProcessProfile.SECRETARIA_FORUM,
                "UNI-1", "CAIXA-1", null, InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO, InstitutionalEntryLandingPanel.PAINEL_CAIXA,
                InstitutionalNominationStatus.ATIVA, Instant.now(), null, true, true, true, true, null, Instant.now(), Instant.now())));

        var snapshot = service.materializar("aff-1", "nom-1");

        assertThat(snapshot.affiliationId()).isEqualTo("aff-1");
        assertThat(snapshot.primaryUnitCode()).isEqualTo("UNI-1");
        assertThat(snapshot.allowedBoxCodes()).contains("CAIXA-1", "CAIXA-2");
        assertThat(snapshot.activeCoverageDelegationIds()).contains("deleg-1");
        assertThat(snapshot.rlsScopeKey()).contains("aff-1", "UNI-1");
        assertThat(snapshot.sessionVariables()).containsEntry("X-PJB-RLS-Affiliation", "aff-1");
        assertThat(snapshot.readOnly()).isFalse();
    }
}
