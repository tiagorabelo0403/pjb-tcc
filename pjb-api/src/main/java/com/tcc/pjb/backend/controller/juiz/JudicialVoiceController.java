package com.tcc.pjb.backend.controller.juiz;

import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceDraftRequest;
import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceSessionChunkRequest;
import com.tcc.pjb.backend.model.dto.juiz.JudicialVoiceSessionOpenRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.juiz.surface.JudicialVoiceSurfaceFacadeService;
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

@RestController
@RequestMapping("/api/v1/magistratura/voice")
public class JudicialVoiceController {

    private final JudicialVoiceSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public JudicialVoiceController(JudicialVoiceSurfaceFacadeService facadeService,
                                   CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/sessoes")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "magistratura_voice_listar");
        return ResponseEntity.ok(facadeService.listar());
    }

    @GetMapping("/sessoes/{sessaoId}")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable Long sessaoId,
                                                            Authentication authentication) {
        enforce(authentication, "magistratura_voice_detalhar");
        return ResponseEntity.ok(facadeService.detalhar(sessaoId));
    }

    @PostMapping("/estruturar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> estruturar(@Valid @RequestBody JudicialVoiceDraftRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "magistratura_voice_estruturar");
        return ResponseEntity.ok(facadeService.estruturar(request));
    }

    @PostMapping("/sessoes")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> abrirSessao(@Valid @RequestBody JudicialVoiceSessionOpenRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "magistratura_voice_sessao_abrir");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.abrirSessao(request));
    }

    @PostMapping("/sessoes/{sessaoId}/trechos")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> adicionarTrecho(@PathVariable Long sessaoId,
                                                                   @Valid @RequestBody JudicialVoiceSessionChunkRequest request,
                                                                   Authentication authentication) {
        enforce(authentication, "magistratura_voice_trecho_adicionar");
        return ResponseEntity.ok(facadeService.adicionarTrecho(sessaoId, request));
    }

    @PostMapping("/sessoes/{sessaoId}/finalizar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> finalizar(@PathVariable Long sessaoId,
                                                             Authentication authentication) {
        enforce(authentication, "magistratura_voice_finalizar");
        return ResponseEntity.ok(facadeService.finalizar(sessaoId));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
