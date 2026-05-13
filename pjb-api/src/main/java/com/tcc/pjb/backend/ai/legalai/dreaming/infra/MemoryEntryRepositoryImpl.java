package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MemoryEntryRepositoryImpl implements MemoryEntryRepository {

    private final MemoryEntryJpaRepository jpaRepository;

    public MemoryEntryRepositoryImpl(MemoryEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MemoryEntry salvar(MemoryEntry entry) {
        MemoryEntryJpaEntity entity = toEntity(entry);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<MemoryEntry> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MemoryEntry> listarPorStore(MemoryStoreId storeId) {
        return jpaRepository.findByStoreIdAndAtivoTrue(storeId.value())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }

    private MemoryEntryJpaEntity toEntity(MemoryEntry entry) {
        MemoryEntryJpaEntity e = new MemoryEntryJpaEntity();
        e.setId(entry.id());
        e.setStoreId(entry.storeId().value());
        e.setChave(entry.chave());
        e.setConteudo(entry.conteudo());
        e.setCriadoEm(entry.criadoEm());
        e.setAtualizadoEm(entry.atualizadoEm());
        e.setAtivo(entry.ativo());
        return e;
    }

    private MemoryEntry toDomain(MemoryEntryJpaEntity e) {
        return new MemoryEntry(
                e.getId(),
                MemoryStoreId.de(e.getStoreId()),
                e.getChave(),
                e.getConteudo(),
                e.getCriadoEm(),
                e.getAtualizadoEm(),
                e.isAtivo()
        );
    }
}
