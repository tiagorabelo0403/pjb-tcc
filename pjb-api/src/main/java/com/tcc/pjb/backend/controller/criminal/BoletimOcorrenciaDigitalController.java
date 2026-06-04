package com.tcc.pjb.backend.controller.criminal;

import com.tcc.pjb.backend.model.dto.criminal.BoletimOcorrenciaCadastroRequest;
import com.tcc.pjb.backend.model.dto.criminal.BoletimOcorrenciaVinculoInqueritoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.criminal.surface.BoletimOcorrenciaDigitalSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.UUID;
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
@RequestMapping("/api/v1/boletins-ocorrencia-digitais")
public class BoletimOcorrenciaDigitalController {

    private final BoletimOcorrenciaDigitalSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public BoletimOcorrenciaDigitalController(BoletimOcorrenciaDigitalSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceCollectionResponse> meus(Authentication authentication) {
        enforce(authentication, "boletim_ocorrencia_listar");
        return ResponseEntity.ok(facadeService.listarMeus());
    }

    @GetMapping("/{boletimUuid}")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> buscar(@PathVariable UUID boletimUuid,
                                                          Authentication authentication) {
        enforce(authentication, "boletim_ocorrencia_buscar");
        return ResponseEntity.ok(facadeService.buscar(boletimUuid));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> registrar(@Valid @RequestBody BoletimOcorrenciaCadastroRequest request,
                                                             Authentication authentication) {
        enforce(authentication, "boletim_ocorrencia_registrar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrar(request));
    }

    @PostMapping("/{boletimUuid}/vinculos/inquerito")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','ESCRIVAO_POLICIAL')")
    public ResponseEntity<SurfaceSnapshotResponse> vincularInquerito(@PathVariable UUID boletimUuid,
                                                                     @Valid @RequestBody BoletimOcorrenciaVinculoInqueritoRequest request,
                                                                     Authentication authentication) {
        enforce(authentication, "boletim_ocorrencia_vincular_inquerito");
        return ResponseEntity.ok(facadeService.vincularInquerito(boletimUuid, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
