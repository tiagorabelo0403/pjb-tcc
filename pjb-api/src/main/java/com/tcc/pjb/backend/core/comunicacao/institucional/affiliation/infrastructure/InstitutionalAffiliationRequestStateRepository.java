package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationRequestStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalAffiliationRequestSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalAffiliationRequestSnapshotRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalAffiliationRequestStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_AFFILIATION_REQUEST";
    private static final List<String> GOVERNANCE_ACTIVE_STATUSES = List.of(
            InstitutionalAffiliationRequestStatus.PENDENTE_VALIDACAO.name(),
            InstitutionalAffiliationRequestStatus.EM_HOMOLOGACAO.name()
    );

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalAffiliationRequestSnapshotRepository jpaRepository;

    public InstitutionalAffiliationRequestStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                          InstitutionalSnapshotJsonCodec codec,
                                                          ObjectProvider<InstitutionalAffiliationRequestSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalAffiliationRequest save(InstitutionalAffiliationRequest request) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(request);
            jpaRepository.findByRequestId(request.requestId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(
                                        request.organizationScope() == null ? null : request.organizationScope().name(),
                                        request.representanteUsuarioId(),
                                        request.materializedAffiliationId(),
                                        request.status().name(),
                                        request.hashIntegridade(),
                                        snapshotJson,
                                        request.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalAffiliationRequestSnapshot(
                                    request.requestId(),
                                    request.destinatarioKind().name(),
                                    request.organizationScope() == null ? null : request.organizationScope().name(),
                                    request.unidadeCodigo(),
                                    request.orgaoSigla(),
                                    request.representanteUsuarioId(),
                                    request.materializedAffiliationId(),
                                    request.status().name(),
                                    request.hashIntegridade(),
                                    snapshotJson,
                                    request.createdAt(),
                                    request.updatedAt())));
        }
        return stateStore.save(DOMAIN, request.requestId(), request.unidadeCodigo(), request, null, null, String.valueOf(request.representanteUsuarioId()), request.status().name());
    }

    public Optional<InstitutionalAffiliationRequest> findByRequestId(String requestId) {
        if (jpaRepository != null) {
            Optional<InstitutionalAffiliationRequest> db = jpaRepository.findByRequestId(requestId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class));
            if (db.isPresent()) return db;
        }
        return stateStore.find(DOMAIN, requestId, InstitutionalAffiliationRequest.class);
    }

    public List<InstitutionalAffiliationRequest> findByUnidadeCodigo(String unidadeCodigo) {
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findByUnidadeCodigoOrderByUpdatedAtAsc(unidadeCodigo).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findBySecondaryKey(DOMAIN, unidadeCodigo, InstitutionalAffiliationRequest.class);
    }

    public List<InstitutionalAffiliationRequest> findByOrganizationScope(InstitutionalOrganizationScope scope) {
        if (scope == null) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findByOrganizationScopeOrderByUpdatedAtAsc(scope.name()).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> item.organizationScope() == scope)
                .toList();
    }

    public List<InstitutionalAffiliationRequest> findByRepresentanteUsuarioId(Long representanteUsuarioId) {
        if (representanteUsuarioId == null) {
            return List.of();
        }
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findByRepresentanteUsuarioIdOrderByUpdatedAtDesc(representanteUsuarioId).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> Objects.equals(item.representanteUsuarioId(), representanteUsuarioId))
                .sorted(Comparator.comparing(InstitutionalAffiliationRequest::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public Optional<InstitutionalAffiliationRequest> findLatestByMaterializedAffiliationId(String materializedAffiliationId) {
        if (materializedAffiliationId == null || materializedAffiliationId.isBlank()) {
            return Optional.empty();
        }
        String normalized = materializedAffiliationId.trim();
        if (jpaRepository != null) {
            return jpaRepository.findByMaterializedAffiliationIdOrderByUpdatedAtDesc(normalized).stream()
                    .findFirst()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class));
        }
        return findAll().stream()
                .filter(item -> normalized.equals(item.materializedAffiliationId()))
                .max(Comparator.comparing(InstitutionalAffiliationRequest::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public List<InstitutionalAffiliationRequest> findLatestByMaterializedAffiliationIds(Collection<String> materializedAffiliationIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (materializedAffiliationIds != null) {
            materializedAffiliationIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(ids::add);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        if (jpaRepository != null) {
            return latestByAffiliationId(jpaRepository.findByMaterializedAffiliationIdInOrderByUpdatedAtDesc(ids).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class))
                    .toList(), ids);
        }
        return latestByAffiliationId(findAll(), ids);
    }

    public List<InstitutionalAffiliationRequest> findGovernanceActive() {
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findByStatusCodigoInOrderByUpdatedAtAsc(GOVERNANCE_ACTIVE_STATUSES).stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findByStatusCodes(DOMAIN, GOVERNANCE_ACTIVE_STATUSES, InstitutionalAffiliationRequest.class);
    }

    public List<InstitutionalAffiliationRequest> findWithoutMaterializedAffiliation() {
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findWithoutMaterializedAffiliationOrderByUpdatedAtAsc().stream()
                    .map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> item.materializedAffiliationId() == null || item.materializedAffiliationId().isBlank())
                .toList();
    }

    public List<InstitutionalAffiliationRequest> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalAffiliationRequest> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalAffiliationRequest.class)).toList();
            if (!db.isEmpty()) return db;
        }
        return stateStore.findAll(DOMAIN, InstitutionalAffiliationRequest.class);
    }

    private List<InstitutionalAffiliationRequest> latestByAffiliationId(List<InstitutionalAffiliationRequest> source, Collection<String> ids) {
        Map<String, InstitutionalAffiliationRequest> latest = new LinkedHashMap<>();
        for (InstitutionalAffiliationRequest item : source) {
            if (item.materializedAffiliationId() == null || !ids.contains(item.materializedAffiliationId())) {
                continue;
            }
            latest.merge(item.materializedAffiliationId(), item, (left, right) -> maxUpdated(left, right));
        }
        return latest.values().stream().toList();
    }

    private InstitutionalAffiliationRequest maxUpdated(InstitutionalAffiliationRequest left, InstitutionalAffiliationRequest right) {
        if (left.updatedAt() == null) {
            return right;
        }
        if (right.updatedAt() == null) {
            return left;
        }
        return left.updatedAt().compareTo(right.updatedAt()) >= 0 ? left : right;
    }
}
