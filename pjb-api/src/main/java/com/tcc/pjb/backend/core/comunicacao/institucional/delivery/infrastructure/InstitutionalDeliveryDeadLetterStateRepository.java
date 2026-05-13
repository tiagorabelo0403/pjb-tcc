package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeadLetterEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeadLetterSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDeadLetterSnapshotRepository;

@Repository
public class InstitutionalDeliveryDeadLetterStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DELIVERY_DLQ";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDeadLetterSnapshotRepository jpaRepository;

    public InstitutionalDeliveryDeadLetterStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                          InstitutionalSnapshotJsonCodec codec,
                                                          ObjectProvider<InstitutionalDeadLetterSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDeadLetterEntry save(InstitutionalDeadLetterEntry entry) {
        if (jpaRepository != null) {
            jpaRepository.save(new InstitutionalDeadLetterSnapshot(
                    entry.entryId(),
                    entry.jobId(),
                    entry.expedicaoUuid(),
                    entry.processoId(),
                    entry.reason() == null ? null : entry.reason().name(),
                    entry.channel().name(),
                    entry.hashIntegridade(),
                    codec.write(entry),
                    entry.createdAt()));
        }
        return stateStore.save(
                DOMAIN,
                entry.entryId(),
                entry.expedicaoUuid(),
                entry,
                entry.processoId(),
                entry.expedicaoUuid(),
                null,
                entry.reason() == null ? "NONE" : entry.reason().name()
        );
    }

    public List<InstitutionalDeadLetterEntry> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalDeadLetterEntry> fromDb = jpaRepository.findByProcessoIdOrderByCreatedAtAsc(processoId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeadLetterEntry.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalDeadLetterEntry.class);
    }

    public List<InstitutionalDeadLetterEntry> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalDeadLetterEntry> fromDb = jpaRepository.findByExpedicaoUuidOrderByCreatedAtAsc(expedicaoUuid).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeadLetterEntry.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalDeadLetterEntry.class);
    }

    public long countAll() {
        if (jpaRepository != null) {
            return jpaRepository.count();
        }
        return stateStore.findAll(DOMAIN, InstitutionalDeadLetterEntry.class).size();
    }

    public List<InstitutionalDeadLetterEntry> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalDeadLetterEntry> fromDb = jpaRepository.findAllByOrderByCreatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeadLetterEntry.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalDeadLetterEntry.class);
    }
}
