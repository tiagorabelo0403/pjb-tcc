package com.tcc.pjb.backend.controller.processual.surface.hardening;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoSigiloInteligenteController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoSigiloInteligenteController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/sigilo-inteligente")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> sigiloInteligente(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.sigiloInteligente(processoId));
    }

    @GetMapping("/{processoId}/sigilo-notificacoes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> sigiloNotificacoes(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.sigiloNotificacoes(processoId));
    }

    @PostMapping("/{processoId}/sigilo-notificacoes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> dispararSigiloNotificacoes(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.dispararSigiloNotificacoes(processoId));
    }
}
