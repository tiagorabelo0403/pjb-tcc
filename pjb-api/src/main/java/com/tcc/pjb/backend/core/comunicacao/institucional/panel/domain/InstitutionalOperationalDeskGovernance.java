package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.util.List;
import java.util.Objects;

public record InstitutionalOperationalDeskGovernance(
        boolean sectionVisible,
        boolean unitScopeBound,
        boolean segregatedByTribunal,
        boolean segregatedByComarca,
        boolean segregatedByUnit,
        boolean segregatedByVaraOrSpecialization,
        boolean magistrateOverrideEnabled,
        boolean secretariatWorkflowEnabled,
        boolean assessorWorkflowEnabled,
        boolean triageWorkflowEnabled,
        boolean mandateWorkflowEnabled,
        boolean communicationWorkflowEnabled,
        boolean opinionWorkflowEnabled,
        boolean calculatorWorkflowEnabled,
        boolean batchWorkflowEnabled,
        boolean distributionWorkflowEnabled,
        boolean expeditionWorkflowEnabled,
        boolean conclusionWorkflowEnabled,
        boolean queueManagementWorkflowEnabled,
        String organizationalScopeKey,
        String territorialScopeKey,
        String unitGroupingKey,
        String operationalIsolationMode,
        String judicialAxis,
        String unitKind,
        String assignmentBoundaryKey,
        List<String> unitTopology,
        List<String> operationalDomains,
        List<String> deskQueues,
        List<String> assignmentBoundaries,
        List<String> counterpartScopes,
        List<String> secretariatActs,
        List<String> assessorActs,
        List<String> judgeOverrideActs,
        List<String> managementActs,
        List<String> distributionActs,
        List<String> expeditionActs,
        List<String> conclusionActs,
        List<String> specializedFlows,
        List<String> forbiddenActs,
        List<String> findings,
        List<String> fundamentos
) {
    public InstitutionalOperationalDeskGovernance {
        Objects.requireNonNull(organizationalScopeKey);
        Objects.requireNonNull(territorialScopeKey);
        Objects.requireNonNull(unitGroupingKey);
        Objects.requireNonNull(operationalIsolationMode);
        Objects.requireNonNull(judicialAxis);
        Objects.requireNonNull(unitKind);
        Objects.requireNonNull(assignmentBoundaryKey);
        Objects.requireNonNull(unitTopology);
        Objects.requireNonNull(operationalDomains);
        Objects.requireNonNull(deskQueues);
        Objects.requireNonNull(assignmentBoundaries);
        Objects.requireNonNull(counterpartScopes);
        Objects.requireNonNull(secretariatActs);
        Objects.requireNonNull(assessorActs);
        Objects.requireNonNull(judgeOverrideActs);
        Objects.requireNonNull(managementActs);
        Objects.requireNonNull(distributionActs);
        Objects.requireNonNull(expeditionActs);
        Objects.requireNonNull(conclusionActs);
        Objects.requireNonNull(specializedFlows);
        Objects.requireNonNull(forbiddenActs);
        Objects.requireNonNull(findings);
        Objects.requireNonNull(fundamentos);
    }
}
