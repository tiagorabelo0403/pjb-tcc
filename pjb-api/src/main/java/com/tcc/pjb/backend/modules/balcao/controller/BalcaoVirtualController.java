package com.tcc.pjb.backend.modules.balcao.controller;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.modules.balcao.dto.AbrirAtendimentoRequest;
import com.tcc.pjb.backend.modules.balcao.dto.BalcaoAtendimentoResponse;
import com.tcc.pjb.backend.modules.balcao.dto.EncerrarAtendimentoRequest;
import com.tcc.pjb.backend.modules.balcao.service.BalcaoVirtualService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = OperationalApiRoutes.BALCAO_VIRTUAL_BASE, produces = MediaType.APPLICATION_JSON_VALUE)
public class BalcaoVirtualController {

    private final BalcaoVirtualService service;

    public BalcaoVirtualController(BalcaoVirtualService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping(value = OperationalApiRoutes.PATH_BALCAO_ATENDIMENTOS, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalcaoAtendimentoResponse> abrir(@Valid @RequestBody AbrirAtendimentoRequest request) {
        return ResponseEntity.status(201).body(service.abrir(request));
    }

    @GetMapping(OperationalApiRoutes.PATH_BALCAO_PROTOCOLO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalcaoAtendimentoResponse> buscarPorProtocolo(@PathVariable String protocolo) {
        return service.buscarPorProtocolo(protocolo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(OperationalApiRoutes.PATH_BALCAO_ATENDIMENTO_ID)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalcaoAtendimentoResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(OperationalApiRoutes.PATH_BALCAO_FILA)
    @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<BalcaoAtendimentoResponse>> listarFila(@RequestParam(required = false) String vara) {
        return ResponseEntity.ok(service.listarFila(vara));
    }

    @PostMapping(value = OperationalApiRoutes.PATH_BALCAO_CHAMAR, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<BalcaoAtendimentoResponse> chamar(@PathVariable Long id,
                                                             @RequestBody(required = false) String atendidoPor) {
        try {
            return ResponseEntity.ok(service.chamar(id, atendidoPor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(OperationalApiRoutes.PATH_BALCAO_INICIAR)
    @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<BalcaoAtendimentoResponse> iniciar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.iniciar(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(value = OperationalApiRoutes.PATH_BALCAO_CONCLUIR, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<BalcaoAtendimentoResponse> concluir(@PathVariable Long id,
                                                               @Valid @RequestBody EncerrarAtendimentoRequest request) {
        try {
            return ResponseEntity.ok(service.concluir(id, request.observacoes()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(OperationalApiRoutes.PATH_BALCAO_NAO_COMPARECEU)
    @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<BalcaoAtendimentoResponse> naoCompareceu(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.naoCompareceu(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @PostMapping(OperationalApiRoutes.PATH_BALCAO_CANCELAR)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BalcaoAtendimentoResponse> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.cancelar(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
