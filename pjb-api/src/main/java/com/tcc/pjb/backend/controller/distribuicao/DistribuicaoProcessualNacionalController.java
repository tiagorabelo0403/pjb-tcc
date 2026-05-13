package com.tcc.pjb.backend.controller.distribuicao;

import com.tcc.pjb.backend.model.dto.distribuicao.DistribuicaoProcessualPayloadResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistribuicaoProcessualResultResponse;
import com.tcc.pjb.backend.model.dto.distribuicao.DistributionWorkbenchResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualDiagnosticoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.distribuicao.surface.DistribuicaoProcessualSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/distribuicao/processual")
public class DistribuicaoProcessualNacionalController {

    private final DistribuicaoProcessualSurfaceFacadeService distribuicaoProcessualSurfaceFacadeService;
    private final CapabilityRateLimiter rateLimiter;

    public DistribuicaoProcessualNacionalController(DistribuicaoProcessualSurfaceFacadeService distribuicaoProcessualSurfaceFacadeService,
                                                    CapabilityRateLimiter rateLimiter) {
        this.distribuicaoProcessualSurfaceFacadeService = distribuicaoProcessualSurfaceFacadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','ADVOGADO','PROCURADOR','ADMINISTRADOR','ADMIN')")
    public ResponseEntity<DistribuicaoProcessualResultResponse> distribuir(@Valid @RequestBody DistribuicaoProcessualRequest request,
                                                                           Authentication authentication) {
        enforce(authentication, "distribuicao_processual_distribuir");
        return ResponseEntity.status(HttpStatus.CREATED).body(distribuicaoProcessualSurfaceFacadeService.distribuir(request));
    }

    @PostMapping("/processos/{processoId}/redistribuicao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO','ADMINISTRADOR','ADMIN')")
    public ResponseEntity<DistribuicaoProcessualPayloadResponse> redistribuir(@PathVariable Long processoId,
                                                                              @RequestParam String motivoImpedimento,
                                                                              Authentication authentication) {
        enforce(authentication, "distribuicao_processual_redistribuir");
        return ResponseEntity.status(HttpStatus.CREATED).body(distribuicaoProcessualSurfaceFacadeService.redistribuir(processoId, motivoImpedimento));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DistribuicaoProcessualPayloadResponse> consultar(@RequestParam String numeroProcesso,
                                                                           Authentication authentication) {
        enforce(authentication, "distribuicao_processual_consultar");
        return ResponseEntity.ok(distribuicaoProcessualSurfaceFacadeService.consultar(numeroProcesso));
    }

    @GetMapping("/workbench")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DistributionWorkbenchResponse> workbench(@RequestParam String numeroProcesso,
                                                                   Authentication authentication) {
        enforce(authentication, "distribuicao_processual_workbench");
        return ResponseEntity.ok(distribuicaoProcessualSurfaceFacadeService.workbench(numeroProcesso));
    }

    @PostMapping("/diagnostico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DistribuicaoProcessualPayloadResponse> diagnosticar(@Valid @RequestBody DistribuicaoProcessualDiagnosticoRequest request,
                                                                              Authentication authentication) {
        enforce(authentication, "distribuicao_processual_diagnostico");
        return ResponseEntity.ok(distribuicaoProcessualSurfaceFacadeService.diagnosticar(request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
