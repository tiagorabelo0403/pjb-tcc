package com.tcc.pjb.backend.controller.advogado;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoClienteAnaliticoItemResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCockpitSnapshotResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCustaItemResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoHonorariosResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOperacaoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoCienciaLoteRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoHonorariosCalculoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoPeticaoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/advogado/cockpit")
@Validated
@PreAuthorize("hasAnyRole('ADVOGADO','OAB_PRESIDENTE_SECCIONAL')")
public class AdvogadoCockpitController {

    private final AdvogadoSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public AdvogadoCockpitController(AdvogadoSurfaceFacadeService facadeService,
                                     CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<AdvogadoCockpitSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "advogado_cockpit_snapshot");
        return ResponseEntity.ok(facadeService.cockpitSnapshot());
    }

    @PostMapping("/processos/{processoId}/peticao")
    public ResponseEntity<AdvogadoOperacaoResponse> protocolizarPeticao(@PathVariable Long processoId,
                                                                        @Valid @RequestBody AdvogadoPeticaoRequest request,
                                                                        Authentication authentication) {
        enforce(authentication, "advogado_cockpit_peticao");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facadeService.protocolizarPeticao(processoId, request.tipoPeticao(), request.conteudo(), request.fundamentacao()));
    }

    @PostMapping("/intimacoes/ciencia-lote")
    public ResponseEntity<AdvogadoOperacaoResponse> darCienciaEmLote(@Valid @RequestBody AdvogadoCienciaLoteRequest request,
                                                                     Authentication authentication) {
        enforce(authentication, "advogado_cockpit_ciencia_lote");
        return ResponseEntity.ok(facadeService.darCienciaEmLote(request.workItemIds()));
    }

    @PostMapping("/processos/{processoId}/honorarios/calcular")
    public ResponseEntity<AdvogadoHonorariosResponse> calcularHonorarios(@PathVariable Long processoId,
                                                                          @Valid @RequestBody AdvogadoHonorariosCalculoRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "advogado_cockpit_honorarios");
        return ResponseEntity.ok(facadeService.calcularHonorarios(processoId, request));
    }

    @GetMapping("/processos/{processoId}/custas")
    public ResponseEntity<List<AdvogadoCustaItemResponse>> listarCustas(@PathVariable Long processoId,
                                                                         Authentication authentication) {
        enforce(authentication, "advogado_cockpit_custas");
        return ResponseEntity.ok(facadeService.listarCustas(processoId));
    }

    @GetMapping("/clientes/analitico")
    public ResponseEntity<List<AdvogadoClienteAnaliticoItemResponse>> analiticoPorCliente(@RequestParam String clienteCpfCnpj,
                                                                                            Authentication authentication) {
        enforce(authentication, "advogado_cockpit_analitico_cliente");
        return ResponseEntity.ok(facadeService.analiticoPorCliente(clienteCpfCnpj));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.LAWYER, authentication, capability, ApiVersion.V1);
    }
}
