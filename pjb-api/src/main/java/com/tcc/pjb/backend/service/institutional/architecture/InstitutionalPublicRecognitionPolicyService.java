package com.tcc.pjb.backend.service.institutional.architecture;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class InstitutionalPublicRecognitionPolicyService {

    private static final String POLICY_VERSION = "PJB-INSTITUCIONAL-RECONHECIMENTO-2026.04-R1";

    private final Clock clock;

    @Inject
    @Autowired
    public InstitutionalPublicRecognitionPolicyService() {
        this(Clock.systemUTC());
    }

    InstitutionalPublicRecognitionPolicyService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public AdminInstitutionalPublicRecognitionResponse assess(String scope,
                                                              boolean officialCatalogMatch,
                                                              boolean publicCnpjActive,
                                                              boolean publicNatureCompatible,
                                                              boolean officialEmailChannel,
                                                              boolean officialDomain,
                                                              boolean legalActPresent,
                                                              boolean territorialMatch,
                                                              boolean representativeGovBrGold,
                                                              boolean representativeIcpBrasilValid,
                                                              boolean subordinateUnitWithoutOwnCnpj,
                                                              boolean parentInstitutionRecognized) {
        Scope normalizedScope = Scope.from(scope);
        boolean channelAnchor = officialEmailChannel && officialDomain;
        boolean digitalAnchor = representativeGovBrGold && representativeIcpBrasilValid;
        boolean subordinateAnchor = subordinateUnitWithoutOwnCnpj && parentInstitutionRecognized && legalActPresent;
        boolean publicRegistryAnchor = officialCatalogMatch || (publicCnpjActive && publicNatureCompatible) || subordinateAnchor;
        boolean automaticEligible = officialCatalogMatch && channelAnchor && digitalAnchor && territorialMatch;
        boolean assistedEligible = !automaticEligible && publicRegistryAnchor && legalActPresent && channelAnchor && digitalAnchor && territorialMatch;
        Status status = resolveStatus(automaticEligible,
                assistedEligible,
                officialCatalogMatch,
                publicCnpjActive,
                publicNatureCompatible,
                subordinateUnitWithoutOwnCnpj,
                parentInstitutionRecognized,
                legalActPresent,
                channelAnchor,
                digitalAnchor,
                territorialMatch);

        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        buildReasonsAndBlockers(normalizedScope,
                officialCatalogMatch,
                publicCnpjActive,
                publicNatureCompatible,
                officialEmailChannel,
                officialDomain,
                legalActPresent,
                territorialMatch,
                representativeGovBrGold,
                representativeIcpBrasilValid,
                subordinateUnitWithoutOwnCnpj,
                parentInstitutionRecognized,
                automaticEligible,
                assistedEligible,
                reasons,
                blockers);

        return new AdminInstitutionalPublicRecognitionResponse(
                POLICY_VERSION,
                clock.instant(),
                normalizedScope.code,
                normalizedScope.label,
                status.code,
                status.label,
                status.recognized,
                status.autoActivatable,
                status.humanReviewRequired,
                normalizedScope.acceptedOfficialSources(),
                buildEvidenceRules(officialCatalogMatch,
                        publicCnpjActive,
                        publicNatureCompatible,
                        officialEmailChannel,
                        officialDomain,
                        legalActPresent,
                        territorialMatch,
                        representativeGovBrGold,
                        representativeIcpBrasilValid,
                        subordinateUnitWithoutOwnCnpj,
                        parentInstitutionRecognized),
                List.of(
                        "o_pjb_nao_reconhece_instituicao_publica_somente_por_formulario_autodeclarado",
                        "a_ativacao_so_elegivel_nasce_de_ancora_oficial_institucional_mais_identidade_pessoal_raiz_do_representante",
                        "subunidade_sem_cnpj_proprio_pode_ser_reconhecida_por_instituicao_pai_mais_ato_legal_mais_topologia_territorial"
                ),
                List.copyOf(reasons),
                List.copyOf(blockers),
                nextSafeSteps(status, normalizedScope)
        );
    }

    private static Status resolveStatus(boolean automaticEligible,
                                        boolean assistedEligible,
                                        boolean officialCatalogMatch,
                                        boolean publicCnpjActive,
                                        boolean publicNatureCompatible,
                                        boolean subordinateUnitWithoutOwnCnpj,
                                        boolean parentInstitutionRecognized,
                                        boolean legalActPresent,
                                        boolean channelAnchor,
                                        boolean digitalAnchor,
                                        boolean territorialMatch) {
        if (automaticEligible) {
            return Status.RECONHECIDA_AUTOMATICAMENTE;
        }
        if (assistedEligible) {
            return Status.RECONHECIDA_COM_HOMOLOGACAO;
        }
        boolean contradictoryPublicEvidence = publicCnpjActive && !publicNatureCompatible && !officialCatalogMatch;
        boolean subordinateWithoutParent = subordinateUnitWithoutOwnCnpj && !parentInstitutionRecognized;
        boolean noRecoverableRoot = !officialCatalogMatch
                && !publicCnpjActive
                && !(subordinateUnitWithoutOwnCnpj && parentInstitutionRecognized)
                && !legalActPresent;
        if (contradictoryPublicEvidence || subordinateWithoutParent || noRecoverableRoot) {
            return Status.NEGADA;
        }
        if (!channelAnchor || !digitalAnchor || !territorialMatch || !legalActPresent) {
            return Status.PENDENTE_EVIDENCIAS;
        }
        return Status.PENDENTE_EVIDENCIAS;
    }

    private static void buildReasonsAndBlockers(Scope scope,
                                                boolean officialCatalogMatch,
                                                boolean publicCnpjActive,
                                                boolean publicNatureCompatible,
                                                boolean officialEmailChannel,
                                                boolean officialDomain,
                                                boolean legalActPresent,
                                                boolean territorialMatch,
                                                boolean representativeGovBrGold,
                                                boolean representativeIcpBrasilValid,
                                                boolean subordinateUnitWithoutOwnCnpj,
                                                boolean parentInstitutionRecognized,
                                                boolean automaticEligible,
                                                boolean assistedEligible,
                                                LinkedHashSet<String> reasons,
                                                LinkedHashSet<String> blockers) {
        reasons.add("scope=" + scope.code);
        if (officialCatalogMatch) {
            reasons.add("ancora_soberana_oficial_presente_em_base_publica_primaria");
        }
        if (publicCnpjActive && publicNatureCompatible) {
            reasons.add("cnpj_ativo_com_natureza_juridica_compativel_com_ente_ou_orgao_publico");
        }
        if (subordinateUnitWithoutOwnCnpj && parentInstitutionRecognized && legalActPresent) {
            reasons.add("subunidade_sem_cnpj_proprio_herdando_confianca_da_instituicao_pai_com_ato_formal");
        }
        if (officialEmailChannel && officialDomain) {
            reasons.add("canal_institucional_oficial_fechado_por_email_e_dominio_controlados");
        }
        if (territorialMatch) {
            reasons.add("topologia_territorial_compativel_com_uf_municipio_ou_cluster_jurisdicional");
        }
        if (representativeGovBrGold && representativeIcpBrasilValid) {
            reasons.add("representante_legal_com_identidade_raiz_em_dose_dupla_govbr_ouro_mais_icp_brasil_pf");
        }
        if (automaticEligible) {
            reasons.add("ativacao_automatica_elegivel_sem_homologacao_manual");
        }
        if (assistedEligible) {
            reasons.add("ativacao_assistida_elegivel_com_homologacao_humana_e_trilha_auditavel");
        }
        if (!officialCatalogMatch && !publicCnpjActive && !(subordinateUnitWithoutOwnCnpj && parentInstitutionRecognized)) {
            blockers.add("sem_ancora_institucional_oficial_suficiente");
        }
        if (publicCnpjActive && !publicNatureCompatible && !officialCatalogMatch) {
            blockers.add("cnpj_ativo_sem_natureza_publica_compativel");
        }
        if (!legalActPresent && !officialCatalogMatch) {
            blockers.add("ato_legal_ou_de_competencia_nao_apresentado_para_fluxo_nao_catalogado");
        }
        if (!officialEmailChannel) {
            blockers.add("email_institucional_oficial_ausente_ou_nao_validado");
        }
        if (!officialDomain) {
            blockers.add("dominio_institucional_oficial_ausente_ou_nao_validado");
        }
        if (!territorialMatch) {
            blockers.add("vinculo_territorial_ou_topologico_nao_confirmado");
        }
        if (!representativeGovBrGold) {
            blockers.add("representante_legal_sem_govbr_ouro");
        }
        if (!representativeIcpBrasilValid) {
            blockers.add("representante_legal_sem_certificado_icp_brasil_pf_valido");
        }
        if (subordinateUnitWithoutOwnCnpj && !parentInstitutionRecognized) {
            blockers.add("subunidade_sem_cnpj_proprio_sem_instituicao_pai_reconhecida");
        }
    }

    private static List<AdminInstitutionalPublicRecognitionResponse.EvidenceRule> buildEvidenceRules(boolean officialCatalogMatch,
                                                                                                      boolean publicCnpjActive,
                                                                                                      boolean publicNatureCompatible,
                                                                                                      boolean officialEmailChannel,
                                                                                                      boolean officialDomain,
                                                                                                      boolean legalActPresent,
                                                                                                      boolean territorialMatch,
                                                                                                      boolean representativeGovBrGold,
                                                                                                      boolean representativeIcpBrasilValid,
                                                                                                      boolean subordinateUnitWithoutOwnCnpj,
                                                                                                      boolean parentInstitutionRecognized) {
        ArrayList<AdminInstitutionalPublicRecognitionResponse.EvidenceRule> out = new ArrayList<>();
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "ANCORA_CATALOGO_SOBERANO",
                "Base soberana oficial compatível com a instituição",
                true,
                officialCatalogMatch,
                "CNJ_DATAJUD_OU_SIORG"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "CNPJ_PUBLICO_ATIVO",
                "CNPJ ativo com situação cadastral regular",
                false,
                publicCnpjActive,
                "RECEITA_CNPJ"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "NATUREZA_JURIDICA_PUBLICA",
                "Natureza jurídica compatível com ente ou órgão público",
                false,
                publicNatureCompatible,
                "RECEITA_CNPJ"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "EMAIL_INSTITUCIONAL",
                "E-mail oficial controlado pela instituição",
                true,
                officialEmailChannel,
                "CANAL_OFICIAL"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "DOMINIO_INSTITUCIONAL",
                "Domínio oficial compatível com o órgão",
                true,
                officialDomain,
                "DNS_E_GOVERNANCA_INSTITUCIONAL"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "ATO_LEGAL",
                "Ato de criação, competência ou delegação da unidade",
                false,
                legalActPresent,
                "ATO_PUBLICADO"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "TOPOLOGIA_TERRITORIAL",
                "Compatibilidade com UF, município, comarca ou cluster jurisdicional",
                true,
                territorialMatch,
                "IBGE_OU_TOPOLOGIA_CNJ"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "REPRESENTANTE_GOVBR_OURO",
                "Representante legal autenticado com conta gov.br Ouro",
                true,
                representativeGovBrGold,
                "GOVBR"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "REPRESENTANTE_ICP_BRASIL",
                "Representante legal com certificado ICP-Brasil PF válido",
                true,
                representativeIcpBrasilValid,
                "ITI_ICP_BRASIL"
        ));
        out.add(new AdminInstitutionalPublicRecognitionResponse.EvidenceRule(
                "SUBUNIDADE_HERDADA",
                "Subunidade sem CNPJ próprio vinculada a instituição-pai reconhecida",
                false,
                subordinateUnitWithoutOwnCnpj && parentInstitutionRecognized,
                "HERANCA_DE_CONFIANCA_INSTITUCIONAL"
        ));
        return List.copyOf(out);
    }

    private static List<String> nextSafeSteps(Status status, Scope scope) {
        return switch (status) {
            case RECONHECIDA_AUTOMATICAMENTE -> List.of(
                    "emitir_codigo_de_ativacao_para_canal_oficial",
                    "exigir_confirmacao_do_representante_legal_em_dose_dupla",
                    "abrir_expansao_interna_governada_por_unidade_lotacao_e_caixa"
            );
            case RECONHECIDA_COM_HOMOLOGACAO -> List.of(
                    "submeter_o_dossie_a_homologacao_assistida_com_trilha_auditavel",
                    "confirmar_ato_legal_e_unidade_responsavel_na_topologia",
                    "emitir_ativacao_limitada_ate_validacao_final_do_orgao"
            );
            case PENDENTE_EVIDENCIAS -> List.of(
                    "coletar_ancoras_oficiais_que_ainda_estao_ausentes",
                    "validar_representante_legal_com_govbr_e_icp_brasil_pf",
                    "confirmar_email_dominio_e_topologia_territorial_para_o_escopo_" + scope.code.toLowerCase(Locale.ROOT)
            );
            case NEGADA -> List.of(
                    "bloquear_ativacao_automaticamente",
                    "exigir_reenquadramento_da_instituicao_ou_documentacao_mais_forte",
                    "encaminhar_para_fila_de_revisao_somente_se_houver_base_legal_para_reanalise"
            );
        };
    }

    private enum Status {
        RECONHECIDA_AUTOMATICAMENTE("RECONHECIDA_AUTOMATICAMENTE", "Reconhecida automaticamente", true, true, false),
        RECONHECIDA_COM_HOMOLOGACAO("RECONHECIDA_COM_HOMOLOGACAO", "Reconhecida com homologação assistida", true, false, true),
        PENDENTE_EVIDENCIAS("PENDENTE_EVIDENCIAS", "Pendente de evidências", false, false, false),
        NEGADA("NEGADA", "Negada", false, false, false);

        private final String code;
        private final String label;
        private final boolean recognized;
        private final boolean autoActivatable;
        private final boolean humanReviewRequired;

        Status(String code, String label, boolean recognized, boolean autoActivatable, boolean humanReviewRequired) {
            this.code = code;
            this.label = label;
            this.recognized = recognized;
            this.autoActivatable = autoActivatable;
            this.humanReviewRequired = humanReviewRequired;
        }
    }

    private enum Scope {
        JUDICIARIO_CNJ("JUDICIARIO_CNJ", "Judiciário catalogado pelo CNJ", List.of("CNJ_DATAJUD", "RECEITA_CNPJ", "IBGE", "GOVBR", "ITI_ICP_BRASIL")),
        EXECUTIVO_FEDERAL_SIORG("EXECUTIVO_FEDERAL_SIORG", "Órgão federal catalogado no SIORG", List.of("SIORG", "RECEITA_CNPJ", "IBGE", "GOVBR", "ITI_ICP_BRASIL")),
        ESTADUAL_MUNICIPAL("ESTADUAL_MUNICIPAL", "Órgão estadual ou municipal", List.of("RECEITA_CNPJ", "ATO_PUBLICADO", "IBGE", "GOVBR", "ITI_ICP_BRASIL")),
        SUBUNIDADE_VINCULADA("SUBUNIDADE_VINCULADA", "Subunidade vinculada a instituição-pai", List.of("INSTITUICAO_PAI_RECONHECIDA", "ATO_PUBLICADO", "IBGE", "GOVBR", "ITI_ICP_BRASIL")),
        SISTEMA_CONVENIADO("SISTEMA_CONVENIADO", "Sistema ou entidade conveniada", List.of("ATO_PUBLICADO", "CONVENIO_FORMAL", "DOMINIO_OFICIAL", "GOVBR", "ITI_ICP_BRASIL"));

        private final String code;
        private final String label;
        private final List<String> acceptedOfficialSources;

        Scope(String code, String label, List<String> acceptedOfficialSources) {
            this.code = code;
            this.label = label;
            this.acceptedOfficialSources = acceptedOfficialSources;
        }

        private List<String> acceptedOfficialSources() {
            return acceptedOfficialSources;
        }

        private static Scope from(String raw) {
            if (raw == null || raw.isBlank()) {
                return ESTADUAL_MUNICIPAL;
            }
            String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
            for (Scope item : values()) {
                if (item.code.equals(normalized)) {
                    return item;
                }
            }
            return ESTADUAL_MUNICIPAL;
        }
    }
}
