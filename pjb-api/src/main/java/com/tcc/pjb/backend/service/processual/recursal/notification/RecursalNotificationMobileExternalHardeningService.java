package com.tcc.pjb.backend.service.processual.recursal.notification;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalNotificationLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalWorkbenchSurfaceCatalog;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationFederatedDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileExternalDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileHardeningRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePostureResponse;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecursalNotificationMobileExternalHardeningService {

    private final RecursalNotificationPreferenceFederationService preferenceFederationService;

    public RecursalNotificationMobileExternalHardeningService(RecursalNotificationPreferenceFederationService preferenceFederationService) {
        this.preferenceFederationService = preferenceFederationService;
    }

    public RecursalNotificationMobilePostureResponse mobilePosture(RecursalNotificationMobileHardeningRequest request) {
        RecursalNotificationFederatedDeliveryResponse federated = preferenceFederationService.federatedDelivery(request.preferencias());
        return toPostureResponse(request, federated);
    }

    public RecursalNotificationMobileExternalDeliveryResponse hardenedDelivery(RecursalNotificationMobileHardeningRequest request) {
        RecursalNotificationFederatedDeliveryResponse federated = preferenceFederationService.federatedDelivery(request.preferencias());
        RecursalNotificationMobilePostureResponse posture = toPostureResponse(request, federated);
        return new RecursalNotificationMobileExternalDeliveryResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.ENDURECIMENTO_MOBILE_EXTERNO_SOBERANO,
                federated.processoReferencia(),
                federated.usuarioReferencia(),
                federated.perfilDestino(),
                posture.plataformaMobile(),
                federated.dominioFederado(),
                resolveExternalChannel(posture, federated),
                resolveEnvelope(posture),
                resolveDeliveryStatus(posture),
                RecursalNotificationLabels.STATUS_POSTURA_APROVADA.equals(posture.posturaStatus()),
                List.of(
                        RecursalNotificationLabels.POLITICA_ATESTACAO_APP_SOBERANO,
                        RecursalNotificationLabels.POLITICA_VINCULAR_TOKEN_AO_DISPOSITIVO,
                        RecursalNotificationLabels.POLITICA_ANTI_REPLAY_MOBILE,
                        RecursalNotificationLabels.POLITICA_CRIPTOGRAFIA_PONTA_A_PONTA,
                        RecursalNotificationLabels.POLITICA_RELAY_SOBERANO_ASSINADO,
                        RecursalNotificationLabels.POLITICA_BIOMETRIA_LOCAL_GOVERNADA,
                        RecursalNotificationLabels.POLITICA_ENDURECER_ENTREGA_EXTERNA_SOBERANA
                ),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationMobilePosture(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationMobileExternalHardening(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationFederatedDelivery(),
                        RecursalWorkbenchSurfaceCatalog.notificationTrackingCiencia()
                ),
                enrichAlerts(federated.alertasTaticos(), List.of(
                        posture.posturaStatus(),
                        resolveExternalChannel(posture, federated),
                        resolveDeliveryStatus(posture)
                ))
        );
    }


    private RecursalNotificationMobilePostureResponse toPostureResponse(RecursalNotificationMobileHardeningRequest request,
                                                                        RecursalNotificationFederatedDeliveryResponse federated) {
        int postureScore = postureScore(request);
        String postureStatus = resolvePostureStatus(request, postureScore);
        boolean channelApproved = !RecursalNotificationLabels.STATUS_POSTURA_BLOQUEADA.equals(postureStatus)
                && request.preferencias().canalPushHabilitado();
        return new RecursalNotificationMobilePostureResponse(
                RecursalNotificationLabels.SUITE_NOTIFICACIONAL_RECURSAL_GOVERNADA,
                RecursalNotificationLabels.POSTURA_MOBILE_SOBERANA,
                federated.processoReferencia(),
                federated.usuarioReferencia(),
                federated.perfilDestino(),
                resolvePlatform(request),
                postureStatus,
                postureScore,
                federated.entregaExternaSoberana(),
                channelApproved,
                RecursalNotificationLabels.POLITICA_ATESTACAO_APP_SOBERANO,
                RecursalNotificationLabels.POLITICA_VINCULAR_TOKEN_AO_DISPOSITIVO,
                RecursalNotificationLabels.POLITICA_RELAY_SOBERANO_ASSINADO,
                List.of(
                        RecursalNotificationLabels.POLITICA_ATESTACAO_APP_SOBERANO,
                        RecursalNotificationLabels.POLITICA_VINCULAR_TOKEN_AO_DISPOSITIVO,
                        RecursalNotificationLabels.POLITICA_ANTI_REPLAY_MOBILE,
                        RecursalNotificationLabels.POLITICA_CRIPTOGRAFIA_PONTA_A_PONTA,
                        RecursalNotificationLabels.POLITICA_RELAY_SOBERANO_ASSINADO,
                        RecursalNotificationLabels.POLITICA_BIOMETRIA_LOCAL_GOVERNADA,
                        RecursalNotificationLabels.POLITICA_ENDURECER_ENTREGA_EXTERNA_SOBERANA
                ),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationMobilePosture(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationMobileExternalHardening(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationFederatedDelivery(),
                        RecursalWorkbenchSurfaceCatalog.recursalNotificationPreferencesFine()
                ),
                enrichAlerts(federated.alertasTaticos(), List.of(
                        postureStatus,
                        resolvePlatform(request),
                        channelApproved ? RecursalNotificationLabels.CANAL_PUSH_SOBERANO_EXTERNO : RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL
                ))
        );
    }

    private int postureScore(RecursalNotificationMobileHardeningRequest request) {
        int score = 0;
        if (request.preferencias().dominioSoberanoExterno()) {
            score += 10;
        }
        if (request.dispositivoConfiavel()) {
            score += 20;
        }
        if (request.appSoberanoAtestado()) {
            score += 20;
        }
        if (request.tokenVinculadoAoDispositivo()) {
            score += 15;
        }
        if (request.antiReplayAtivo()) {
            score += 10;
        }
        if (request.criptografiaPontaAPontaAtiva()) {
            score += 10;
        }
        if (request.relaySoberanoAtivo()) {
            score += 10;
        }
        if (request.biometriaLocalAtiva()) {
            score += 5;
        }
        return score;
    }

    private String resolvePostureStatus(RecursalNotificationMobileHardeningRequest request, int postureScore) {
        if (request.preferencias().dominioSoberanoExterno()
                && request.dispositivoConfiavel()
                && request.appSoberanoAtestado()
                && request.tokenVinculadoAoDispositivo()
                && postureScore >= 80) {
            return RecursalNotificationLabels.STATUS_POSTURA_APROVADA;
        }
        if (postureScore >= 60) {
            return RecursalNotificationLabels.STATUS_POSTURA_RESTRITA;
        }
        return RecursalNotificationLabels.STATUS_POSTURA_BLOQUEADA;
    }

    private String resolvePlatform(RecursalNotificationMobileHardeningRequest request) {
        String platform = request.plataformaMobile();
        if (platform == null || platform.isBlank()) {
            return "MOBILE_GOVERNADO";
        }
        return platform.trim().toUpperCase();
    }

    private String resolveExternalChannel(RecursalNotificationMobilePostureResponse posture, RecursalNotificationFederatedDeliveryResponse federated) {
        if (RecursalNotificationLabels.STATUS_POSTURA_APROVADA.equals(posture.posturaStatus())) {
            return RecursalNotificationLabels.CANAL_PUSH_SOBERANO_EXTERNO;
        }
        if (RecursalNotificationLabels.STATUS_POSTURA_RESTRITA.equals(posture.posturaStatus())) {
            return federated.canalExternoPrioritario();
        }
        return RecursalNotificationLabels.CANAL_INBOX_OPERACIONAL;
    }

    private String resolveEnvelope(RecursalNotificationMobilePostureResponse posture) {
        if (RecursalNotificationLabels.STATUS_POSTURA_APROVADA.equals(posture.posturaStatus())) {
            return "ENVELOPE_PUSH_SOBERANO_ASSINADO";
        }
        if (RecursalNotificationLabels.STATUS_POSTURA_RESTRITA.equals(posture.posturaStatus())) {
            return "ENVELOPE_ENTREGA_RESTRITA_COM_DEGRADACAO_CONTROLADA";
        }
        return "ENTREGA_EXTERNA_BLOQUEADA_RETORNO_INBOX";
    }

    private String resolveDeliveryStatus(RecursalNotificationMobilePostureResponse posture) {
        if (RecursalNotificationLabels.STATUS_POSTURA_APROVADA.equals(posture.posturaStatus())) {
            return RecursalNotificationLabels.STATUS_ENTREGA_MOBILE_APROVADA;
        }
        if (RecursalNotificationLabels.STATUS_POSTURA_RESTRITA.equals(posture.posturaStatus())) {
            return RecursalNotificationLabels.STATUS_ENTREGA_MOBILE_DEGRADADA;
        }
        return RecursalNotificationLabels.STATUS_ENTREGA_MOBILE_BLOQUEADA;
    }

    private List<String> enrichAlerts(List<String> current, List<String> additions) {
        LinkedHashSet<String> enriched = new LinkedHashSet<>(current);
        enriched.addAll(additions);
        return List.copyOf(enriched);
    }
}
