package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalNominationSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalNominationSnapshotRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalNominationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_NOMINATION";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalNominationSnapshotRepository jpaRepository;

    public InstitutionalNominationStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                  InstitutionalSnapshotJsonCodec codec,
                                                  ObjectProvider<InstitutionalNominationSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalNomination save(InstitutionalNomination nomination) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(nomination);
            jpaRepository.findByNominationId(nomination.nominationId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(nomination.status().name(), nomination.hashIntegridade(), snapshotJson, nomination.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalNominationSnapshot(
                                    nomination.nominationId(),
                                    nomination.affiliationId(),
                                    nomination.nominatedUserId(),
                                    nomination.unidadeCodigo(),
                                    nomination.caixaCodigo(),
                                    nomination.status().name(),
                                    nomination.hashIntegridade(),
                                    snapshotJson,
                                    nomination.createdAt(),
                                    nomination.updatedAt())));
        }
        return stateStore.save(DOMAIN, nomination.nominationId(), InstitutionalTopologyKeys.queueKey(nomination.unidadeCodigo(), nomination.caixaCodigo()), nomination, null, null, String.valueOf(nomination.nominatedUserId()), nomination.status().name());
    }

    public Optional<InstitutionalNomination> findByNominationId(String nominationId) {
        if (jpaRepository != null) {
            Optional<InstitutionalNomination> db = jpaRepository.findByNominationId(nominationId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalNomination.class));
            if (db.isPresent()) return db;
        }
        return stateStore.find(DOMAIN, nominationId, InstitutionalNomination.class);
    }

    public List<InstitutionalNomination> findByNominatedUserId(Long userId) {
        if (jpaRepository != null) {
            List<InstitutionalNomination> db = jpaRepository.findByNominatedUserIdOrderByUpdatedAtAsc(userId).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalNomination.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findAll(DOMAIN, InstitutionalNomination.class).stream().filter(item -> Objects.equals(item.nominatedUserId(), userId)).toList();
    }

    public List<InstitutionalNomination> findByAffiliationId(String affiliationId) {
        if (affiliationId == null || affiliationId.isBlank()) {
            return List.of();
        }
        if (jpaRepository != null) {
            List<InstitutionalNomination> db = jpaRepository.findByAffiliationIdOrderByUpdatedAtAsc(affiliationId).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalNomination.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return findAll().stream().filter(item -> affiliationId.equals(item.affiliationId())).toList();
    }

    public List<InstitutionalNomination> findByAffiliationIds(Collection<String> affiliationIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (affiliationIds != null) {
            affiliationIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        if (jpaRepository != null) {
            List<InstitutionalNomination> db = jpaRepository.findByAffiliationIdInOrderByUpdatedAtAsc(ids).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalNomination.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return findAll().stream().filter(item -> ids.contains(item.affiliationId())).toList();
    }

    public List<InstitutionalNomination> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalNomination> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalNomination.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findAll(DOMAIN, InstitutionalNomination.class);
    }

    public Optional<InstitutionalNomination> findActiveFor(Long userId, String unidadeCodigo, String caixaCodigo, Instant now) {
        Instant ref = now == null ? Instant.now() : now;
        return findByNominatedUserId(userId).stream()
                .filter(item -> item.ativaEm(ref))
                .filter(item -> InstitutionalTopologyKeys.matchesQueue(item.unidadeCodigo(), item.caixaCodigo(), unidadeCodigo, caixaCodigo))
                .findFirst();
    }
}
