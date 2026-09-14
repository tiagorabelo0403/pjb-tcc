package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.application.MemoryStoreApplicationService;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicApiUnavailableException;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreCreateRequest;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreResponse;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreUpdateRequest;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/legal-ai/memory-stores", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemoryStoreController {

    private final MemoryStoreApplicationService memoryStoreService;

    public MemoryStoreController(MemoryStoreApplicationService memoryStoreService) {
        this.memoryStoreService = Objects.requireNonNull(memoryStoreService);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryStoreResponse> criar(@Valid @RequestBody MemoryStoreCreateRequest request) {
        try {
            MemoryStore salvo = memoryStoreService.criar(
                    request.nome(), request.descricao(), request.moduloOrigem(), request.sigiloNivel());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(salvo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (AnthropicApiUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryStoreResponse> buscar(@PathVariable UUID storeId) {
        return memoryStoreService.buscarPorId(MemoryStoreId.de(storeId))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemoryStoreResponse>> listar() {
        List<MemoryStoreResponse> stores = memoryStoreService.listarAtivos()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(stores);
    }

    @PatchMapping(path = "/{storeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryStoreResponse> atualizar(
            @PathVariable UUID storeId,
            @RequestBody MemoryStoreUpdateRequest request) {
        return memoryStoreService.atualizar(MemoryStoreId.de(storeId), request.nome(), request.descricao())
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> arquivar(@PathVariable UUID storeId) {
        return memoryStoreService.arquivar(MemoryStoreId.de(storeId))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private MemoryStoreResponse toResponse(MemoryStore store) {
        return new MemoryStoreResponse(
                store.id().value().toString(),
                store.nome(),
                store.descricao(),
                store.anthropicStoreId(),
                store.moduloOrigem(),
                store.sigiloNivel() != null ? store.sigiloNivel().name() : null,
                store.accessType() != null ? store.accessType().name() : null,
                store.criadoEm(),
                store.arquivadoEm(),
                !store.isArquivado());
    }
}
