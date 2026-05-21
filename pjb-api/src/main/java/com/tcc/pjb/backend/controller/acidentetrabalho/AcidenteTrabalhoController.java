package com.tcc.pjb.backend.controller.acidentetrabalho;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.acidentetrabalho.AcidenteTrabalhoChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/acidentetrabalho")
public class AcidenteTrabalhoController {

    private final AcidenteTrabalhoChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public AcidenteTrabalhoController(AcidenteTrabalhoChecklistService checklistService,
                                      CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<AcidenteTrabalhoChecklistService.AcidenteTrabalhoResult> checklist(
            @Valid @RequestBody AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "acidentetrabalho_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
