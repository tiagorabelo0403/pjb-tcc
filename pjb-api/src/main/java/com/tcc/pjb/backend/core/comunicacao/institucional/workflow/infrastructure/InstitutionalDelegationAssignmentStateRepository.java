package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDelegationAssignmentSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDelegationAssignmentSnapshotRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalDelegationAssignmentStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DELEGATION_ASSIGNMENT";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDelegationAssignmentSnapshotRepository jpaRepository;

    public InstitutionalDelegationAssignmentStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                            InstitutionalSnapshotJsonCodec codec,
                                                            ObjectProvider<InstitutionalDelegationAssignmentSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDelegationAssignment save(InstitutionalDelegationAssignment assignment) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(assignment);
            jpaRepository.findByAssignmentId(assignment.assignmentId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(assignment.status().name(), assignment.hashIntegridade(), snapshotJson, assignment.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalDelegationAssignmentSnapshot(
                                    assignment.assignmentId(),
                                    assignment.expedicaoUuid(),
                                    assignment.processoId(),
                                    assignment.unidadeCodigo(),
                                    assignment.caixaCodigo(),
                                    assignment.status().name(),
                                    assignment.tipoFluxo().name(),
                                    assignment.delegadoUsuarioId(),
                                    assignment.hashIntegridade(),
                                    snapshotJson,
                                    assignment.createdAt(),
                                    assignment.updatedAt())));
        }
        return stateStore.save(DOMAIN, assignment.assignmentId(), assignment.expedicaoUuid(), assignment, assignment.processoId(), assignment.expedicaoUuid(), null, assignment.status().name());
    }

    public Optional<InstitutionalDelegationAssignment> findByAssignmentId(String assignmentId) {
        if (jpaRepository != null) {
            Optional<InstitutionalDelegationAssignment> db = jpaRepository.findByAssignmentId(assignmentId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalDelegationAssignment.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return stateStore.find(DOMAIN, assignmentId, InstitutionalDelegationAssignment.class);
    }

    public List<InstitutionalDelegationAssignment> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalDelegationAssignment> db = jpaRepository.findByExpedicaoUuidOrderByUpdatedAtAsc(expedicaoUuid).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDelegationAssignment.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalDelegationAssignment.class);
    }

    public List<InstitutionalDelegationAssignment> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalDelegationAssignment> db = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDelegationAssignment.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalDelegationAssignment.class);
    }


    public List<InstitutionalDelegationAssignment> findByDelegadoUsuarioId(Long delegadoUsuarioId) {
        if (jpaRepository != null) {
            List<InstitutionalDelegationAssignment> db = jpaRepository.findByDelegadoUsuarioIdOrderByUpdatedAtAsc(delegadoUsuarioId).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalDelegationAssignment.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> Objects.equals(item.delegadoUsuarioId(), delegadoUsuarioId))
                .toList();
    }

    public List<InstitutionalDelegationAssignment> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalDelegationAssignment> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalDelegationAssignment.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalDelegationAssignment.class);
    }
}
