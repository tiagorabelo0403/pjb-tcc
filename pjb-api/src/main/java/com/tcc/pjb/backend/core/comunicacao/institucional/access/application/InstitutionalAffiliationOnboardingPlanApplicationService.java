package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAffiliationOnboardingPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationPolicyClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalOnboardingStep;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAffiliationOnboardingPlanApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService;
    private final InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService;

    public InstitutionalAffiliationOnboardingPlanApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                   InstitutionalNominationStateRepository nominationRepository,
                                                                   InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService,
                                                                   InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.operatingModelClosureApplicationService = Objects.requireNonNull(operatingModelClosureApplicationService);
        this.authenticationPolicyClosureApplicationService = Objects.requireNonNull(authenticationPolicyClosureApplicationService);
    }

    public InstitutionalAffiliationOnboardingPlan consolidar(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(Objects.requireNonNull(affiliationId))
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationId(affiliation.affiliationId());
        InstitutionalOperatingModelClosure operatingModel = operatingModelClosureApplicationService.consolidar(
                affiliation, nominations, affiliation.destinatarioKind(), affiliation.comarca(), affiliation.uf());
        InstitutionalAuthenticationPolicyClosure authPolicy = authenticationPolicyClosureApplicationService.consolidar(affiliationId);
        List<InstitutionalOnboardingStep> steps = buildSteps(affiliation, operatingModel, authPolicy, nominations);
        LinkedHashSet<String> findings = new LinkedHashSet<>(operatingModel.findings());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(operatingModel.fundamentos());
        fundamentos.addAll(authPolicy.fundamentos());
        fundamentos.add("afiliacao_institucional_ativa_com_provisionamento_progressivo_por_unidade_caixa_e_lotacao");
        fundamentos.add("usuarios_nao_assinantes_podem_ser_criados_pela_instituicao_sem_quebrar_o_rastro_da_identidade_pessoal_raiz");
        fundamentos.add("perfis_assinantes_e_peticionantes_dependem_de_govbr_e_certificado_qualificado_em_dose_dupla");
        if (operatingModel.coverageRoute() != null && !operatingModel.coverageRoute().localUnitPresent()) {
            findings.add("cobertura_local_sera_roteada_para_sede_competente_ate_homologacao_de_unidade_local_completa");
        }
        if (nominations.stream().noneMatch(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())) {
            findings.add("afiliacao_sem_nomeacao_ativa_de_gestao_mestra_para_auto-administracao_plena");
        }
        return new InstitutionalAffiliationOnboardingPlan(
                affiliation.affiliationId(),
                affiliation.orgaoSigla(),
                affiliation.orgaoNome(),
                affiliation.unidadeCodigo(),
                affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation.blueprintCode(),
                operatingModel.coverageMode(),
                operatingModel.coverageRoute() == null ? null : operatingModel.coverageRoute().responsibleUnitCode(),
                operatingModel.coverageRoute() == null ? null : operatingModel.coverageRoute().responsibleUnitName(),
                true,
                true,
                true,
                affiliation.requerDuplaAprovacaoAdministrador(),
                steps,
                authPolicy.lanePolicies(),
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private List<InstitutionalOnboardingStep> buildSteps(InstitutionalAffiliation affiliation,
                                                         InstitutionalOperatingModelClosure operatingModel,
                                                         InstitutionalAuthenticationPolicyClosure authPolicy,
                                                         List<InstitutionalNomination> nominations) {
        ArrayList<InstitutionalOnboardingStep> out = new ArrayList<>();
        out.add(new InstitutionalOnboardingStep(
                "IDENTIDADE_RAIZ_GOVBR",
                "Vincular identidade pessoal raiz",
                "PJB e administrador institucional",
                true,
                List.of("CPF validado", "conta gov.br", "nível prata ou ouro para contexto institucional"),
                List.of(
                        "todo_usuario_institucional_entra_com_identidade_pessoal_rastreavel",
                        "a_conta_local_da_instituicao_nao_substitui_a_identidade_govbr")));
        out.add(new InstitutionalOnboardingStep(
                "VALIDACAO_INSTITUCIONAL",
                "Validar órgão, unidade e cobertura territorial",
                "PJB e órgão aderente",
                true,
                List.of("ato de criação ou competência", "CNPJ quando aplicável", "domínio institucional", "evidência da unidade responsável"),
                List.of(
                        "cobertura_municipal_sem_unidade_propria_pode_apontar_para_sede_competente",
                        "coverage_mode=" + operatingModel.coverageMode())));
        out.add(new InstitutionalOnboardingStep(
                "NOMEACOES_E_GESTAO_MESTRA",
                "Nomear administradores e lotações raiz",
                "Órgão aderente",
                true,
                List.of("administrador institucional", "gestor de unidade", "caixas iniciais", "regras de aprovação"),
                List.of(
                        affiliation.requerDuplaAprovacaoAdministrador()
                                ? "dupla_aprovacao_administrativa_ativa"
                                : "aprovacao_administrativa_simples_com_auditoria",
                        "nomeacoes_ativas_existentes=" + nominations.size())));
        out.add(new InstitutionalOnboardingStep(
                "PROVISIONAMENTO_OPERACIONAL",
                "Provisionar unidades, caixas e painéis",
                "PJB",
                true,
                List.of("blueprint institucional", "lanes operacionais", "panelCode", "partição horizontal"),
                List.of(
                        "provisionamento_por_unidade_caixa_lotacao_e_painel",
                        "lane_policies=" + authPolicy.lanePolicies().size())));
        out.add(new InstitutionalOnboardingStep(
                "CREDENCIAIS_INTERNAS",
                "Emitir logins internos para rotinas não assinantes",
                "Órgão aderente",
                false,
                List.of("matrícula interna", "perfil de lotação", "dispositivo homologado quando exigido"),
                List.of(
                        "login_gerenciado_institucional_permitido_para_faixas_operacionais_nao_assinantes",
                        "o_login_local_continua_vinculado_ao_rastro_da_identidade_pessoal")));
        out.add(new InstitutionalOnboardingStep(
                "ASSINATURA_FORTE_E_ATOS_SENSIVEIS",
                "Habilitar certificado e política de atos sensíveis",
                "Órgão aderente e PJB",
                true,
                List.of("certificado ICP-Brasil do assinante", "política de rede ou autorização remota", "MFA/step-up"),
                List.of(
                        "assinar_ou_peticionar_em_nome_do_orgao_exige_govbr_e_certificado",
                        "autorizacao_remota_temporal_so_serve_quando_o_blueprint_e_a_afiliacao_permitirem")));
        out.add(new InstitutionalOnboardingStep(
                "AUDITORIA_E_RECERTIFICACAO",
                "Ativar recertificação periódica e trilha de auditoria",
                "PJB",
                false,
                List.of("recertificação", "trilha de aprovação", "revisão de lotações", "revisão de perfis"),
                List.of(
                        "rastro_permanente_para_identidade_nomeacao_afiliacao_e_ato_sensivel",
                        "recertificacao_periodica_e_obrigatoria_para_auto-gestao_institucional")));
        return List.copyOf(out);
    }
}
