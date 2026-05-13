package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.service.institutional.architecture.InstitutionalPublicRecognitionPolicyService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPublicRecognitionGateApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalPublicRecognitionPolicyService policyService;

    public InstitutionalPublicRecognitionGateApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                               InstitutionalAffiliationRequestStateRepository requestRepository,
                                                               InstitutionalPublicRecognitionPolicyService policyService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.policyService = Objects.requireNonNull(policyService);
    }

    public AdminInstitutionalPublicRecognitionResponse avaliarAfiliacao(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        return avaliarAfiliacao(affiliation);
    }

    public AdminInstitutionalPublicRecognitionResponse avaliarSolicitacao(String requestId) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("solicitacao_adesao_institucional_nao_encontrada"));
        return avaliarSolicitacao(request);
    }

    public RecognitionInput inspecionarAfiliacao(String affiliationId) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        return inspecionarAfiliacao(affiliation);
    }

    public RecognitionInput inspecionarSolicitacao(String requestId) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("solicitacao_adesao_institucional_nao_encontrada"));
        return inspecionarSolicitacao(request);
    }

    public AdminInstitutionalPublicRecognitionResponse avaliarAfiliacao(InstitutionalAffiliation affiliation) {
        RecognitionInput input = inspecionarAfiliacao(affiliation);
        return policyService.assess(
                input.scope(),
                input.officialCatalogMatch(),
                input.publicCnpjActive(),
                input.publicNatureCompatible(),
                input.officialEmailChannel(),
                input.officialDomain(),
                input.legalActPresent(),
                input.territorialMatch(),
                input.representativeGovBrGold(),
                input.representativeIcpBrasilValid(),
                input.subordinateUnitWithoutOwnCnpj(),
                input.parentInstitutionRecognized()
        );
    }

    public AdminInstitutionalPublicRecognitionResponse avaliarSolicitacao(InstitutionalAffiliationRequest request) {
        RecognitionInput input = inspecionarSolicitacao(request);
        return policyService.assess(
                input.scope(),
                input.officialCatalogMatch(),
                input.publicCnpjActive(),
                input.publicNatureCompatible(),
                input.officialEmailChannel(),
                input.officialDomain(),
                input.legalActPresent(),
                input.territorialMatch(),
                input.representativeGovBrGold(),
                input.representativeIcpBrasilValid(),
                input.subordinateUnitWithoutOwnCnpj(),
                input.parentInstitutionRecognized()
        );
    }

    public RecognitionInput inspecionarAfiliacao(InstitutionalAffiliation affiliation) {
        String normalizedDomain = normalizeDomain(affiliation.dominioInstitucional());
        boolean validCnpj = isValidCnpj(affiliation.cnpj());
        boolean subordinateUnit = isSubordinateUnit(affiliation.organizationScope(), affiliation.unidadeCodigo(), affiliation.unidadeNome(), validCnpj);
        boolean officialAnchorDocument = hasOfficialAnchor(List.of(), affiliation.fundamentos());
        boolean judiciaryCatalog = isJudiciaryCatalog(affiliation.destinatarioKind(), affiliation.organizationScope(), affiliation.orgaoSigla(), affiliation.unidadeCodigo());
        boolean federalCatalog = isFederalExecutiveCatalog(affiliation.organizationScope(), affiliation.esferaAdministrativa(), normalizedDomain);
        boolean officialCatalogMatch = officialAnchorDocument || judiciaryCatalog || federalCatalog;
        boolean representativeIcp = hasIcpEvidence(List.of(), affiliation.fundamentos())
                || affiliation.requerCertificadoICP()
                || trustAtLeast(affiliation.trustFloor(), InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO);
        boolean representativeGovBr = hasGovBrEvidence(List.of(), affiliation.fundamentos())
                || (affiliation.representanteUsuarioId() != null && representativeIcp)
                || trustAtLeast(affiliation.trustFloor(), InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO);
        return new RecognitionInput(
                resolveScope(affiliation.destinatarioKind(), affiliation.organizationScope(), subordinateUnit, hasFormalIntegration(affiliation.conveniosIntegracoes())),
                officialCatalogMatch,
                validCnpj,
                isPublicNatureCompatible(affiliation.esferaAdministrativa(), affiliation.destinatarioKind(), affiliation.organizationScope(), subordinateUnit, hasFormalIntegration(affiliation.conveniosIntegracoes())),
                normalizedDomain != null,
                isOfficialInstitutionalDomain(normalizedDomain),
                hasLegalAct(affiliation.autoridadeAderenteCargo(), List.of(), affiliation.fundamentos(), affiliation.conveniosIntegracoes(), subordinateUnit),
                hasTerritorialMatch(affiliation.uf(), affiliation.comarca(), affiliation.abrangenciasTerritoriais(), affiliation.unidadeCodigo()),
                representativeGovBr,
                representativeIcp,
                subordinateUnit,
                officialCatalogMatch || hasRecognizedParent(affiliation.orgaoSigla(), affiliation.organizationScope())
        );
    }

    public RecognitionInput inspecionarSolicitacao(InstitutionalAffiliationRequest request) {
        String normalizedDomain = normalizeDomain(request.dominioInstitucional());
        boolean validCnpj = isValidCnpj(request.cnpj());
        boolean subordinateUnit = isSubordinateUnit(request.organizationScope(), request.unidadeCodigo(), request.unidadeNome(), validCnpj);
        boolean officialAnchorDocument = hasOfficialAnchor(request.documentos(), request.fundamentos());
        boolean judiciaryCatalog = isJudiciaryCatalog(request.destinatarioKind(), request.organizationScope(), request.orgaoSigla(), request.unidadeCodigo());
        boolean federalCatalog = isFederalExecutiveCatalog(request.organizationScope(), request.esferaAdministrativa(), normalizedDomain);
        boolean officialCatalogMatch = officialAnchorDocument || judiciaryCatalog || federalCatalog;
        boolean representativeIcp = hasIcpEvidence(request.documentos(), request.fundamentos())
                || request.requerCertificadoICP()
                || trustAtLeast(request.trustFloorProposto(), InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO);
        boolean representativeGovBr = hasGovBrEvidence(request.documentos(), request.fundamentos())
                || (request.representanteUsuarioId() != null && representativeIcp)
                || trustAtLeast(request.trustFloorProposto(), InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO);
        boolean officialEmailChannel = normalizedDomain != null || hasOfficialEmailEvidence(request.documentos(), request.fundamentos());
        return new RecognitionInput(
                resolveScope(request.destinatarioKind(), request.organizationScope(), subordinateUnit, hasFormalIntegration(request.conveniosIntegracoes())),
                officialCatalogMatch,
                validCnpj,
                isPublicNatureCompatible(request.esferaAdministrativa(), request.destinatarioKind(), request.organizationScope(), subordinateUnit, hasFormalIntegration(request.conveniosIntegracoes())),
                officialEmailChannel,
                isOfficialInstitutionalDomain(normalizedDomain),
                hasLegalAct(request.autoridadeAderenteCargo(), request.documentos(), request.fundamentos(), request.conveniosIntegracoes(), subordinateUnit),
                hasTerritorialMatch(request.uf(), request.comarca(), request.abrangenciasTerritoriais(), request.unidadeCodigo()),
                representativeGovBr,
                representativeIcp,
                subordinateUnit,
                officialCatalogMatch || hasRecognizedParent(request.orgaoSigla(), request.organizationScope())
        );
    }

    private static String resolveScope(DestinatarioInstitucionalKind kind,
                                       InstitutionalOrganizationScope scope,
                                       boolean subordinateUnit,
                                       boolean formalIntegration) {
        if (formalIntegration && !isJudiciaryScope(scope, kind)) {
            return "SISTEMA_CONVENIADO";
        }
        if (subordinateUnit) {
            return "SUBUNIDADE_VINCULADA";
        }
        if (isJudiciaryScope(scope, kind)) {
            return "JUDICIARIO_CNJ";
        }
        if (isFederalScope(scope) || containsAny(scope == null ? null : scope.name(), "FEDERAL")) {
            return "EXECUTIVO_FEDERAL_SIORG";
        }
        return "ESTADUAL_MUNICIPAL";
    }

    private static boolean isJudiciaryScope(InstitutionalOrganizationScope scope, DestinatarioInstitucionalKind kind) {
        return isJudiciaryCatalog(kind, scope, null, null);
    }

    private static boolean isJudiciaryCatalog(DestinatarioInstitucionalKind kind,
                                              InstitutionalOrganizationScope scope,
                                              String orgaoSigla,
                                              String unidadeCodigo) {
        if (kind != null && containsAny(kind.name(), "JUIZ", "TRIBUNAL", "GABINETE", "VARA", "FORUM", "SECRETARIA")) {
            return true;
        }
        if (scope != null && containsAny(scope.name(), "TRIBUNAL", "FORUM", "VARA", "COMARCA", "SECAO", "SUBSECAO", "ZONA_ELEITORAL", "JUIZADO", "POSTO")) {
            return true;
        }
        return containsAny(orgaoSigla, "TJ", "TRF", "TRT", "TRE", "STJ", "STF", "TST", "TSE", "STM") || containsAny(unidadeCodigo, "VARA", "FORUM", "COMARCA", "TRF", "TRT", "TRE", "TJ", "ZONA");
    }

    private static boolean isFederalExecutiveCatalog(InstitutionalOrganizationScope scope,
                                                     String esferaAdministrativa,
                                                     String normalizedDomain) {
        return isFederalScope(scope)
                || containsAny(esferaAdministrativa, "FEDERAL", "UNIAO")
                || endsWithAny(normalizedDomain, ".gov.br", ".jus.br", ".mp.br", ".def.br");
    }

    private static boolean isFederalScope(InstitutionalOrganizationScope scope) {
        return scope != null && containsAny(scope.name(), "FEDERAL", "NACIONAL", "SUPERIOR");
    }

    private static boolean isSubordinateUnit(InstitutionalOrganizationScope scope,
                                             String unidadeCodigo,
                                             String unidadeNome,
                                             boolean validCnpj) {
        if (validCnpj) {
            return false;
        }
        return (scope != null && containsAny(scope.name(), "VARA", "FORUM", "COMARCA", "SECAO", "SUBSECAO", "ZONA", "POSTO", "UNIDADE"))
                || containsAny(unidadeCodigo, "VARA", "FORUM", "COMARCA", "SECAO", "SUBSECAO", "ZONA", "POSTO", "UNIDADE", "DELEGACIA", "IML", "CONSELHO")
                || containsAny(unidadeNome, "vara", "fórum", "forum", "comarca", "subseção", "subsecao", "zona eleitoral", "posto", "delegacia", "conselho tutelar", "iml");
    }

    private static boolean isPublicNatureCompatible(String esferaAdministrativa,
                                                    DestinatarioInstitucionalKind kind,
                                                    InstitutionalOrganizationScope scope,
                                                    boolean subordinateUnit,
                                                    boolean formalIntegration) {
        if (formalIntegration) {
            return true;
        }
        if (subordinateUnit) {
            return true;
        }
        if (containsAny(esferaAdministrativa, "FEDERAL", "ESTADUAL", "MUNICIPAL", "DISTRITAL", "PUBLICA", "PÚBLICA", "JUDICIARIO", "JUDICIÁRIO")) {
            return true;
        }
        if (kind != null && containsAny(kind.name(), "MINISTERIO_PUBLICO", "DEFENSORIA", "PROCURADORIA", "POLICIA", "DELEGACIA", "CONSELHO_TUTELAR", "TRIBUNAL", "VARA", "FORUM", "SECRETARIA", "JUIZ")) {
            return true;
        }
        return scope != null && containsAny(scope.name(), "TRIBUNAL", "VARA", "FORUM", "COMARCA", "FEDERAL", "ESTADUAL", "MUNICIPAL", "UNIDADE", "SECAO", "SUBSECAO", "ZONA");
    }

    private static boolean hasLegalAct(String autoridadeAderenteCargo,
                                       List<InstitutionalAffiliationDocument> documentos,
                                       List<String> fundamentos,
                                       List<String> conveniosIntegracoes,
                                       boolean subordinateUnit) {
        return notBlank(autoridadeAderenteCargo)
                || subordinateUnit
                || hasKeywordDocument(documentos, "ATO", "PORTARIA", "LEI", "DECRETO", "RESOLUCAO", "RESOLUÇÃO", "REGIMENTO", "CONVENIO", "CONVÊNIO")
                || containsAnyJoined(fundamentos, "ato_legal", "portaria", "lei", "decreto", "resolucao", "resolução", "regimento", "base_legal", "convenio", "convênio")
                || containsAnyJoined(conveniosIntegracoes, "convenio", "convênio", "acordo", "integracao_formal");
    }

    private static boolean hasTerritorialMatch(String uf,
                                               String comarca,
                                               List<String> abrangenciasTerritoriais,
                                               String unidadeCodigo) {
        return notBlank(uf) || notBlank(comarca) || (abrangenciasTerritoriais != null && !abrangenciasTerritoriais.isEmpty()) || containsAny(unidadeCodigo, "UF", "COMARCA", "SECAO", "SUBSECAO", "ZONA");
    }

    private static boolean hasRecognizedParent(String orgaoSigla, InstitutionalOrganizationScope scope) {
        return notBlank(orgaoSigla) && (scope == null || !containsAny(scope.name(), "CONVENIADO"));
    }

    private static boolean hasFormalIntegration(List<String> conveniosIntegracoes) {
        return containsAnyJoined(conveniosIntegracoes, "convenio", "convênio", "acordo", "integracao", "cooperação", "cooperacao");
    }

    private static boolean hasOfficialAnchor(List<InstitutionalAffiliationDocument> documentos, List<String> fundamentos) {
        return hasKeywordDocument(documentos, "DATAJUD", "CNJ", "SIORG", "RECEITA", "CNPJ", "IBGE", "REDESIM")
                || containsAnyJoined(fundamentos, "datajud", "cnj", "siorg", "receita", "cnpj", "ibge", "redesim", "anchor_official", "ancora_oficial", "âncora_oficial");
    }

    private static boolean hasGovBrEvidence(List<InstitutionalAffiliationDocument> documentos, List<String> fundamentos) {
        return hasKeywordDocument(documentos, "GOVBR", "GOV.BR", "SELO_OURO") || containsAnyJoined(fundamentos, "govbr", "gov.br", "ouro", "selo_ouro");
    }

    private static boolean hasIcpEvidence(List<InstitutionalAffiliationDocument> documentos, List<String> fundamentos) {
        return hasKeywordDocument(documentos, "ICP", "ICP-BRASIL", "E-CPF", "CERTIFICADO", "VALIDAR") || containsAnyJoined(fundamentos, "icp", "icp_brasil", "certificado", "e-cpf", "ecpf", "validar");
    }

    private static boolean hasOfficialEmailEvidence(List<InstitutionalAffiliationDocument> documentos, List<String> fundamentos) {
        return hasKeywordDocument(documentos, "EMAIL", "DOMINIO", "DOMÍNIO") || containsAnyJoined(fundamentos, "email_institucional", "dominio_institucional", "domínio_institucional");
    }

    private static boolean hasKeywordDocument(List<InstitutionalAffiliationDocument> documentos, String... terms) {
        if (documentos == null || documentos.isEmpty()) {
            return false;
        }
        for (InstitutionalAffiliationDocument documento : documentos) {
            if (documento == null) {
                continue;
            }
            String joined = join(documento.codigo(), documento.nome(), documento.tipo(), documento.referenciaExterna(), documento.hashDocumento());
            if (containsAny(joined, terms)) {
                return documento.validado() || notBlank(documento.referenciaExterna());
            }
        }
        return false;
    }

    private static boolean containsAnyJoined(List<String> values, String... terms) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (containsAny(value, terms)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String raw, String... terms) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = normalize(raw);
        for (String term : terms) {
            if (normalized.contains(normalize(term))) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithAny(String raw, String... suffixes) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        }
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        int at = normalized.indexOf('@');
        if (at >= 0) {
            normalized = normalized.substring(at + 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean isOfficialInstitutionalDomain(String normalizedDomain) {
        return endsWithAny(normalizedDomain, ".gov.br", ".jus.br", ".mp.br", ".def.br", ".leg.br", ".tc.br");
    }

    private static boolean trustAtLeast(InstitutionalTrustLevel level, InstitutionalTrustLevel minimum) {
        return level != null && minimum != null && level.atende(minimum);
    }

    private static boolean notBlank(String raw) {
        return raw != null && !raw.isBlank();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Ô', 'O').replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replace('á', 'a').replace('à', 'a').replace('ã', 'a').replace('â', 'a')
                .replace('é', 'e').replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o').replace('ô', 'o').replace('õ', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c')
                .toLowerCase(Locale.ROOT);
    }

    private static String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private static boolean isValidCnpj(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 14 || digits.chars().distinct().count() == 1) {
            return false;
        }
        return computeCnpjDigit(digits, 12) == Character.getNumericValue(digits.charAt(12))
                && computeCnpjDigit(digits, 13) == Character.getNumericValue(digits.charAt(13));
    }

    private static int computeCnpjDigit(String digits, int position) {
        int[] weights = position == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public record RecognitionInput(
            String scope,
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
            boolean parentInstitutionRecognized
    ) {
    }
}
