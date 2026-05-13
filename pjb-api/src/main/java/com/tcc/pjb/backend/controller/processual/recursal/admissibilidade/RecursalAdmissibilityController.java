package com.tcc.pjb.backend.controller.processual.recursal.admissibilidade;

import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityFacadeService;
import com.tcc.pjb.backend.service.recursal.RecursalContextualAccessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/recursal/admissibilidade")
public class RecursalAdmissibilityController {

    private final RecursalAdmissibilityFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final RecursalContextualAccessService accessService;

    public RecursalAdmissibilityController(RecursalAdmissibilityFacadeService facadeService,
                                           CapabilityRateLimiter rateLimiter,
                                           RecursalContextualAccessService accessService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.accessService = accessService;
    }

    @PostMapping("/avaliar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADVOGADO','ROLE_MEMBRO_MINISTERIO_PUBLICO','ROLE_PROMOTOR_ELEITORAL','ROLE_PROMOTOR_TRABALHISTA','ROLE_PROCURADOR_GERAL_REPUBLICA','ROLE_DEFENSOR_PUBLICO','ROLE_DEFENSOR_PUBLICO_FEDERAL','ROLE_PROCURADOR','ROLE_PROCURADORIA_MUNICIPAL','ROLE_PROCURADORIA_ESTADUAL','ROLE_PROCURADORIA_FEDERAL','ROLE_JUIZ','ROLE_JUIZ_ESTADUAL','ROLE_JUIZ_FEDERAL','ROLE_JUIZ_ESPECIAL','ROLE_JUIZ_ELEITORAL','ROLE_JUIZ_TRABALHISTA','ROLE_JUIZ_MILITAR','ROLE_MAGISTRADO','ROLE_DESEMBARGADOR','ROLE_DESEMBARGADOR_FEDERAL','ROLE_MINISTRO')")
    public ResponseEntity<RecursalAdmissibilityResponse> avaliar(@Valid @RequestBody RecursalAdmissibilityRequest request,
                                                                 Authentication authentication) {
        accessService.requireAdmissibilityAccess(request);
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "recursal_admissibilidade_avaliar", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.avaliarRecursal(request));
    }
}
