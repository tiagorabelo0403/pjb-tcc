package com.tcc.pjb.backend.controller.locacao;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.locacao.LocacaoInquilinaoChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locacao")
public class LocacaoController {

    private final LocacaoInquilinaoChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public LocacaoController(LocacaoInquilinaoChecklistService checklistService,
                             CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM')")
    public ResponseEntity<LocacaoInquilinaoChecklistService.LocacaoResult> checklist(
            @Valid @RequestBody LocacaoInquilinaoChecklistService.LocacaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "locacao_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
