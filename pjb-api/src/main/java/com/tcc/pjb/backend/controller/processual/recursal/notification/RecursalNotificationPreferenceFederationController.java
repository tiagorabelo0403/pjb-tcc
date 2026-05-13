package com.tcc.pjb.backend.controller.processual.recursal.notification;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationFederatedDeliveryResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationPreferencePolicyResponse;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationPreferenceFederationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalNotificationPreferenceFederationController {

    private final RecursalNotificationPreferenceFederationService service;

    public RecursalNotificationPreferenceFederationController(RecursalNotificationPreferenceFederationService service) {
        this.service = service;
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_PREFERENCES_FINE)
    public ResponseEntity<RecursalNotificationPreferencePolicyResponse> preferences(@RequestBody RecursalNotificationPreferencePolicyRequest request) {
        return ResponseEntity.ok(service.preferences(request));
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_FEDERATED_DELIVERY)
    public ResponseEntity<RecursalNotificationFederatedDeliveryResponse> federatedDelivery(@RequestBody RecursalNotificationPreferencePolicyRequest request) {
        return ResponseEntity.ok(service.federatedDelivery(request));
    }
}
