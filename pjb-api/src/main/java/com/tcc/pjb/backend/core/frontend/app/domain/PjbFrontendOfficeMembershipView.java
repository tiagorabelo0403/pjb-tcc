package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeMembershipView(
        Long equipeId,
        String equipeNome,
        String papelEquipe,
        String cargo,
        Long seniorUserId,
        String seniorNome,
        boolean officePolicyEnabled,
        boolean bloqueiaCausasProprias,
        boolean membroAtivo,
        boolean elegivelAutoAtivacao,
        boolean activeSelection,
        List<String> allowedRamos,
        boolean canViewAllRamos,
        Integer trustScore,
        String trustLevel,
        Integer minTrustRequired,
        boolean patronCertificateRequired,
        Integer workspacePriority
) {
}
