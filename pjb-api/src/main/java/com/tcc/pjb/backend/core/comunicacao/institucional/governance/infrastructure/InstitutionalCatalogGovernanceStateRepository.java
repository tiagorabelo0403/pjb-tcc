package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogGovernanceSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCatalogGovernanceSnapshotRepository;
import jakarta.inject.Inject;

@Repository
public class InstitutionalCatalogGovernanceStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_CATALOG_GOVERNANCE";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalCatalogGovernanceSnapshotRepository jpaRepository;
    private final Map<String, InstitutionalCatalogGovernanceEntry> inMemoryStore;

    @Inject
    public InstitutionalCatalogGovernanceStateRepository() {
        this.stateStore = null;
        this.codec = null;
        this.jpaRepository = null;
        this.inMemoryStore = new ConcurrentHashMap<>();
    }

    public InstitutionalCatalogGovernanceStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                         InstitutionalSnapshotJsonCodec codec,
                                                         ObjectProvider<InstitutionalCatalogGovernanceSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.jpaRepository = repositoryProvider.getIfAvailable();
        this.inMemoryStore = new ConcurrentHashMap<>();
    }

    public InstitutionalCatalogGovernanceEntry save(InstitutionalCatalogGovernanceEntry entry) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(entry);
            jpaRepository.findByGovernanceId(entry.governanceId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(
                                        entry.unidadeCodigo(),
                                        entry.destinatarioKind().name(),
                                        entry.uf(),
                                        entry.comarca(),
                                        entry.foro(),
                                        entry.ramoDireito() == null ? null : entry.ramoDireito().name(),
                                        entry.grauJurisdicao() == null ? null : entry.grauJurisdicao().name(),
                                        entry.abrangencia().name(),
                                        entry.ativa(),
                                        entry.suspendeEntregaExterna(),
                                        entry.exigeHomologacaoAdministrativa(),
                                        entry.unidadeSubstitutaCodigo(),
                                        entry.vigenciaInicio(),
                                        entry.vigenciaFim(),
                                        entry.updatedAt(),
                                        snapshotJson);
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalCatalogGovernanceSnapshot(
                                    entry.governanceId(),
                                    entry.unidadeCodigo(),
                                    entry.destinatarioKind().name(),
                                    entry.uf(),
                                    entry.comarca(),
                                    entry.foro(),
                                    entry.ramoDireito() == null ? null : entry.ramoDireito().name(),
                                    entry.grauJurisdicao() == null ? null : entry.grauJurisdicao().name(),
                                    entry.abrangencia().name(),
                                    entry.ativa(),
                                    entry.suspendeEntregaExterna(),
                                    entry.exigeHomologacaoAdministrativa(),
                                    entry.unidadeSubstitutaCodigo(),
                                    entry.vigenciaInicio(),
                                    entry.vigenciaFim(),
                                    snapshotJson,
                                    entry.createdAt(),
                                    entry.updatedAt())));
        }
        if (stateStore == null) {
            inMemoryStore.put(entry.governanceId(), entry);
            return entry;
        }
        return stateStore.save(DOMAIN, entry.governanceId(), entry.unidadeCodigo(), entry, null, null, null, entry.ativa() ? "ATIVA" : "INATIVA");
    }

    public Optional<InstitutionalCatalogGovernanceEntry> findByGovernanceId(String governanceId) {
        if (jpaRepository != null) {
            Optional<InstitutionalCatalogGovernanceEntry> db = jpaRepository.findByGovernanceId(governanceId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalCatalogGovernanceEntry.class));
            if (db.isPresent()) {
                return db;
            }
        }
        if (stateStore == null) {
            return Optional.ofNullable(inMemoryStore.get(governanceId));
        }
        return stateStore.find(DOMAIN, governanceId, InstitutionalCatalogGovernanceEntry.class);
    }

    public List<InstitutionalCatalogGovernanceEntry> findByUnitCode(String unitCode) {
        if (jpaRepository != null) {
            List<InstitutionalCatalogGovernanceEntry> db = jpaRepository.findByUnidadeCodigoOrderByVigenciaInicioDesc(unitCode).stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalCatalogGovernanceEntry.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        if (stateStore == null) {
            return inMemoryStore.values().stream().filter(entry -> unitCode.equalsIgnoreCase(entry.unidadeCodigo())).toList();
        }
        return stateStore.findBySecondaryKey(DOMAIN, unitCode, InstitutionalCatalogGovernanceEntry.class);
    }

    public List<InstitutionalCatalogGovernanceEntry> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalCatalogGovernanceEntry> db = jpaRepository.findAllByOrderByUpdatedAtDesc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalCatalogGovernanceEntry.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        if (stateStore == null) {
            return inMemoryStore.values().stream().toList();
        }
        return stateStore.findAll(DOMAIN, InstitutionalCatalogGovernanceEntry.class);
    }

    public List<InstitutionalCatalogGovernanceEntry> findEffectiveAt(Instant reference) {
        return findAll().stream().filter(entry -> entry.isEffectiveAt(reference)).toList();
    }
}
