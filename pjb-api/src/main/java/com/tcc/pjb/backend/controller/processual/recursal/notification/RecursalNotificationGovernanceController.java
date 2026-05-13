package com.tcc.pjb.backend.controller.processual.recursal.notification;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationMobilePreviewResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.notification.RecursalNotificationScienceResponse;
import com.tcc.pjb.backend.service.processual.recursal.notification.RecursalNotificationGovernanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalNotificationGovernanceController {

    private final RecursalNotificationGovernanceService governanceService;

    public RecursalNotificationGovernanceController(RecursalNotificationGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_MOBILE_PREVIEW)
    public ResponseEntity<RecursalNotificationMobilePreviewResponse> mobilePreview(@RequestBody RecursalNotificationGovernanceRequest request) {
        return ResponseEntity.ok(governanceService.mobilePreview(request));
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_GOVERNANCE)
    public ResponseEntity<RecursalNotificationGovernanceResponse> governance(@RequestBody RecursalNotificationGovernanceRequest request) {
        return ResponseEntity.ok(governanceService.governance(request));
    }

    @PostMapping(RecursalRoutes.NOTIFICATION_SCIENCE)
    public ResponseEntity<RecursalNotificationScienceResponse> science(@RequestBody RecursalNotificationGovernanceRequest request) {
        return ResponseEntity.ok(governanceService.science(request));
    }
}
