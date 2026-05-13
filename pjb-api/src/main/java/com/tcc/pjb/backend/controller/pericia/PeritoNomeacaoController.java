package com.tcc.pjb.backend.controller.pericia;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoResponse;
import com.tcc.pjb.backend.service.pericia.PeritoNomeacaoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MAGISTRADO','ROLE_JUIZ','ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR_FORUM','ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class PeritoNomeacaoController {

    private final PeritoNomeacaoService service;

    @PostMapping("/processos/{processoId}/peritos/nomear")
    public ResponseEntity<PeritoNomeacaoResponse> nomear(
            @PathVariable Long processoId,
            @Valid @RequestBody PeritoNomeacaoRequest req
    ) {
        return ResponseEntity.ok(service.nomear(processoId, req));
    }

    @PostMapping("/processos/{processoId}/peritos/{peritoId}/revogar")
    public ResponseEntity<PeritoNomeacaoResponse> revogar(
            @PathVariable Long processoId,
            @PathVariable Long peritoId,
            @RequestParam(value = "observacao", required = false) String observacao
    ) {
        return ResponseEntity.ok(service.revogar(processoId, peritoId, observacao));
    }

    @GetMapping("/processos/{processoId}/peritos")
    public ResponseEntity<List<PeritoNomeacaoResponse>> listar(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.listar(processoId));
    }
}
