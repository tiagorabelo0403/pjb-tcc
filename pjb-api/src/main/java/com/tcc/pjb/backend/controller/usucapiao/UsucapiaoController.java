package com.tcc.pjb.backend.controller.usucapiao;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.usucapiao.UsucapiaoChecklistService;
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
@RequestMapping("/api/v1/usucapiao")
public class UsucapiaoController {

    private final UsucapiaoChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public UsucapiaoController(UsucapiaoChecklistService checklistService,
                               CapabilityRateLimiter rateLimiter) {
        this.checklistService = Objects.requireNonNull(checklistService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','ADVOGADO','SERVIDOR_FORUM')")
    public ResponseEntity<UsucapiaoChecklistService.UsucapiaoResult> checklist(
            @Valid @RequestBody UsucapiaoChecklistService.UsucapiaoInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "usucapiao_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
