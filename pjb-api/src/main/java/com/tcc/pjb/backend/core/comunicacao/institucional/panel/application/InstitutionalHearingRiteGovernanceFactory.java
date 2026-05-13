package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class InstitutionalHearingRiteGovernanceFactory {

    private final InstitutionalHearingSchedulingScopeSupport scopeSupport;

    InstitutionalHearingRiteGovernanceFactory(InstitutionalHearingSchedulingScopeSupport scopeSupport) {
        this.scopeSupport = scopeSupport;
    }

    void addIfRelevant(List<InstitutionalHearingRiteGovernance> target, InstitutionalHearingRiteGovernance rite) {
        if (rite != null && rite.sectionVisible()) {
            target.add(rite);
        }
    }

    List<String> mergeSegregationGuards(List<String> base, String... extra) {
        return scopeSupport.mergeSegregationGuards(base, extra);
    }

    InstitutionalHearingRiteGovernance buildRite(InstitutionalOperationalProfileProjection profile,
                                                 InstitutionalProcessWorkspace workspace,
                                                 String schedulingScopeKey,
                                                 String riteCode,
                                                 String justiceBranch,
                                                 String jurisdictionAxis,
                                                 String specializationAxis,
                                                 String hearingKind,
                                                 boolean relevant,
                                                 boolean canRequest,
                                                 boolean canSuggest,
                                                 boolean canOperational,
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
                                                 Set<String> requestActors,
                                                 Set<String> preparatoryActors,
                                                 Set<String> communicationActors,
                                                 Set<String> operationalActors,
                                                 Set<String> trackingActors,
                                                 Set<String> oversightActors,
                                                 List<String> specificAllowedActs,
                                                 List<String> specificForbiddenActs,
                                                 List<String> segregationGuards) {
        String queueScopeKey = scopeSupport.buildRiteQueueScopeKey(profile, workspace, schedulingScopeKey, riteCode, specializationAxis);

        LinkedHashSet<String> allowedActs = new LinkedHashSet<>();
        if (canRequest) {
            allowedActs.add("solicitar_audiencia");
        }
        if (canSuggest) {
            allowedActs.add("sugerir_janela_de_pauta");
        }
        if (canOperational) {
            allowedActs.add("agendar_audiencia");
            allowedActs.add("encaixar_em_pauta_da_unidade");
        }
        if (canReschedule) {
            allowedActs.add("remarcar_audiencia");
        }
        if (canCancel) {
            allowedActs.add("cancelar_audiencia");
        }
        if (canReserveRoom) {
            allowedActs.add("reservar_sala");
        }
        if (canManageVirtualRoom) {
            allowedActs.add("gerir_sala_virtual");
        }
        if (canConfirmAttendance) {
            allowedActs.add("confirmar_presenca");
        }
        if (canRecordTerm) {
            allowedActs.add("registrar_termo_ou_ata");
        }
        if (canIssueHearingCommunications) {
            allowedActs.add("expedir_intimacoes_de_audiencia");
            allowedActs.add("registrar_comunicacoes_de_audiencia");
        }
        if (canPrepareHearingBundle) {
            allowedActs.add("preparar_pasta_audiencia");
            allowedActs.add("saneamento_operacional_de_pauta");
        }
        if (specificAllowedActs != null) {
            allowedActs.addAll(specificAllowedActs);
        }

        LinkedHashSet<String> forbiddenActs = new LinkedHashSet<>();
        if (!canOperational) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_AUTONOMOUS_JUDICIAL_DESIGNATION);
        }
        if (!canRecordTerm) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_FINAL_MINUTES_WITHOUT_PERMISSION);
        }
        if (!canIssueHearingCommunications) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_COMMUNICATION_WITHOUT_ORDER);
        }
        if (!canPrepareHearingBundle) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_BUNDLE_WITHOUT_SEGREGATION);
        }
        if (requiresUnitIsolation) {
            forbiddenActs.add(InstitutionalHearingGovernanceMessages.NO_CROSS_UNIT_SCHEDULING);
        }
        if (specificForbiddenActs != null) {
            forbiddenActs.addAll(specificForbiddenActs);
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalHearingGovernanceMessages.riteFoundation(riteCode, justiceBranch, hearingKind));
        fundamentos.add(InstitutionalHearingGovernanceMessages.jurisdictionAxis(jurisdictionAxis));
        fundamentos.add(InstitutionalHearingGovernanceMessages.specializationAxis(specializationAxis));
        fundamentos.add(InstitutionalHearingGovernanceMessages.queueScopeKey(queueScopeKey));
        fundamentos.add(InstitutionalHearingGovernanceMessages.unitIsolation(requiresUnitIsolation));
        fundamentos.add(InstitutionalHearingGovernanceMessages.communications(canIssueHearingCommunications));
        fundamentos.add(InstitutionalHearingGovernanceMessages.hearingBundle(canPrepareHearingBundle));
        fundamentos.add("request=" + canRequest);
        fundamentos.add("operational=" + canOperational);
        fundamentos.add("tracking=" + (trackingActors != null && !trackingActors.isEmpty()));
        if (workspace != null) {
            fundamentos.add(InstitutionalHearingGovernanceMessages.workspaceTabs(workspace.tabs().size()));
        }

        boolean canOnlyTrack = relevant && !canRequest && !canOperational && ((trackingActors != null && !trackingActors.isEmpty()) || canConfirmAttendance);
        return new InstitutionalHearingRiteGovernance(
                riteCode,
                justiceBranch,
                jurisdictionAxis,
                specializationAxis,
                hearingKind,
                relevant,
                canRequest,
                canSuggest,
                canOperational,
                canReschedule,
                canCancel,
                canReserveRoom,
                canManageVirtualRoom,
                canConfirmAttendance,
                canRecordTerm,
                canIssueHearingCommunications,
                canPrepareHearingBundle,
                canOnlyTrack,
                requiresUnitIsolation,
                requiresJudicialAuthorization && canOperational,
                requiresSecretariatCoordination && (canRequest || canOperational || canOnlyTrack),
                queueScopeKey,
                List.copyOf(allowedActs),
                List.copyOf(forbiddenActs),
                copySet(canRequest ? requestActors : Set.of()),
                copySet(canPrepareHearingBundle ? preparatoryActors : Set.of()),
                copySet(canIssueHearingCommunications ? communicationActors : Set.of()),
                copySet(canOperational ? operationalActors : Set.of()),
                copySet(trackingActors == null || trackingActors.isEmpty() ? Set.of() : trackingActors),
                copySet(oversightActors),
                copyList(segregationGuards),
                List.copyOf(fundamentos));
    }

    private static List<String> copySet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<String> copyList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
