package com.tcc.pjb.backend.service.dashboard;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.BehavioralAuditResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.ExternalSystemStatus;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.OnboardingResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PlantaoResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PrazoRadarItem;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SessionRiskResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SigiloAtivoResumo;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class PerfilDashboardContextFactory {

    private final PerfilPainelSupportService supportService;
    private final UserPersonaService userPersonaService;

    public PerfilDashboardContextFactory(PerfilPainelSupportService supportService, UserPersonaService userPersonaService) {
        this.supportService = supportService;
        this.userPersonaService = userPersonaService;
    }

    public PerfilDashboardContext build() {
        Usuario usuario = supportService.currentUser();
        UserPersona persona = userPersonaService.getRequiredPersona();
        List<PrazoRadarItem> prazoRadar = supportService.prazoRadar(usuario);
        SessionRiskResumo sessionRisk = supportService.sessionRisk(usuario);
        SigiloAtivoResumo sigiloAtivo = supportService.sigiloAtivo(usuario);
        PlantaoResumo plantao = supportService.plantao(usuario);
        OnboardingResumo onboarding = supportService.onboarding(usuario);
        List<ExternalSystemStatus> externalSystems = supportService.externalSystems(usuario);
        BehavioralAuditResumo behavioralAudit = supportService.behavioralAudit(usuario);
        List<String> pendencias = supportService.pendencias(usuario, onboarding, prazoRadar, sessionRisk);
        return new PerfilDashboardContext(
                usuario,
                persona,
                LocalDateTime.now(),
                usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "USUARIO",
                persona.tratamento(),
                pendencias,
                prazoRadar,
                sessionRisk,
                sigiloAtivo,
                plantao,
                onboarding,
                externalSystems,
                behavioralAudit
        );
    }
}
