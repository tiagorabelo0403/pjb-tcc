package com.tcc.pjb.backend.controller.inventario;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.inventario.InventarioPartilhaChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {

    private final InventarioPartilhaChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public InventarioController(InventarioPartilhaChecklistService checklistService,
                                CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/partilha/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<InventarioPartilhaChecklistService.InventarioPartilhaResult> checklist(
            @Valid @RequestBody InventarioPartilhaChecklistService.InventarioPartilhaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "inventario_partilha_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
