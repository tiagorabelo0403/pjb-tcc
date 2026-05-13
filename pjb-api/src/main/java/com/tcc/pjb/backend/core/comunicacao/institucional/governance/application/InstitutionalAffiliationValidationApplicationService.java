package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalAffiliationValidationStateRepository;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAffiliationValidationApplicationService {

    private final InstitutionalAffiliationValidationStateRepository repository;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;

    public InstitutionalAffiliationValidationApplicationService(InstitutionalAffiliationValidationStateRepository repository,
                                                               InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService) {
        this.repository = Objects.requireNonNull(repository);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
    }

    public InstitutionalAffiliationValidationReport validar(InstitutionalAffiliationRequest request) {
        Objects.requireNonNull(request);
        Map<String, List<InstitutionalAffiliationDocument>> docs = request.documentos().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(item -> normalize(item.tipo()), Collectors.toList()));
        ArrayList<InstitutionalAffiliationValidationFinding> findings = new ArrayList<>();
        LinkedHashSet<String> requiredTypes = requiredDocumentTypes(request);
        boolean documentosObrigatoriosPresentes = true;
        for (String requiredType : requiredTypes) {
            List<InstitutionalAffiliationDocument> matching = docs.getOrDefault(requiredType, List.of());
            if (matching.isEmpty()) {
                documentosObrigatoriosPresentes = false;
                findings.add(finding("DOC_" + requiredType + "_AUSENTE", InstitutionalRiskSeverity.CRITICA, true,
                        "Documento obrigatório da adesão institucional não foi apresentado: " + requiredType,
                        List.of("requestId=" + request.requestId(), "scope=" + scopeName(request))));
                continue;
            }
            boolean anyValidated = matching.stream().anyMatch(InstitutionalAffiliationDocument::validado);
            if (!anyValidated) {
                documentosObrigatoriosPresentes = false;
                findings.add(finding("DOC_" + requiredType + "_NAO_VALIDADO", InstitutionalRiskSeverity.ALTA, true,
                        "Documento obrigatório existe, mas ainda não foi marcado como validado: " + requiredType,
                        List.of("requestId=" + request.requestId(), "scope=" + scopeName(request))));
            }
        }

        boolean representativeValid = request.representanteUsuarioId() != null
                && notBlank(request.representanteNome())
                && request.representativeRole() != null
                && notBlank(request.autoridadeAderenteCargo());
        if (!representativeValid) {
            findings.add(finding("REPRESENTANTE_INCOMPLETO", InstitutionalRiskSeverity.CRITICA, true,
                    "Representante, cargo aderente ou papel de nomeação estão incompletos para a adesão institucional.",
                    List.of("representanteUserId=" + request.representanteUsuarioId(), "representativeRole=" + String.valueOf(request.representativeRole()))));
        }

        boolean domainValid = validInstitutionalDomain(request.dominioInstitucional());
        if (!domainValid) {
            findings.add(finding("DOMINIO_INSTITUCIONAL_INVALIDO", InstitutionalRiskSeverity.ALTA, true,
                    "Domínio institucional não passou na validação material mínima exigida para adesão delegada.",
                    List.of("dominio=" + String.valueOf(request.dominioInstitucional()), "orgao=" + request.orgaoSigla())));
        }

        boolean certificateMaterialValid = !request.requerCertificadoICP() || hasDoc(docs, "CERTIFICADO_ICP") || hasDoc(docs, "CERTIFICADO_DIGITAL_INSTITUCIONAL");
        if (!certificateMaterialValid) {
            findings.add(finding("CERTIFICADO_ICP_AUSENTE", InstitutionalRiskSeverity.CRITICA, true,
                    "A adesão exige certificado ICP, mas não há documento material correspondente.",
                    List.of("scope=" + scopeName(request), "orgao=" + request.orgaoSigla())));
        }

        boolean trustChainValid = !request.requerCertificadoICP() || hasDoc(docs, "CADEIA_CONFIANCA_ICP") || hasDoc(docs, "CADEIA_CONFIANCA");
        if (!trustChainValid) {
            findings.add(finding("CADEIA_CONFIANCA_ICP_AUSENTE", InstitutionalRiskSeverity.ALTA, true,
                    "A cadeia de confiança do certificado institucional ainda não foi comprovada.",
                    List.of("orgao=" + request.orgaoSigla(), "unidade=" + request.unidadeCodigo())));
        }

        if (request.requerDuplaAprovacaoAdministrador() && request.bootstrapAdministrators().size() < 2) {
            findings.add(finding("DUPLA_ADMINISTRACAO_INICIAL_NAO_ATINGIDA", InstitutionalRiskSeverity.CRITICA, true,
                    "A adesão institucional exige ao menos dois administradores mestres iniciais nomeados pelo órgão.",
                    List.of("adminsIniciais=" + request.bootstrapAdministrators().size())));
        }

        if (request.canaisHabilitados().isEmpty()) {
            findings.add(finding("SEM_CANAIS_HABILITADOS", InstitutionalRiskSeverity.MEDIA, false,
                    "Nenhum canal técnico foi habilitado na adesão; a ativação operacional ficará limitada.",
                    List.of("orgao=" + request.orgaoSigla())));
        }
        if (request.politicaCiencia().isEmpty()) {
            findings.add(finding("POLITICA_CIENCIA_AUSENTE", InstitutionalRiskSeverity.MEDIA, false,
                    "A política de ciência institucional não foi explicitada na adesão.",
                    List.of("unidade=" + request.unidadeCodigo())));
        }
        if (request.sla().isEmpty()) {
            findings.add(finding("SLA_AUSENTE", InstitutionalRiskSeverity.BAIXA, false,
                    "A adesão não trouxe SLA formal; a recertificação deve exigir complemento.",
                    List.of("scope=" + scopeName(request))));
        }
        if (request.regrasFallback().isEmpty()) {
            findings.add(finding("FALLBACK_AUSENTE", InstitutionalRiskSeverity.MEDIA, false,
                    "A adesão não informa fallback institucional para indisponibilidade ou exceção operacional.",
                    List.of("scope=" + scopeName(request))));
        }
        if (request.conveniosIntegracoes().isEmpty()) {
            findings.add(finding("INTEGRACOES_NAO_MAPEADAS", InstitutionalRiskSeverity.BAIXA, false,
                    "Não foram mapeados convênios ou integrações; o órgão ficará em regime estritamente interno até completar a governança.",
                    List.of("orgao=" + request.orgaoSigla())));
        }
        if (isHighCriticalScope(request.organizationScope()) && request.ramosMateriais().isEmpty()) {
            findings.add(finding("RAMO_MATERIAL_AUSENTE", InstitutionalRiskSeverity.ALTA, true,
                    "Escopo institucional crítico exige ramo material explícito para evitar abertura territorial ou funcional indevida.",
                    List.of("scope=" + scopeName(request))));
        }
        if (request.abrangenciasTerritoriais().isEmpty()) {
            findings.add(finding("ABRANGENCIA_TERRITORIAL_AUSENTE", InstitutionalRiskSeverity.MEDIA, false,
                    "A abrangência territorial não foi explicitada; isso pode gerar roteamento indevido.",
                    List.of("orgao=" + request.orgaoSigla())));
        }

        boolean apt = findings.stream().noneMatch(InstitutionalAffiliationValidationFinding::blocking);
        InstitutionalAffiliationValidationReport report = new InstitutionalAffiliationValidationReport(
                UUID.randomUUID().toString(),
                request.requestId(),
                scopeName(request),
                request.orgaoSigla(),
                request.unidadeCodigo(),
                apt,
                documentosObrigatoriosPresentes,
                representativeValid,
                domainValid,
                certificateMaterialValid,
                trustChainValid,
                List.copyOf(findings),
                buildFundamentos(request, requiredTypes, findings),
                Instant.now(),
                null
        );
        return repository.save(report);
    }

    public InstitutionalAffiliationValidationReport validarOuBuscar(String requestId, java.util.function.Supplier<InstitutionalAffiliationRequest> requestLoader) {
        Optional<InstitutionalAffiliationValidationReport> current = repository.findLatestByRequestId(requestId);
        if (current.isPresent()) {
            return current.get();
        }
        return validar(requestLoader.get());
    }

    public InstitutionalAffiliationValidationReport assertAptaParaHomologacao(InstitutionalAffiliationRequest request) {
        InstitutionalAffiliationValidationReport report = validar(request);
        if (!report.aptaParaHomologacao()) {
            throw new IllegalStateException("Solicitação de adesão institucional não está apta para homologação: "
                    + report.findings().stream().filter(InstitutionalAffiliationValidationFinding::blocking).map(InstitutionalAffiliationValidationFinding::message).collect(Collectors.joining(" | ")));
        }
        return report;
    }

    public Optional<InstitutionalAffiliationValidationReport> buscarUltimo(String requestId) {
        return repository.findLatestByRequestId(requestId);
    }

    private boolean isHighCriticalScope(InstitutionalOrganizationScope scope) {
        if (scope == null) {
            return false;
        }
        return switch (scope) {
            case FORUM, PROMOTORIA, NUCLEO_DEFENSORIA, PROCURADORIA_PUBLICA, DELEGACIA, POLICIA_PENAL, UNIDADE_PRISIONAL, CENTRAL_MANDADOS -> true;
            default -> false;
        };
    }

    private boolean validInstitutionalDomain(String raw) {
        if (!notBlank(raw)) {
            return false;
        }
        String domain = raw.trim().toLowerCase(Locale.ROOT)
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");
        int slash = domain.indexOf('/');
        if (slash >= 0) {
            domain = domain.substring(0, slash);
        }
        return domain.endsWith(".gov.br")
                || domain.endsWith(".jus.br")
                || domain.endsWith(".leg.br")
                || domain.endsWith(".mp.br")
                || domain.endsWith(".def.br")
                || domain.endsWith(".pol.br")
                || domain.contains("mp") && domain.endsWith(".br")
                || domain.contains("defensoria") && domain.endsWith(".br")
                || domain.contains("procuradoria") && domain.endsWith(".br")
                || domain.contains("tribunal") && domain.endsWith(".br")
                || domain.contains("tj") && domain.endsWith(".jus.br");
    }

    private LinkedHashSet<String> requiredDocumentTypes(InstitutionalAffiliationRequest request) {
        InstitutionalOrganizationScope scope = request.organizationScope() == null
                ? blueprintCatalogApplicationService.inferScope(request.destinatarioKind(), request.unidadeCodigo(), request.orgaoSigla(), request.unidadeNome())
                : request.organizationScope();
        LinkedHashSet<String> required = new LinkedHashSet<>();
        required.add("ATO_DESIGNACAO_REPRESENTANTE");
        required.add("COMPROVANTE_DOMINIO_INSTITUCIONAL");
        required.add("ATO_CRIACAO_UNIDADE");
        if (request.requerCertificadoICP()) {
            required.add("CERTIFICADO_ICP");
            required.add("CADEIA_CONFIANCA_ICP");
        }
        if (scope != null) {
            switch (scope) {
                case FORUM, SECRETARIA_UNIDADE_JUDICIARIA, CENTRAL_AUDIENCIAS, CENTRAL_MANDADOS -> required.add("ATO_DIRETORIA_UNIDADE");
                case PROMOTORIA, NUCLEO_DEFENSORIA, PROCURADORIA_PUBLICA -> required.add("ATO_NOMEACAO_CHEFIA_INSTITUCIONAL");
                case DELEGACIA, POLICIA_PENAL, UNIDADE_PRISIONAL -> required.add("ATO_DESIGNACAO_AUTORIDADE_OPERACIONAL");
                case CEJUSC, CONTADORIA, EQUIPE_PSICOSSOCIAL, CARTORIO_INTEGRADO, ORGAO_TECNICO_CONVENIADO, CONSELHO_TUTELAR -> required.add("ATO_DESIGNACAO_COORDENACAO");
                case COOPERACAO_JUDICIAL_EXTERNA, GENERICO_INSTITUCIONAL -> required.add("DOCUMENTO_EQUIVALENTE_DE_COMPETENCIA");
            }
        }
        if (request.requerDuplaAprovacaoAdministrador()) {
            required.add("ROL_ADMINISTRADORES_INICIAIS");
        }
        return required;
    }

    private boolean hasDoc(Map<String, List<InstitutionalAffiliationDocument>> docs, String type) {
        return docs.getOrDefault(type, List.of()).stream().anyMatch(doc -> doc.validado() || notBlank(doc.hashDocumento()) || notBlank(doc.referenciaExterna()));
    }

    private InstitutionalAffiliationValidationFinding finding(String code,
                                                              InstitutionalRiskSeverity severity,
                                                              boolean blocking,
                                                              String message,
                                                              List<String> evidences) {
        return new InstitutionalAffiliationValidationFinding(code, severity, blocking, message, evidences);
    }

    private String scopeName(InstitutionalAffiliationRequest request) {
        InstitutionalOrganizationScope scope = request.organizationScope() == null
                ? blueprintCatalogApplicationService.inferScope(request.destinatarioKind(), request.unidadeCodigo(), request.orgaoSigla(), request.unidadeNome())
                : request.organizationScope();
        return scope == null ? "GENERICO_INSTITUCIONAL" : scope.name();
    }

    private List<String> buildFundamentos(InstitutionalAffiliationRequest request,
                                          LinkedHashSet<String> requiredTypes,
                                          List<InstitutionalAffiliationValidationFinding> findings) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("validacao_documental_da_adesao");
        fundamentos.add("escopo=" + scopeName(request));
        fundamentos.add("documentos_obrigatorios=" + String.join(",", requiredTypes));
        fundamentos.add("findings_total=" + findings.size());
        fundamentos.add("bloqueios_total=" + findings.stream().filter(InstitutionalAffiliationValidationFinding::blocking).count());
        if (request.fundamentos() != null) {
            fundamentos.addAll(request.fundamentos());
        }
        return List.copyOf(fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(Predicate.not(String::isBlank)).distinct().toList());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

}
