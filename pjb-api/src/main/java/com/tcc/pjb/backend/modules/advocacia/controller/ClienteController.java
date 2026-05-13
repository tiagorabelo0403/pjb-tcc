package com.tcc.pjb.backend.modules.advocacia.controller;

import java.net.URI;
import java.time.Instant;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.ai.core.LegalAiService;
import com.tcc.pjb.backend.modules.advocacia.dto.ClienteDTO;
import com.tcc.pjb.backend.modules.advocacia.service.ClienteService;

@RestController
@RequestMapping("/api/v1/modulos/advocacia/clientes")
@Validated
public class ClienteController {

    private final ClienteService clienteService;
    private final LegalAiService legalAiService;

    public ClienteController(ClienteService clienteService, LegalAiService legalAiService) {
        this.clienteService = clienteService;
        this.legalAiService = legalAiService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
    public ResponseEntity<ClienteDTO.ClienteResponse> criarCliente(@Valid @RequestBody ClienteDTO.ClienteRequest dto) {
        ClienteDTO.ClienteResponse response = clienteService.criarCliente(dto);
        legalAiService.analisarCadastroCliente(response.getNomeCompleto(), response.getCpfCnpj(), Instant.now());

        return ResponseEntity
                .created(URI.create("/api/v1/modulos/advocacia/clientes/" + response.getId()))
                .body(response);
    }

    @GetMapping("/busca")
    @PreAuthorize("hasAnyAuthority('ROLE_ADVOGADO','ROLE_SERVIDOR')")
    public ResponseEntity<Page<ClienteDTO.ClienteResponse>> buscarClientes(ClienteDTO.ClienteQuery query, Pageable pageable) {
        Page<ClienteDTO.ClienteResponse> pagina = clienteService.buscarClientes(query, pageable);
        legalAiService.auditarBuscaClientes(query, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADVOGADO','ROLE_SERVIDOR')")
    public ResponseEntity<ClienteDTO.ClienteResponse> buscarPorId(@PathVariable Long id) {
        ClienteDTO.ClienteResponse cliente = clienteService.buscarPorId(id);
        legalAiService.analisarHistoricoCliente(id);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
    public ResponseEntity<ClienteDTO.ClienteResponse> atualizarCliente(@PathVariable Long id,
                                                                       @Valid @RequestBody ClienteDTO.ClienteRequest dto) {
        ClienteDTO.ClienteResponse atualizado = clienteService.atualizarCliente(id, dto);
        legalAiService.verificarCoerenciaAtualizacao(id, atualizado.getNomeCompleto());
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADVOGADO')")
    public ResponseEntity<Void> excluirCliente(@PathVariable Long id) {
        clienteService.excluirCliente(id);
        legalAiService.auditarExclusaoCliente(id);
        return ResponseEntity.noContent().build();
    }
}
