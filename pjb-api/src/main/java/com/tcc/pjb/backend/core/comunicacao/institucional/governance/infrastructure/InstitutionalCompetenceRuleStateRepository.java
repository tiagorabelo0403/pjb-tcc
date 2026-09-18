package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCompetenceRuleSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCompetenceRuleSnapshotRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import jakarta.inject.Inject;

@Repository
public class InstitutionalCompetenceRuleStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_COMPETENCE_RULE";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalCompetenceRuleSnapshotRepository jpaRepository;
    private final ComarcaResolutionService comarcaResolutionService;
    private final Map<String, InstitutionalCompetenceRule> inMemoryStore;

    public InstitutionalCompetenceRuleStateRepository() {
        this.stateStore = null;
        this.codec = null;
        this.jpaRepository = null;
        this.comarcaResolutionService = null;
        this.inMemoryStore = new ConcurrentHashMap<>();
    }

    @Inject
    public InstitutionalCompetenceRuleStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                      InstitutionalSnapshotJsonCodec codec,
                                                      ObjectProvider<InstitutionalCompetenceRuleSnapshotRepository> repositoryProvider,
                                                      ComarcaResolutionService comarcaResolutionService) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.jpaRepository = repositoryProvider.getIfAvailable();
        this.comarcaResolutionService = Objects.requireNonNull(comarcaResolutionService, "comarcaResolutionService");
        this.inMemoryStore = new ConcurrentHashMap<>();
    }

    public InstitutionalCompetenceRule save(InstitutionalCompetenceRule rule) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(rule);
            Comarca comarcaEntidade = resolveComarca(rule.comarca(), rule.uf());
            jpaRepository.findByRuleId(rule.ruleId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(
                                        rule.uf(),
                                        rule.comarca(),
                                        rule.foro(),
                                        rule.ramoDireito() == null ? null : rule.ramoDireito().name(),
                                        rule.grauJurisdicao() == null ? null : rule.grauJurisdicao().name(),
                                        rule.unidadeCodigo(),
                                        rule.prioridade(),
                                        rule.ativa(),
                                        rule.vigenciaInicio(),
                                        rule.vigenciaFim(),
                                        rule.updatedAt(),
                                        snapshotJson);
                                existing.setComarcaEntidade(comarcaEntidade);
                                jpaRepository.save(existing);
                            },
                            () -> {
                                InstitutionalCompetenceRuleSnapshot novo = new InstitutionalCompetenceRuleSnapshot(
                                        rule.ruleId(),
                                        rule.destinatarioKind().name(),
                                        rule.papelProcessual().name(),
                                        rule.uf(),
                                        rule.comarca(),
                                        rule.foro(),
                                        rule.ramoDireito() == null ? null : rule.ramoDireito().name(),
                                        rule.grauJurisdicao() == null ? null : rule.grauJurisdicao().name(),
                                        rule.unidadeCodigo(),
                                        rule.prioridade(),
                                        rule.ativa(),
                                        rule.vigenciaInicio(),
                                        rule.vigenciaFim(),
                                        snapshotJson,
                                        rule.createdAt(),
                                        rule.updatedAt());
                                novo.setComarcaEntidade(comarcaEntidade);
                                jpaRepository.save(novo);
                            });
        }
        if (stateStore == null) {
            inMemoryStore.put(rule.ruleId(), rule);
            return rule;
        }
        return stateStore.save(DOMAIN, rule.ruleId(), rule.unidadeCodigo(), rule, null, null, null, rule.ativa() ? "ATIVA" : "INATIVA");
    }

    public Optional<InstitutionalCompetenceRule> findByRuleId(String ruleId) {
        if (jpaRepository != null) {
            Optional<InstitutionalCompetenceRule> db = jpaRepository.findByRuleId(ruleId).map(s -> codec.read(s.getSnapshotJson(), InstitutionalCompetenceRule.class));
            if (db.isPresent()) {
                return db;
            }
        }
        if (stateStore == null) {
            return Optional.ofNullable(inMemoryStore.get(ruleId));
        }
        return stateStore.find(DOMAIN, ruleId, InstitutionalCompetenceRule.class);
    }

    public List<InstitutionalCompetenceRule> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalCompetenceRule> db = jpaRepository.findAllByOrderByPrioridadeDescUpdatedAtDesc().stream().map(s -> codec.read(s.getSnapshotJson(), InstitutionalCompetenceRule.class)).toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        if (stateStore == null) {
            return inMemoryStore.values().stream().toList();
        }
        return stateStore.findAll(DOMAIN, InstitutionalCompetenceRule.class);
    }

    public List<InstitutionalCompetenceRule> findEffectiveAt(Instant reference) {
        return findAll().stream().filter(rule -> rule.isEffectiveAt(reference)).toList();
    }

    private Comarca resolveComarca(String comarca, String uf) {
        if (comarca == null || comarca.isBlank()) {
            return null;
        }
        return comarcaResolutionService.resolver(comarca, uf).orElse(null);
    }
}
