package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class InstitutionalOperationalDeskBaselineAssembler {

    private final InstitutionalOperationalDeskSupport support;
    private final InstitutionalOperationalDeskCounterpartScopeResolver counterpartScopeResolver;

    InstitutionalOperationalDeskBaselineAssembler(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
        this.counterpartScopeResolver = new InstitutionalOperationalDeskCounterpartScopeResolver(support);
    }

    void apply(InstitutionalOperationalDeskGovernanceDraft draft,
               InstitutionalOperationalDeskSnapshot snapshot,
               InstitutionalProcessWorkspace workspace) {
        addOperationalDomains(draft.operationalDomains(), snapshot, workspace);
        draft.deskQueues().addAll(resolveDeskQueues(snapshot));
        draft.assignmentBoundaries().addAll(resolveAssignmentBoundaries(snapshot));
        draft.counterpartScopes().addAll(counterpartScopeResolver.resolve(snapshot));
    }

    private void addOperationalDomains(LinkedHashSet<String> domains,
                                       InstitutionalOperationalDeskSnapshot snapshot,
                                       InstitutionalProcessWorkspace workspace) {
        if (snapshot.secretariatWorkflowEnabled()) {
            domains.add("EXPEDIENTE_SECRETARIA");
            domains.add("ORGANIZACAO_PROCESSUAL");
            domains.add("PUBLICACOES_INTIMACOES_E_PRAZOS");
            domains.add("AUTUACAO_REAUTUACAO_E_SANEAMENTO");
            domains.add("BAIXA_ARQUIVAMENTO_E_RETORNO");
        }
        if (snapshot.assessorWorkflowEnabled()) {
            domains.add("MINUTAS_E_DOSSIES");
        }
        if (snapshot.triageWorkflowEnabled()) {
            domains.add("TRIAGEM_E_ENCAMINHAMENTO");
        }
        if (snapshot.mandateWorkflowEnabled()) {
            domains.add("MANDADOS_E_CERTIDOES");
        }
        if (snapshot.communicationWorkflowEnabled()) {
            domains.add("COMUNICACOES_PROCESSUAIS");
        }
        if (snapshot.opinionWorkflowEnabled()) {
            domains.add("PARECERES_E_MANIFESTACOES");
        }
        if (snapshot.calculatorWorkflowEnabled()) {
            domains.add("CALCULOS_E_LIQUIDACOES");
        }
        if (snapshot.batchWorkflowEnabled()) {
            domains.add("OPERACOES_EM_LOTE");
        }
        if (snapshot.distributionWorkflowEnabled()) {
            domains.add("PROTOCOLO_DISTRIBUICAO_AUTUACAO");
        }
        if (snapshot.expeditionWorkflowEnabled()) {
            domains.add("EXPEDIENTES_E_CUMPRIMENTOS");
        }
        if (snapshot.conclusionWorkflowEnabled()) {
            domains.add("CONCLUSOES_E_GABINETE");
        }
        if (snapshot.queueManagementWorkflowEnabled()) {
            domains.add("GESTAO_DE_FILAS_E_COBERTURAS");
        }
        if (snapshot.prisonFlow() || support.containsToken(snapshot.branchAxis(), "CUSTODIA", "PENAL")) {
            domains.add("FLUXO_CUSTODIAL_OU_PENAL");
        }
        if (support.containsToken(snapshot.scope(), "CEJUSC", "CONCILIACAO")
                || support.containsWorkspaceSignals(workspace, "AUDIENCIA", "CONCILIACAO")) {
            domains.add("PAUTA_E_AUDIENCIAS");
            domains.add("SALAS_TEMPOS_E_PRE_AUDIENCIA");
        }
        if (support.containsWorkspaceSignals(workspace, "PROTOCOLO", "MALOTE")) {
            domains.add("PROTOCOLO_E_MALOTE");
        }
    }

    private List<String> resolveDeskQueues(InstitutionalOperationalDeskSnapshot snapshot) {
        InstitutionalOperationalDeskUnitFingerprint fingerprint = snapshot.fingerprint();
        String unitKind = snapshot.unitKind();
        String judicialAxis = snapshot.judicialAxis();
        InstitutionalProcessProfile processProfile = snapshot.processProfile();
        LinkedHashSet<String> queues = new LinkedHashSet<>();
        queues.add("fila_entrada_unidade=" + support.normalize(fingerprint.groupingKey()));
        if (snapshot.secretariatWorkflowEnabled()) {
            queues.add("fila_secretaria=" + support.normalize(unitKind));
            queues.add("fila_publicacoes_prazos=" + support.normalize(fingerprint.specializationCluster()));
            queues.add("fila_subfluxos_documentos=" + support.normalize(fingerprint.groupingKey()));
            queues.add("fila_baixa_arquivamento=" + support.normalize(fingerprint.varaCluster()));
        }
        if (snapshot.assessorWorkflowEnabled() || snapshot.conclusionWorkflowEnabled()) {
            queues.add("fila_gabinete_minutas=" + support.normalize(judicialAxis));
        }
        if (snapshot.triageWorkflowEnabled() || snapshot.distributionWorkflowEnabled()) {
            queues.add("fila_triagem_distribuicao=" + support.normalize(fingerprint.specializationCluster()));
        }
        if (snapshot.mandateWorkflowEnabled() || snapshot.expeditionWorkflowEnabled()) {
            queues.add("fila_expedientes_mandados=" + support.normalize(fingerprint.varaCluster()));
            if (support.containsToken(unitKind, "CENTRAL_MANDADOS")) {
                queues.add("fila_grupos_diligencia=" + support.normalize(fingerprint.groupingKey()));
            }
        }
        if (snapshot.communicationWorkflowEnabled()) {
            queues.add("fila_comunicacoes_ciencias=" + support.normalize(fingerprint.specializationCluster()));
        }
        if (snapshot.opinionWorkflowEnabled() || snapshot.legalInstitution()) {
            queues.add("fila_manifestacoes_pareceres=" + support.normalize(unitKind));
        }
        if (snapshot.calculatorWorkflowEnabled()) {
            queues.add("fila_calculos_liquidacao=" + support.normalize(judicialAxis));
            queues.add("fila_requisicoes_tecnicas_calculo=" + support.normalize(fingerprint.groupingKey()));
        }
        if (snapshot.queueManagementWorkflowEnabled()) {
            queues.add("fila_cobertura_substituicao=" + support.normalize(fingerprint.groupingKey()));
        }
        if (snapshot.batchWorkflowEnabled()) {
            queues.add("fila_operacao_lote=" + support.normalize(fingerprint.groupingKey()));
        }
        if (snapshot.prisonFlow()) {
            queues.add("fila_fluxo_prisional=" + support.normalize(processProfile == null ? null : processProfile.name()));
        }
        if (support.containsToken(unitKind, "CEJUSC", "CENTRAL_AUDIENCIAS") || support.containsToken(judicialAxis, "JUIZADOS_ESPECIAIS")) {
            queues.add("fila_pre_audiencia_e_confirmacoes=" + support.normalize(fingerprint.groupingKey()));
        }
        return List.copyOf(queues);
    }

    private List<String> resolveAssignmentBoundaries(InstitutionalOperationalDeskSnapshot snapshot) {
        InstitutionalOperationalDeskUnitFingerprint fingerprint = snapshot.fingerprint();
        InstitutionalProcessProfile processProfile = snapshot.processProfile();
        LinkedHashSet<String> boundaries = new LinkedHashSet<>();
        boundaries.add("tribunal=" + support.normalize(snapshot.tribunal()));
        boundaries.add("comarca=" + support.normalize(snapshot.comarca()));
        boundaries.add("unidade=" + support.normalize(snapshot.unidadeCodigo()));
        boundaries.add("caixa=" + support.normalize(snapshot.caixa()));
        boundaries.add("scope=" + support.normalize(snapshot.scope()));
        boundaries.add("vara_cluster=" + support.normalize(fingerprint.varaCluster()));
        boundaries.add("especializacao=" + support.normalize(fingerprint.specializationCluster()));
        boundaries.add("tipo_unidade=" + support.normalize(snapshot.unitKind()));
        boundaries.add("eixo_judicial=" + support.normalize(snapshot.judicialAxis()));
        boundaries.add("perfil_processo=" + support.normalize(processProfile == null ? null : processProfile.name()));
        if ("UPJ".equals(fingerprint.varaCluster())) {
            boundaries.add("upj_preserva_independencia_das_varas_origem");
        }
        if ("CEJUSC".equals(fingerprint.varaCluster())) {
            boundaries.add("cejusc_nao_substitui_secretaria_nem_gabinete_do_feito_principal");
        }
        if (fingerprint.varaCluster().startsWith("VARA_")) {
            boundaries.add("vedada_mistura_entre_" + support.normalize(fingerprint.varaCluster()) + "_e_demais_varas_sem_redistribuicao_formal");
        }
        return List.copyOf(boundaries);
    }

}
