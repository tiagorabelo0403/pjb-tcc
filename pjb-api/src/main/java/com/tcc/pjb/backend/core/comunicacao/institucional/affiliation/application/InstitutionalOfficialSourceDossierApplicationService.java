package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class InstitutionalOfficialSourceDossierApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final Clock clock;

    @Inject
    @Autowired
    public InstitutionalOfficialSourceDossierApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                               InstitutionalAffiliationRequestStateRepository requestRepository,
                                                               InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService) {
        this(affiliationRepository, requestRepository, publicRecognitionGateApplicationService, Clock.systemUTC());
    }

    InstitutionalOfficialSourceDossierApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                         InstitutionalAffiliationRequestStateRepository requestRepository,
                                                         InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
                                                         Clock clock) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.publicRecognitionGateApplicationService = Objects.requireNonNull(publicRecognitionGateApplicationService);
        this.clock = Objects.requireNonNull(clock);
    }

    public InstitutionalOfficialSourceDossier gerarAfiliacao(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        return gerarAfiliacao(affiliation);
    }

    public InstitutionalOfficialSourceDossier gerarSolicitacao(String requestId) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("solicitacao_adesao_institucional_nao_encontrada"));
        return gerarSolicitacao(request);
    }

    public InstitutionalOfficialSourceDossier gerarAfiliacao(InstitutionalAffiliation affiliation) {
        SubjectMetadata metadata = SubjectMetadata.fromAffiliation(affiliation);
        return build(
                "AFILIACAO",
                affiliation.affiliationId(),
                affiliation.affiliationId(),
                null,
                metadata,
                publicRecognitionGateApplicationService.inspecionarAfiliacao(affiliation),
                publicRecognitionGateApplicationService.avaliarAfiliacao(affiliation),
                affiliation.fundamentos(),
                max(affiliation.createdAt(), affiliation.updatedAt())
        );
    }

    public InstitutionalOfficialSourceDossier gerarSolicitacao(InstitutionalAffiliationRequest request) {
        SubjectMetadata metadata = SubjectMetadata.fromRequest(request);
        return build(
                "SOLICITACAO",
                request.requestId(),
                request.materializedAffiliationId(),
                request.requestId(),
                metadata,
                publicRecognitionGateApplicationService.inspecionarSolicitacao(request),
                publicRecognitionGateApplicationService.avaliarSolicitacao(request),
                request.fundamentos(),
                max(request.createdAt(), max(request.decidedAt(), request.updatedAt()))
        );
    }

    private InstitutionalOfficialSourceDossier build(String subjectType,
                                                     String subjectId,
                                                     String affiliationId,
                                                     String requestId,
                                                     SubjectMetadata metadata,
                                                     InstitutionalPublicRecognitionGateApplicationService.RecognitionInput input,
                                                     AdminInstitutionalPublicRecognitionResponse report,
                                                     List<String> fundamentosOriginais,
                                                     Instant evidenceAt) {
        Instant now = Instant.now(clock);
        Instant baseEvidenceAt = evidenceAt == null ? now : evidenceAt;
        boolean judiciaryCatalogApplicable = isJudiciaryCatalogApplicable(metadata);
        boolean siorgApplicable = isSiorgApplicable(metadata, judiciaryCatalogApplicable);
        List<InstitutionalOfficialSourceEvidence> sources = List.of(
                source("CNJ_DATAJUD", "Base CNJ/DataJud", "CNJ_DATAJUD_OU_SIORG", judiciaryCatalogApplicable, judiciaryCatalogApplicable && input.officialCatalogMatch(), mandatory(report, "CNJ_DATAJUD_OU_SIORG"), baseEvidenceAt, 30,
                        signals("catalogo_judiciario", metadata.orgaoSigla(), metadata.unidadeCodigo()), issues(judiciaryCatalogApplicable && !input.officialCatalogMatch(), "catalogo_cnj_datajud_ausente_ou_nao_confirmado"),
                        fundamentos("escopo_judiciario", metadata.scopeCode())),
                source("SIORG", "Cadastro SIORG", "CNJ_DATAJUD_OU_SIORG", siorgApplicable, siorgApplicable && input.officialCatalogMatch(), mandatory(report, "CNJ_DATAJUD_OU_SIORG"), baseEvidenceAt, 30,
                        signals("estrutura_federal", metadata.esferaAdministrativa(), metadata.domain()), issues(siorgApplicable && !input.officialCatalogMatch(), "cadastro_siorg_ausente_ou_nao_confirmado"),
                        fundamentos("escopo_federal_executivo", metadata.scopeCode())),
                source("RECEITA_CNPJ", "Receita/CNPJ", "RECEITA_CNPJ", !input.subordinateUnitWithoutOwnCnpj() || metadata.hasOwnCnpj(), input.publicCnpjActive() && input.publicNatureCompatible(), mandatory(report, "RECEITA_CNPJ"), baseEvidenceAt, 30,
                        signals(metadata.cnpj(), metadata.esferaAdministrativa()), issues(!metadata.hasOwnCnpj() && !input.subordinateUnitWithoutOwnCnpj(), "cnpj_nao_informado", metadata.hasOwnCnpj() && !input.publicCnpjActive(), "cnpj_sem_validacao_ativa", metadata.hasOwnCnpj() && !input.publicNatureCompatible(), "natureza_juridica_publica_insuficiente"),
                        fundamentos("identidade_juridica", metadata.orgaoSigla())),
                source("CANAL_OFICIAL", "Canal institucional oficial", "CANAL_OFICIAL", true, input.officialEmailChannel(), mandatory(report, "CANAL_OFICIAL"), baseEvidenceAt, 30,
                        signals(metadata.domain(), metadata.emailContatoSeguranca()), issues(!input.officialEmailChannel(), "canal_oficial_nao_confirmado"),
                        fundamentos("ativacao_via_canal_oficial")),
                source("DNS_E_GOVERNANCA_INSTITUCIONAL", "Domínio institucional governado", "DNS_E_GOVERNANCA_INSTITUCIONAL", true, input.officialDomain(), mandatory(report, "DNS_E_GOVERNANCA_INSTITUCIONAL"), baseEvidenceAt, 30,
                        signals(metadata.domain()), issues(!input.officialDomain(), "dominio_oficial_nao_confirmado"),
                        fundamentos("dominio_governado_pela_instituicao")),
                source("ATO_PUBLICADO", "Ato publicado ou delegação formal", "ATO_PUBLICADO", true, input.legalActPresent(), mandatory(report, "ATO_PUBLICADO"), baseEvidenceAt, 90,
                        signals(metadata.autoridadeAderenteCargo(), metadata.orgaoSigla()), issues(!input.legalActPresent(), "ato_formal_ou_delegacao_nao_confirmado"),
                        fundamentos("base_legal_para_ativacao_ou_subunidade")),
                source("IBGE_OU_TOPOLOGIA_CNJ", "Topologia territorial oficial", "IBGE_OU_TOPOLOGIA_CNJ", true, input.territorialMatch(), mandatory(report, "IBGE_OU_TOPOLOGIA_CNJ"), baseEvidenceAt, 180,
                        signals(metadata.uf(), metadata.comarca(), join(metadata.abrangenciasTerritoriais())), issues(!input.territorialMatch(), "topologia_territorial_nao_confirmada"),
                        fundamentos("uf_municipio_comarca_cluster")),
                source("GOVBR", "Identidade gov.br do representante", "GOVBR", true, input.representativeGovBrGold(), mandatory(report, "GOVBR"), baseEvidenceAt, 30,
                        signals(String.valueOf(metadata.representanteUsuarioId())), issues(!input.representativeGovBrGold(), "representante_sem_govbr_ouro_confirmado"),
                        fundamentos("identidade_pessoal_raiz")),
                source("ITI_ICP_BRASIL", "Certificado ICP-Brasil do representante", "ITI_ICP_BRASIL", true, input.representativeIcpBrasilValid(), mandatory(report, "ITI_ICP_BRASIL"), baseEvidenceAt, 30,
                        signals(metadata.autoridadeAderenteCargo(), String.valueOf(metadata.representanteUsuarioId())), issues(!input.representativeIcpBrasilValid(), "representante_sem_icp_brasil_pf_confirmado"),
                        fundamentos("assinatura_qualificada_pessoal")),
                source("HERANCA_DE_CONFIANCA_INSTITUCIONAL", "Herança de confiança da instituição-pai", "HERANCA_DE_CONFIANCA_INSTITUCIONAL", input.subordinateUnitWithoutOwnCnpj(), input.subordinateUnitWithoutOwnCnpj() && input.parentInstitutionRecognized(), input.subordinateUnitWithoutOwnCnpj() || mandatory(report, "HERANCA_DE_CONFIANCA_INSTITUCIONAL"), baseEvidenceAt, 15,
                        signals(metadata.orgaoSigla(), metadata.unidadeCodigo()), issues(input.subordinateUnitWithoutOwnCnpj() && !input.parentInstitutionRecognized(), "instituicao_pai_nao_reconhecida"),
                        fundamentos("subunidade_sem_cnpj_proprio"))
        );
        List<InstitutionalOfficialSourceEvidence> applicableSources = sources.stream().filter(InstitutionalOfficialSourceEvidence::applicable).toList();
        boolean sovereignRecognitionReady = applicableSources.stream()
                .filter(InstitutionalOfficialSourceEvidence::mandatoryForAutomaticActivation)
                .allMatch(InstitutionalOfficialSourceEvidence::satisfied)
                && !"NEGADA".equalsIgnoreCase(report.statusCode());
        Instant nextMandatoryReviewAt = applicableSources.stream()
                .filter(InstitutionalOfficialSourceEvidence::mandatoryForAutomaticActivation)
                .map(InstitutionalOfficialSourceEvidence::nextReviewAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        if (report.blockers() != null) {
            report.blockers().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .forEach(blockingIssues::add);
        }
        applicableSources.stream()
                .filter(InstitutionalOfficialSourceEvidence::mandatoryForAutomaticActivation)
                .forEach(item -> {
                    if (!item.satisfied()) {
                        blockingIssues.addAll(item.pendingIssues());
                    }
                    if (item.stale()) {
                        blockingIssues.add("fonte_oficial_expirada=" + item.sourceCode());
                    }
                });
        boolean dueNow = report.humanReviewRequired()
                || applicableSources.stream().anyMatch(item -> item.mandatoryForAutomaticActivation() && item.stale())
                || !blockingIssues.isEmpty();
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("status_reconhecimento_publico=" + report.statusCode());
        fundamentos.add("reconhecimento_soberano_pronto=" + sovereignRecognitionReady);
        if (report.acceptedOfficialSources() != null) {
            report.acceptedOfficialSources().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .forEach(item -> fundamentos.add("fonte_aceita=" + item));
        }
        if (report.nextSafeSteps() != null) {
            report.nextSafeSteps().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .forEach(item -> fundamentos.add("proximo_passo=" + item));
        }
        if (fundamentosOriginais != null) {
            fundamentos.addAll(fundamentosOriginais);
        }
        return new InstitutionalOfficialSourceDossier(
                subjectType,
                subjectId,
                affiliationId,
                requestId,
                metadata.scopeCode(),
                metadata.orgaoSigla(),
                metadata.unidadeCodigo(),
                report.statusCode(),
                sovereignRecognitionReady,
                dueNow,
                nextMandatoryReviewAt,
                List.copyOf(blockingIssues.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList()),
                sources,
                List.copyOf(fundamentos.stream().filter(Objects::nonNull).distinct().toList()),
                now
        );
    }

    private InstitutionalOfficialSourceEvidence source(String sourceCode,
                                                       String sourceLabel,
                                                       String sourceGroup,
                                                       boolean applicable,
                                                       boolean satisfied,
                                                       boolean mandatoryForAutomaticActivation,
                                                       Instant lastEvidenceAt,
                                                       long reviewDays,
                                                       List<String> evidenceSignals,
                                                       List<String> pendingIssues,
                                                       List<String> fundamentos) {
        Instant nextReviewAt = applicable && lastEvidenceAt != null ? lastEvidenceAt.plus(reviewDays, ChronoUnit.DAYS) : null;
        Instant now = Instant.now(clock);
        boolean stale = applicable && nextReviewAt != null && !nextReviewAt.isAfter(now);
        return new InstitutionalOfficialSourceEvidence(
                sourceCode,
                sourceLabel,
                sourceGroup,
                applicable,
                applicable && satisfied,
                mandatoryForAutomaticActivation,
                stale,
                applicable ? lastEvidenceAt : null,
                nextReviewAt,
                evidenceSignals,
                pendingIssues,
                fundamentos
        );
    }

    private boolean mandatory(AdminInstitutionalPublicRecognitionResponse report, String sourceType) {
        if (report == null || report.requiredEvidence() == null) {
            return false;
        }
        return report.requiredEvidence().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> item.mandatoryForAutomaticActivation() && sourceType.equalsIgnoreCase(item.sourceType()));
    }

    private static boolean isJudiciaryCatalogApplicable(SubjectMetadata metadata) {
        if (metadata.organizationScope() == InstitutionalOrganizationScope.FORUM
                || metadata.organizationScope() == InstitutionalOrganizationScope.SECRETARIA_UNIDADE_JUDICIARIA
                || metadata.organizationScope() == InstitutionalOrganizationScope.CENTRAL_AUDIENCIAS
                || metadata.organizationScope() == InstitutionalOrganizationScope.CENTRAL_MANDADOS
                || metadata.organizationScope() == InstitutionalOrganizationScope.CEJUSC
                || metadata.organizationScope() == InstitutionalOrganizationScope.CONTADORIA
                || metadata.organizationScope() == InstitutionalOrganizationScope.CARTORIO_INTEGRADO
                || metadata.organizationScope() == InstitutionalOrganizationScope.EQUIPE_PSICOSSOCIAL) {
            return true;
        }
        if (metadata.destinatarioKind() == DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.JUIZO_DEPRECADO
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.CEJUSC
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.ASSISTENTE_SOCIAL_JUDICIAL
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.PERICIA_JUDICIAL
                || metadata.destinatarioKind() == DestinatarioInstitucionalKind.PERITO_JUDICIAL) {
            return true;
        }
        String sigla = metadata.orgaoSigla() == null ? "" : metadata.orgaoSigla().toUpperCase(Locale.ROOT);
        return sigla.startsWith("TJ") || sigla.startsWith("TRF") || sigla.startsWith("TRT") || sigla.startsWith("TRE")
                || sigla.startsWith("TJM") || sigla.startsWith("STM") || sigla.startsWith("STJ") || sigla.startsWith("STF")
                || sigla.startsWith("TST") || sigla.startsWith("TSE") || sigla.startsWith("CNJ");
    }

    private static boolean isSiorgApplicable(SubjectMetadata metadata, boolean judiciaryCatalogApplicable) {
        if (judiciaryCatalogApplicable) {
            return false;
        }
        String esfera = metadata.esferaAdministrativa() == null ? "" : metadata.esferaAdministrativa().toUpperCase(Locale.ROOT);
        if (esfera.contains("FEDERAL") || esfera.contains("UNIAO") || esfera.contains("UNIÃO")) {
            return true;
        }
        if (metadata.destinatarioKind() == DestinatarioInstitucionalKind.AGU || metadata.destinatarioKind() == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL) {
            return true;
        }
        return metadata.domain() != null && metadata.domain().endsWith(".gov.br");
    }

    private static List<String> signals(String... values) {
        ArrayList<String> out = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    out.add(value.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    private static List<String> issues(boolean condition, String value) {
        ArrayList<String> out = new ArrayList<>();
        if (condition && value != null && !value.isBlank()) {
            out.add(value.trim());
        }
        return List.copyOf(out);
    }

    private static List<String> issues(boolean firstCondition, String firstValue, boolean secondCondition, String secondValue, boolean thirdCondition, String thirdValue) {
        ArrayList<String> out = new ArrayList<>();
        if (firstCondition && firstValue != null && !firstValue.isBlank()) {
            out.add(firstValue.trim());
        }
        if (secondCondition && secondValue != null && !secondValue.isBlank()) {
            out.add(secondValue.trim());
        }
        if (thirdCondition && thirdValue != null && !thirdValue.isBlank()) {
            out.add(thirdValue.trim());
        }
        return List.copyOf(out);
    }

    private static List<String> fundamentos(String... values) {
        return signals(values);
    }

    private static Instant max(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private record SubjectMetadata(
            DestinatarioInstitucionalKind destinatarioKind,
            InstitutionalOrganizationScope organizationScope,
            String scopeCode,
            String orgaoSigla,
            String unidadeCodigo,
            String esferaAdministrativa,
            String domain,
            String autoridadeAderenteCargo,
            String emailContatoSeguranca,
            String uf,
            String comarca,
            String cnpj,
            boolean hasOwnCnpj,
            Long representanteUsuarioId,
            List<String> abrangenciasTerritoriais
    ) {
        private static SubjectMetadata fromAffiliation(InstitutionalAffiliation affiliation) {
            return new SubjectMetadata(
                    affiliation.destinatarioKind(),
                    affiliation.organizationScope(),
                    affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    affiliation.orgaoSigla(),
                    affiliation.unidadeCodigo(),
                    affiliation.esferaAdministrativa(),
                    affiliation.dominioInstitucional(),
                    affiliation.autoridadeAderenteCargo(),
                    affiliation.emailContatoSeguranca(),
                    affiliation.uf(),
                    affiliation.comarca(),
                    affiliation.cnpj(),
                    affiliation.cnpj() != null && !affiliation.cnpj().isBlank(),
                    affiliation.representanteUsuarioId(),
                    affiliation.abrangenciasTerritoriais()
            );
        }

        private static SubjectMetadata fromRequest(InstitutionalAffiliationRequest request) {
            return new SubjectMetadata(
                    request.destinatarioKind(),
                    request.organizationScope(),
                    request.organizationScope() == null ? null : request.organizationScope().name(),
                    request.orgaoSigla(),
                    request.unidadeCodigo(),
                    request.esferaAdministrativa(),
                    request.dominioInstitucional(),
                    request.autoridadeAderenteCargo(),
                    null,
                    request.uf(),
                    request.comarca(),
                    request.cnpj(),
                    request.cnpj() != null && !request.cnpj().isBlank(),
                    request.representanteUsuarioId(),
                    request.abrangenciasTerritoriais()
            );
        }
    }
}
