package com.tcc.pjb.backend.service.notification.surface;

import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceRequest;
import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceResponse;
import com.tcc.pjb.backend.service.notification.NotificationPreferenceService;
import org.springframework.stereotype.Service;

@Service
public class NotificationPreferenceSurfaceFacadeService {

    private final NotificationPreferenceService service;

    public NotificationPreferenceSurfaceFacadeService(NotificationPreferenceService service) {
        this.service = service;
    }

    public NotificationPreferenceResponse consultar(Long usuarioId) {
        return toResponse(service.consultar(usuarioId));
    }

    public NotificationPreferenceResponse salvar(Long usuarioId, NotificationPreferenceRequest request) {
        return toResponse(service.salvar(usuarioId, new NotificationPreferenceService.PreferenceRequest(
                request.allowEmail(),
                request.allowPush(),
                request.allowWhatsapp(),
                request.allowArDigital(),
                request.allowWebhook(),
                request.allowDigest(),
                request.onlyHighPriority(),
                request.antiSpamWindowMinutes(),
                request.pushEndpoint(),
                request.whatsappNumber(),
                request.arDigitalAddress(),
                request.webhookUrl()
        )));
    }

    private NotificationPreferenceResponse toResponse(NotificationPreferenceService.PreferenceView view) {
        return new NotificationPreferenceResponse(
                view.usuarioId(),
                view.allowEmail(),
                view.allowPush(),
                view.allowWhatsapp(),
                view.allowArDigital(),
                view.allowWebhook(),
                view.allowDigest(),
                view.onlyHighPriority(),
                view.antiSpamWindowMinutes(),
                view.pushEndpoint(),
                view.whatsappNumber(),
                view.arDigitalAddress(),
                view.webhookUrl()
        );
    }
}
