package com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalEntryActivationDecision(
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String profileKey,
        String profileState,
        String targetEnvironment,
        String entryMode,
        String contextId,
        String panelCode,
        String landingPath,
        String processAreaCode,
        String unidadeCodigo,
        String caixaCodigo,
        String horizontalDataPlaneKey,
        String readReplicaCode,
        String sessionRiskLevel,
        int sessionRiskScore,
        String govBrNivelGarantia,
        String recommendedSensitiveAct,
        String stepUpStartPath,
        boolean institutionalProfileVisible,
        boolean directInstitutionalContextAvailable,
        boolean activateInstitutionalContext,
        boolean panelProvisioningComplete,
        boolean sharedExperienceReady,
        boolean requiresPanelProvisioningReview,
        boolean routeToPersonalPanel,
        boolean blocked,
        boolean requiresGovBrBinding,
        boolean requiresTrustedDevice,
        boolean requiresStepUp,
        boolean requiresQualifiedCertificate,
        boolean requiresInstitutionalNetwork,
        boolean acceptsRemoteCertificateAuthorization,
        boolean requiresManualApproval,
        List<String> panelProvisioningFindings,
        List<String> blockers,
        List<String> warnings,
        List<String> garantias,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalEntryActivationDecision {
        panelProvisioningFindings = panelProvisioningFindings == null ? List.of() : List.copyOf(panelProvisioningFindings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        garantias = garantias == null ? List.of() : List.copyOf(garantias);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
