package com.tcc.pjb.backend.ai.legalai.application;

import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicMemoryStoreClient;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida dos memory stores da IA jurídica.
 *
 * <p>A regra que manda aqui não é de camada, é de proteção de dado: só memória classificada como
 * {@code PUBLIC} ou {@code INSTITUCIONAL} pode ser transmitida para a API externa da Anthropic.
 * {@code SIGILOSO} e {@code CRITICO} ficam em casa, na criação e no arquivamento — quem decide é
 * {@link MemoryAccessPolicy#podeEnviarParaAnthropicApi}, consultada pelo próprio agregado.
 */
@Service
public class MemoryStoreApplicationService {

    private final MemoryStoreRepository memoryStoreRepository;
    private final AnthropicMemoryStoreClient anthropicMemoryStoreClient;
    private final Clock clock;

    public MemoryStoreApplicationService(MemoryStoreRepository memoryStoreRepository,
                                         AnthropicMemoryStoreClient anthropicMemoryStoreClient,
                                         Clock clock) {
        this.memoryStoreRepository = Objects.requireNonNull(memoryStoreRepository);
        this.anthropicMemoryStoreClient = Objects.requireNonNull(anthropicMemoryStoreClient);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @throws IllegalArgumentException se o nível de sigilo informado não existir
     * @throws com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicApiUnavailableException se a API
     *     externa estiver fora do ar — e nesse caso nada é persistido, para não deixar um store local
     *     sem a contraparte externa que ele deveria ter
     */
    @Transactional
    public MemoryStore criar(String nome, String descricao, String moduloOrigem, String sigiloNivel) {
        MemorySigiloNivel nivel = MemorySigiloNivel.valueOf(sigiloNivel);
        MemoryAccessPolicy.AccessType access = MemoryAccessPolicy.computarAccessType(nivel);
        Instant agora = Instant.now(clock);

        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), nome, descricao, moduloOrigem, nivel, agora);

        if (store.podeEnviarParaAnthropicApi()) {
            AnthropicMemoryStoreClient.AnthropicStoreRef ref =
                    anthropicMemoryStoreClient.criarStore(nome, descricao, access);
            store = store.comAnthropicStoreId(ref.id());
        }

        return memoryStoreRepository.salvar(store);
    }

    @Transactional(readOnly = true)
    public Optional<MemoryStore> buscarPorId(MemoryStoreId storeId) {
        return memoryStoreRepository.buscarPorId(storeId);
    }

    @Transactional(readOnly = true)
    public List<MemoryStore> listarAtivos() {
        return memoryStoreRepository.listarAtivos();
    }

    /** Campo ausente no pedido mantém o valor atual; nulo não apaga o que já estava gravado. */
    @Transactional
    public Optional<MemoryStore> atualizar(MemoryStoreId storeId, String novoNome, String novaDescricao) {
        return memoryStoreRepository.buscarPorId(storeId).map(store -> {
            MemoryStore atualizado = new MemoryStore(
                    store.id(),
                    novoNome != null ? novoNome : store.nome(),
                    novaDescricao != null ? novaDescricao : store.descricao(),
                    store.anthropicStoreId(), store.moduloOrigem(), store.sigiloNivel(),
                    store.accessType(), store.criadoEm(), store.arquivadoEm(), store.entries());
            return memoryStoreRepository.salvar(atualizado);
        });
    }

    /** @return {@code false} quando não existe store com esse id. */
    @Transactional
    public boolean arquivar(MemoryStoreId storeId) {
        Optional<MemoryStore> encontrado = memoryStoreRepository.buscarPorId(storeId);
        if (encontrado.isEmpty()) {
            return false;
        }
        MemoryStore store = encontrado.get();
        if (store.anthropicStoreId() != null && store.podeEnviarParaAnthropicApi()) {
            anthropicMemoryStoreClient.arquivarStore(store.anthropicStoreId());
        }
        memoryStoreRepository.salvar(store.arquivado(Instant.now(clock)));
        return true;
    }
}
