package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class InstitutionalHearingRiteGovernanceContextFactory {

    private final InstitutionalHearingSchedulingActorCatalog actorCatalog;
    private final InstitutionalHearingSchedulingScopeSupport scopeSupport;

    InstitutionalHearingRiteGovernanceContextFactory(InstitutionalHearingSchedulingActorCatalog actorCatalog,
                                                     InstitutionalHearingSchedulingScopeSupport scopeSupport) {
        this.actorCatalog = actorCatalog;
        this.scopeSupport = scopeSupport;
    }

    InstitutionalHearingRiteGovernanceContext build(InstitutionalOperationalProfileProjection profile,
                                                    InstitutionalProcessProfile processProfile,
                                                    InstitutionalNominationRole nominationRole,
                                                    Set<CapacidadeCaixaInstitucional> capacities,
                                                    String scope,
                                                    InstitutionalProcessWorkspace workspace,
                                                    boolean sectionVisible,
                                                    boolean canRequestHearing,
                                                    boolean canSuggestSlot,
                                                    boolean canOperationallySchedule,
                                                    boolean canReschedule,
                                                    boolean canCancel,
                                                    boolean canReserveRoom,
                                                    boolean canManageVirtualRoom,
                                                    boolean canConfirmAttendance,
                                                    boolean canRecordTerm,
                                                    boolean canIssueHearingCommunications,
                                                    boolean canPrepareHearingBundle,
                                                    boolean requiresUnitIsolation,
                                                    boolean requiresJudicialAuthorization,
                                                    boolean requiresSecretariatCoordination,
                                                    boolean legalInstitution,
                                                    boolean secretariat,
                                                    boolean scheduler,
                                                    boolean technicalSupport,
                                                    boolean management,
                                                    boolean prisonFlow,
                                                    boolean hybridJudicial,
                                                    String schedulingScopeKey,
                                                    List<String> topLevelSegregationGuards,
                                                    List<String> topLevelOversightActors) {
        LinkedHashSet<String> requestActors = actorCatalog.requestActors(processProfile, legalInstitution, technicalSupport, secretariat, scheduler, management, prisonFlow);
        LinkedHashSet<String> preparatoryActors = actorCatalog.preparatoryActors(processProfile, technicalSupport, secretariat, scheduler, management, prisonFlow);
        LinkedHashSet<String> communicationActors = actorCatalog.communicationActors(processProfile, secretariat, scheduler, management, prisonFlow);
        LinkedHashSet<String> operationalActors = actorCatalog.operationalActors(processProfile, secretariat, scheduler, management, hybridJudicial);
        LinkedHashSet<String> trackingActors = actorCatalog.trackingActors(processProfile, legalInstitution, technicalSupport, secretariat, scheduler, management, prisonFlow, hybridJudicial);
        LinkedHashSet<String> oversightActors = new LinkedHashSet<>(actorCatalog.oversightActors(processProfile, scope, management, hybridJudicial, true));
        if (topLevelOversightActors != null) {
            topLevelOversightActors.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .forEach(oversightActors::add);
        }

        boolean federalScope = scopeSupport.isFederalScope(scope, profile, workspace, processProfile);
        boolean laborScope = scopeSupport.isLaborScope(scope, profile, workspace);
        boolean electoralScope = scopeSupport.isElectoralScope(scope, profile, workspace);
        boolean militaryScope = scopeSupport.isMilitaryScope(scope, profile, workspace);
        boolean militaryFederalScope = militaryScope && scopeSupport.scopeMatches(scope, "FED");
        boolean broadCommonScope = !federalScope && !laborScope && !electoralScope && !militaryScope;
        boolean cejuscScope = scopeSupport.scopeMatches(scope, "CEJUSC")
                || processProfile == InstitutionalProcessProfile.CONCILIADOR
                || processProfile == InstitutionalProcessProfile.MEDIADOR
                || processProfile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO;

        boolean civilBroad = sectionVisible && (legalInstitution || technicalSupport || secretariat || scheduler || management);
        boolean juizadosBroad = civilBroad;
        boolean publicTreasuryBroad = sectionVisible && (legalInstitution || technicalSupport || secretariat || scheduler || management);
        boolean childhoodBroad = sectionVisible && (legalInstitution || technicalSupport || secretariat || scheduler || management);
        boolean penalBroad = sectionVisible && (legalInstitution || technicalSupport || secretariat || scheduler || management || prisonFlow);
        boolean recursalBroad = sectionVisible && (legalInstitution || secretariat || management || hybridJudicial);

        return new InstitutionalHearingRiteGovernanceContext(
                profile,
                workspace,
                scope,
                schedulingScopeKey,
                topLevelSegregationGuards,
                requestActors,
                preparatoryActors,
                communicationActors,
                operationalActors,
                trackingActors,
                oversightActors,
                sectionVisible,
                canRequestHearing,
                canSuggestSlot,
                canOperationallySchedule,
                canReschedule,
                canCancel,
                canReserveRoom,
                canManageVirtualRoom,
                canConfirmAttendance,
                canRecordTerm,
                canIssueHearingCommunications,
                canPrepareHearingBundle,
                requiresUnitIsolation,
                requiresJudicialAuthorization,
                requiresSecretariatCoordination,
                legalInstitution,
                secretariat,
                scheduler,
                technicalSupport,
                management,
                prisonFlow,
                hybridJudicial,
                federalScope,
                laborScope,
                electoralScope,
                militaryScope,
                militaryFederalScope,
                broadCommonScope,
                cejuscScope,
                civilBroad,
                juizadosBroad,
                publicTreasuryBroad,
                childhoodBroad,
                penalBroad,
                recursalBroad
        );
    }
}
