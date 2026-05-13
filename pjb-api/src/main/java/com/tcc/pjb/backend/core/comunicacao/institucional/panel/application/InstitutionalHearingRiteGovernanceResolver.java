package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class InstitutionalHearingRiteGovernanceResolver {

    private final InstitutionalHearingSchedulingActorCatalog actorCatalog;
    private final InstitutionalHearingRiteGovernanceContextFactory contextFactory;
    private final InstitutionalHearingCivilAndJuizadosRiteResolver civilAndJuizadosRiteResolver;
    private final InstitutionalHearingPublicProtectionAndPenalRiteResolver publicProtectionAndPenalRiteResolver;
    private final InstitutionalHearingSpecializedJusticeRiteResolver specializedJusticeRiteResolver;
    private final InstitutionalHearingRecursalRiteResolver recursalRiteResolver;

    InstitutionalHearingRiteGovernanceResolver() {
        this(new InstitutionalHearingSchedulingActorCatalog(), new InstitutionalHearingSchedulingScopeSupport());
    }

    InstitutionalHearingRiteGovernanceResolver(InstitutionalHearingSchedulingActorCatalog actorCatalog,
                                               InstitutionalHearingSchedulingScopeSupport scopeSupport) {
        this.actorCatalog = actorCatalog;
        InstitutionalHearingRiteGovernanceFactory governanceFactory = new InstitutionalHearingRiteGovernanceFactory(scopeSupport);
        this.contextFactory = new InstitutionalHearingRiteGovernanceContextFactory(actorCatalog, scopeSupport);
        this.civilAndJuizadosRiteResolver = new InstitutionalHearingCivilAndJuizadosRiteResolver(governanceFactory);
        this.publicProtectionAndPenalRiteResolver = new InstitutionalHearingPublicProtectionAndPenalRiteResolver(governanceFactory);
        this.specializedJusticeRiteResolver = new InstitutionalHearingSpecializedJusticeRiteResolver(governanceFactory);
        this.recursalRiteResolver = new InstitutionalHearingRecursalRiteResolver(governanceFactory);
    }

    List<InstitutionalHearingRiteGovernance> buildRiteGovernances(InstitutionalOperationalProfileProjection profile,
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
        InstitutionalHearingRiteGovernanceContext context = contextFactory.build(
                profile,
                processProfile,
                nominationRole,
                capacities,
                scope,
                workspace,
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
                schedulingScopeKey,
                topLevelSegregationGuards,
                topLevelOversightActors
        );

        ArrayList<InstitutionalHearingRiteGovernance> rites = new ArrayList<>();
        rites.addAll(civilAndJuizadosRiteResolver.resolve(context));
        rites.addAll(publicProtectionAndPenalRiteResolver.resolve(context));
        rites.addAll(specializedJusticeRiteResolver.resolve(context));
        rites.addAll(recursalRiteResolver.resolve(context));
        return List.copyOf(rites);
    }

    Set<String> oversightActors(InstitutionalProcessProfile processProfile,
                                String scope,
                                boolean management,
                                boolean hybridJudicial,
                                boolean includeJudicial) {
        return actorCatalog.oversightActors(processProfile, scope, management, hybridJudicial, includeJudicial);
    }
}
