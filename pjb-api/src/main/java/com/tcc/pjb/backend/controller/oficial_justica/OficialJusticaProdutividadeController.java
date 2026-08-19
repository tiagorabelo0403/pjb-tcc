package com.tcc.pjb.backend.controller.oficial_justica;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProdutividadeService;
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
@RequestMapping("/api/v1/oficial-justica/produtividade")
public class OficialJusticaProdutividadeController {

    private static final String ROLES = "hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')";

    private final OficialJusticaProdutividadeService produtividadeService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public OficialJusticaProdutividadeController(OficialJusticaProdutividadeService produtividadeService,
                                                 CurrentUserService currentUserService,
                                                 CapabilityRateLimiter rateLimiter) {
        this.produtividadeService = Objects.requireNonNull(produtividadeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping
    @PreAuthorize(ROLES)
    public ResponseEntity<OficialJusticaProdutividadePainelResponse> painel(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int diasJanela,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_produtividade_painel", ApiVersion.V1);
        Usuario oficial = currentUserService.getRequired();
        return ResponseEntity.ok(produtividadeService.painel(oficial.getId(), diasJanela));
    }
}
