package com.tcc.pjb.backend.controller.contrato;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.contrato.ContratoVicioResolucaoChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contrato")
public class ContratoController {

    private final ContratoVicioResolucaoChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public ContratoController(ContratoVicioResolucaoChecklistService checklistService,
                              CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/vicio-resolucao/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoResult> checklist(
            @Valid @RequestBody ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "contrato_vicio_resolucao_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
