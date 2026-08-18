package com.tcc.pjb.backend.controller.juiz;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.juiz.produtividade.JuizProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.juiz.produtividade.JuizProdutividadeService;
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
@RequestMapping(OperationalApiRoutes.JUDGE_PRODUTIVIDADE_BASE)
public class JuizProdutividadeController {

    private static final String JUDGE_ROLES = "hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR')";

    private final JuizProdutividadeService produtividadeService;
    private final CurrentUserService currentUserService;
    private final CapabilityRateLimiter rateLimiter;

    public JuizProdutividadeController(JuizProdutividadeService produtividadeService,
                                       CurrentUserService currentUserService,
                                       CapabilityRateLimiter rateLimiter) {
        this.produtividadeService = Objects.requireNonNull(produtividadeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
    }

    @GetMapping
    @PreAuthorize(JUDGE_ROLES)
    public ResponseEntity<JuizProdutividadePainelResponse> painel(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int diasJanela,
                                                                   Authentication authentication) {
        enforce(authentication, "juiz_produtividade_painel");
        Usuario magistrado = currentUserService.getRequired();
        return ResponseEntity.ok(produtividadeService.painel(magistrado.getId(), diasJanela));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
