package com.tcc.pjb.backend.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaCreateRequest;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaResponse;
import com.tcc.pjb.backend.model.dto.workspace.etiqueta.WorkspaceEtiquetaUpdateRequest;
import com.tcc.pjb.backend.service.workspace.WorkspaceEtiquetaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceEtiquetaController {

    private final WorkspaceEtiquetaService etiquetaService;

    

    @PostMapping("/etiquetas")
    public ResponseEntity<WorkspaceEtiquetaResponse> criar(@Valid @RequestBody WorkspaceEtiquetaCreateRequest req) {
        return ResponseEntity.ok(etiquetaService.criar(req));
    }

    @GetMapping("/etiquetas")
    public ResponseEntity<List<WorkspaceEtiquetaResponse>> listar() {
        return ResponseEntity.ok(etiquetaService.listarMinhas());
    }

    @PutMapping("/etiquetas/{id}")
    public ResponseEntity<WorkspaceEtiquetaResponse> atualizar(@PathVariable UUID id,
                                                              @Valid @RequestBody WorkspaceEtiquetaUpdateRequest req) {
        return ResponseEntity.ok(etiquetaService.atualizar(id, req));
    }

    @DeleteMapping("/etiquetas/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        etiquetaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    

    @PostMapping("/processos/{processoId}/etiquetas/{etiquetaId}")
    public ResponseEntity<Void> atribuir(@PathVariable Long processoId,
                                         @PathVariable UUID etiquetaId) {
        etiquetaService.atribuirAoProcesso(processoId, etiquetaId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/processos/{processoId}/etiquetas/{etiquetaId}")
    public ResponseEntity<Void> remover(@PathVariable Long processoId,
                                        @PathVariable UUID etiquetaId) {
        etiquetaService.removerDoProcesso(processoId, etiquetaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/processos/{processoId}/etiquetas")
    public ResponseEntity<List<WorkspaceEtiquetaResponse>> listarDoProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(etiquetaService.listarDoProcesso(processoId));
    }
}
