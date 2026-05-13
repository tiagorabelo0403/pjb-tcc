package com.tcc.pjb.backend.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.JurisdicaoRequest;
import com.tcc.pjb.backend.model.dto.JurisdicaoResponse;
import com.tcc.pjb.backend.service.JurisdicaoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/jurisdicoes")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class JurisdicaoController {

    private final JurisdicaoService jurisdicaoService;

    @GetMapping
    public ResponseEntity<List<JurisdicaoResponse>> listar() {
        return ResponseEntity.ok(jurisdicaoService.listarTodas());
    }

    
    @GetMapping("/suggest")
    public ResponseEntity<List<JurisdicaoResponse>> suggest(
            @RequestParam("q") String termo,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(jurisdicaoService.sugerir(termo, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JurisdicaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jurisdicaoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<JurisdicaoResponse> criar(@Valid @RequestBody JurisdicaoRequest dto) {
        JurisdicaoResponse response = jurisdicaoService.criarJurisdicao(dto);
        URI location = URI.create("/api/v1/jurisdicoes/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<JurisdicaoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody JurisdicaoRequest dto
    ) {
        return ResponseEntity.ok(jurisdicaoService.atualizarJurisdicao(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        jurisdicaoService.desativarJurisdicao(id);
        return ResponseEntity.noContent().build();
    }
}
