package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.UsuarioRequest;
import com.tcc.pjb.backend.model.dto.UsuarioResponse;
import com.tcc.pjb.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/usuarios") 
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        List<UsuarioResponse> lista = usuarioService.listarTodosUsuarios();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        UsuarioResponse response = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criarUsuario(
            @Valid @RequestBody UsuarioRequest dto
    ) {
        UsuarioResponse response = usuarioService.criarUsuario(dto);
        return ResponseEntity.status(201).body(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest dto
    ) {
        UsuarioResponse response = usuarioService.atualizarUsuario(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    
    public ResponseEntity<Void> desativarUsuario(@PathVariable Long id) {
        usuarioService.desativarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}