package com.tcc.pjb.backend.controller.delegado;

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
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoDiligenciaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoInqueritoMultimidiaRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;

/**
 * Superfície de painel do delegado apoiada em {@link InstitutionalPainelSurfaceFacadeService}:
 * inquéritos, mandados, acesso a processo, requisição de diligência, alertas e localizador de
 * pessoas. Extraído de {@link DelegadoPainelController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoPainelSurfaceController {

    private final CapabilityRateLimiter rateLimiter;
    private final InstitutionalPainelSurfaceFacadeService facadeService;

    public DelegadoPainelSurfaceController(CapabilityRateLimiter rateLimiter,
                                           InstitutionalPainelSurfaceFacadeService facadeService) {
        this.rateLimiter = rateLimiter;
        this.facadeService = facadeService;
    }

    @GetMapping("/inqueritos/pendentes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> inqueritosPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_inqueritos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoInqueritosPendentes());
    }

    @PostMapping("/inqueritos/{inqueritoId}/peca-multimidia")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceActionResponse> registrarPecaInquerito(@PathVariable Long inqueritoId,
                                                                        @Valid @RequestBody DelegadoInqueritoMultimidiaRequest request,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_inquerito_peca_multimidia", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.delegadoRegistrarPecaInquerito(inqueritoId, request));
    }

    @GetMapping("/mandados/cumprir")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> mandadosPendentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_mandados", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoMandadosPendentes());
    }

    @GetMapping("/processos/{processoId}/acesso")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceActionResponse> solicitarAcessoProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_acesso_processo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoSolicitarAcessoProcesso(processoId));
    }

    @PostMapping("/requisicao/diligencia")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceActionResponse> requisitarDiligencia(@Valid @RequestBody DelegadoDiligenciaRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencia", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.delegadoRequisitarDiligencia(request));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> alertas(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_alertas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoAlertas());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/recentes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> consultasRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_consultas_recentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoConsultasRecentes());
    }

    @GetMapping("/inteligencia/pessoas/localizacao/metricas")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> metricasLocalizador(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_metricas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoMetricasLocalizador());
    }

    @PostMapping("/inteligencia/pessoas/localizacao")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<PessoaLocalizacaoResponse> localizarPessoa(@Valid @RequestBody PessoaLocalizacaoRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_localizador_pessoas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.delegadoLocalizarPessoa(request));
    }
}
