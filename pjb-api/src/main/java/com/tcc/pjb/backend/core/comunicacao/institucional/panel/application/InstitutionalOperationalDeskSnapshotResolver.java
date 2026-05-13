package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.util.Objects;
import java.util.Set;

final class InstitutionalOperationalDeskSnapshotResolver {

    private final InstitutionalOperationalDeskSupport support;

    InstitutionalOperationalDeskSnapshotResolver(InstitutionalOperationalDeskSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    InstitutionalOperationalDeskSnapshot resolve(InstitutionalOperationalProfileProjection profile,
                                                 InstitutionalAccessProfileCatalogEntry catalogEntry,
                                                 InstitutionalProcessWorkspace workspace) {
        Set<CapacidadeCaixaInstitucional> capacities = support.resolveCapacities(profile, catalogEntry);
        InstitutionalProcessProfile processProfile = support.resolveProcessProfile(profile, catalogEntry);
        InstitutionalNominationRole nominationRole = support.resolveNominationRole(profile, catalogEntry);

        String tribunal = support.firstNonBlank(profile.responsibleTribunalCode(), profile.orgaoSigla(), "SEM_TRIBUNAL");
        String comarca = support.firstNonBlank(profile.responsibleComarca(), profile.unidadeNome(), profile.responsibleUnitName(), "SEM_COMARCA");
        String unidadeCodigo = support.firstNonBlank(profile.unidadeCodigo(), profile.responsibleUnitCode(), "SEM_UNIDADE");
        String unidadeNome = support.firstNonBlank(profile.unidadeNome(), profile.responsibleUnitName(), unidadeCodigo, "SEM_UNIDADE");
        String caixa = support.firstNonBlank(profile.caixaCodigo(), "SEM_CAIXA");
        String scope = support.firstNonBlank(profile.organizationScope(), catalogEntry == null ? null : catalogEntry.codigo(), "SEM_ESCOPO");
        String branchAxis = support.firstNonBlank(workspace == null ? null : workspace.ramoDireito(), workspace == null ? null : workspace.ritoProcessual(), scope, processProfile == null ? null : processProfile.name(), "SEM_RAMO");

        InstitutionalOperationalDeskUnitFingerprint fingerprint = support.resolveUnitFingerprint(tribunal, comarca, unidadeCodigo, unidadeNome, caixa, branchAxis);

        boolean legalInstitution = support.isLegalInstitutionProfile(processProfile);
        boolean secretariat = processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM
                || processProfile == InstitutionalProcessProfile.DIRETOR_FORUM
                || profile.funcaoOperacional() != null && support.containsToken(profile.funcaoOperacional(), "SECRETARIA", "CARTORIO");
        boolean assessor = processProfile == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.TECNICO_INSTITUCIONAL;
        boolean triage = processProfile == InstitutionalProcessProfile.SERVIDOR_TRIAGEM
                || support.containsWorkspaceSignals(workspace, "TRIAGEM", "CLASSIFICACAO", "ENTRADA_NOVA");
        boolean management = processProfile == InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.COORDENADOR_UNIDADE
                || nominationRole != null && nominationRole.isGestaoMestre();
        boolean magistrateProfile = processProfile == InstitutionalProcessProfile.MAGISTRADO_COOPERANTE
                || profile.nominationRole() != null && support.containsToken(profile.nominationRole(), "MAGISTRADO", "JUIZ", "DESEMBARGADOR", "MINISTRO");
        boolean prisonFlow = processProfile == InstitutionalProcessProfile.POLICIAL_PENAL
                || processProfile == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                || processProfile == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL;

        boolean sectionVisible = secretariat
                || assessor
                || triage
                || management
                || legalInstitution
                || prisonFlow
                || !capacities.isEmpty()
                || support.containsWorkspaceSignals(workspace, "MANDADO", "CERTIDAO", "MINUTA", "PARECER", "CALCULADORA", "AUDIENCIA");

        boolean unitScopeBound = support.hasText(tribunal) && support.hasText(comarca) && support.hasText(unidadeCodigo);
        boolean segregatedByTribunal = support.hasText(tribunal);
        boolean segregatedByComarca = support.hasText(comarca);
        boolean segregatedByUnit = support.hasText(unidadeCodigo) && support.hasText(caixa);
        boolean segregatedByVaraOrSpecialization = !"UNIDADE_GERAL".equals(fingerprint.varaCluster()) || !"GERAL".equals(fingerprint.specializationCluster());
        boolean magistrateOverrideEnabled = sectionVisible && (management || magistrateProfile || secretariat);
        boolean secretariatWorkflowEnabled = sectionVisible && (secretariat || management || capacities.contains(CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS));
        boolean assessorWorkflowEnabled = sectionVisible && (assessor || capacities.contains(CapacidadeCaixaInstitucional.PREPARAR_MINUTA) || capacities.contains(CapacidadeCaixaInstitucional.EMITIR_PARECER));
        boolean triageWorkflowEnabled = sectionVisible && (triage || support.containsWorkspaceSignals(workspace, "TRIAGEM", "CLASSIFICACAO"));
        boolean mandateWorkflowEnabled = sectionVisible && (secretariat || management || support.containsWorkspaceSignals(workspace, "MANDADO", "CERTIDAO", "CUMPRIMENTO") || support.containsToken(scope, "MANDADOS"));
        boolean communicationWorkflowEnabled = sectionVisible && (secretariat || management || capacities.contains(CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO) || capacities.contains(CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA));
        boolean opinionWorkflowEnabled = sectionVisible && (legalInstitution || assessor || capacities.contains(CapacidadeCaixaInstitucional.EMITIR_PARECER) || support.containsWorkspaceSignals(workspace, "PARECER", "MINUTA", "MANIFESTACAO"));
        boolean calculatorWorkflowEnabled = sectionVisible && (capacities.contains(CapacidadeCaixaInstitucional.ACESSAR_CALCULADORA_JUDICIAL) || support.containsWorkspaceSignals(workspace, "CALCULADORA", "CALCULO", "LIQUIDACAO"));
        boolean batchWorkflowEnabled = sectionVisible && (management || support.containsWorkspaceSignals(workspace, "LOTE", "MALOTE", "BLOCO"));
        boolean distributionWorkflowEnabled = sectionVisible && (triage || secretariat || management
                || support.containsWorkspaceSignals(workspace, "DISTRIBUICAO", "PROTOCOLO", "AUTUACAO", "MALOTE"));
        boolean expeditionWorkflowEnabled = sectionVisible && (mandateWorkflowEnabled || communicationWorkflowEnabled || secretariat || management
                || support.containsWorkspaceSignals(workspace, "EXPEDIENTE", "OFICIO", "EDITAL", "CARTA", "ALVARA", "MANDADO"));
        boolean conclusionWorkflowEnabled = sectionVisible && (assessor || secretariat || management || magistrateProfile
                || support.containsWorkspaceSignals(workspace, "CONCLUSAO", "GABINETE", "MINUTA", "VOTO", "SENTENCA", "DECISAO"));
        boolean queueManagementWorkflowEnabled = sectionVisible && (management || triage || secretariat
                || capacities.stream().anyMatch(CapacidadeCaixaInstitucional::isMutacaoFila));

        String judicialAxis = support.resolveJudicialAxis(unidadeNome, scope, branchAxis, tribunal);
        String unitKind = support.resolveUnitKind(unidadeNome, scope, processProfile, fingerprint);
        String organizationalScopeKey = support.normalize(tribunal) + '|' + support.normalize(scope) + '|' + support.normalize(processProfile == null ? null : processProfile.name());
        String territorialScopeKey = support.normalize(tribunal) + '|' + support.normalize(comarca) + '|' + support.normalize(branchAxis);
        String unitGroupingKey = fingerprint.groupingKey();
        String assignmentBoundaryKey = unitGroupingKey + '|' + support.normalize(unitKind) + '|' + support.normalize(judicialAxis);

        return new InstitutionalOperationalDeskSnapshot(
                processProfile,
                tribunal,
                comarca,
                unidadeCodigo,
                caixa,
                scope,
                branchAxis,
                fingerprint,
                legalInstitution,
                management,
                magistrateProfile,
                prisonFlow,
                sectionVisible,
                unitScopeBound,
                segregatedByTribunal,
                segregatedByComarca,
                segregatedByUnit,
                segregatedByVaraOrSpecialization,
                magistrateOverrideEnabled,
                secretariatWorkflowEnabled,
                assessorWorkflowEnabled,
                triageWorkflowEnabled,
                mandateWorkflowEnabled,
                communicationWorkflowEnabled,
                opinionWorkflowEnabled,
                calculatorWorkflowEnabled,
                batchWorkflowEnabled,
                distributionWorkflowEnabled,
                expeditionWorkflowEnabled,
                conclusionWorkflowEnabled,
                queueManagementWorkflowEnabled,
                judicialAxis,
                unitKind,
                organizationalScopeKey,
                territorialScopeKey,
                unitGroupingKey,
                assignmentBoundaryKey);
    }
}
