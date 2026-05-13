package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.oab.OabEventoCreateRequest;
import com.tcc.pjb.backend.model.dto.oab.OabEventoResponse;
import com.tcc.pjb.backend.service.oab.OabInstitucionalService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/oab/solicitacoes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADVOGADO')")
public class OabSolicitacaoController {

    private final OabInstitucionalService oabService;

    @PostMapping
    public ResponseEntity<OabEventoResponse> criar(@RequestBody @Valid OabEventoCreateRequest request) {
        return ResponseEntity.ok(oabService.criarSolicitacao(request));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<OabEventoResponse>> minhas() {
        return ResponseEntity.ok(oabService.listarMinhasSolicitacoes());
    }
}
