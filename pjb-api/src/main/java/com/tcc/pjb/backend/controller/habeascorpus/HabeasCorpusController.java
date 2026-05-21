package com.tcc.pjb.backend.controller.habeascorpus;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/habeascorpus")
public class HabeasCorpusController {

    private final HabeasCorpusChecklistService checklistService;
    private final CapabilityRateLimiter rateLimiter;

    public HabeasCorpusController(HabeasCorpusChecklistService checklistService,
                                  CapabilityRateLimiter rateLimiter) {
        this.checklistService = checklistService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/checklist")
    @PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<HabeasCorpusChecklistService.HabeasCorpusResult> checklist(
            @Valid @RequestBody HabeasCorpusChecklistService.HabeasCorpusInput input,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "habeascorpus_checklist", ApiVersion.V1);
        return ResponseEntity.ok(checklistService.avaliar(input));
    }
}
