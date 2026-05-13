package com.tcc.pjb.backend.controller.oficial_justica;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaOperationalIntelligenceResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaOperationalIntelligenceService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/oficial-justica/inteligencia-operacional")
public class OficialJusticaOperationalIntelligenceController {

    private final OficialJusticaOperationalIntelligenceService service;
    private final CapabilityRateLimiter rateLimiter;
    private final ApiResponseFactory responseFactory;

    public OficialJusticaOperationalIntelligenceController(OficialJusticaOperationalIntelligenceService service,
                                                           CapabilityRateLimiter rateLimiter,
                                                           ApiResponseFactory responseFactory) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ApiQueryResponse<OficialJusticaOperationalIntelligenceResponse>> painel(@RequestParam(defaultValue = "3") int horizonDays,
                                                                                                   @RequestParam(defaultValue = "25") long filaCriticaThreshold,
                                                                                                   Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "oficial_inteligencia_operacional", ApiVersion.V1);
        return ResponseEntity.ok(responseFactory.queryOk(service.analyze(horizonDays, filaCriticaThreshold), List.of()));
    }
}
