package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierCheck;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class InstitutionalOfficialIdentifierDossierApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOfficialSourceConnectorRegistry connectorRegistry;
    private final Clock clock;

    @Inject
    @Autowired
    public InstitutionalOfficialIdentifierDossierApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                                    InstitutionalAffiliationRequestStateRepository requestRepository,
                                                                    InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
                                                                    InstitutionalOfficialSourceConnectorRegistry connectorRegistry) {
        this(affiliationRepository, requestRepository, publicRecognitionGateApplicationService, connectorRegistry, Clock.systemUTC());
    }

    InstitutionalOfficialIdentifierDossierApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                             InstitutionalAffiliationRequestStateRepository requestRepository,
                                                             InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
                                                             InstitutionalOfficialSourceConnectorRegistry connectorRegistry,
                                                             Clock clock) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.publicRecognitionGateApplicationService = Objects.requireNonNull(publicRecognitionGateApplicationService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    public InstitutionalOfficialIdentifierDossier gerarAfiliacao(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        return gerarAfiliacao(affiliation);
    }

    public InstitutionalOfficialIdentifierDossier gerarSolicitacao(String requestId) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("solicitacao_adesao_institucional_nao_encontrada"));
        return gerarSolicitacao(request);
    }

    public InstitutionalOfficialIdentifierDossier gerarAfiliacao(InstitutionalAffiliation affiliation) {
        return build(
                "AFILIACAO",
                affiliation.affiliationId(),
                affiliation.affiliationId(),
                null,
                SubjectMetadata.fromAffiliation(affiliation),
                publicRecognitionGateApplicationService.inspecionarAfiliacao(affiliation),
                affiliation.fundamentos()
        );
    }

    public InstitutionalOfficialIdentifierDossier gerarSolicitacao(InstitutionalAffiliationRequest request) {
        return build(
                "SOLICITACAO",
                request.requestId(),
                request.materializedAffiliationId(),
                request.requestId(),
                SubjectMetadata.fromRequest(request),
                publicRecognitionGateApplicationService.inspecionarSolicitacao(request),
                request.fundamentos()
        );
    }

    private InstitutionalOfficialIdentifierDossier build(String subjectType,
                                                         String subjectId,
                                                         String affiliationId,
                                                         String requestId,
                                                         SubjectMetadata metadata,
                                                         InstitutionalPublicRecognitionGateApplicationService.RecognitionInput recognitionInput,
                                                         List<String> fundamentosOriginais) {
        Instant now = Instant.now(clock);
        List<InstitutionalOfficialIdentifierCheck> checks = List.of(
                buildCnpjCheck(metadata, recognitionInput),
                buildIbgeCheck(metadata),
                buildDataJudCheck(metadata),
                buildSiorgCheck(metadata),
                buildDomainCheck(metadata),
                buildOfficialEmailCheck(metadata)
        );
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (fundamentosOriginais != null) {
            fundamentos.addAll(fundamentosOriginais);
        }
        checks.stream().map(InstitutionalOfficialIdentifierCheck::pendingIssues).filter(Objects::nonNull).forEach(blockers::addAll);
        boolean materialEvidenceReady = materialEvidenceReady(checks);
        String overallStatus = resolveOverallStatus(checks);
        fundamentos.add("identificadores_oficiais_checks=" + checks.size());
        fundamentos.add("material_evidence_ready=" + materialEvidenceReady);
        fundamentos.add("identificadores_oficiais_status=" + overallStatus);
        return new InstitutionalOfficialIdentifierDossier(
                subjectType,
                subjectId,
                affiliationId,
                requestId,
                metadata.scopeCode(),
                metadata.orgaoSigla(),
                metadata.unidadeCodigo(),
                overallStatus,
                materialEvidenceReady,
                now,
                List.copyOf(blockers),
                checks,
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildCnpjCheck(SubjectMetadata metadata,
                                                                InstitutionalPublicRecognitionGateApplicationService.RecognitionInput recognitionInput) {
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("RECEITA_CNPJ");
        String normalized = InstitutionalOfficialIdentifierResolverSupport.normalizeCnpj(metadata.cnpj());
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        signals.add("connector_status=" + connector.connectorStatus());
        String status;
        boolean ready;
        if (recognitionInput.subordinateUnitWithoutOwnCnpj() && normalized == null) {
            status = "HERDADO_DA_INSTITUICAO_PAI";
            ready = false;
            fundamentos.add("subunidade_sem_cnpj_proprio");
            if (!recognitionInput.parentInstitutionRecognized()) {
                issues.add("instituicao_pai_nao_reconhecida_para_heranca_de_confianca");
            }
        } else if (normalized == null) {
            status = "PENDENTE_IDENTIFICADOR";
            ready = false;
            issues.add("cnpj_nao_informado");
        } else if (!InstitutionalOfficialIdentifierResolverSupport.isValidCnpj(normalized)) {
            status = "IDENTIFICADOR_INVALIDO";
            ready = false;
            issues.add("cnpj_invalido");
        } else if (connector.liveVerificationSupported()) {
            status = "APTO_VERIFICACAO_REMOTA";
            ready = true;
            fundamentos.add("cnpj_checksum_valido");
        } else {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            ready = false;
            fundamentos.add("cnpj_checksum_valido");
            if (!connector.enabled()) {
                issues.add("conector_receita_desabilitado");
            } else if (connector.connectorStatus() != null && connector.connectorStatus().contains("DRY_RUN")) {
                issues.add("conector_receita_em_dry_run");
            }
        }
        return new InstitutionalOfficialIdentifierCheck(
                "CNPJ",
                "CNPJ institucional",
                "RECEITA_CNPJ",
                metadata.cnpj(),
                normalized,
                status,
                true,
                !recognitionInput.subordinateUnitWithoutOwnCnpj(),
                ready,
                connector.connectorStatus(),
                InstitutionalOfficialIdentifierResolverSupport.buildCnpjLookupUrl(normalized),
                List.copyOf(signals),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildIbgeCheck(SubjectMetadata metadata) {
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("IBGE_OU_TOPOLOGIA_CNJ");
        String codigo = InstitutionalOfficialIdentifierResolverSupport.deriveIbgeMunicipioCode(metadata.comarca(), metadata.unidadeCodigo(), metadata.abrangenciasTerritoriais());
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (metadata.uf() != null) {
            signals.add("uf=" + metadata.uf());
        }
        if (metadata.comarca() != null) {
            signals.add("comarca=" + sanitizeSignal(metadata.comarca()));
        }
        String status;
        boolean ready;
        if (codigo == null) {
            status = "PENDENTE_IDENTIFICADOR";
            ready = false;
            issues.add("codigo_ibge_municipio_ausente");
        } else if (connector.liveVerificationSupported()) {
            status = "APTO_VERIFICACAO_REMOTA";
            ready = true;
            fundamentos.add("codigo_ibge_candidato=" + codigo);
        } else {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            ready = false;
            fundamentos.add("codigo_ibge_candidato=" + codigo);
            if (connector.connectorStatus() != null && connector.connectorStatus().contains("DRY_RUN")) {
                issues.add("conector_ibge_em_dry_run");
            }
        }
        return new InstitutionalOfficialIdentifierCheck(
                "CODIGO_IBGE_MUNICIPIO",
                "Código IBGE do município",
                "IBGE_OU_TOPOLOGIA_CNJ",
                codigo,
                codigo,
                status,
                true,
                true,
                ready,
                connector.connectorStatus(),
                InstitutionalOfficialIdentifierResolverSupport.buildIbgeLookupUrl(codigo),
                List.copyOf(signals),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildDataJudCheck(SubjectMetadata metadata) {
        boolean applicable = InstitutionalOfficialIdentifierResolverSupport.isJudiciaryScope(metadata.organizationScope(), metadata.orgaoSigla());
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("CNJ_DATAJUD");
        String alias = InstitutionalOfficialIdentifierResolverSupport.deriveDataJudAlias(metadata.orgaoSigla());
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (metadata.orgaoSigla() != null) {
            signals.add("orgao_sigla=" + metadata.orgaoSigla());
        }
        String status;
        boolean ready;
        if (!applicable) {
            status = "NAO_APLICAVEL";
            ready = false;
        } else if (alias == null) {
            status = "PENDENTE_IDENTIFICADOR";
            ready = false;
            issues.add("alias_datajud_nao_derivado");
        } else if (connector.liveVerificationSupported()) {
            status = "APTO_VERIFICACAO_REMOTA";
            ready = true;
            fundamentos.add("alias_datajud=" + alias);
        } else {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            ready = false;
            fundamentos.add("alias_datajud=" + alias);
            if (connector.connectorStatus() != null && connector.connectorStatus().contains("DRY_RUN")) {
                issues.add("conector_datajud_em_dry_run");
            }
        }
        return new InstitutionalOfficialIdentifierCheck(
                "ALIAS_DATAJUD",
                "Alias DataJud/CNJ",
                "CNJ_DATAJUD",
                alias,
                alias,
                status,
                applicable,
                applicable,
                ready,
                connector.connectorStatus(),
                InstitutionalOfficialIdentifierResolverSupport.buildDataJudLookupUrl(alias),
                List.copyOf(signals),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildSiorgCheck(SubjectMetadata metadata) {
        boolean applicable = InstitutionalOfficialIdentifierResolverSupport.isFederalExecutiveScope(metadata.esferaAdministrativa(), metadata.organizationScope());
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("SIORG");
        String code = InstitutionalOfficialIdentifierResolverSupport.deriveSiorgUnitCode(metadata.unidadeCodigo());
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (metadata.esferaAdministrativa() != null) {
            signals.add("esfera=" + sanitizeSignal(metadata.esferaAdministrativa()));
        }
        String status;
        boolean ready;
        if (!applicable) {
            status = "NAO_APLICAVEL";
            ready = false;
        } else if (code == null) {
            status = "PENDENTE_IDENTIFICADOR";
            ready = false;
            issues.add("codigo_unidade_siorg_nao_derivado");
        } else if (connector.liveVerificationSupported()) {
            status = "APTO_VERIFICACAO_REMOTA";
            ready = true;
            fundamentos.add("codigo_siorg_candidato=" + code);
        } else {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            ready = false;
            fundamentos.add("codigo_siorg_candidato=" + code);
            if (connector.connectorStatus() != null && connector.connectorStatus().contains("DRY_RUN")) {
                issues.add("conector_siorg_em_dry_run");
            }
        }
        return new InstitutionalOfficialIdentifierCheck(
                "CODIGO_UNIDADE_SIORG",
                "Código da unidade no SIORG",
                "SIORG",
                code,
                code,
                status,
                applicable,
                applicable,
                ready,
                connector.connectorStatus(),
                InstitutionalOfficialIdentifierResolverSupport.buildSiorgLookupUrl(code),
                List.copyOf(signals),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildDomainCheck(SubjectMetadata metadata) {
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("DNS_E_GOVERNANCA_INSTITUCIONAL");
        String normalized = InstitutionalOfficialIdentifierResolverSupport.normalizeDomain(metadata.dominioInstitucional());
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String status;
        if (normalized == null) {
            status = "PENDENTE_IDENTIFICADOR";
            issues.add("dominio_institucional_nao_informado");
        } else if (InstitutionalOfficialIdentifierResolverSupport.isOfficialInstitutionalDomain(normalized)) {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            fundamentos.add("dominio_oficial=true");
        } else {
            status = "IDENTIFICADOR_INVALIDO";
            issues.add("dominio_institucional_nao_oficial");
        }
        return new InstitutionalOfficialIdentifierCheck(
                "DOMINIO_INSTITUCIONAL",
                "Domínio institucional",
                "DNS_E_GOVERNANCA_INSTITUCIONAL",
                metadata.dominioInstitucional(),
                normalized,
                status,
                true,
                true,
                false,
                connector.connectorStatus(),
                null,
                List.of(),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private InstitutionalOfficialIdentifierCheck buildOfficialEmailCheck(SubjectMetadata metadata) {
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe("CANAL_OFICIAL");
        String normalized = InstitutionalOfficialIdentifierResolverSupport.normalizeDomain(metadata.emailContatoSeguranca());
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String status;
        if (normalized == null) {
            status = "PENDENTE_IDENTIFICADOR";
            issues.add("email_oficial_nao_informado");
        } else if (InstitutionalOfficialIdentifierResolverSupport.isOfficialInstitutionalDomain(normalized)) {
            status = "IDENTIFICADOR_LOCAL_VALIDO";
            fundamentos.add("email_oficial_com_dominio_publico");
        } else {
            status = "REQUER_HOMOLOGACAO_HUMANA";
            issues.add("email_oficial_com_dominio_nao_governamental");
        }
        return new InstitutionalOfficialIdentifierCheck(
                "EMAIL_OFICIAL",
                "Canal oficial de segurança",
                "CANAL_OFICIAL",
                metadata.emailContatoSeguranca(),
                normalized,
                status,
                true,
                true,
                false,
                connector.connectorStatus(),
                null,
                List.of(),
                List.copyOf(issues),
                List.copyOf(fundamentos),
                null
        );
    }

    private boolean materialEvidenceReady(List<InstitutionalOfficialIdentifierCheck> checks) {
        return checks.stream()
                .filter(InstitutionalOfficialIdentifierCheck::applicable)
                .filter(InstitutionalOfficialIdentifierCheck::requiredForRecognition)
                .allMatch(check -> switch (check.status()) {
                    case "APTO_VERIFICACAO_REMOTA", "IDENTIFICADOR_LOCAL_VALIDO", "HERDADO_DA_INSTITUICAO_PAI" -> true;
                    default -> false;
                });
    }

    private String resolveOverallStatus(List<InstitutionalOfficialIdentifierCheck> checks) {
        boolean invalid = checks.stream().anyMatch(check -> "IDENTIFICADOR_INVALIDO".equals(check.status()));
        boolean pending = checks.stream().filter(InstitutionalOfficialIdentifierCheck::applicable).anyMatch(check -> "PENDENTE_IDENTIFICADOR".equals(check.status()));
        boolean remoteReady = checks.stream().anyMatch(InstitutionalOfficialIdentifierCheck::readyForRemoteLookup);
        if (invalid) {
            return "IDENTIFICADORES_INVALIDOS";
        }
        if (pending) {
            return "PENDENTE_IDENTIFICADORES_MATERIAIS";
        }
        if (remoteReady) {
            return "APTO_VALIDACAO_MATERIAL_REMOTA";
        }
        return "BASE_MATERIAL_LOCAL_FORMADA";
    }

    private static String sanitizeSignal(String value) {
        return value == null ? null : value.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    private record SubjectMetadata(
            InstitutionalOrganizationScope organizationScope,
            String scopeCode,
            String orgaoSigla,
            String unidadeCodigo,
            String comarca,
            String uf,
            String cnpj,
            String esferaAdministrativa,
            List<String> abrangenciasTerritoriais,
            String dominioInstitucional,
            String emailContatoSeguranca
    ) {
        static SubjectMetadata fromAffiliation(InstitutionalAffiliation affiliation) {
            return new SubjectMetadata(
                    affiliation.organizationScope(),
                    affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    affiliation.orgaoSigla(),
                    affiliation.unidadeCodigo(),
                    affiliation.comarca(),
                    affiliation.uf(),
                    affiliation.cnpj(),
                    affiliation.esferaAdministrativa(),
                    affiliation.abrangenciasTerritoriais(),
                    affiliation.dominioInstitucional(),
                    affiliation.emailContatoSeguranca()
            );
        }

        static SubjectMetadata fromRequest(InstitutionalAffiliationRequest request) {
            String email = request.documentos().stream()
                    .filter(Objects::nonNull)
                    .map(com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument::referenciaExterna)
                    .filter(Objects::nonNull)
                    .filter(reference -> reference.contains("@"))
                    .findFirst()
                    .orElse(null);
            return new SubjectMetadata(
                    request.organizationScope(),
                    request.organizationScope() == null ? null : request.organizationScope().name(),
                    request.orgaoSigla(),
                    request.unidadeCodigo(),
                    request.comarca(),
                    request.uf(),
                    request.cnpj(),
                    request.esferaAdministrativa(),
                    request.abrangenciasTerritoriais(),
                    request.dominioInstitucional(),
                    email
            );
        }
    }
}
