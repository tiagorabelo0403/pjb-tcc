package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioRequest;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioResponse;
import com.tcc.pjb.backend.service.OrgaoJudiciarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orgaos-judiciarios")
@RequiredArgsConstructor
public class OrgaoJudiciarioController {

    private final OrgaoJudiciarioService orgaoJudiciarioService;
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<OrgaoJudiciarioResponse>> listarOrgaos() {
        return ResponseEntity.ok(orgaoJudiciarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OrgaoJudiciarioResponse> buscarOrgaoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(orgaoJudiciarioService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<OrgaoJudiciarioResponse> criarOrgao(@Valid @RequestBody OrgaoJudiciarioRequest dto) {
        OrgaoJudiciarioResponse response = orgaoJudiciarioService.criarOrgaoJudiciario(dto);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<OrgaoJudiciarioResponse> atualizarOrgao(@PathVariable Long id, @Valid @RequestBody OrgaoJudiciarioRequest dto) {
        OrgaoJudiciarioResponse response = orgaoJudiciarioService.atualizarOrgaoJudiciario(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> desativarOrgao(@PathVariable Long id) {
        orgaoJudiciarioService.desativarOrgaoJudiciario(id);
        return ResponseEntity.noContent().build();
    }
}