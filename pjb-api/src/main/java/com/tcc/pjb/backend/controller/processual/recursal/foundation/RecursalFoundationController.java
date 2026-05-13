package com.tcc.pjb.backend.controller.processual.recursal.foundation;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.controller.processual.recursal.routes.RecursalRoutes;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalFoundationResponse;
import com.tcc.pjb.backend.service.processual.recursal.foundation.RecursalFoundationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecursalRoutes.BASE)
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR','PROCURADOR','MAGISTRADO','ADMIN','ADMINISTRADOR')")
public class RecursalFoundationController {

    private final RecursalFoundationService foundationService;

    public RecursalFoundationController(RecursalFoundationService foundationService) {
        this.foundationService = foundationService;
    }

    @GetMapping(RecursalRoutes.FOUNDATION)
    public ResponseEntity<RecursalFoundationResponse> describe() {
        return ResponseEntity.ok(foundationService.describe());
    }
}
