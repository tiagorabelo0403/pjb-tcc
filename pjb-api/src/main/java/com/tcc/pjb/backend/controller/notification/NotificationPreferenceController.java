package com.tcc.pjb.backend.controller.notification;

import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceRequest;
import com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceResponse;
import com.tcc.pjb.backend.service.notification.surface.NotificationPreferenceSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificacoes/preferencias")
public class NotificationPreferenceController {

    private final NotificationPreferenceSurfaceFacadeService facadeService;

    public NotificationPreferenceController(NotificationPreferenceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/usuarios/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM','ADVOGADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','MINISTRO','DESEMBARGADOR')")
    public ResponseEntity<NotificationPreferenceResponse> consultar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(facadeService.consultar(usuarioId));
    }

    @PutMapping("/usuarios/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM','ADVOGADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','MINISTRO','DESEMBARGADOR')")
    public ResponseEntity<NotificationPreferenceResponse> salvar(@PathVariable Long usuarioId,
                                                                 @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(facadeService.salvar(usuarioId, request));
    }
}
