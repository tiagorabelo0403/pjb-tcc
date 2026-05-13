package com.tcc.pjb.backend.core.lgpd;

import java.util.List;

public record ProcessoSigiloRlsEnvelope(
        Long processoId,
        String tableName,
        String sigiloLevel,
        String requiredSigiloClearance,
        String requiredUnitCode,
        String requiredTribunalCode,
        String rlsScopeKey,
        boolean judicialSecrecyAware,
        boolean rlsRecommended,
        boolean requiresStepUp,
        boolean requiresQualifiedCertificate,
        boolean readOnlyRecommended,
        List<String> classificationCategories,
        List<String> findings,
        String competencia,
        String comarca
) {
    public ProcessoSigiloRlsEnvelope(String tableName,
                                     java.util.List<String> classificationCategories,
                                     String sigiloLevel,
                                     String requiredSigiloClearance,
                                     String requiredTribunalCode,
                                     String requiredUnitCode,
                                     String rlsScopeKey,
                                     boolean judicialSecrecyAware,
                                     boolean rlsRecommended,
                                     boolean requiresStepUp) {
        this(null, tableName, sigiloLevel, requiredSigiloClearance, requiredUnitCode, requiredTribunalCode, rlsScopeKey, judicialSecrecyAware, rlsRecommended, requiresStepUp, false, false, classificationCategories, java.util.List.of(), null, null);
    }

    public ProcessoSigiloRlsEnvelope {
        classificationCategories = classificationCategories == null ? List.of() : List.copyOf(classificationCategories);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
