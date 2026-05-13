package com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEvent;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalTimelineEventSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalTimelineEventSnapshotRepository;

@Repository
public class InstitutionalTimelineStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_TIMELINE_EVENT";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalTimelineEventSnapshotRepository jpaRepository;

    public InstitutionalTimelineStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                InstitutionalSnapshotJsonCodec codec,
                                                ObjectProvider<InstitutionalTimelineEventSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalTimelineEvent save(InstitutionalTimelineEvent event) {
        if (jpaRepository != null) {
            jpaRepository.save(new InstitutionalTimelineEventSnapshot(
                    event.eventId(),
                    event.expedicaoUuid(),
                    event.processoId(),
                    event.eventType().name(),
                    event.statusComunicacao().name(),
                    event.unidadeCodigo(),
                    event.caixaCodigo(),
                    event.occurredAt(),
                    event.hashIntegridade(),
                    codec.write(event)));
        }
        return stateStore.save(
                DOMAIN,
                event.eventId(),
                event.expedicaoUuid(),
                event,
                event.processoId(),
                event.expedicaoUuid(),
                null,
                event.statusComunicacao().name()
        );
    }

    public List<InstitutionalTimelineEvent> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalTimelineEvent> fromDb = jpaRepository.findByExpedicaoUuidOrderByOccurredAtAsc(expedicaoUuid).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalTimelineEvent.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalTimelineEvent.class);
    }
}
