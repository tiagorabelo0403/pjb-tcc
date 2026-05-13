package com.tcc.pjb.backend.controller.processual.recursal.notification;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileExternalDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobileHardeningRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePostureResponse;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationMobileExternalHardeningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalNotificationMobileExternalHardeningController {

    private final RecursalNotificationMobileExternalHardeningService service;

    public RecursalNotificationMobileExternalHardeningController(RecursalNotificationMobileExternalHardeningService service) {
        this.service = service;
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_MOBILE_POSTURE)
    public ResponseEntity<RecursalNotificationMobilePostureResponse> mobilePosture(@RequestBody RecursalNotificationMobileHardeningRequest request) {
        return ResponseEntity.ok(service.mobilePosture(request));
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_MOBILE_EXTERNAL_HARDENING)
    public ResponseEntity<RecursalNotificationMobileExternalDeliveryResponse> hardenedDelivery(@RequestBody RecursalNotificationMobileHardeningRequest request) {
        return ResponseEntity.ok(service.hardenedDelivery(request));
    }
}
