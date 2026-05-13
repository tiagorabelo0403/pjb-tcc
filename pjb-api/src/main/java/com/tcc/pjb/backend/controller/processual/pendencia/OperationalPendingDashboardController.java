package com.tcc.pjb.backend.controller.processual.pendencia;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.processual.pendencia.OperationalPendingDashboardResponse;
import com.tcc.pjb.backend.service.processual.pendencia.OperationalPendingDashboardService;

@RestController
@RequestMapping(OperationalApiRoutes.PROCESSUAL_PENDENCIAS_BASE)
public class OperationalPendingDashboardController {

    private final OperationalPendingDashboardService service;

    public OperationalPendingDashboardController(OperationalPendingDashboardService service) {
        this.service = service;
    }

    @GetMapping(OperationalApiRoutes.PATH_PROCESSUAL_PENDENCIAS_PAINEL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OperationalPendingDashboardResponse> painel(@RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(service.dashboard(limite));
    }
}
