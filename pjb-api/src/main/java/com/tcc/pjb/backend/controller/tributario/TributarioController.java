package com.tcc.pjb.backend.controller.tributario;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.tributario.TributarioPrescrDecadChecklistService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tributario")
public class TributarioController {

    private final TributarioPrescrDecadChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public TributarioController(TributarioPrescrDecadChecklistService checklistService,
                                CapabilityRateLimiter rateLimiter) {
        this.checklistService = Objects.requireNonNull(checklistService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/prescricao-decadencia/checklist")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','ADVOGADO','SERVIDOR_FORUM','PROCURADOR_FAZENDA')")
    public ResponseEntity<TributarioPrescrDecadChecklistService.TributarioPrescrDecadResult> prescricaoDecadenciaChecklist(
            @Valid @RequestBody TributarioPrescrDecadChecklistService.TributarioPrescrDecadInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "tributario_prescr_decad_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
