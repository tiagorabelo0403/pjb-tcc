package com.tcc.pjb.backend.service.processual.recursal.notification;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNotificationLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalWorkbenchSurfaceCatalog;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePreviewResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationScienceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursalNotificationGovernanceService {

    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService;

    public RecursalNotificationGovernanceService(RecursalIntelligenceSurfaceService intelligenceSurfaceService) {
        this.intelligenceSurfaceService = intelligenceSurfaceService;
    }

    public RecursalNotificationMobilePreviewResponse mobilePreview(RecursalNotificationGovernanceRequest request) {
        RecursalSpecializedSurfaceResponse surface = intelligenceSurfaceService.buildIntelligenceSurface(request.contexto());
        return new RecursalNotificationMobilePreviewResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.PREVIEW_MOBILE_PENDENCIAS_RECURSAIS,
                request.processoReferencia(),
                request.usuarioReferencia(),
                request.perfilDestino(),
                resolvePriorityChannel(request),
                resolveCriticalWindow(request),
                resolveAudience(request),
                request.mobileAtivo(),
                request.sigiloso(),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        RecursalNotificationLabels.POLITICA_REUSAR_PREVIEW_CALENDARIO,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_GLOBAIS,
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                )),
                List.of(
                        RecursalNotificationLabels.POLITICA_SEM_SCHEDULER_PARALELO,
                        RecursalNotificationLabels.POLITICA_SEM_EXECUTOR_PARALELO,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREVIEW_CALENDARIO,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_GLOBAIS,
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                ),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificaPendencias(),
                        RecursalWorkbenchSurfaceCatalog.recursalMobileAcompanhamento()
                )
        );
    }

    public RecursalNotificationGovernanceResponse governance(RecursalNotificationGovernanceRequest request) {
        RecursalSpecializedSurfaceResponse surface = intelligenceSurfaceService.buildIntelligenceSurface(request.contexto());
        return new RecursalNotificationGovernanceResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.GOVERNANCA_NOTIFICACIONAL_RECURSAL,
                request.processoReferencia(),
                request.usuarioReferencia(),
                request.perfilDestino(),
                RecursalNotificationLabels.STATUS_FLUXO_GOVERNADO,
                resolvePriorityChannel(request),
                RecursalWorkbenchSurfaceCatalog.calendarNotificationPreview(),
                RecursalWorkbenchSurfaceCatalog.calendarPreferences(),
                RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch(),
                RecursalWorkbenchSurfaceCatalog.recursalNotificaPendencias(),
                false,
                false,
                request.exigeCiencia(),
                List.of(
                        RecursalNotificationLabels.POLITICA_SEM_SCHEDULER_PARALELO,
                        RecursalNotificationLabels.POLITICA_SEM_EXECUTOR_PARALELO,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_GLOBAIS,
                        RecursalNotificationLabels.POLITICA_REUSAR_CIENCIA_MULTICANAL,
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                ),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        RecursalNotificationLabels.POLITICA_REUSAR_PREVIEW_CALENDARIO,
                        resolveAudience(request),
                        resolveCriticalWindow(request)
                ))
        );
    }

    public RecursalNotificationScienceResponse science(RecursalNotificationGovernanceRequest request) {
        RecursalSpecializedSurfaceResponse surface = intelligenceSurfaceService.buildIntelligenceSurface(request.contexto());
        boolean cienciaRegistrada = request.exigeCiencia() && request.tokenRastreio() != null && !request.tokenRastreio().isBlank();
        return new RecursalNotificationScienceResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.CIENCIA_NOTIFICACIONAL_RECURSAL,
                request.processoReferencia(),
                request.usuarioReferencia(),
                request.tokenRastreio(),
                cienciaRegistrada ? RecursalNotificationLabels.STATUS_CIENCIA_REGISTRADA : RecursalNotificationLabels.STATUS_CIENCIA_PENDENTE,
                RecursalWorkbenchSurfaceCatalog.notificationTrackingPixel(),
                RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia(),
                cienciaRegistrada,
                false,
                List.of(
                        RecursalNotificationLabels.POLITICA_REUSAR_CIENCIA_MULTICANAL,
                        RecursalNotificationLabels.POLITICA_SEM_SCHEDULER_PARALELO,
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                ),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        resolveAudience(request),
                        request.exigeCiencia() ? RecursalNotificationLabels.STATUS_CIENCIA_REGISTRADA : RecursalNotificationLabels.STATUS_CIENCIA_PENDENTE
                ))
        );
    }

    private List<String> enrichAlerts(List<String> current, List<String> additions) {
        LinkedHashSet<String> enriched = new LinkedHashSet<>(current);
        enriched.addAll(additions);
        return List.copyOf(enriched);
    }

    private String resolvePriorityChannel(RecursalNotificationGovernanceRequest request) {
        if (request.mobileAtivo() || request.exigeCiencia() || request.urgente() || request.prazoCriticoHoras() <= 24) {
            return RecursalNotificationLabels.CANAL_PUSH_GOVERNADO;
        }
        if (request.sigiloso()) {
            return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
        }
        return RecursalNotificationLabels.CANAL_CALENDARIO;
    }

    private String resolveCriticalWindow(RecursalNotificationGovernanceRequest request) {
        if (request.urgente() || request.prazoCriticoHoras() <= 6) {
            return RecursalNotificationLabels.JANELA_CRITICIDADE_IMEDIATA;
        }
        if (request.prazoCriticoHoras() <= 24) {
            return RecursalNotificationLabels.JANELA_CRITICIDADE_TATICA;
        }
        return RecursalNotificationLabels.JANELA_CRITICIDADE_MONITORADA;
    }

    private String resolveAudience(RecursalNotificationGovernanceRequest request) {
        return switch (request.perfilDestino() == null ? "" : request.perfilDestino().trim().toUpperCase()) {
            case "CIDADAO" -> RecursalNotificationLabels.AUDIENCIA_CIDADAO;
            case "SECRETARIA", "MAGISTRATURA", "MP", "PROCURADORIA", "DEFENSORIA" -> RecursalNotificationLabels.AUDIENCIA_SECRETARIA_MAGISTRATURA;
            default -> RecursalNotificationLabels.AUDIENCIA_REPRESENTACAO_TECNICA;
        };
    }
}
