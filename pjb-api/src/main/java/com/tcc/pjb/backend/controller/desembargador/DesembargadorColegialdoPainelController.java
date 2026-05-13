package com.tcc.pjb.backend.controller.desembargador;

import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorAcordaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorDestaqueRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorImpedimentoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorSessaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorSustentacaoOralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorVistaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DesembargadorVotoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.painel.PainelSharedExperienceResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.desembargador.surface.DesembargadorColegialSurfaceFacadeService;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/desembargador/colegiado")
@Validated
public class DesembargadorColegialdoPainelController {

    private static final String ROLES = "hasAnyRole('DESEMBARGADOR','DESEMBARGADOR_FEDERAL')";
    private final DesembargadorColegialSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;

    public DesembargadorColegialdoPainelController(DesembargadorColegialSurfaceFacadeService facadeService,
                                                   CapabilityRateLimiter rateLimiter,
                                            PainelSharedExperienceService sharedExperienceService) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "desembargador_snapshot");
        return ResponseEntity.ok(facadeService.snapshot());
    }

    @GetMapping("/governanca")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> governanca(Authentication authentication) {
        enforce(authentication, "desembargador_governanca");
        return ResponseEntity.ok(facadeService.governanca());
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        enforce(authentication, "desembargador_malha");
        return ResponseEntity.ok(facadeService.malhaProcesso(processoId));
    }

    @PostMapping("/processos/{processoId}/voto")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> proferirVoto(@PathVariable Long processoId,
                                                              @Valid @RequestBody DesembargadorVotoRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "desembargador_voto");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.proferirVoto(processoId, request.voto(), request.fundamentacao(), request.decisao()));
    }

    @PostMapping("/processos/{processoId}/acordao")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> lavrarAcordao(@PathVariable Long processoId,
                                                               @Valid @RequestBody DesembargadorAcordaoRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "desembargador_acordao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.lavrarAcordao(processoId, request.ementa(), request.dispositivo(), request.fundamentacao()));
    }

    @PostMapping("/processos/{processoId}/vista")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> pedirVista(@PathVariable Long processoId,
                                                            @Valid @RequestBody DesembargadorVistaRequest request,
                                                            Authentication authentication) {
        enforce(authentication, "desembargador_vista");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.pedirVista(processoId, request.diasVista()));
    }

    @PostMapping("/processos/{processoId}/destaque")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> gerenciarDestaque(@PathVariable Long processoId,
                                                                   @RequestBody(required = false) DesembargadorDestaqueRequest request,
                                                                   Authentication authentication) {
        enforce(authentication, "desembargador_destaque");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.gerenciarDestaque(processoId, request == null ? null : request.motivo()));
    }

    @PostMapping("/processos/{processoId}/sustentacao-oral")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> deferirSustentacao(@PathVariable Long processoId,
                                                                    @RequestBody DesembargadorSustentacaoOralRequest request,
                                                                    Authentication authentication) {
        enforce(authentication, "desembargador_sustentacao_oral");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.deferirSustentacao(processoId, request.deferido(), request.observacao()));
    }

    @PostMapping("/sessao/abrir")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> abrirSessao(@RequestBody(required = false) DesembargadorSessaoRequest request,
                                                             Authentication authentication) {
        enforce(authentication, "desembargador_sessao_abrir");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.abrirSessao(request == null ? null : request.sessaoId(), request == null ? null : request.pauta()));
    }

    @PostMapping("/sessao/fechar")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> fecharSessao(@RequestBody(required = false) DesembargadorSessaoRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "desembargador_sessao_fechar");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.fecharSessao(request == null ? null : request.sessaoId(), request == null ? null : request.pauta()));
    }

    @PostMapping("/processos/{processoId}/impedimento")
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> registrarImpedimento(@PathVariable Long processoId,
                                                                      @RequestBody DesembargadorImpedimentoRequest request,
                                                                      Authentication authentication) {
        enforce(authentication, "desembargador_impedimento");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.registrarImpedimento(processoId, request.tipo(), request.fundamento()));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }


    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize(ROLES)
    public ResponseEntity<PainelSharedExperienceResponse> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "desembargador_painel_experiencia_compartilhada", ApiVersion.V1);
        var snapshot = sharedExperienceService.snapshot("DESEMBARGADOR");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(new PainelSharedExperienceResponse(
                        String.valueOf(snapshot.get("panelCode")),
                        snapshot.get("calendar"),
                        snapshot.get("deadlines"),
                        snapshot.get("colors"),
                        snapshot.get("calculator"),
                        snapshot.get("reading"),
                        snapshot.get("notes")));
    }

}
