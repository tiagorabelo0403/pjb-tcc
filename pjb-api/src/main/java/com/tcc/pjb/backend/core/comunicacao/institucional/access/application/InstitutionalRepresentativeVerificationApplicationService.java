package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRepresentativeVerification;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalRepresentativeVerificationApplicationService {

    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalAffiliationValidationApplicationService validationApplicationService;
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService;

    public InstitutionalRepresentativeVerificationApplicationService(InstitutionalAffiliationRequestStateRepository requestRepository,
                                                                    InstitutionalAffiliationValidationApplicationService validationApplicationService,
                                                                    InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService) {
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.validationApplicationService = Objects.requireNonNull(validationApplicationService);
        this.approvalTrailApplicationService = Objects.requireNonNull(approvalTrailApplicationService);
    }

    public InstitutionalRepresentativeVerification verificar(String requestId) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("solicitacao_adesao_institucional_nao_encontrada"));
        InstitutionalAffiliationValidationReport validation = validationApplicationService.validarOuBuscar(requestId, () -> request);
        InstitutionalAffiliationApprovalTrail trail = approvalTrailApplicationService.buscarUltima(requestId).orElse(null);
        Map<String, List<InstitutionalAffiliationDocument>> docs = request.documentos().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(item -> normalize(item.tipo()), Collectors.toList()));
        boolean representativeIdentityComplete = request.representanteUsuarioId() != null
                && notBlank(request.representanteNome())
                && request.representativeRole() != null
                && notBlank(request.autoridadeAderenteCargo());
        boolean representativeDocumentValidated = hasValidatedDoc(docs, "ATO_DESIGNACAO_REPRESENTANTE")
                || hasValidatedDoc(docs, "ATO_NOMEACAO_CHEFIA_INSTITUCIONAL")
                || hasValidatedDoc(docs, "ATO_DIRETORIA_UNIDADE")
                || hasValidatedDoc(docs, "ATO_DESIGNACAO_AUTORIDADE_OPERACIONAL")
                || hasValidatedDoc(docs, "ATO_DESIGNACAO_COORDENACAO");
        ArrayList<String> findings = new ArrayList<>();
        if (!representativeIdentityComplete) {
            findings.add("representante_institucional_incompleto");
        }
        if (!representativeDocumentValidated) {
            findings.add("documento_material_do_representante_nao_validado");
        }
        if (!validation.dominioInstitucionalValidado()) {
            findings.add("dominio_institucional_nao_validado");
        }
        if (!validation.certificadoMaterialValidado()) {
            findings.add("material_certificado_nao_validado");
        }
        if (!validation.cadeiaConfiancaValidada()) {
            findings.add("cadeia_confianca_nao_validada");
        }
        if (trail == null) {
            findings.add("trilha_duas_chaves_nao_localizada");
        } else {
            if (!trail.representativeSigned()) {
                findings.add("assinatura_do_representante_pendente");
            }
            if (!Boolean.TRUE.equals(trail.approvedByPjb())) {
                findings.add("homologacao_pjb_pendente");
            }
        }
        boolean dualKeySatisfied = trail != null && trail.dualKeySatisfied();
        boolean homologationReady = validation.aptaParaHomologacao()
                && representativeIdentityComplete
                && representativeDocumentValidated
                && dualKeySatisfied
                && trail != null
                && Boolean.TRUE.equals(trail.approvedByPjb());
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("adesao_institucional_delegada");
        fundamentos.add("duas_chaves_representante_e_pjb");
        fundamentos.add("scope=" + validation.organizationScope());
        fundamentos.add("orgao=" + request.orgaoSigla());
        fundamentos.add("unidade=" + request.unidadeCodigo());
        fundamentos.addAll(validation.fundamentos());
        if (trail != null) {
            fundamentos.addAll(trail.fundamentos());
        }
        return new InstitutionalRepresentativeVerification(
                request.requestId(),
                request.representanteUsuarioId(),
                request.representanteNome(),
                request.representativeRole() == null ? null : request.representativeRole().name(),
                request.autoridadeAderenteCargo(),
                representativeIdentityComplete,
                representativeDocumentValidated,
                validation.dominioInstitucionalValidado(),
                validation.certificadoMaterialValidado(),
                validation.cadeiaConfiancaValidada(),
                dualKeySatisfied,
                homologationReady,
                List.copyOf(findings),
                fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList(),
                Instant.now());
    }

    public Optional<InstitutionalRepresentativeVerification> buscarSeExistir(String requestId) {
        return requestRepository.findByRequestId(requestId).map(item -> verificar(requestId));
    }

    private boolean hasValidatedDoc(Map<String, List<InstitutionalAffiliationDocument>> docs, String type) {
        return docs.getOrDefault(normalize(type), List.of()).stream().anyMatch(item -> item.validado() || notBlank(item.hashDocumento()) || notBlank(item.referenciaExterna()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
