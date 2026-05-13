package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalOperationalDeskGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.Objects;

final class InstitutionalOperationalDeskGovernanceAssembler {

    private final InstitutionalOperationalDeskBaselineAssembler baselineAssembler;
    private final InstitutionalOperationalDeskRoleActsAssembler roleActsAssembler;
    private final InstitutionalOperationalDeskUnitAugmenter unitAugmenter;
    private final InstitutionalOperationalDeskJudicialAxisAugmenter judicialAxisAugmenter;

    InstitutionalOperationalDeskGovernanceAssembler(InstitutionalOperationalDeskSupport support) {
        Objects.requireNonNull(support);
        this.baselineAssembler = new InstitutionalOperationalDeskBaselineAssembler(support);
        this.roleActsAssembler = new InstitutionalOperationalDeskRoleActsAssembler(support);
        this.unitAugmenter = new InstitutionalOperationalDeskUnitAugmenter(support);
        this.judicialAxisAugmenter = new InstitutionalOperationalDeskJudicialAxisAugmenter(support);
    }

    InstitutionalOperationalDeskGovernance missingProfile() {
        return InstitutionalOperationalDeskGovernanceDraft.missingProfile();
    }

    InstitutionalOperationalDeskGovernance assemble(InstitutionalOperationalDeskSnapshot snapshot,
                                                    InstitutionalProcessWorkspace workspace) {
        InstitutionalOperationalDeskGovernanceDraft draft = InstitutionalOperationalDeskGovernanceDraft.from(snapshot);
        baselineAssembler.apply(draft, snapshot, workspace);
        roleActsAssembler.apply(draft, snapshot);
        unitAugmenter.apply(draft, snapshot);
        judicialAxisAugmenter.apply(draft, snapshot);
        finalizeDraft(draft, snapshot);
        return draft.build(snapshot);
    }

    private void finalizeDraft(InstitutionalOperationalDeskGovernanceDraft draft,
                               InstitutionalOperationalDeskSnapshot snapshot) {
        draft.forbiddenActs().add(InstitutionalOperationalDeskGovernanceMessages.CROSS_VARA_BLOCK);
        if (!snapshot.unitScopeBound()) {
            draft.findings().add(InstitutionalOperationalDeskGovernanceMessages.MISSING_UNIT_SCOPE);
        }
        if (!snapshot.segregatedByVaraOrSpecialization()) {
            draft.forbiddenActs().add("operacao_sem_bloco_de_vara_ou_especializacao_resolvido");
        }
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.organizationScope(snapshot.scope()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.territorialScope(snapshot.territorialScopeKey()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.groupingKey(snapshot.unitGroupingKey()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.isolationMode(snapshot.fingerprint().isolationMode()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.topology(draft.unitTopology().size()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.operationalDomains(draft.operationalDomains().size()));
        if (snapshot.secretariatWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.SECRETARIAT_FOUNDATION);
        }
        if (snapshot.assessorWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.ASSESSOR_FOUNDATION);
        }
        if (snapshot.triageWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.TRIAGE_FOUNDATION);
        }
        if (snapshot.mandateWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.MANDATE_FOUNDATION);
        }
        if (snapshot.communicationWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.COMMUNICATION_FOUNDATION);
        }
        if (snapshot.batchWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.BATCH_FOUNDATION);
        }
        if (snapshot.distributionWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.DISTRIBUTION_FOUNDATION);
        }
        if (snapshot.expeditionWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.EXPEDITION_FOUNDATION);
        }
        if (snapshot.conclusionWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.CONCLUSION_FOUNDATION);
        }
        if (snapshot.queueManagementWorkflowEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.QUEUE_MANAGEMENT_FOUNDATION);
        }
        if (snapshot.magistrateOverrideEnabled()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.JUDGE_OVERRIDE_FOUNDATION);
        }
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.judicialAxis(snapshot.judicialAxis()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.unitKind(snapshot.unitKind()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.assignmentBoundaryKey(snapshot.assignmentBoundaryKey()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.queues(draft.deskQueues().size()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.boundaries(draft.assignmentBoundaries().size()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.counterparts(draft.counterpartScopes().size()));
        draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.specializedFlows(draft.specializedFlows().size()));
        if (!draft.specializedFlows().isEmpty()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.SPECIALIZED_FLOW_FOUNDATION);
        }
        if (snapshot.sectionVisible() && draft.deskQueues().isEmpty()) {
            draft.findings().add("mesa_operacional_sem_filas_derivadas_por_unidade");
        }
        if (snapshot.sectionVisible() && draft.assignmentBoundaries().isEmpty()) {
            draft.findings().add("mesa_operacional_sem_fronteiras_de_atribuicao");
        }
        if (snapshot.sectionVisible() && draft.findings().isEmpty()) {
            draft.fundamentos().add(InstitutionalOperationalDeskGovernanceMessages.GOVERNANCE_ACTIVE);
        }
        if (!snapshot.sectionVisible()) {
            draft.findings().add(InstitutionalOperationalDeskGovernanceMessages.MISSING_PROFILE);
        }
    }
}
