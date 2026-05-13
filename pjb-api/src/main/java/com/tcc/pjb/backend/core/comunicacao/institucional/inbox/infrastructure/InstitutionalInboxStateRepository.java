package com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalInboxItemSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalInboxItemSnapshotRepository;

@Repository
public class InstitutionalInboxStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_INBOX_ITEM";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalInboxItemSnapshotRepository jpaRepository;

    public InstitutionalInboxStateRepository(ComunicacaoJudicialStateStore stateStore,
                                             InstitutionalSnapshotJsonCodec codec,
                                             ObjectProvider<InstitutionalInboxItemSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalInboxItem save(InstitutionalInboxItem item) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(item);
            jpaRepository.findByExpedicaoUuid(item.expedicaoUuid())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(item.caixaCodigoAtual(), item.status().name(), item.prazoCienciaEm(), item.prazoRespostaEm(),
                                        item.hashIntegridade(), snapshotJson, item.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalInboxItemSnapshot(
                                    item.inboxItemId(),
                                    item.expedicaoUuid(),
                                    item.processoId(),
                                    item.unidadeCodigo(),
                                    item.caixaCodigoAtual(),
                                    item.status().name(),
                                    item.prazoCienciaEm(),
                                    item.prazoRespostaEm(),
                                    item.hashIntegridade(),
                                    snapshotJson,
                                    item.disponibilizadaEm(),
                                    item.updatedAt())));
        }
        return stateStore.save(
                DOMAIN,
                item.expedicaoUuid(),
                InstitutionalTopologyKeys.queueKey(item.unidadeCodigo(), item.caixaCodigoAtual()),
                item,
                item.processoId(),
                item.expedicaoUuid(),
                null,
                item.status().name()
        );
    }

    public Optional<InstitutionalInboxItem> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            Optional<InstitutionalInboxItem> fromDb = jpaRepository.findByExpedicaoUuid(expedicaoUuid)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class));
            if (fromDb.isPresent()) {
                return fromDb;
            }
        }
        return stateStore.find(DOMAIN, expedicaoUuid, InstitutionalInboxItem.class);
    }

    public List<InstitutionalInboxItem> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findAllByOrderByUpdatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalInboxItem.class);
    }

    public List<InstitutionalInboxItem> findByUnidadeCodigo(String unidadeCodigo) {
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByUnidadeCodigoOrderByUpdatedAtAsc(unidadeCodigo).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return findAll().stream()
                .filter(item -> item.unidadeCodigo() != null && unidadeCodigo != null && item.unidadeCodigo().equalsIgnoreCase(unidadeCodigo))
                .toList();
    }

    public List<InstitutionalInboxItem> findByUnidadeCodigoContainingIgnoreCase(String unidadeFragment) {
        if (unidadeFragment == null || unidadeFragment.isBlank()) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(unidadeFragment.trim()).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        String normalized = unidadeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findAll().stream()
                .filter(item -> item.unidadeCodigo() != null && item.unidadeCodigo().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalInboxItem> findByUnidadeCodigos(Collection<String> unidadeCodigos) {
        if (unidadeCodigos == null || unidadeCodigos.isEmpty()) {
            return List.of();
        }
        List<String> normalized = unidadeCodigos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByUnidadeCodigoInOrderByUpdatedAtAsc(normalized).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        java.util.Set<String> normalizedSet = normalized.stream()
                .map(value -> value.toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return findAll().stream()
                .filter(item -> item.unidadeCodigo() != null && normalizedSet.contains(item.unidadeCodigo().toUpperCase(java.util.Locale.ROOT)))
                .toList();
    }

    public List<InstitutionalInboxItem> findPendentesDecurso(String unidadeCodigo, Instant reference) {
        Instant resolvedReference = reference == null ? Instant.now() : reference;
        List<String> statuses = List.of("DISPONIBILIZADA", "RECEBIDA");
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByStatusCodigoInAndPrazoCienciaEmBefore(unidadeCodigo, statuses, resolvedReference).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return (unidadeCodigo == null || unidadeCodigo.isBlank() ? findAll() : findByUnidadeCodigo(unidadeCodigo)).stream()
                .filter(item -> item.status() != null && statuses.contains(item.status().name()))
                .filter(item -> item.prazoCienciaEm() != null && item.prazoCienciaEm().isBefore(resolvedReference))
                .toList();
    }

    public List<InstitutionalInboxItem> findByUnidadeCodigoAndCaixaCodigo(String unidadeCodigo, String caixaCodigo) {
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByUnidadeCodigoAndCaixaCodigoAtualOrderByUpdatedAtAsc(unidadeCodigo, caixaCodigo).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, InstitutionalTopologyKeys.queueKey(unidadeCodigo, caixaCodigo), InstitutionalInboxItem.class);
    }

    public List<InstitutionalInboxItem> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalInboxItem> fromDb = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalInboxItem.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalInboxItem.class);
    }
}
