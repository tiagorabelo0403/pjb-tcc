package com.tcc.pjb.backend.controller.violenciadomestica;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService;
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
@RequestMapping("/api/v1/violencia-domestica")
public class ViolenciaDomesticaController {

    private final ViolenciaDomesticaChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public ViolenciaDomesticaController(ViolenciaDomesticaChecklistService checklistService,
                                        CapabilityRateLimiter rateLimiter) {
        this.checklistService = Objects.requireNonNull(checklistService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','MINISTERIO_PUBLICO')")
    public ResponseEntity<ViolenciaDomesticaChecklistService.ViolenciaDomesticaResult> checklist(
            @Valid @RequestBody ViolenciaDomesticaChecklistService.ViolenciaDomesticaInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "violencia_domestica_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
