package com.tcc.pjb.backend.controller.notification;

import com.tcc.pjb.backend.model.dto.notification.NotificationTrackingCienciaResponse;
import com.tcc.pjb.backend.service.notification.NotificationTrackingService;
import com.tcc.pjb.backend.service.notification.surface.NotificationSurfaceFacadeService;
import org.springframework.http.CacheControl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificacoes/track")
@PreAuthorize("permitAll()")
public class NotificationTrackingController {

    private final NotificationTrackingService trackingService;
    private final NotificationSurfaceFacadeService facadeService;

    public NotificationTrackingController(NotificationTrackingService trackingService,
                                          NotificationSurfaceFacadeService facadeService) {
        this.trackingService = trackingService;
        this.facadeService = facadeService;
    }

    @GetMapping("/{token}.gif")
    public ResponseEntity<byte[]> pixel(@PathVariable String token) {
        trackingService.markRead(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(trackingService.mediaType())
                .body(trackingService.pixelBytes());
    }

    @PostMapping("/{token}/ciencia")
    public ResponseEntity<NotificationTrackingCienciaResponse> ciencia(@PathVariable String token) {
        return ResponseEntity.ok(facadeService.ciencia(token));
    }
}
