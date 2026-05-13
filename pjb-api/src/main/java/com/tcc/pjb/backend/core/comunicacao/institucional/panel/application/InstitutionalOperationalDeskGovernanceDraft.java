package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import java.util.LinkedHashSet;
import java.util.List;

final class InstitutionalOperationalDeskGovernanceDraft {

    private final LinkedHashSet<String> unitTopology = new LinkedHashSet<>();
    private final LinkedHashSet<String> operationalDomains = new LinkedHashSet<>();
    private final LinkedHashSet<String> deskQueues = new LinkedHashSet<>();
    private final LinkedHashSet<String> assignmentBoundaries = new LinkedHashSet<>();
    private final LinkedHashSet<String> counterpartScopes = new LinkedHashSet<>();
    private final LinkedHashSet<String> secretariatActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> assessorActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> judgeOverrideActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> managementActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> distributionActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> expeditionActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> conclusionActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> specializedFlows = new LinkedHashSet<>();
    private final LinkedHashSet<String> forbiddenActs = new LinkedHashSet<>();
    private final LinkedHashSet<String> findings = new LinkedHashSet<>();
    private final LinkedHashSet<String> fundamentos = new LinkedHashSet<>();

    static InstitutionalOperationalDeskGovernance missingProfile() {
        return new InstitutionalOperationalDeskGovernance(
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
                false,
                false,
                false,
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                "NAO_INFORMADO",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(InstitutionalOperationalDeskGovernanceMessages.CROSS_VARA_BLOCK),
                List.of(InstitutionalOperationalDeskGovernanceMessages.MISSING_PROFILE),
                List.of(InstitutionalOperationalDeskGovernanceMessages.MISSING_PROFILE));
    }

    static InstitutionalOperationalDeskGovernanceDraft from(InstitutionalOperationalDeskSnapshot snapshot) {
        InstitutionalOperationalDeskGovernanceDraft draft = new InstitutionalOperationalDeskGovernanceDraft();
        draft.unitTopology.addAll(snapshot.fingerprint().topology());
        return draft;
    }

    LinkedHashSet<String> unitTopology() {
        return unitTopology;
    }

    LinkedHashSet<String> operationalDomains() {
        return operationalDomains;
    }

    LinkedHashSet<String> deskQueues() {
        return deskQueues;
    }

    LinkedHashSet<String> assignmentBoundaries() {
        return assignmentBoundaries;
    }

    LinkedHashSet<String> counterpartScopes() {
        return counterpartScopes;
    }

    LinkedHashSet<String> secretariatActs() {
        return secretariatActs;
    }

    LinkedHashSet<String> assessorActs() {
        return assessorActs;
    }

    LinkedHashSet<String> judgeOverrideActs() {
        return judgeOverrideActs;
    }

    LinkedHashSet<String> managementActs() {
        return managementActs;
    }

    LinkedHashSet<String> distributionActs() {
        return distributionActs;
    }

    LinkedHashSet<String> expeditionActs() {
        return expeditionActs;
    }

    LinkedHashSet<String> conclusionActs() {
        return conclusionActs;
    }

    LinkedHashSet<String> specializedFlows() {
        return specializedFlows;
    }

    LinkedHashSet<String> forbiddenActs() {
        return forbiddenActs;
    }

    LinkedHashSet<String> findings() {
        return findings;
    }

    LinkedHashSet<String> fundamentos() {
        return fundamentos;
    }

    InstitutionalOperationalDeskGovernance build(InstitutionalOperationalDeskSnapshot snapshot) {
        return new InstitutionalOperationalDeskGovernance(
                snapshot.sectionVisible(),
                snapshot.unitScopeBound(),
                snapshot.segregatedByTribunal(),
                snapshot.segregatedByComarca(),
                snapshot.segregatedByUnit(),
                snapshot.segregatedByVaraOrSpecialization(),
                snapshot.magistrateOverrideEnabled(),
                snapshot.secretariatWorkflowEnabled(),
                snapshot.assessorWorkflowEnabled(),
                snapshot.triageWorkflowEnabled(),
                snapshot.mandateWorkflowEnabled(),
                snapshot.communicationWorkflowEnabled(),
                snapshot.opinionWorkflowEnabled(),
                snapshot.calculatorWorkflowEnabled(),
                snapshot.batchWorkflowEnabled(),
                snapshot.distributionWorkflowEnabled(),
                snapshot.expeditionWorkflowEnabled(),
                snapshot.conclusionWorkflowEnabled(),
                snapshot.queueManagementWorkflowEnabled(),
                snapshot.organizationalScopeKey(),
                snapshot.territorialScopeKey(),
                snapshot.unitGroupingKey(),
                snapshot.fingerprint().isolationMode(),
                snapshot.judicialAxis(),
                snapshot.unitKind(),
                snapshot.assignmentBoundaryKey(),
                List.copyOf(unitTopology),
                List.copyOf(operationalDomains),
                List.copyOf(deskQueues),
                List.copyOf(assignmentBoundaries),
                List.copyOf(counterpartScopes),
                List.copyOf(secretariatActs),
                List.copyOf(assessorActs),
                List.copyOf(judgeOverrideActs),
                List.copyOf(managementActs),
                List.copyOf(distributionActs),
                List.copyOf(expeditionActs),
                List.copyOf(conclusionActs),
                List.copyOf(specializedFlows),
                List.copyOf(forbiddenActs),
                List.copyOf(findings),
                List.copyOf(fundamentos));
    }
}
