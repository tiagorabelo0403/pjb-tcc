package com.tcc.pjb.backend.controller.audiencia;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaResponse;
import com.tcc.pjb.backend.model.dto.audiencia.DesignarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.ReagendarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.RealizarAudienciaRequest;
import com.tcc.pjb.backend.service.audiencia.PautaAudienciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = OperationalApiRoutes.PAUTA_AUDIENCIA_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR','ROLE_MAGISTRADO')")
public class PautaAudienciaController {

    private final PautaAudienciaService service;

    public PautaAudienciaController(PautaAudienciaService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AudienciaResponse> designar(@Valid @RequestBody DesignarAudienciaRequest request) {
        try {
            return ResponseEntity.status(201).body(service.designar(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(OperationalApiRoutes.PATH_PAUTA_POR_ID)
    public ResponseEntity<AudienciaResponse> buscarPorId(@PathVariable Long audienciaId) {
        return service.buscarPorId(audienciaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(OperationalApiRoutes.PATH_PAUTA_AGENDA)
    public ResponseEntity<List<AudienciaResponse>> agenda(
            @RequestParam String vara,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(service.listarAgendaPorVara(vara, inicio, fim));
    }

    @GetMapping("/processo/{processoId}")
    public ResponseEntity<List<AudienciaResponse>> porProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.listarPorProcesso(processoId));
    }

    @PostMapping(value = OperationalApiRoutes.PATH_PAUTA_REDESIGNAR, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AudienciaResponse> reagendar(@PathVariable Long audienciaId,
                                                        @Valid @RequestBody ReagendarAudienciaRequest request) {
        try {
            return service.reagendar(audienciaId, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(value = OperationalApiRoutes.PATH_PAUTA_CANCELAR, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AudienciaResponse> cancelar(@PathVariable Long audienciaId,
                                                       @RequestBody(required = false) String motivo) {
        try {
            return service.cancelar(audienciaId, motivo)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(value = OperationalApiRoutes.PATH_PAUTA_REALIZAR, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AudienciaResponse> realizar(@PathVariable Long audienciaId,
                                                       @Valid @RequestBody RealizarAudienciaRequest request) {
        return service.realizar(audienciaId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
