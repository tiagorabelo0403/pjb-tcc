package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MemoryStoreRepositoryImpl implements MemoryStoreRepository {

    private final MemoryStoreJpaRepository jpaRepository;

    public MemoryStoreRepositoryImpl(MemoryStoreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MemoryStore salvar(MemoryStore store) {
        MemoryStoreJpaEntity entity = toEntity(store);
        MemoryStoreJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MemoryStore> buscarPorId(MemoryStoreId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<MemoryStore> buscarPorAnthropicStoreId(String anthropicStoreId) {
        return jpaRepository.findByAnthropicStoreId(anthropicStoreId).map(this::toDomain);
    }

    @Override
    public List<MemoryStore> listarAtivos() {
        return jpaRepository.findAllAtivos().stream().map(this::toDomain).toList();
    }

    @Override
    public List<MemoryStore> listarPorModulo(String moduloOrigem) {
        return jpaRepository.findByModuloOrigemAndArquivadoEmIsNull(moduloOrigem)
                .stream().map(this::toDomain).toList();
    }

    private MemoryStoreJpaEntity toEntity(MemoryStore store) {
        MemoryStoreJpaEntity e = new MemoryStoreJpaEntity();
        e.setId(store.id().value());
        e.setNome(store.nome());
        e.setDescricao(store.descricao());
        e.setAnthropicStoreId(store.anthropicStoreId());
        e.setModuloOrigem(store.moduloOrigem());
        e.setSigiloNivel(store.sigiloNivel() != null ? store.sigiloNivel().name() : null);
        e.setAccessType(store.accessType() != null ? store.accessType().name() : null);
        e.setCriadoEm(store.criadoEm());
        e.setArquivadoEm(store.arquivadoEm());
        return e;
    }

    private MemoryStore toDomain(MemoryStoreJpaEntity e) {
        MemorySigiloNivel nivel = e.getSigiloNivel() != null
                ? MemorySigiloNivel.valueOf(e.getSigiloNivel())
                : MemorySigiloNivel.PUBLIC;
        MemoryAccessPolicy.AccessType access = e.getAccessType() != null
                ? MemoryAccessPolicy.AccessType.valueOf(e.getAccessType())
                : MemoryAccessPolicy.computarAccessType(nivel);
        return new MemoryStore(
                MemoryStoreId.de(e.getId()),
                e.getNome(),
                e.getDescricao(),
                e.getAnthropicStoreId(),
                e.getModuloOrigem(),
                nivel,
                access,
                e.getCriadoEm(),
                e.getArquivadoEm(),
                List.of()
        );
    }
}
