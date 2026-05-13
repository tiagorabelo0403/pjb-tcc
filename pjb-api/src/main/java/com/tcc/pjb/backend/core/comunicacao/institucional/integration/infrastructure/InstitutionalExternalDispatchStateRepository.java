package com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalExternalDispatchSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalExternalDispatchSnapshotRepository;

@Repository
public class InstitutionalExternalDispatchStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_EXTERNAL_DISPATCH";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalExternalDispatchSnapshotRepository jpaRepository;

    public InstitutionalExternalDispatchStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                        InstitutionalSnapshotJsonCodec codec,
                                                        ObjectProvider<InstitutionalExternalDispatchSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalExternalDispatch save(InstitutionalExternalDispatch dispatch) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(dispatch);
            jpaRepository.findByDispatchId(dispatch.dispatchId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(dispatch.status().name(), dispatch.providerReference(), dispatch.failureReason(), dispatch.responsePayload(), dispatch.updatedAt(), dispatch.payloadHash(), snapshotJson);
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalExternalDispatchSnapshot(
                                    dispatch.dispatchId(),
                                    dispatch.jobId(),
                                    dispatch.expedicaoUuid(),
                                    dispatch.processoId(),
                                    dispatch.unidadeCodigo(),
                                    dispatch.destinatarioKind().name(),
                                    dispatch.channel().name(),
                                    dispatch.provider(),
                                    dispatch.status().name(),
                                    dispatch.providerReference(),
                                    dispatch.failureReason(),
                                    dispatch.payloadHash(),
                                    snapshotJson,
                                    dispatch.createdAt(),
                                    dispatch.updatedAt())));
        }
        return stateStore.save(DOMAIN, dispatch.dispatchId(), dispatch.expedicaoUuid(), dispatch, dispatch.processoId(), dispatch.expedicaoUuid(), null, dispatch.status().name());
    }

    public Optional<InstitutionalExternalDispatch> findByDispatchId(String dispatchId) {
        if (jpaRepository != null) {
            Optional<InstitutionalExternalDispatch> db = jpaRepository.findByDispatchId(dispatchId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return stateStore.find(DOMAIN, dispatchId, InstitutionalExternalDispatch.class);
    }

    public List<InstitutionalExternalDispatch> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findByExpedicaoUuidOrderByUpdatedAtAsc(expedicaoUuid).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalExternalDispatch.class);
    }

    public List<InstitutionalExternalDispatch> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalExternalDispatch.class);
    }

    public List<InstitutionalExternalDispatch> findByUnidadeCodigoContainingIgnoreCase(String unidadeFragment) {
        if (unidadeFragment == null || unidadeFragment.isBlank()) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(unidadeFragment.trim()).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        String normalized = unidadeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findAll().stream()
                .filter(item -> item.unidadeCodigo() != null && item.unidadeCodigo().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalExternalDispatch> findByDestinatarioKind(DestinatarioInstitucionalKind destinatarioKind) {
        if (destinatarioKind == null) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findByDestinatarioKindCodigoOrderByUpdatedAtAsc(destinatarioKind.name()).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> item.destinatarioKind() == destinatarioKind)
                .toList();
    }

    public List<InstitutionalExternalDispatch> findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKind(String unidadeFragment,
                                                                                                            DestinatarioInstitucionalKind destinatarioKind) {
        if (destinatarioKind == null) {
            return findByUnidadeCodigoContainingIgnoreCase(unidadeFragment);
        }
        if (unidadeFragment == null || unidadeFragment.isBlank()) {
            return findByDestinatarioKind(destinatarioKind);
        }
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKindCodigoOrderByUpdatedAtAsc(unidadeFragment.trim(), destinatarioKind.name()).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        String normalized = unidadeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findByDestinatarioKind(destinatarioKind).stream()
                .filter(item -> item.unidadeCodigo() != null && item.unidadeCodigo().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalExternalDispatch> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalExternalDispatch> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalExternalDispatch.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalExternalDispatch.class);
    }
}
