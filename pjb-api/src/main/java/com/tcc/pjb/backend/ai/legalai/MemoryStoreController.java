package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicApiUnavailableException;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicMemoryStoreClient;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreCreateRequest;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreResponse;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryStoreUpdateRequest;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import jakarta.validation.Valid;
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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/legal-ai/memory-stores", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemoryStoreController {

    private final MemoryStoreRepository memoryStoreRepository;
    private final AnthropicMemoryStoreClient anthropicMemoryStoreClient;
    private final Clock clock;

    public MemoryStoreController(
            MemoryStoreRepository memoryStoreRepository,
            AnthropicMemoryStoreClient anthropicMemoryStoreClient,
            Clock clock) {
        this.memoryStoreRepository = memoryStoreRepository;
        this.anthropicMemoryStoreClient = anthropicMemoryStoreClient;
        this.clock = clock;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryStoreResponse> criar(@Valid @RequestBody MemoryStoreCreateRequest request) {
        try {
            MemorySigiloNivel nivel = MemorySigiloNivel.valueOf(request.sigiloNivel());
            MemoryAccessPolicy.AccessType access = MemoryAccessPolicy.computarAccessType(nivel);
            MemoryStoreId storeId = MemoryStoreId.gerar();
            Instant agora = Instant.now(clock);

            MemoryStore store = MemoryStore.criar(storeId, request.nome(), request.descricao(), request.moduloOrigem(), nivel, agora);

            if (store.podeEnviarParaAnthropicApi()) {
                AnthropicMemoryStoreClient.AnthropicStoreRef ref = anthropicMemoryStoreClient.criarStore(
                        request.nome(), request.descricao(), access);
                store = store.comAnthropicStoreId(ref.id());
            }

            MemoryStore salvo = memoryStoreRepository.salvar(store);
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
        return memoryStoreRepository.buscarPorId(MemoryStoreId.de(storeId))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemoryStoreResponse>> listar() {
        List<MemoryStoreResponse> stores = memoryStoreRepository.listarAtivos()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(stores);
    }

    @PatchMapping(path = "/{storeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryStoreResponse> atualizar(
            @PathVariable UUID storeId,
            @RequestBody MemoryStoreUpdateRequest request) {
        return memoryStoreRepository.buscarPorId(MemoryStoreId.de(storeId))
                .map(store -> {
                    String novoNome = request.nome() != null ? request.nome() : store.nome();
                    String novaDescricao = request.descricao() != null ? request.descricao() : store.descricao();
                    MemoryStore atualizado = new MemoryStore(
                            store.id(), novoNome, novaDescricao, store.anthropicStoreId(),
                            store.moduloOrigem(), store.sigiloNivel(), store.accessType(),
                            store.criadoEm(), store.arquivadoEm(), store.entries()
                    );
                    return ResponseEntity.ok(toResponse(memoryStoreRepository.salvar(atualizado)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> arquivar(@PathVariable UUID storeId) {
        var storeOpt = memoryStoreRepository.buscarPorId(MemoryStoreId.de(storeId));
        if (storeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        MemoryStore store = storeOpt.get();
        if (store.anthropicStoreId() != null && store.podeEnviarParaAnthropicApi()) {
            anthropicMemoryStoreClient.arquivarStore(store.anthropicStoreId());
        }
        memoryStoreRepository.salvar(store.arquivado(Instant.now(clock)));
        return ResponseEntity.noContent().build();
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
                !store.isArquivado()
        );
    }
}
