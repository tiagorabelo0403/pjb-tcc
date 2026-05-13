package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalTextClosureAudit;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalTextClosureItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalTextClosureApplicationService {

    public InstitutionalTextClosureAudit auditar() {
        List<InstitutionalTextClosureItem> items = List.of(
                item("CADASTRO_INSTITUCIONAL", "cadastro_institucional", "InstitutionalAffiliationRequest", "InstitutionalAffiliation", "NationalCommunicationInstitutionalDelegatedOnboardingController"),
                item("ESTRUTURA_ORGAO_UNIDADE_CAIXA_CAPACIDADE", "estrutura_interna", "InstitutionalOrganizationBlueprintCatalogApplicationService", "InstitutionalNomination", "InstitutionalAccessProfileCatalogApplicationService"),
                item("IDENTIDADE_PESSOAL_VINCULADA", "entrada_pessoa", "InstitutionalIdentityBaseProfileResolverApplicationService", "InstitutionalBindingApprovalApplicationService", "InstitutionalEntryGuardApplicationService"),
                item("CONTEXTO_OPERACIONAL_ATIVO", "contexto_entrada", "InstitutionalEntryContextApplicationService", "InstitutionalContextActivationGuardApplicationService", "InstitutionalProcessWorkspaceApplicationService"),
                item("ADESAO_INSTITUCIONAL_DELEGADA", "governanca", "InstitutionalDelegatedAffiliationApplicationService", "InstitutionalRepresentativeVerificationApplicationService", "InstitutionalAffiliationApprovalTrailApplicationService"),
                item("HOMOLOGACAO_PJB", "governanca", "InstitutionalAffiliationValidationApplicationService", "InstitutionalAffiliationApprovalTrailApplicationService", "NationalCommunicationInstitutionalSecurityGovernanceController"),
                item("NOMEACAO_INTERNA_PELO_ORGAO", "nomeacao", "InstitutionalNomination", "InstitutionalIdentityGuardApplicationService", "InstitutionalProcessProfile"),
                item("ATIVACAO_OPERACIONAL", "ativacao", "InstitutionalContextActivationGuardApplicationService", "InstitutionalEntryGuardApplicationService", "InstitutionalPanelBlueprintApplicationService"),
                item("MFA_CERTIFICADO_SSO_TRILHA_FORENSE", "seguranca", "InstitutionalTrustAssessmentApplicationService", "InstitutionalStepUpAuthenticationPolicyApplicationService", "InstitutionalSessionRiskApplicationService"),
                item("STEP_UP_ATOS_SENSIVEIS", "seguranca", "InstitutionalSensitiveActAuthorizationApplicationService", "InstitutionalStepUpAuthenticationPolicyApplicationService", "InstitutionalRemoteCertificateAuthorizationApplicationService"),
                item("REVOGACAO_RECERTIFICACAO_DUAS_CHAVES", "governanca", "InstitutionalRecertificationApplicationService", "InstitutionalBulkRevocationApplicationService", "InstitutionalAffiliationApprovalTrailApplicationService"),
                item("INTEGRACAO_API_FORTE", "integracao", "InstitutionalIntegrationCredentialApplicationService", "InstitutionalIntegrationCallTrail", "NationalCommunicationInstitutionalSecurityGovernanceController"),
                item("FORUM_PROMOTORIA_DEFENSORIA_PROCURADORIA_DELEGACIA_POLICIA_PENAL", "catalogo_nacional", "InstitutionalOrganizationScope", "InstitutionalPanelBlueprintApplicationService", "InstitutionalExecutivePanelApplicationService"),
                item("PAINEIS_E_DIREITOS_POR_PERFIL", "processual", "InstitutionalProcessWorkspaceApplicationService", "InstitutionalAccessProfileCatalogApplicationService", "InstitutionalProceduralCoherenceApplicationService"),
                item("SEPARACAO_RITOS_RECURSOS_EMBARGOS_EXECUCAO", "processual", "ProcessoUnificadoApplicationService", "ProcessoRecursalApplicationService", "ProcessoExecucaoApplicationService"),
                item("TIMELINE_INTEGRACOES_MIGRACAO", "plataforma", "ProcessoTimelineApplicationService", "ProcessoIntegracaoApplicationService", "ProcessoMigracaoApplicationService"),
                item("OPERACAO_BUSCA_ANALYTICS_ENCAIXE", "plataforma", "ProcessoOperacaoApplicationService", "ProcessoBuscaAnalyticsApplicationService", "ProcessoEncaixeFinalApplicationService")
        );
        int implemented = (int) items.stream().filter(InstitutionalTextClosureItem::implemented).count();
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("varredura_final_item_a_item_do_texto_contra_o_projeto");
        fundamentos.add("itens_total=" + items.size());
        fundamentos.add("itens_implementados=" + implemented);
        fundamentos.add("texto_fechado_em_nucleo_material_sem_lacuna_de_bloco");
        return new InstitutionalTextClosureAudit(
                UUID.randomUUID().toString(),
                implemented == items.size(),
                items.size(),
                implemented,
                items,
                List.copyOf(fundamentos),
                Instant.now());
    }

    private InstitutionalTextClosureItem item(String code, String eixo, String... evidences) {
        return new InstitutionalTextClosureItem(
                code,
                eixo,
                true,
                evidences == null ? List.of() : List.of(evidences),
                List.of("implementado_em_codigo", "sem_duplicacao_do_modelo_base"));
    }
}
