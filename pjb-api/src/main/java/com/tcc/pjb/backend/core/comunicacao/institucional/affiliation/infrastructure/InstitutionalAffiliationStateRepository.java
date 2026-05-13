package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalAffiliationSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalAffiliationSnapshotRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalAffiliationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_AFFILIATION";
    private static final List<String> ACTIVE_STATUSES = List.of(InstitutionalAffiliationStatus.HOMOLOGADA.name());

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalAffiliationSnapshotRepository jpaRepository;

    public InstitutionalAffiliationStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                   InstitutionalSnapshotJsonCodec codec,
                                                   ObjectProvider<InstitutionalAffiliationSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalAffiliation save(InstitutionalAffiliation affiliation) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(affiliation);
            jpaRepository.findByAffiliationId(affiliation.affiliationId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(
                                        affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                                        affiliation.status().name(),
                                        affiliation.hashIntegridade(),
                                        snapshotJson,
                                        affiliation.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalAffiliationSnapshot(
                                    affiliation.affiliationId(),
                                    affiliation.destinatarioKind().name(),
                                    affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                                    affiliation.unidadeCodigo(),
                                    affiliation.orgaoSigla(),
                                    affiliation.status().name(),
                                    affiliation.hashIntegridade(),
                                    snapshotJson,
                                    affiliation.createdAt(),
                                    affiliation.updatedAt())));
        }
        return stateStore.save(DOMAIN, affiliation.affiliationId(), affiliation.unidadeCodigo(), affiliation, null, null, null, affiliation.status().name());
    }

    public Optional<InstitutionalAffiliation> findByAffiliationId(String affiliationId) {
        if (jpaRepository != null) {
            Optional<InstitutionalAffiliation> db = jpaRepository.findByAffiliationId(affiliationId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class));
            if (db.isPresent()) return db;
        }
        return stateStore.find(DOMAIN, affiliationId, InstitutionalAffiliation.class);
    }

    public List<InstitutionalAffiliation> findByAffiliationIds(Collection<String> affiliationIds) {
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
            List<InstitutionalAffiliation> db = jpaRepository.findByAffiliationIdInOrderByUpdatedAtAsc(ids).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> ids.contains(item.affiliationId()))
                .toList();
    }

    public List<InstitutionalAffiliation> findByUnidadeCodigo(String unidadeCodigo) {
        if (jpaRepository != null) {
            List<InstitutionalAffiliation> db = jpaRepository.findByUnidadeCodigoOrderByUpdatedAtAsc(unidadeCodigo).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findBySecondaryKey(DOMAIN, unidadeCodigo, InstitutionalAffiliation.class);
    }

    public List<InstitutionalAffiliation> findByOrganizationScope(InstitutionalOrganizationScope organizationScope) {
        if (organizationScope == null) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalAffiliation> db = jpaRepository.findByOrganizationScopeOrderByUpdatedAtAsc(organizationScope.name()).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> item.organizationScope() == organizationScope)
                .toList();
    }

    public List<InstitutionalAffiliation> findActive() {
        if (jpaRepository != null) {
            List<InstitutionalAffiliation> db = jpaRepository.findByStatusCodigoInOrderByUpdatedAtAsc(ACTIVE_STATUSES).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findByStatusCodes(DOMAIN, ACTIVE_STATUSES, InstitutionalAffiliation.class);
    }

    public List<InstitutionalAffiliation> findActiveByOrganizationScope(InstitutionalOrganizationScope organizationScope) {
        if (organizationScope == null) {
            return findActive();
        }
        if (jpaRepository != null) {
            List<InstitutionalAffiliation> db = jpaRepository.findByOrganizationScopeAndStatusCodigoInOrderByUpdatedAtAsc(organizationScope.name(), ACTIVE_STATUSES).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findByOrganizationScope(organizationScope).stream()
                .filter(InstitutionalAffiliation::ativa)
                .toList();
    }

    public List<InstitutionalAffiliation> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalAffiliation> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliation.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findAll(DOMAIN, InstitutionalAffiliation.class);
    }
}
