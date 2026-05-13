package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;

@Service
public class RecursalMeshProjectionService {

    private final RecursalProcessIntegrationStateRepository repository;
    private final RecursalMeshFingerprintService fingerprintService;
    private final RecursalMeshSlaService slaService;
    private final ObjectProvider<RecursalMeshSearchIndexerService> searchIndexerProvider;
    private final ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider;

    public RecursalMeshProjectionService(RecursalProcessIntegrationStateRepository repository,
                                         RecursalMeshFingerprintService fingerprintService,
                                         RecursalMeshSlaService slaService,
                                         ObjectProvider<RecursalMeshSearchIndexerService> searchIndexerProvider) {
        this(repository, fingerprintService, slaService, searchIndexerProvider, null);
    }

    public RecursalMeshProjectionService(RecursalProcessIntegrationStateRepository repository,
                                         RecursalMeshFingerprintService fingerprintService,
                                         RecursalMeshSlaService slaService,
                                         ObjectProvider<RecursalMeshSearchIndexerService> searchIndexerProvider,
                                         ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider) {
        this.repository = repository;
        this.fingerprintService = fingerprintService;
        this.slaService = slaService;
        this.searchIndexerProvider = searchIndexerProvider;
        this.retryExecutorProvider = retryExecutorProvider;
    }

    @Transactional
    public void sync(RecursalAggregateState aggregate, RecursalTransitionEvent lastEvent, String actor, Instant transitionAt, int totalTransitions) {
        RecursalProcessIntegrationState projection = repository.findById(aggregate.getRecursoId())
                .orElseGet(RecursalProcessIntegrationState::new);
        projection.setRecursoId(aggregate.getRecursoId());
        projection.setProcesso(aggregate.getProcesso());
        projection.setNumeroProcesso(aggregate.getNumeroProcesso());
        projection.setSpeciesCode(aggregate.getSpeciesCode());
        projection.setProfileName(aggregate.getProfileName());
        projection.setCurrentState(aggregate.getCurrentState());
        projection.setTribunalAtual(aggregate.getTribunalAtual());
        projection.setTribunalDetalhadoAtual(aggregate.getTribunalDetalhadoAtual());
        projection.setInstanciaAtual(aggregate.getInstanciaAtual());
        projection.setAutoridadeAtual(aggregate.getAutoridadeAtual());
        projection.setLastEvent(lastEvent);
        projection.setCurrentRevision(readRevision(totalTransitions));
        projection.setTotalTransitions(Math.max(totalTransitions, 0));
        projection.setIteracoesEmbargos(aggregate.getIteracoesEmbargos());
        projection.setTransitadoEmJulgado(aggregate.getCurrentState() == RecursalLifecycleState.TRANSITADO_EM_JULGADO);
        projection.setLastActor(trimToNull(actor));
        projection.setLastTransitionAt(transitionAt);
        projection.setSnapshotJson(aggregate.getSnapshotJson());
        projection.setRoutePlanJson(aggregate.getRoutePlanJson());
        projection.setIntegrityFingerprint(fingerprintService.projectionFingerprint(projection));
        projection = repository.save(projection);
        RecursalMeshSearchIndexerService searchIndexer = searchIndexerProvider.getIfAvailable();
        if (searchIndexer != null) {
            RecursalProcessIntegrationState finalProjection = projection;
            executeIndexing(() -> searchIndexer.index(finalProjection));
        }
    }

    @Transactional(readOnly = true)
    public List<RecursalMeshProcessLinkView> findByProcesso(Long processoId) {
        return repository.findTop50ByProcesso_IdOrderByUpdatedAtDesc(processoId).stream()
                .map(this::viewOf)
                .toList();
    }

    private void executeIndexing(Runnable action) {
        RecursalMeshRetryExecutor retryExecutor = retryExecutorProvider == null ? null : retryExecutorProvider.getIfAvailable();
        if (retryExecutor == null) {
            action.run();
            return;
        }
        retryExecutor.executeVoid("index", "projection-sync", action);
    }

    private int readRevision(int totalTransitions) {
        return Math.max(totalTransitions - 1, 0);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    RecursalMeshProcessLinkView viewOf(RecursalProcessIntegrationState projection) {
        return new RecursalMeshProcessLinkView(
                projection.getRecursoId(),
                projection.getProcesso() == null ? null : projection.getProcesso().getId(),
                projection.getNumeroProcesso(),
                projection.getSpeciesCode(),
                projection.getProfileName(),
                projection.getCurrentState(),
                projection.getTribunalAtual(),
                projection.getTribunalDetalhadoAtual(),
                projection.getInstanciaAtual(),
                projection.getAutoridadeAtual(),
                projection.getLastEvent(),
                projection.getCurrentRevision(),
                projection.getTotalTransitions(),
                projection.getIteracoesEmbargos(),
                projection.isTransitadoEmJulgado(),
                projection.getLastActor(),
                projection.getLastTransitionAt(),
                slaService.snapshot(
                        projection.getCurrentState(),
                        projection.getTribunalAtual(),
                        projection.getTribunalDetalhadoAtual(),
                        projection.getLastTransitionAt() == null ? projection.getUpdatedAt() : projection.getLastTransitionAt(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getUf(),
                        projection.getProcesso() == null ? null : projection.getProcesso().getComarca()
                ).orElse(null),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }


    Optional<com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot> slaSnapshotOf(RecursalProcessIntegrationState projection) {
        if (projection == null) {
            return Optional.empty();
        }
        return slaService.snapshot(
                projection.getCurrentState(),
                projection.getTribunalAtual(),
                projection.getTribunalDetalhadoAtual(),
                projection.getLastTransitionAt() == null ? projection.getUpdatedAt() : projection.getLastTransitionAt(),
                projection.getProcesso() == null ? null : projection.getProcesso().getUf(),
                projection.getProcesso() == null ? null : projection.getProcesso().getComarca()
        );
    }

    com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot toSlaSnapshot(com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel model) {
        if (model == null || model.getSlaDiasUteisEsperados() == null || model.getSlaDataPrevistaSaida() == null) {
            return null;
        }
        return new com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot(
                parseEnum(com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState.class, model.getCurrentState()),
                parseEnum(com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal.class, model.getTribunalAtual()),
                model.getSlaDiasUteisEsperados(),
                Boolean.TRUE.equals(model.getSlaFatalParaPartes()),
                model.getSlaFundamentoLegal(),
                model.getLastTransitionAt() == null ? null : model.getLastTransitionAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                model.getSlaDataPrevistaSaida(),
                Boolean.TRUE.equals(model.getSlaVencido()),
                model.getSlaDiasUteisExcedidos() == null ? 0 : model.getSlaDiasUteisExcedidos(),
                model.getSlaSeveridade()
        );
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}
