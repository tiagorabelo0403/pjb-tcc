package com.tcc.pjb.backend.controller.notification;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.intimacao.CriarIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.intimacao.IntimacaoAudienciaResponse;
import com.tcc.pjb.backend.service.notification.IntimacaoAudienciaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = OperationalApiRoutes.INTIMACAO_AUDIENCIA_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class IntimacaoAudienciaController {

    private final IntimacaoAudienciaService service;

    public IntimacaoAudienciaController(IntimacaoAudienciaService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public ResponseEntity<List<IntimacaoAudienciaResponse>> listar(@PathVariable Long audienciaId) {
        return ResponseEntity.ok(service.listarPorAudiencia(audienciaId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntimacaoAudienciaResponse> intimar(@PathVariable Long audienciaId,
                                                               @Valid @RequestBody CriarIntimacaoRequest request) {
        return service.intimar(audienciaId, request)
                .map(r -> ResponseEntity.status(201).body(r))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/lote", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<IntimacaoAudienciaResponse>> intimarLote(@PathVariable Long audienciaId,
                                                                          @RequestBody List<@Valid CriarIntimacaoRequest> requests) {
        try {
            return ResponseEntity.status(201).body(service.intimarLote(audienciaId, requests));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = OperationalApiRoutes.PATH_INTIMACAO_CIENCIA)
    public ResponseEntity<IntimacaoAudienciaResponse> registrarCiencia(@PathVariable Long audienciaId,
                                                                         @PathVariable Long intimacaoId) {
        try {
            return service.registrarCiencia(intimacaoId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
