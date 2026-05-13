package com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalGateStateSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalGateStateSnapshotRepository;

@Repository
public class InstitutionalGateStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_GATE_STATE";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalGateStateSnapshotRepository jpaRepository;

    public InstitutionalGateStateRepository(ComunicacaoJudicialStateStore stateStore,
                                            InstitutionalSnapshotJsonCodec codec,
                                            ObjectProvider<InstitutionalGateStateSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalGateState save(InstitutionalGateState state) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(state);
            jpaRepository.findByExpedicaoUuid(state.expedicaoUuid())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(state.status().name(), state.bloqueado(), state.releasedAt(), state.hashIntegridade(), snapshotJson, state.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalGateStateSnapshot(
                                    state.gateStateId(),
                                    state.expedicaoUuid(),
                                    state.processoId(),
                                    state.gateCode(),
                                    state.status().name(),
                                    state.bloqueado(),
                                    state.releasedAt(),
                                    state.hashIntegridade(),
                                    snapshotJson,
                                    state.createdAt(),
                                    state.updatedAt())));
        }
        return stateStore.save(
                DOMAIN,
                state.expedicaoUuid(),
                state.gateCode(),
                state,
                state.processoId(),
                state.expedicaoUuid(),
                null,
                state.status().name()
        );
    }

    public Optional<InstitutionalGateState> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            Optional<InstitutionalGateState> fromDb = jpaRepository.findByExpedicaoUuid(expedicaoUuid)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalGateState.class));
            if (fromDb.isPresent()) {
                return fromDb;
            }
        }
        return stateStore.find(DOMAIN, expedicaoUuid, InstitutionalGateState.class);
    }

    public List<InstitutionalGateState> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalGateState> fromDb = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalGateState.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalGateState.class);
    }

    public List<InstitutionalGateState> findByGateCodeContainingIgnoreCase(String gateCodeFragment) {
        if (gateCodeFragment == null || gateCodeFragment.isBlank()) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalGateState> fromDb = jpaRepository.findByGateCodeContainingIgnoreCaseOrderByUpdatedAtAsc(gateCodeFragment.trim()).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalGateState.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        String normalized = gateCodeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findAll().stream()
                .filter(item -> item.gateCode() != null && item.gateCode().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalGateState> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalGateState> fromDb = jpaRepository.findAllByOrderByUpdatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalGateState.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalGateState.class);
    }
}
