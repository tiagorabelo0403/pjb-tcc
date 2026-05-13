package com.tcc.pjb.backend.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.workspace.localizador.*;
import com.tcc.pjb.backend.service.workspace.WorkspaceLocalizadorQueryService;
import com.tcc.pjb.backend.service.workspace.WorkspaceLocalizadorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceLocalizadorController {

    private final WorkspaceLocalizadorService localizadorService;
    private final WorkspaceLocalizadorQueryService localizadorQueryService;

    @PostMapping("/localizadores")
    public ResponseEntity<WorkspaceLocalizadorResponse> criar(@Valid @RequestBody WorkspaceLocalizadorCreateRequest req) {
        return ResponseEntity.ok(localizadorService.criar(req));
    }

    @GetMapping("/localizadores")
    public ResponseEntity<List<WorkspaceLocalizadorResponse>> listar() {
        return ResponseEntity.ok(localizadorService.listar());
    }

    @GetMapping("/localizadores/{id}")
    public ResponseEntity<WorkspaceLocalizadorResponse> obter(@PathVariable UUID id) {
        return ResponseEntity.ok(localizadorService.obter(id));
    }

    @PutMapping("/localizadores/{id}")
    public ResponseEntity<WorkspaceLocalizadorResponse> atualizar(@PathVariable UUID id,
                                                                  @Valid @RequestBody WorkspaceLocalizadorUpdateRequest req) {
        return ResponseEntity.ok(localizadorService.atualizar(id, req));
    }

    @DeleteMapping("/localizadores/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        localizadorService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/localizadores/{id}/processos")
    public ResponseEntity<Page<WorkspaceProcessoResumoResponse>> listarProcessos(
            @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "dataUltimaMovimentacao") String sortBy,
            @RequestParam(name = "dir", defaultValue = "desc") String dir
    ) {
        return ResponseEntity.ok(localizadorQueryService.listarProcessos(id, page, size, sortBy, dir));
    }

    
    @PostMapping("/localizadores/preview")
    public ResponseEntity<Page<WorkspaceProcessoResumoResponse>> preview(
            @Valid @RequestBody WorkspaceLocalizadorCriteria criteria,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "dataUltimaMovimentacao") String sortBy,
            @RequestParam(name = "dir", defaultValue = "desc") String dir
    ) {
        return ResponseEntity.ok(localizadorQueryService.preview(criteria, page, size, sortBy, dir));
    }
}
