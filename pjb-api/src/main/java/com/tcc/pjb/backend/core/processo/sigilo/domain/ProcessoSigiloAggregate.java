package com.tcc.pjb.backend.core.processo.sigilo.domain;

import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoSigiloAggregate(
        ProcessoUnificadoIdentity identity,
        NivelSigilo nivelSigilo,
        String disclosureMode,
        boolean exigeCredencial,
        boolean exigeStepUp,
        boolean exigeDuplaAutorizacao,
        long pendingApprovals,
        long approvedCredentials,
        long totalGuardas,
        long totalFindings,
        List<String> chips,
        List<String> allowedDirectProfiles,
        List<ProcessoSigiloGuarda> guardas,
        List<ProcessoSigiloFinding> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
    public ProcessoSigiloAggregate {
        Objects.requireNonNull(identity);
        nivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
        disclosureMode = disclosureMode == null ? "PUBLICO_CONTROLADO" : disclosureMode;
        chips = chips == null ? List.of() : List.copyOf(chips);
        allowedDirectProfiles = allowedDirectProfiles == null ? List.of() : List.copyOf(allowedDirectProfiles);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
