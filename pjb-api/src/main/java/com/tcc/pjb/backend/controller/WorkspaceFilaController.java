package com.tcc.pjb.backend.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.workspace.fila.*;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceProcessoResumoResponse;
import com.tcc.pjb.backend.service.workspace.WorkspaceFilaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceFilaController {

    private final WorkspaceFilaService filaService;

    @GetMapping("/filas")
    public ResponseEntity<List<WorkspaceFilaResponse>> listar() {
        return ResponseEntity.ok(filaService.listar());
    }

    @PostMapping("/filas")
    public ResponseEntity<WorkspaceFilaResponse> criar(@Valid @RequestBody WorkspaceFilaCreateRequest req) {
        return ResponseEntity.ok(filaService.criar(req));
    }

    @GetMapping("/filas/{id}")
    public ResponseEntity<WorkspaceFilaResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(filaService.obter(id));
    }

    @PutMapping("/filas/{id}")
    public ResponseEntity<WorkspaceFilaResponse> atualizar(@PathVariable UUID id,
                                                          @Valid @RequestBody WorkspaceFilaUpdateRequest req) {
        return ResponseEntity.ok(filaService.atualizar(id, req));
    }

    @DeleteMapping("/filas/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        filaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filas/{id}/processos")
    public ResponseEntity<Page<WorkspaceProcessoResumoResponse>> processos(
            @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "dataUltimaMovimentacao") String sortBy,
            @RequestParam(name = "dir", defaultValue = "desc") String dir
    ) {
        return ResponseEntity.ok(filaService.listarProcessos(id, page, size, sortBy, dir));
    }

    @GetMapping("/filas/{id}/workitems")
    public ResponseEntity<Page<WorkspaceFilaWorkItemResumoResponse>> workitems(
            @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(filaService.listarWorkItems(id, page, size));
    }
}
