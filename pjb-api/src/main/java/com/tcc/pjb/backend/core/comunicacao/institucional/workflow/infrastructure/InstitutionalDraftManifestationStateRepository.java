package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDraftManifestationSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDraftManifestationSnapshotRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalDraftManifestationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DRAFT_MANIFESTATION";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDraftManifestationSnapshotRepository jpaRepository;

    public InstitutionalDraftManifestationStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                          InstitutionalSnapshotJsonCodec codec,
                                                          ObjectProvider<InstitutionalDraftManifestationSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDraftManifestation save(InstitutionalDraftManifestation draft) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(draft);
            jpaRepository.findByDraftId(draft.draftId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(draft.status().name(), draft.aprovadorUsuarioId(), draft.hashIntegridade(), snapshotJson, draft.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalDraftManifestationSnapshot(
                                    draft.draftId(),
                                    draft.expedicaoUuid(),
                                    draft.processoId(),
                                    draft.unidadeCodigo(),
                                    draft.caixaCodigo(),
                                    draft.status().name(),
                                    draft.aprovadorUsuarioId(),
                                    draft.hashIntegridade(),
                                    snapshotJson,
                                    draft.createdAt(),
                                    draft.updatedAt())));
        }
        return stateStore.save(DOMAIN, draft.draftId(), draft.expedicaoUuid(), draft, draft.processoId(), draft.expedicaoUuid(), null, draft.status().name());
    }

    public Optional<InstitutionalDraftManifestation> findByDraftId(String draftId) {
        if (jpaRepository != null) {
            Optional<InstitutionalDraftManifestation> db = jpaRepository.findByDraftId(draftId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalDraftManifestation.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return stateStore.find(DOMAIN, draftId, InstitutionalDraftManifestation.class);
    }

    public List<InstitutionalDraftManifestation> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalDraftManifestation> db = jpaRepository.findByExpedicaoUuidOrderByUpdatedAtAsc(expedicaoUuid).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDraftManifestation.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalDraftManifestation.class);
    }

    public List<InstitutionalDraftManifestation> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalDraftManifestation> db = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDraftManifestation.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalDraftManifestation.class);
    }

    public List<InstitutionalDraftManifestation> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalDraftManifestation> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDraftManifestation.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalDraftManifestation.class);
    }
}
