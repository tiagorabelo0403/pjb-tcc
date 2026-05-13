package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalOperationalCoverageRuleSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalOperationalCoverageRuleSnapshotRepository;
import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalTopologyKeys;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalOperationalCoverageRuleStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_OPERATIONAL_COVERAGE_RULE";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalOperationalCoverageRuleSnapshotRepository jpaRepository;

    public InstitutionalOperationalCoverageRuleStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                              InstitutionalSnapshotJsonCodec codec,
                                                              ObjectProvider<InstitutionalOperationalCoverageRuleSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalOperationalCoverageRule save(InstitutionalOperationalCoverageRule rule) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(rule);
            jpaRepository.findByRuleId(rule.ruleId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(rule.status().name(), rule.hashIntegridade(), snapshotJson, rule.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalOperationalCoverageRuleSnapshot(
                                    rule.ruleId(),
                                    rule.unidadeCodigo(),
                                    rule.caixaCodigo(),
                                    rule.titularUsuarioId(),
                                    rule.coberturaUsuarioId(),
                                    rule.status().name(),
                                    rule.tipoCobertura().name(),
                                    rule.hashIntegridade(),
                                    snapshotJson,
                                    rule.createdAt(),
                                    rule.updatedAt())));
        }
        return stateStore.save(DOMAIN, rule.ruleId(), InstitutionalTopologyKeys.queueKey(rule.unidadeCodigo(), rule.caixaCodigo()), rule, null, null, null, rule.status().name());
    }

    public Optional<InstitutionalOperationalCoverageRule> findByRuleId(String ruleId) {
        if (jpaRepository != null) {
            Optional<InstitutionalOperationalCoverageRule> fromDb = jpaRepository.findByRuleId(ruleId)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOperationalCoverageRule.class));
            if (fromDb.isPresent()) {
                return fromDb;
            }
        }
        return stateStore.find(DOMAIN, ruleId, InstitutionalOperationalCoverageRule.class);
    }

    public List<InstitutionalOperationalCoverageRule> findByUnidadeCodigo(String unidadeCodigo) {
        if (jpaRepository != null) {
            List<InstitutionalOperationalCoverageRule> fromDb = jpaRepository.findByUnidadeCodigoOrderByUpdatedAtAsc(unidadeCodigo).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOperationalCoverageRule.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalOperationalCoverageRule.class).stream()
                .filter(item -> item.unidadeCodigo().equalsIgnoreCase(unidadeCodigo))
                .toList();
    }

    public List<InstitutionalOperationalCoverageRule> findByCoberturaUsuarioId(Long coberturaUsuarioId) {
        if (jpaRepository != null) {
            List<InstitutionalOperationalCoverageRule> fromDb = jpaRepository.findByCoberturaUsuarioIdOrderByUpdatedAtAsc(coberturaUsuarioId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOperationalCoverageRule.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return findAll().stream()
                .filter(item -> Objects.equals(item.coberturaUsuarioId(), coberturaUsuarioId))
                .toList();
    }

    public List<InstitutionalOperationalCoverageRule> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalOperationalCoverageRule> fromDb = jpaRepository.findAllByOrderByUpdatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOperationalCoverageRule.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalOperationalCoverageRule.class);
    }
}
