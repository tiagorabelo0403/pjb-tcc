package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.oab.OabEventoResponse;
import com.tcc.pjb.backend.model.dto.oab.OabEventoStatusUpdateRequest;
import com.tcc.pjb.backend.model.dto.oab.OabProvidenciaCreateRequest;
import com.tcc.pjb.backend.model.dto.oab.OabProvidenciaResponse;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;
import com.tcc.pjb.backend.service.oab.OabInstitucionalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/oab/seccional")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OAB_PRESIDENTE_SECCIONAL')")
public class OabSeccionalController {

    private final OabInstitucionalService oabService;

    @GetMapping("/eventos")
    public ResponseEntity<List<OabEventoResponse>> listar(
            @RequestParam(name = "status", required = false) List<StatusEventoInstitucional> status,
            @RequestParam(name = "processoId", required = false) Long processoId,
            @RequestParam(name = "numeroProcesso", required = false) String numeroProcesso,
            @RequestParam(name = "orgao", required = false) String orgao,
            @RequestParam(name = "tribunal", required = false) String tribunal,
            @RequestParam(name = "includeProvidencias", defaultValue = "false") boolean includeProvidencias,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(oabService.listarSeccional(status, processoId, numeroProcesso, orgao, tribunal, includeProvidencias, limit));
    }

    @PatchMapping("/eventos/{id}/status")
    public ResponseEntity<OabEventoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid OabEventoStatusUpdateRequest req
    ) {
        return ResponseEntity.ok(oabService.atualizarStatus(id, req.getStatus()));
    }

    @PostMapping("/eventos/{id}/providencias")
    public ResponseEntity<OabProvidenciaResponse> providencia(
            @PathVariable Long id,
            @RequestBody @Valid OabProvidenciaCreateRequest req
    ) {
        return ResponseEntity.ok(oabService.adicionarProvidencia(id, req));
    }
}
