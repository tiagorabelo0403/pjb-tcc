package com.tcc.pjb.backend.controller.oficial_justica;

import jakarta.validation.Valid;
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
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaPessoaRastreioResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;

/**
 * Rota do dia e localizador de pessoas do oficial de justiça, apoiados em
 * {@link InstitutionalPainelSurfaceFacadeService}. Extraído de {@link OficialJusticaCampoController}
 * (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaLocalizadorSurfaceController {

    private final CapabilityRateLimiter rateLimiter;
    private final InstitutionalPainelSurfaceFacadeService facadeService;

    public OficialJusticaLocalizadorSurfaceController(CapabilityRateLimiter rateLimiter,
                                                       InstitutionalPainelSurfaceFacadeService facadeService) {
        this.rateLimiter = rateLimiter;
        this.facadeService = facadeService;
    }

    @GetMapping("/rota/dia")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceCollectionResponse> rotaDia(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRotaDia());
    }

    @GetMapping("/localizador/consultas/recentes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceCollectionResponse> consultasRecentes(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_consultas_recentes", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialConsultasRecentes());
    }

    @GetMapping("/localizador/rastreio/resumo")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> resumoRastreioOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastreio_resumo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialResumoRastreioOperacional());
    }

    @GetMapping("/localizador/triagem-enderecos")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> triagemEnderecos(@RequestParam(defaultValue = "8") int limit,
                                                                    @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                    @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                    @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_triagem_enderecos", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialTriagemEnderecos(limit, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @GetMapping("/localizador/mandados/{mandadoId}/rastreio")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaPessoaRastreioResponse> rastrearMandado(@PathVariable String mandadoId,
                                                                                 @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                                 @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                                 @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastrear_mandado", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRastrearMandado(mandadoId, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @GetMapping("/localizador/processos/{processoId}/alvos/{polo}/rastreio")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<OficialJusticaPessoaRastreioResponse> rastrearProcessoAlvo(@PathVariable Long processoId,
                                                                                      @PathVariable String polo,
                                                                                      @RequestParam(defaultValue = "true") boolean incluirEnderecoEstrito,
                                                                                      @RequestParam(defaultValue = "true") boolean incluirProntuario,
                                                                                      @RequestParam(defaultValue = "false") boolean incluirRestricoes,
                                                                                      Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_rastrear_processo_alvo", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialRastrearProcessoAlvo(processoId, polo, incluirEnderecoEstrito, incluirProntuario, incluirRestricoes));
    }

    @PostMapping("/localizador/pessoas")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<PessoaLocalizacaoResponse> localizarPessoa(@Valid @RequestBody PessoaLocalizacaoRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_localizador_pessoas", ApiVersion.V1);
        return ResponseEntity.ok(facadeService.oficialLocalizarPessoa(request));
    }
}
