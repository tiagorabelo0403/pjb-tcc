package com.tcc.pjb.backend.controller.responsabilidadecivil;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService;
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
@RequestMapping("/api/v1/responsabilidade-civil")
public class ResponsabilidadeCivilController {

    private final ResponsabilidadeCivilChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public ResponsabilidadeCivilController(ResponsabilidadeCivilChecklistService checklistService,
                                           CapabilityRateLimiter rateLimiter) {
        this.checklistService = Objects.requireNonNull(checklistService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','ADVOGADO','SERVIDOR_FORUM')")
    public ResponseEntity<ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilResult> checklist(
            @Valid @RequestBody ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "resp_civil_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
