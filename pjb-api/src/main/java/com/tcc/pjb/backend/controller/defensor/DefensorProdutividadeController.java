package com.tcc.pjb.backend.controller.defensor;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.produtividade.InstitutionalProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.institutional.produtividade.InstitutionalProdutividadeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/defensor/produtividade")
public class DefensorProdutividadeController {

    private static final String DEFENSOR_ROLES = "hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')";

    private final InstitutionalProdutividadeService produtividadeService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public DefensorProdutividadeController(InstitutionalProdutividadeService produtividadeService,
                                           CurrentUserService currentUserService,
                                           CapabilityRateLimiter rateLimiter) {
        this.produtividadeService = Objects.requireNonNull(produtividadeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping
    @PreAuthorize(DEFENSOR_ROLES)
    public ResponseEntity<InstitutionalProdutividadePainelResponse> painel(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int diasJanela,
                                                                            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "defensor_produtividade_painel", ApiVersion.V1);
        Usuario defensor = currentUserService.getRequired();
        return ResponseEntity.ok(produtividadeService.painel(defensor.getId(), diasJanela));
    }
}
