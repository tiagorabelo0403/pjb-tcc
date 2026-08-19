package com.tcc.pjb.backend.modules.suporte.controller;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.suporte.dto.AbrirChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.ResolverChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.SupportTicketResponse;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import com.tcc.pjb.backend.modules.suporte.service.SupportTicketService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suporte/chamados")
public class SupportTicketController {

    private final SupportTicketService service;
    private final CurrentUserService currentUserService;

    public SupportTicketController(SupportTicketService service, CurrentUserService currentUserService) {
        this.service = Objects.requireNonNull(service);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketResponse> abrir(@Valid @RequestBody AbrirChamadoRequest request) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrir(usuario, request));
    }

    @GetMapping("/meus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupportTicketResponse>> meus() {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(service.meusChamados(usuario.getId()));
    }

    @GetMapping("/fila")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPORTE_TECNICO','ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<SupportTicketResponse>> fila(@RequestParam(required = false) SupportTicketStatus status) {
        return ResponseEntity.ok(service.fila(status));
    }

    @PostMapping("/{id}/assumir")
    @PreAuthorize("hasAuthority('ROLE_SUPORTE_TECNICO')")
    public ResponseEntity<SupportTicketResponse> assumir(@PathVariable Long id) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(service.assumir(id, usuario));
    }

    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasAuthority('ROLE_SUPORTE_TECNICO')")
    public ResponseEntity<SupportTicketResponse> resolver(@PathVariable Long id,
                                                           @Valid @RequestBody ResolverChamadoRequest request) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(service.resolver(id, usuario, request));
    }

    @PostMapping("/{id}/fechar")
    @PreAuthorize("hasAuthority('ROLE_SUPORTE_TECNICO')")
    public ResponseEntity<SupportTicketResponse> fechar(@PathVariable Long id) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(service.fechar(id, usuario));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketResponse> cancelar(@PathVariable Long id) {
        Usuario usuario = currentUserService.getRequired();
        return ResponseEntity.ok(service.cancelar(id, usuario));
    }
}
