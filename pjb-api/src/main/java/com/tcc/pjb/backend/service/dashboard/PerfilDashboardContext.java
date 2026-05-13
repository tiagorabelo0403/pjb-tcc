package com.tcc.pjb.backend.service.dashboard;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.BehavioralAuditResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.ExternalSystemStatus;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.OnboardingResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PlantaoResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PrazoRadarItem;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SessionRiskResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SigiloAtivoResumo;
import com.tcc.pjb.backend.model.entity.Usuario;

public record PerfilDashboardContext(
        Usuario usuario,
        UserPersona persona,
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        List<String> pendencias,
        List<PrazoRadarItem> prazoRadar,
        SessionRiskResumo sessionRisk,
        SigiloAtivoResumo sigiloAtivo,
        PlantaoResumo plantao,
        OnboardingResumo onboarding,
        List<ExternalSystemStatus> externalSystems,
        BehavioralAuditResumo behavioralAudit
) {
}
