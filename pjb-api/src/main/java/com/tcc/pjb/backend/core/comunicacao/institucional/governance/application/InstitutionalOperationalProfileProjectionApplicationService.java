package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOperationalProfileProjectionApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;
    private final InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService;

    public InstitutionalOperationalProfileProjectionApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                      InstitutionalNominationStateRepository nominationRepository,
                                                                      InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService,
                                                                      InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.trustGovernanceOrchestrationApplicationService = Objects.requireNonNull(trustGovernanceOrchestrationApplicationService);
        this.horizontalDataPlaneApplicationService = Objects.requireNonNull(horizontalDataPlaneApplicationService);
    }

    public InstitutionalOperationalProfileProjection materializar(String affiliationId, String nominationId) {
        Instant now = Instant.now();
        InstitutionalNomination nomination = resolveNomination(nominationId);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        InstitutionalTrustGovernanceProfile trustProfile = trustGovernanceOrchestrationApplicationService.avaliarAtual(resolveAffiliationId(affiliationId, nomination), nominationId);
        InstitutionalHorizontalDataPlanePlan dataPlanePlan = horizontalDataPlaneApplicationService.avaliarAtual(resolveAffiliationId(affiliationId, nomination), nominationId);
        if (nomination == null) {
            LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
            fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_VISIBLE_IN_PJB);
            fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_DERIVED_FROM_NOMINATION);
            fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_CANNOT_BYPASS_GOVERNANCE);
            fundamentos.add(InstitutionalOperationalProfileMessages.state("AUSENTE"));
            if (trustProfile != null) {
                fundamentos.addAll(trustProfile.fundamentos());
            }
            if (dataPlanePlan != null) {
                fundamentos.addAll(dataPlanePlan.fundamentos());
            }
            List<String> findings = new ArrayList<>();
            if (trustProfile != null) {
                findings.addAll(trustProfile.findings());
            }
            if (dataPlanePlan != null) {
                findings.addAll(dataPlanePlan.findings());
            }
            if (findings.stream().noneMatch("nomeacao_institucional_ausente"::equals)) {
                findings.add("nomeacao_institucional_ausente");
            }
            return new InstitutionalOperationalProfileProjection(
                    trustProfile == null ? null : trustProfile.profileKey(),
                    "AUSENTE",
                    false,
                    affiliation == null ? null : affiliation.affiliationId(),
                    nominationId,
                    null,
                    null,
                    null,
                    affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                    affiliation == null ? null : affiliation.orgaoSigla(),
                    affiliation == null ? null : affiliation.orgaoNome(),
                    affiliation == null ? null : affiliation.unidadeCodigo(),
                    affiliation == null ? null : affiliation.unidadeNome(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    trustProfile == null ? null : trustProfile.panelCode(),
                    trustProfile == null ? null : trustProfile.landingPath(),
                    trustProfile == null ? null : trustProfile.accentColor(),
                    trustProfile == null ? null : trustProfile.processAreaCode(),
                    trustProfile == null ? null : trustProfile.trustFloor(),
                    false,
                    trustProfile != null && trustProfile.fullyApproved(),
                    dataPlanePlan != null && dataPlanePlan.readyForInstitutionalPanel(),
                    trustProfile != null && trustProfile.routeToPersonalPanel(),
                    trustProfile != null && trustProfile.directPersonalAccessAvailable(),
                    dataPlanePlan != null && dataPlanePlan.localUnitPresent(),
                    dataPlanePlan == null ? null : dataPlanePlan.coverageMode(),
                    dataPlanePlan == null ? null : dataPlanePlan.responsibleTribunalCode(),
                    dataPlanePlan == null ? null : dataPlanePlan.responsibleUnitCode(),
                    dataPlanePlan == null ? null : dataPlanePlan.responsibleUnitName(),
                    dataPlanePlan == null ? null : dataPlanePlan.responsibleComarca(),
                    dataPlanePlan == null ? null : dataPlanePlan.horizontalDataPlaneKey(),
                    dataPlanePlan == null ? null : dataPlanePlan.primaryWritePartitionKey(),
                    dataPlanePlan == null ? null : dataPlanePlan.readReplicaCode(),
                    List.of(),
                    trustProfile == null ? List.of() : trustProfile.requiredApprovals(),
                    trustProfile == null ? List.of() : trustProfile.approvedApprovals(),
                    trustProfile == null ? List.of() : trustProfile.pendingApprovals(),
                    List.copyOf(findings),
                    List.copyOf(fundamentos),
                    now);
        }
        String profileState = determineProfileState(nomination, trustProfile, dataPlanePlan, now);
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_VISIBLE_IN_PJB);
        fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_DERIVED_FROM_NOMINATION);
        fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_ROUTED_BY_PANEL);
        fundamentos.add(InstitutionalOperationalProfileMessages.PROFILE_CANNOT_BYPASS_GOVERNANCE);
        fundamentos.add(InstitutionalOperationalProfileMessages.state(profileState));
        fundamentos.add(InstitutionalOperationalProfileMessages.role(nomination.nominationRole().name()));
        fundamentos.add(InstitutionalOperationalProfileMessages.unit(nomination.unidadeCodigo()));
        fundamentos.add(InstitutionalOperationalProfileMessages.box(nomination.caixaCodigo()));
        fundamentos.add(InstitutionalOperationalProfileMessages.audience(nomination.tipoUsuario() == null ? "INSTITUCIONAL" : nomination.tipoUsuario().name()));
        if (affiliation != null) {
            fundamentos.addAll(affiliation.fundamentos());
        }
        if (trustProfile != null) {
            fundamentos.addAll(trustProfile.fundamentos());
        }
        if (dataPlanePlan != null) {
            fundamentos.addAll(dataPlanePlan.fundamentos());
        }
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        if (trustProfile != null) {
            findings.addAll(trustProfile.findings());
        }
        if (dataPlanePlan != null) {
            findings.addAll(dataPlanePlan.findings());
        }
        return new InstitutionalOperationalProfileProjection(
                trustProfile == null ? null : trustProfile.profileKey(),
                profileState,
                true,
                nomination.affiliationId(),
                nomination.nominationId(),
                nomination.nominatedUserId(),
                nomination.nominatedUserName(),
                nomination.tipoUsuario() == null ? null : nomination.tipoUsuario().name(),
                affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                affiliation == null ? null : affiliation.orgaoSigla(),
                affiliation == null ? null : affiliation.orgaoNome(),
                nomination.unidadeCodigo(),
                affiliation == null ? null : affiliation.unidadeNome(),
                nomination.caixaCodigo(),
                nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name(),
                nomination.nominationRole().name(),
                nomination.funcaoOperacional().name(),
                nomination.processProfile().name(),
                trustProfile == null ? null : trustProfile.panelCode(),
                trustProfile == null ? null : trustProfile.landingPath(),
                trustProfile == null ? null : trustProfile.accentColor(),
                trustProfile == null ? null : trustProfile.processAreaCode(),
                trustProfile == null ? null : trustProfile.trustFloor(),
                nomination.ativaEm(now),
                trustProfile != null && trustProfile.fullyApproved(),
                dataPlanePlan != null && dataPlanePlan.readyForInstitutionalPanel(),
                trustProfile != null && trustProfile.routeToPersonalPanel(),
                trustProfile != null && trustProfile.directPersonalAccessAvailable(),
                dataPlanePlan != null && dataPlanePlan.localUnitPresent(),
                dataPlanePlan == null ? null : dataPlanePlan.coverageMode(),
                dataPlanePlan == null ? null : dataPlanePlan.responsibleTribunalCode(),
                dataPlanePlan == null ? null : dataPlanePlan.responsibleUnitCode(),
                dataPlanePlan == null ? null : dataPlanePlan.responsibleUnitName(),
                dataPlanePlan == null ? null : dataPlanePlan.responsibleComarca(),
                dataPlanePlan == null ? null : dataPlanePlan.horizontalDataPlaneKey(),
                dataPlanePlan == null ? null : dataPlanePlan.primaryWritePartitionKey(),
                dataPlanePlan == null ? null : dataPlanePlan.readReplicaCode(),
                nomination.capacidades().stream().map(Enum::name).toList(),
                trustProfile == null ? List.of() : trustProfile.requiredApprovals(),
                trustProfile == null ? List.of() : trustProfile.approvedApprovals(),
                trustProfile == null ? List.of() : trustProfile.pendingApprovals(),
                List.copyOf(findings),
                List.copyOf(fundamentos),
                now);
    }

    private InstitutionalNomination resolveNomination(String nominationId) {
        if (nominationId == null || nominationId.isBlank()) {
            return null;
        }
        return nominationRepository.findByNominationId(nominationId).orElse(null);
    }

    private InstitutionalAffiliation resolveAffiliation(String affiliationId, InstitutionalNomination nomination) {
        String target = resolveAffiliationId(affiliationId, nomination);
        if (target == null || target.isBlank()) {
            return null;
        }
        return affiliationRepository.findByAffiliationId(target).orElse(null);
    }

    private String resolveAffiliationId(String affiliationId, InstitutionalNomination nomination) {
        if (affiliationId != null && !affiliationId.isBlank()) {
            return affiliationId.trim();
        }
        return nomination == null ? null : nomination.affiliationId();
    }

    private String determineProfileState(InstitutionalNomination nomination,
                                         InstitutionalTrustGovernanceProfile trustProfile,
                                         InstitutionalHorizontalDataPlanePlan dataPlanePlan,
                                         Instant now) {
        if (!nomination.ativaEm(now)) {
            return "INATIVO";
        }
        if (trustProfile != null && !trustProfile.fullyApproved()) {
            return "AGUARDANDO_GOVERNANCA";
        }
        if (dataPlanePlan != null && !dataPlanePlan.readyForInstitutionalPanel()) {
            return "AGUARDANDO_ATIVACAO_PAINEL";
        }
        return "ATIVO_NO_PJB";
    }
}
