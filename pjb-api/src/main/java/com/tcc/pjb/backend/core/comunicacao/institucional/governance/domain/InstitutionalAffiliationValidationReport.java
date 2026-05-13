package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalAffiliationValidationReport(
        String validationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        boolean aptaParaHomologacao,
        boolean documentosObrigatoriosPresentes,
        boolean representanteValidado,
        boolean dominioInstitucionalValidado,
        boolean certificadoMaterialValidado,
        boolean cadeiaConfiancaValidada,
        List<InstitutionalAffiliationValidationFinding> findings,
        List<String> fundamentos,
        Instant validatedAt,
        String hashIntegridade
) {
    public InstitutionalAffiliationValidationReport {
        Objects.requireNonNull(validationId);
        Objects.requireNonNull(requestId);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(validationId, requestId, organizationScope, orgaoSigla, unidadeCodigo, aptaParaHomologacao,
                    documentosObrigatoriosPresentes, representanteValidado, dominioInstitucionalValidado, certificadoMaterialValidado,
                    cadeiaConfiancaValidada, findings, fundamentos, validatedAt);
        }
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_affiliation_validation");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
