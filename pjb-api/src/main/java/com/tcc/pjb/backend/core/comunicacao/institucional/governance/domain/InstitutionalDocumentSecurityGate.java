package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record InstitutionalDocumentSecurityGate(
        String operationCode,
        String affiliationId,
        String nominationId,
        String unitCode,
        String boxCode,
        boolean enforced,
        boolean allowed,
        boolean requiresQualifiedCertificate,
        boolean requiresGovBr,
        boolean requiresMfa,
        boolean manualApproval,
        boolean blocked,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalDocumentSecurityGate {
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("operationCode", operationCode);
        out.put("affiliationId", affiliationId);
        out.put("nominationId", nominationId);
        out.put("unitCode", unitCode);
        out.put("boxCode", boxCode);
        out.put("enforced", enforced);
        out.put("allowed", allowed);
        out.put("requiresQualifiedCertificate", requiresQualifiedCertificate);
        out.put("requiresGovBr", requiresGovBr);
        out.put("requiresMfa", requiresMfa);
        out.put("manualApproval", manualApproval);
        out.put("blocked", blocked);
        out.put("findings", findings);
        out.put("fundamentos", fundamentos);
        out.put("generatedAt", generatedAt);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
