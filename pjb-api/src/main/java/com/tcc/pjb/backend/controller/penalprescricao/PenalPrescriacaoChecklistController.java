package com.tcc.pjb.backend.controller.penalprescricao;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.penalprescricao.PenalPrescriacaoChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/penal/prescricao")
public class PenalPrescriacaoChecklistController {

    private final PenalPrescriacaoChecklistService service;
    private final CapabilityRateLimiter rateLimiter;

    public PenalPrescriacaoChecklistController(PenalPrescriacaoChecklistService service,
                                               CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<PenalPrescriacaoChecklistService.PenalPrescriacaoResult> avaliar(
            @Valid @RequestBody PenalPrescriacaoChecklistService.PenalPrescriacaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "penal_prescricao_checklist", ApiVersion.V1);
        return ResponseEntity.ok(service.avaliar(input));
    }
}
