package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalHearingSchedulingGovernanceApplicationService {

    private final InstitutionalHearingSchedulingCapabilityResolver capabilityResolver;
    private final InstitutionalHearingSchedulingScopeSupport scopeSupport;
    private final InstitutionalHearingRiteGovernanceResolver riteGovernanceResolver;

    public InstitutionalHearingSchedulingGovernanceApplicationService() {
        this(new InstitutionalHearingSchedulingCapabilityResolver(),
                new InstitutionalHearingSchedulingScopeSupport(),
                new InstitutionalHearingRiteGovernanceResolver());
    }

    InstitutionalHearingSchedulingGovernanceApplicationService(InstitutionalHearingSchedulingCapabilityResolver capabilityResolver,
                                                               InstitutionalHearingSchedulingScopeSupport scopeSupport,
                                                               InstitutionalHearingRiteGovernanceResolver riteGovernanceResolver) {
        this.capabilityResolver = capabilityResolver;
        this.scopeSupport = scopeSupport;
        this.riteGovernanceResolver = riteGovernanceResolver;
    }

    public InstitutionalHearingSchedulingGovernance avaliar(InstitutionalOperationalProfileProjection profile,
                                                            InstitutionalAccessProfileCatalogEntry catalogEntry,
                                                            InstitutionalProcessWorkspace workspace) {
        if (profile == null) {
            return governanceForMissingProfile();
        }

        InstitutionalHearingSchedulingCapabilityProfile capability = capabilityResolver.resolve(profile, catalogEntry);
        boolean requiresUnitIsolation = capability.sectionVisible() && scopeSupport.hasOperationalUnitContext(profile);
        String schedulingScopeKey = scopeSupport.buildSchedulingScopeKey(profile, workspace, capability.scope(), capability.processProfile());
        List<String> operationalQueues = scopeSupport.resolveOperationalQueues(
                profile,
                workspace,
                capability.scope(),
                capability.processProfile(),
                capability.secretariat(),
                capability.scheduler(),
                capability.management(),
                capability.prisonFlow());
        List<String> segregationGuards = scopeSupport.resolveSegregationGuards(
                profile,
                workspace,
                capability.scope(),
                requiresUnitIsolation,
                capability.secretariat(),
                capability.scheduler(),
                capability.management(),
                capability.prisonFlow());
        List<String> oversightActors = List.copyOf(riteGovernanceResolver.oversightActors(
                capability.processProfile(),
                capability.scope(),
                capability.management(),
                capability.hybridJudicial(),
                true));

        List<InstitutionalHearingRiteGovernance> riteGovernances = riteGovernanceResolver.buildRiteGovernances(
                profile,
                capability.processProfile(),
                capability.nominationRole(),
                capability.capacities(),
                capability.scope(),
                workspace,
                capability.sectionVisible(),
                capability.canRequestHearing(),
                capability.canSuggestSlot(),
                capability.canOperationallySchedule(),
                capability.canReschedule(),
                capability.canCancel(),
                capability.canReserveRoom(),
                capability.canManageVirtualRoom(),
                capability.canConfirmAttendance(),
                capability.canRecordTerm(),
                capability.canIssueHearingCommunications(),
                capability.canPrepareHearingBundle(),
                requiresUnitIsolation,
                capability.requiresJudicialAuthorization(),
                capability.requiresSecretariatCoordination(),
                capability.legalInstitution(),
                capability.secretariat(),
                capability.scheduler(),
                capability.technicalSupport(),
                capability.management(),
                capability.prisonFlow(),
                capability.hybridJudicial(),
                schedulingScopeKey,
                segregationGuards,
                oversightActors);

        LinkedHashSet<String> allowedRiteGroups = new LinkedHashSet<>();
        LinkedHashSet<String> forbiddenActs = new LinkedHashSet<>();
        int operationalRiteCount = 0;
        int trackingOnlyRiteCount = 0;
        for (InstitutionalHearingRiteGovernance rite : riteGovernances) {
            if (rite.sectionVisible() && (rite.canRequestHearing() || rite.canOperationallySchedule() || rite.canOnlyTrack())) {
                allowedRiteGroups.add(rite.riteCode());
            }
            if (rite.canOperationallySchedule()) {
                operationalRiteCount++;
            }
            if (rite.canOnlyTrack()) {
                trackingOnlyRiteCount++;
            }
            forbiddenActs.addAll(rite.forbiddenActs());
        }

        if (!capability.hybridJudicial()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_AUTONOMOUS_JUDICIAL_DESIGNATION);
        }
        if (!capability.canRecordTerm()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_FINAL_MINUTES_WITHOUT_PERMISSION);
        }
        if (capability.technicalSupport()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.DOCUMENTAL_SUPPORT_AUTONOMOUS_SCHEDULING);
        }
        if (capability.legalInstitution()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_SHARED_COLLECTIVE_ACCOUNT);
        }
        if (capability.prisonFlow()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_PRISON_FLOW_CHANGES_WITHOUT_ORDER);
        }
        if (requiresUnitIsolation) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_CROSS_UNIT_SCHEDULING);
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_CROSS_BRANCH_SCHEDULING);
        }
        if (!capability.canIssueHearingCommunications() && capability.sectionVisible()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_COMMUNICATION_WITHOUT_ORDER);
        }
        if (!capability.canPrepareHearingBundle() && capability.sectionVisible()) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_BUNDLE_WITHOUT_SEGREGATION);
        }

        LinkedHashSet<String> findings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalHearingGovernanceMessages.sectionVisible(capability.sectionVisible()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.hearingProfile(capability.processProfile() == null ? null : capability.processProfile().name()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.nominationRole(capability.nominationRole() == null ? null : capability.nominationRole().name()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.scope(capability.scope()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.schedulingScopeKey(schedulingScopeKey));
        fundamentos.add(InstitutionalHearingGovernanceMessages.totalRites(riteGovernances.size()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.operationalRites(operationalRiteCount));
        fundamentos.add(InstitutionalHearingGovernanceMessages.trackingOnlyRites(trackingOnlyRiteCount));
        fundamentos.add(InstitutionalHearingGovernanceMessages.communications(capability.canIssueHearingCommunications()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.hearingBundle(capability.canPrepareHearingBundle()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.unitIsolation(requiresUnitIsolation));
        fundamentos.add(InstitutionalHearingGovernanceMessages.operationalQueues(operationalQueues.size()));
        fundamentos.add(InstitutionalHearingGovernanceMessages.segregationGuards(segregationGuards.size()));

        if (capability.requiresJudicialAuthorization()) {
            findings.add(InstitutionalHearingGovernanceMessages.JUDICIAL_AUTHORIZATION_REQUIRED);
        }
        if (capability.requiresSecretariatCoordination()) {
            findings.add(InstitutionalHearingGovernanceMessages.SECRETARIAT_COORDINATION_REQUIRED);
        }
        if (capability.prisonFlow()) {
            findings.add(InstitutionalHearingGovernanceMessages.PRISON_FLOW_PRESENTATION_CONFIRMATION);
        }
        if (allowedRiteGroups.isEmpty()) {
            findings.add(InstitutionalHearingGovernanceMessages.NO_OPERATIONAL_RITES);
        }
        if (trackingOnlyRiteCount > 0 && operationalRiteCount == 0) {
            findings.add(InstitutionalHearingGovernanceMessages.TRACKING_ONLY_PANEL);
        }

        return new InstitutionalHearingSchedulingGovernance(
                capability.sectionVisible(),
                capability.canRequestHearing(),
                capability.canSuggestSlot(),
                capability.canOrganizeDocket(),
                capability.canOperationallySchedule(),
                capability.canReschedule(),
                capability.canCancel(),
                capability.canReserveRoom(),
                capability.canManageVirtualRoom(),
                capability.canConfirmAttendance(),
                capability.canRecordTerm(),
                capability.canIssueHearingCommunications(),
                capability.canPrepareHearingBundle(),
                requiresUnitIsolation,
                capability.requiresJudicialAuthorization(),
                capability.requiresSecretariatCoordination(),
                schedulingScopeKey,
                operationalQueues,
                segregationGuards,
                oversightActors,
                List.copyOf(allowedRiteGroups),
                riteGovernances,
                List.copyOf(forbiddenActs),
                List.copyOf(findings),
                List.copyOf(fundamentos));
    }

    private InstitutionalHearingSchedulingGovernance governanceForMissingProfile() {
        return new InstitutionalHearingSchedulingGovernance(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "NAO_INFORMADO",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(InstitutionalHearingGovernanceMessages.PROFILE_MISSING),
                List.of(InstitutionalHearingGovernanceMessages.GOVERNANCE_MISSING));
    }
}
