package com.tcc.pjb.backend.service.processual.recursal.notification;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNotificationLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalWorkbenchSurfaceCatalog;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationFederatedDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursalNotificationPreferenceFederationService {

    private final RecursalIntelligenceSurfaceService intelligenceSurfaceService;

    public RecursalNotificationPreferenceFederationService(RecursalIntelligenceSurfaceService intelligenceSurfaceService) {
        this.intelligenceSurfaceService = intelligenceSurfaceService;
    }

    public RecursalNotificationPreferencePolicyResponse preferences(RecursalNotificationPreferencePolicyRequest request) {
        RecursalNotificationGovernanceRequest governance = request.governanca();
        RecursalSpecializedSurfaceResponse surface = intelligenceSurfaceService.buildIntelligenceSurface(governance.contexto());
        String canalPreferencial = resolvePreferredChannel(request);
        List<String> canaisHabilitados = enabledChannels(request);
        return new RecursalNotificationPreferencePolicyResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.PREFERENCIAS_FINAS_RECURSAIS,
                governance.processoReferencia(),
                governance.usuarioReferencia(),
                governance.perfilDestino(),
                canalPreferencial,
                canaisHabilitados,
                resolveProfilePolicy(governance),
                resolveFederatedPolicy(request),
                List.of(
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_GLOBAIS,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_POR_CANAL,
                        resolveProfilePolicy(governance)
                ),
                List.of(
                        RecursalNotificationLabels.POLITICA_SEM_SCHEDULER_PARALELO,
                        RecursalNotificationLabels.POLITICA_SEM_EXECUTOR_PARALELO,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_GLOBAIS,
                        RecursalNotificationLabels.POLITICA_REUSAR_PREFERENCIAS_POR_CANAL,
                        resolveFederatedPolicy(request),
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                ),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.notificationPreferencesUser(),
                        RecursalWorkbenchSurfaceCatalog.calendarPreferences(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationPreferencesFine(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationFederatedDelivery()
                ),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        canalPreferencial,
                        resolveProfilePolicy(governance),
                        resolveFederatedPolicy(request)
                ))
        );
    }

    public RecursalNotificationFederatedDeliveryResponse federatedDelivery(RecursalNotificationPreferencePolicyRequest request) {
        RecursalNotificationGovernanceRequest governance = request.governanca();
        RecursalSpecializedSurfaceResponse surface = intelligenceSurfaceService.buildIntelligenceSurface(governance.contexto());
        String politicaFederada = resolveFederatedPolicy(request);
        return new RecursalNotificationFederatedDeliveryResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.ENTREGA_FEDERADA_SOBERANA,
                governance.processoReferencia(),
                governance.usuarioReferencia(),
                governance.perfilDestino(),
                resolveFederatedDomain(request),
                request.dominioSoberanoExterno(),
                resolveFederatedExternalChannel(request),
                politicaFederada,
                resolveDeliverySignature(request),
                List.of(
                        RecursalNotificationLabels.POLITICA_SEM_SCHEDULER_PARALELO,
                        RecursalNotificationLabels.POLITICA_SEM_EXECUTOR_PARALELO,
                        RecursalNotificationLabels.POLITICA_ENDURECER_ENTREGA_EXTERNA_SOBERANA,
                        RecursalNotificationLabels.POLITICA_FEDERAR_ENTREGA_INSTITUCIONAL,
                        politicaFederada,
                        RecursalNotificationLabels.POLITICA_SIGILO_POR_PERFIL_E_PROCESSO
                ),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.notificationMulticanalDispatch(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationGovernance(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationFederatedDelivery(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationScience()
                ),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        resolveFederatedExternalChannel(request),
                        politicaFederada,
                        resolveDeliverySignature(request)
                ))
        );
    }

    private List<String> enabledChannels(RecursalNotificationPreferencePolicyRequest request) {
        List<String> channels = new ArrayList<>();
        if (request.canalPushHabilitado()) {
            channels.add(RecursalNotificationLabels.CANAL_PUSH_GOVERNADO);
        }
        if (request.canalInboxHabilitado()) {
            channels.add(RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL);
        }
        if (request.canalCalendarioHabilitado()) {
            channels.add(RecursalNotificationLabels.CANAL_CALENDARIO);
        }
        if (request.canalEmailHabilitado()) {
            channels.add(RecursalNotificationLabels.CANAL_EMAIL_GOVERNADO);
        }
        if (request.canalSmsHabilitado()) {
            channels.add(RecursalNotificationLabels.CANAL_SMS_GOVERNADO);
        }
        if (channels.isEmpty()) {
            channels.add(RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL);
        }
        return List.copyOf(channels);
    }

    private String resolvePreferredChannel(RecursalNotificationPreferencePolicyRequest request) {
        RecursalNotificationGovernanceRequest governance = request.governanca();
        String profilePolicy = resolveProfilePolicy(governance);
        if (governance.sigiloso()) {
            return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
        }
        if (RecursalNotificationLabels.POLITICA_PERFIL_CIDADAO.equals(profilePolicy)) {
            if (request.canalPushHabilitado() && governance.mobileAtivo()) {
                return RecursalNotificationLabels.CANAL_PUSH_GOVERNADO;
            }
            if (request.canalEmailHabilitado()) {
                return RecursalNotificationLabels.CANAL_EMAIL_GOVERNADO;
            }
            return RecursalNotificationLabels.CANAL_CALENDARIO;
        }
        if (RecursalNotificationLabels.POLITICA_PERFIL_OPERACIONAL.equals(profilePolicy)) {
            if (request.canalInboxHabilitado()) {
                return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
            }
            return RecursalNotificationLabels.CANAL_MULTICANAL_PJB;
        }
        if (request.canalPushHabilitado() && governance.mobileAtivo()) {
            return RecursalNotificationLabels.CANAL_PUSH_GOVERNADO;
        }
        if (request.canalInboxHabilitado()) {
            return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
        }
        return RecursalNotificationLabels.CANAL_CALENDARIO;
    }

    private String resolveProfilePolicy(RecursalNotificationGovernanceRequest governance) {
        String profile = governance.perfilDestino() == null ? "" : governance.perfilDestino().trim().toUpperCase();
        return switch (profile) {
            case "CIDADAO" -> RecursalNotificationLabels.POLITICA_PERFIL_CIDADAO;
            case "SECRETARIA", "MAGISTRATURA", "MP", "PROCURADORIA", "DEFENSORIA" -> RecursalNotificationLabels.POLITICA_PERFIL_OPERACIONAL;
            default -> RecursalNotificationLabels.POLITICA_PERFIL_REPRESENTACAO_TECNICA;
        };
    }

    private String resolveFederatedPolicy(RecursalNotificationPreferencePolicyRequest request) {
        if (request.dominioSoberanoExterno()) {
            return RecursalNotificationLabels.POLITICA_FEDERADA_SOBERANA;
        }
        if (request.federacaoInstitucionalAtiva()) {
            return RecursalNotificationLabels.POLITICA_FEDERADA_INSTITUCIONAL;
        }
        return RecursalNotificationLabels.POLITICA_FEDERADA_NOTIFICACIONAL;
    }

    private String resolveFederatedDomain(RecursalNotificationPreferencePolicyRequest request) {
        String domain = request.dominioFederadoReferencia();
        if (domain != null && !domain.isBlank()) {
            return domain;
        }
        return request.dominioSoberanoExterno() ? "DOMINIO_SOBERANO_EXTERNO" : "PJB_INTERNO";
    }

    private String resolveFederatedExternalChannel(RecursalNotificationPreferencePolicyRequest request) {
        if (request.dominioSoberanoExterno()) {
            if (request.canalInboxHabilitado()) {
                return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
            }
            return RecursalNotificationLabels.CANAL_MULTICANAL_PJB;
        }
        if (request.canalPushHabilitado() && request.governanca().mobileAtivo()) {
            return RecursalNotificationLabels.CANAL_PUSH_GOVERNADO;
        }
        if (request.canalEmailHabilitado()) {
            return RecursalNotificationLabels.CANAL_EMAIL_GOVERNADO;
        }
        return RecursalNotificationLabels.CANAL_CALENDARIO;
    }

    private String resolveDeliverySignature(RecursalNotificationPreferencePolicyRequest request) {
        if (request.dominioSoberanoExterno()) {
            return "ASSINATURA_ENTREGA_SOBERANA";
        }
        if (request.federacaoInstitucionalAtiva()) {
            return "ASSINATURA_ENTREGA_FEDERADA_INSTITUCIONAL";
        }
        return "ASSINATURA_ENTREGA_GOVERNADA_PJB";
    }

    private List<String> enrichAlerts(List<String> current, List<String> additions) {
        LinkedHashSet<String> enriched = new LinkedHashSet<>(current);
        enriched.addAll(additions);
        return List.copyOf(enriched);
    }
}
