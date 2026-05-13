package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
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
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAccessContextMaterializationApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService;
    private final InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService;
    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;
    private final InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService;
    private final InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService;
    private final InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService;
    private final InstitutionalNominationStateRepository nominationRepository;

    public InstitutionalAccessContextMaterializationApplicationService(CurrentUserService currentUserService,
                                                                      InstitutionalHorizontalDataPlaneApplicationService horizontalDataPlaneApplicationService,
                                                                      InstitutionalOperationalProfileProjectionApplicationService operationalProfileProjectionApplicationService,
                                                                      InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService,
                                                                      InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService,
                                                                      InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService,
                                                                      InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService,
                                                                      InstitutionalNominationStateRepository nominationRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.horizontalDataPlaneApplicationService = Objects.requireNonNull(horizontalDataPlaneApplicationService);
        this.operationalProfileProjectionApplicationService = Objects.requireNonNull(operationalProfileProjectionApplicationService);
        this.trustGovernanceOrchestrationApplicationService = Objects.requireNonNull(trustGovernanceOrchestrationApplicationService);
        this.strongSignaturePolicyApplicationService = Objects.requireNonNull(strongSignaturePolicyApplicationService);
        this.coverageDelegationApplicationService = Objects.requireNonNull(coverageDelegationApplicationService);
        this.unitGovernanceApplicationService = Objects.requireNonNull(unitGovernanceApplicationService);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
    }

    public InstitutionalAccessContextSnapshot materializar(String affiliationId, String nominationId) {
        InstitutionalHorizontalDataPlanePlan horizontalPlan = horizontalDataPlaneApplicationService.avaliarAtual(affiliationId, nominationId);
        InstitutionalOperationalProfileProjection profile = operationalProfileProjectionApplicationService.materializar(affiliationId, nominationId);
        InstitutionalTrustGovernanceProfile trust = trustGovernanceOrchestrationApplicationService.avaliarAtual(affiliationId, nominationId);
        String effectiveAffiliationId = firstNonBlank(
                affiliationId,
                profile == null ? null : profile.affiliationId(),
                horizontalPlan == null ? null : horizontalPlan.affiliationId(),
                trust == null ? null : trust.affiliationId());
        String effectiveNominationId = firstNonBlank(
                nominationId,
                profile == null ? null : profile.nominationId(),
                horizontalPlan == null ? null : horizontalPlan.nominationId(),
                trust == null ? null : trust.nominationId());
        var signaturePolicy = strongSignaturePolicyApplicationService.avaliar(effectiveAffiliationId, effectiveNominationId);
        InstitutionalUnitGovernanceSnapshot governance = effectiveAffiliationId == null ? null : unitGovernanceApplicationService.consolidar(effectiveAffiliationId);
        var coverageSnapshot = effectiveAffiliationId == null ? null : coverageDelegationApplicationService.consolidar(effectiveAffiliationId);
        Long currentUserId = currentUserService.currentUserIdOrZero();
        InstitutionalNomination nomination = resolveNomination(effectiveNominationId);
        Set<String> unitCodes = new LinkedHashSet<>();
        Set<String> boxCodes = new LinkedHashSet<>();
        Set<String> laneCodes = new LinkedHashSet<>();
        Set<String> activeDelegationIds = new LinkedHashSet<>();
        if (profile != null) {
            add(unitCodes, profile.unidadeCodigo());
            add(boxCodes, profile.caixaCodigo());
            add(laneCodes, profile.accessLaneKind());
        }
        if (horizontalPlan != null) {
            add(unitCodes, horizontalPlan.responsibleUnitCode());
            add(boxCodes, horizontalPlan.caixaCodigo());
        }
        if (governance != null) {
            for (InstitutionalLotationGovernanceEntry lotation : governance.lotacoes()) {
                if (lotation == null || !lotation.active() || currentUserId == 0L || !Objects.equals(lotation.userId(), currentUserId)) {
                    continue;
                }
                add(unitCodes, lotation.unitCode());
                add(boxCodes, lotation.boxCode());
                add(laneCodes, lotation.laneCode());
            }
        }
        if (coverageSnapshot != null) {
            for (InstitutionalCoverageDelegationEntry delegation : coverageSnapshot.delegations()) {
                if (delegation == null || !delegation.active()) {
                    continue;
                }
                boolean touchesCurrentUser = currentUserId != 0L && (Objects.equals(delegation.sourceUserId(), currentUserId) || Objects.equals(delegation.targetUserId(), currentUserId));
                boolean touchesCurrentNomination = effectiveNominationId != null && governance != null && governance.lotacoes().stream().anyMatch(item -> Objects.equals(item.nominationId(), effectiveNominationId) && (Objects.equals(item.lotationId(), delegation.sourceLotationId()) || Objects.equals(item.lotationId(), delegation.targetLotationId())));
                if (!touchesCurrentUser && !touchesCurrentNomination) {
                    continue;
                }
                add(activeDelegationIds, delegation.delegationId());
                add(unitCodes, delegation.unitCode());
                add(boxCodes, delegation.boxCode());
                add(laneCodes, delegation.laneCode());
            }
        }
        String primaryUnitCode = firstNonBlank(
                profile == null ? null : profile.unidadeCodigo(),
                horizontalPlan == null ? null : horizontalPlan.responsibleUnitCode(),
                unitCodes.stream().findFirst().orElse(null));
        String primaryBoxCode = firstNonBlank(
                profile == null ? null : profile.caixaCodigo(),
                horizontalPlan == null ? null : horizontalPlan.caixaCodigo(),
                boxCodes.stream().findFirst().orElse(null));
        ArrayList<String> restrictions = new ArrayList<>();
        if (trust != null && trust.requiresStepUp()) {
            restrictions.add("STEP_UP_REQUIRED");
        }
        if (signaturePolicy != null && signaturePolicy.qualifiedCertificateRequired()) {
            restrictions.add("CERTIFICADO_QUALIFICADO_EXIGIDO_PARA_ATOS_FORTES");
        }
        if (trust != null && trust.requiresInstitutionalNetwork()) {
            restrictions.add("REDE_INSTITUCIONAL_OU_AUTORIZACAO_REMOTA");
        }
        if (trust != null && !trust.pendingApprovals().isEmpty()) {
            restrictions.add("APROVACOES_PENDENTES");
        }
        if (coverageSnapshot != null && coverageSnapshot.activeDelegations() > 0) {
            restrictions.add("COBERTURA_TERRITORIAL_GOVERNADA");
        }
        if (governance != null && primaryUnitCode != null) {
            governance.units().stream()
                    .filter(item -> Objects.equals(item.unitCode(), primaryUnitCode))
                    .findFirst()
                    .map(InstitutionalManagedUnitEntry::territorialScope)
                    .filter(value -> value != null && !value.isBlank())
                    .ifPresent(scope -> restrictions.add("ESCOPO_TERRITORIAL=" + scope.trim().toUpperCase(Locale.ROOT)));
        }
        boolean readOnly = profile == null
                || !profile.visibleInPjb()
                || !profile.activeNomination()
                || (trust != null && !trust.pendingApprovals().isEmpty() && !trust.fullyApproved());
        LinkedHashMap<String, String> sessionVariables = new LinkedHashMap<>();
        put(sessionVariables, "X-PJB-Affiliation-Id", effectiveAffiliationId);
        put(sessionVariables, "X-PJB-Nomination-Id", effectiveNominationId);
        put(sessionVariables, "X-PJB-Institutional-Unit-Code", primaryUnitCode);
        put(sessionVariables, "X-PJB-Institutional-Box-Code", primaryBoxCode);
        put(sessionVariables, "X-PJB-Institutional-Data-Plane-Key", horizontalPlan == null ? null : horizontalPlan.horizontalDataPlaneKey());
        put(sessionVariables, "X-PJB-RLS-Affiliation", effectiveAffiliationId);
        put(sessionVariables, "X-PJB-RLS-Unit", primaryUnitCode);
        put(sessionVariables, "X-PJB-RLS-Box", primaryBoxCode);
        put(sessionVariables, "X-PJB-RLS-Read-Only", Boolean.toString(readOnly));
        String rlsScopeKey = buildRlsScopeKey(effectiveAffiliationId, primaryUnitCode, primaryBoxCode, unitCodes, boxCodes);
        ArrayList<String> findings = new ArrayList<>();
        merge(findings, profile == null ? List.of("perfil_operacional_nao_materializado") : profile.findings());
        merge(findings, horizontalPlan == null ? List.of("plano_horizontal_nao_materializado") : horizontalPlan.findings());
        merge(findings, trust == null ? List.of("governanca_confianca_nao_materializada") : trust.findings());
        if (governance != null) {
            merge(findings, governance.findings());
        }
        if (coverageSnapshot != null) {
            merge(findings, coverageSnapshot.findings());
        }
        ArrayList<String> fundamentos = new ArrayList<>();
        merge(fundamentos, profile == null ? List.of() : profile.fundamentos());
        merge(fundamentos, horizontalPlan == null ? List.of() : horizontalPlan.fundamentos());
        merge(fundamentos, trust == null ? List.of() : trust.fundamentos());
        if (signaturePolicy != null) {
            merge(fundamentos, signaturePolicy.fundamentos());
        }
        if (nomination != null && nomination.nominationId() != null) {
            fundamentos.add("nomeacao_resolvida=" + nomination.nominationId());
        }
        fundamentos.add("rlsScopeKey=" + rlsScopeKey);
        return new InstitutionalAccessContextSnapshot(
                firstNonBlank(profile == null ? null : profile.profileKey(), horizontalPlan == null ? null : horizontalPlan.profileKey(), trust == null ? null : trust.profileKey()),
                effectiveAffiliationId,
                effectiveNominationId,
                firstNonBlank(profile == null ? null : profile.panelCode(), horizontalPlan == null ? null : horizontalPlan.panelCode(), trust == null ? null : trust.panelCode()),
                profile == null ? null : profile.processAreaCode(),
                primaryUnitCode,
                primaryBoxCode,
                firstNonBlank(profile == null ? null : profile.coverageMode(), horizontalPlan == null ? null : horizontalPlan.coverageMode()),
                firstNonBlank(profile == null ? null : profile.horizontalDataPlaneKey(), horizontalPlan == null ? null : horizontalPlan.horizontalDataPlaneKey(), trust == null ? null : trust.horizontalDataPlaneKey()),
                firstNonBlank(profile == null ? null : profile.primaryWritePartitionKey(), horizontalPlan == null ? null : horizontalPlan.primaryWritePartitionKey()),
                firstNonBlank(profile == null ? null : profile.readReplicaCode(), horizontalPlan == null ? null : horizontalPlan.readReplicaCode()),
                firstNonBlank(profile == null ? null : profile.trustFloor(), trust == null ? null : trust.trustFloor(), nomination == null || nomination.trustFloor() == null ? null : nomination.trustFloor().name()),
                profile != null && profile.readyForInstitutionalPanel(),
                trust != null && trust.fullyApproved(),
                readOnly,
                trust != null && trust.requiresStepUp(),
                trust != null && trust.requiresCertificate(),
                trust != null && trust.judicialFlowSensitive(),
                rlsScopeKey,
                List.copyOf(unitCodes),
                List.copyOf(boxCodes),
                List.copyOf(laneCodes),
                List.copyOf(activeDelegationIds),
                sanitize(restrictions),
                Map.copyOf(sessionVariables),
                sanitize(findings),
                sanitize(fundamentos),
                Instant.now());
    }

    private InstitutionalNomination resolveNomination(String nominationId) {
        if (nominationId == null || nominationId.isBlank()) {
            return null;
        }
        Optional<InstitutionalNomination> direct = nominationRepository.findByNominationId(nominationId.trim());
        return direct.orElse(null);
    }

    private String buildRlsScopeKey(String affiliationId,
                                    String primaryUnitCode,
                                    String primaryBoxCode,
                                    Set<String> unitCodes,
                                    Set<String> boxCodes) {
        String unitScope = primaryUnitCode != null ? primaryUnitCode : (unitCodes.isEmpty() ? "NO_UNIT" : String.join(",", unitCodes));
        String boxScope = primaryBoxCode != null ? primaryBoxCode : (boxCodes.isEmpty() ? "NO_BOX" : String.join(",", boxCodes));
        return String.join("::", safe(affiliationId, "NO_AFFILIATION"), unitScope, boxScope);
    }

    private void put(Map<String, String> target, String key, String value) {
        if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
            target.put(key.trim(), value.trim());
        }
    }

    private void add(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private void merge(List<String> target, List<String> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        target.addAll(source);
    }

    private List<String> sanitize(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
