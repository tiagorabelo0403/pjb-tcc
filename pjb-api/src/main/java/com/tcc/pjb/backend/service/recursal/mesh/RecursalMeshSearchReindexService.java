package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.inject.Inject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshReindexRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshReindexResponse;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalMeshReindexCheckpoint;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalMeshReindexCheckpointRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.platform.cluster.PjbClusterLockService;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class RecursalMeshSearchReindexService {

    private static final Duration LOCK_TTL = Duration.ofMinutes(20);

    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final ObjectProvider<RecursalMeshSearchIndexerService> indexerServiceProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider;
    private final RecursalMeshReindexCheckpointRepository checkpointRepository;
    private final PjbClusterLockService clusterLockService;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;
    private final ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider;

    RecursalMeshSearchReindexService(RecursalProcessIntegrationStateRepository projectionRepository,
                                     ObjectProvider<RecursalMeshSearchIndexerService> indexerServiceProvider,
                                     ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                     ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider) {
        this(projectionRepository, indexerServiceProvider, elasticsearchOperationsProvider, queryRepositoryProvider, null, null, null, null);
    }

    RecursalMeshSearchReindexService(RecursalProcessIntegrationStateRepository projectionRepository,
                                     ObjectProvider<RecursalMeshSearchIndexerService> indexerServiceProvider,
                                     ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                     ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                     RecursalMeshReindexCheckpointRepository checkpointRepository,
                                     PjbClusterLockService clusterLockService,
                                     ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider) {
        this(projectionRepository, indexerServiceProvider, elasticsearchOperationsProvider, queryRepositoryProvider, checkpointRepository, clusterLockService, telemetryProvider, null);
    }

    @Inject
    public RecursalMeshSearchReindexService(RecursalProcessIntegrationStateRepository projectionRepository,
                                            ObjectProvider<RecursalMeshSearchIndexerService> indexerServiceProvider,
                                            ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                            ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                            RecursalMeshReindexCheckpointRepository checkpointRepository,
                                            PjbClusterLockService clusterLockService,
                                            ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider,
                                            ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider) {
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.indexerServiceProvider = Objects.requireNonNull(indexerServiceProvider);
        this.elasticsearchOperationsProvider = Objects.requireNonNull(elasticsearchOperationsProvider);
        this.queryRepositoryProvider = Objects.requireNonNull(queryRepositoryProvider);
        this.checkpointRepository = checkpointRepository;
        this.clusterLockService = clusterLockService;
        this.telemetryProvider = telemetryProvider;
        this.retryExecutorProvider = retryExecutorProvider;
    }

    @PjbTransactionalBudget(operation = "recursal.mesh.reindex", maxMillis = 15000)
    @Transactional
    public RecursalMeshReindexResponse reindex(RecursalMeshReindexRequest request) {
        RecursalMeshReindexRequest normalized = request == null
                ? new RecursalMeshReindexRequest(false, 200, null, true, true, null)
                : request;
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        RecursalMeshQueryRepository queryRepository = queryRepositoryProvider.getIfAvailable();
        RecursalMeshSearchIndexerService indexerService = indexerServiceProvider.getIfAvailable();
        String checkpointKey = normalized.checkpointKey();
        if (operations == null || queryRepository == null || indexerService == null) {
            return new RecursalMeshReindexResponse(
                    "SEARCH_DISABLED",
                    RecursalMeshQueryModel.INDEX_NAME,
                    false,
                    0,
                    0,
                    0,
                    normalized.batchSize(),
                    normalized.maxDocuments(),
                    checkpointKey,
                    checkpointOf(checkpointKey).map(RecursalMeshReindexCheckpoint::getStatus).orElse("UNAVAILABLE"),
                    null,
                    false,
                    checkpointOf(checkpointKey).map(RecursalMeshReindexCheckpoint::getLastProcessedRecursoId).orElse(null)
            );
        }
        Optional<PjbClusterLockService.Lease> leaseOptional = clusterLockService == null
                ? Optional.of(new NoopLease(lockKey(checkpointKey), "local-noop"))
                : clusterLockService.tryAcquire(lockKey(checkpointKey), LOCK_TTL);
        if (leaseOptional.isEmpty()) {
            telemetry().ifPresent(it -> it.recordReindexLockContention(checkpointKey));
            RecursalMeshReindexCheckpoint checkpoint = checkpointOf(checkpointKey).orElse(null);
            return new RecursalMeshReindexResponse(
                    "LOCK_CONTENDED",
                    RecursalMeshQueryModel.INDEX_NAME,
                    false,
                    checkpoint == null ? 0 : checkpoint.getProcessedCount(),
                    checkpoint == null ? 0 : checkpoint.getIndexedCount(),
                    checkpoint == null ? 0 : checkpoint.getSkippedCount(),
                    normalized.batchSize(),
                    normalized.maxDocuments(),
                    checkpointKey,
                    checkpoint == null ? "LOCK_CONTENDED" : checkpoint.getStatus(),
                    checkpoint == null ? null : checkpoint.getLockOwner(),
                    false,
                    checkpoint == null ? null : checkpoint.getLastProcessedRecursoId()
            );
        }
        try (PjbClusterLockService.Lease lease = leaseOptional.orElseThrow()) {
            telemetry().ifPresent(it -> it.recordReindexStarted(checkpointKey));
            RecursalMeshReindexCheckpoint checkpoint = prepareCheckpoint(normalized, lease.owner());
            boolean resumed = Boolean.TRUE.equals(normalized.resumeFromCheckpoint())
                    && !Boolean.TRUE.equals(normalized.recreateIndex())
                    && checkpoint.getLastProcessedUpdatedAt() != null;
            if (!resumed) {
                checkpoint.setProcessedCount(0);
                checkpoint.setIndexedCount(0);
                checkpoint.setSkippedCount(0);
                checkpoint.setBatchCount(0);
                checkpoint.setLastProcessedUpdatedAt(null);
                checkpoint.setLastProcessedRecursoId(null);
            }
            checkpoint.setStatus("RUNNING");
            checkpoint.setLockOwner(lease.owner());
            checkpoint.setStartedAt(checkpoint.getStartedAt() == null || !resumed ? Instant.now() : checkpoint.getStartedAt());
            checkpoint.setCompletedAt(null);
            checkpoint.setLastError(null);

            IndexOperations indexOperations = operations.indexOps(RecursalMeshQueryModel.class);
            boolean recreated = executeIndexing("reindex-prepare-index", () -> prepareIndex(indexOperations, normalized.recreateIndex()));
            if (recreated) {
                checkpoint.setLastProcessedUpdatedAt(null);
                checkpoint.setLastProcessedRecursoId(null);
                checkpoint.setProcessedCount(0);
                checkpoint.setIndexedCount(0);
                checkpoint.setSkippedCount(0);
                checkpoint.setBatchCount(0);
                resumed = false;
            }

            long remaining = normalized.maxDocuments() == null ? Long.MAX_VALUE : normalized.maxDocuments().longValue();
            if (resumed) {
                remaining = remaining == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, remaining - checkpoint.getProcessedCount());
            }

            while (remaining > 0) {
                List<RecursalProcessIntegrationState> page = projectionRepository.findNextForReindex(
                        checkpoint.getLastProcessedUpdatedAt(),
                        checkpoint.getLastProcessedRecursoId() == null ? "" : checkpoint.getLastProcessedRecursoId(),
                        PageRequest.of(0, normalized.batchSize(), Sort.by(Sort.Order.asc("updatedAt"), Sort.Order.asc("recursoId")))
                );
                if (page.isEmpty()) {
                    break;
                }
                List<RecursalMeshQueryModel> batch = new ArrayList<>();
                int processedDelta = 0;
                int skippedDelta = 0;
                for (RecursalProcessIntegrationState projection : page) {
                    if (remaining <= 0) {
                        break;
                    }
                    processedDelta++;
                    remaining--;
                    checkpoint.setLastProcessedUpdatedAt(projection == null ? checkpoint.getLastProcessedUpdatedAt() : projection.getUpdatedAt());
                    checkpoint.setLastProcessedRecursoId(projection == null ? checkpoint.getLastProcessedRecursoId() : projection.getRecursoId());
                    if (projection == null || projection.getRecursoId() == null || projection.getRecursoId().isBlank()) {
                        skippedDelta++;
                        continue;
                    }
                    batch.add(indexerService.toDocument(projection));
                }
                int indexedDelta = batch.size();
                if (indexedDelta > 0) {
                    List<RecursalMeshQueryModel> documentsToSave = List.copyOf(batch);
                    executeIndexingVoid("reindex-batch-save", () -> queryRepository.saveAll(documentsToSave));
                }
                int processedDeltaSnapshot = processedDelta;
                int skippedDeltaSnapshot = skippedDelta;
                checkpoint.setProcessedCount(checkpoint.getProcessedCount() + processedDeltaSnapshot);
                checkpoint.setIndexedCount(checkpoint.getIndexedCount() + indexedDelta);
                checkpoint.setSkippedCount(checkpoint.getSkippedCount() + skippedDeltaSnapshot);
                checkpoint.setBatchCount(checkpoint.getBatchCount() + 1);
                telemetry().ifPresent(it -> it.recordReindexBatch(checkpointKey, processedDeltaSnapshot, indexedDelta, skippedDeltaSnapshot));
                if (page.size() < normalized.batchSize() || remaining <= 0) {
                    break;
                }
            }

            if (Boolean.TRUE.equals(normalized.refreshAtEnd())) {
                executeIndexingVoid("reindex-refresh", indexOperations::refresh);
            }

            checkpoint.setStatus("COMPLETED");
            checkpoint.setCompletedAt(Instant.now());
            checkpoint.setLockOwner(null);
            checkpointRepositorySave(checkpoint);
            telemetry().ifPresent(it -> it.recordReindexCompleted(checkpointKey, checkpoint.getProcessedCount(), checkpoint.getIndexedCount(), checkpoint.getSkippedCount(), checkpoint.getBatchCount()));
            return new RecursalMeshReindexResponse(
                    "REINDEXED",
                    RecursalMeshQueryModel.INDEX_NAME,
                    recreated,
                    checkpoint.getProcessedCount(),
                    checkpoint.getIndexedCount(),
                    checkpoint.getSkippedCount(),
                    normalized.batchSize(),
                    normalized.maxDocuments(),
                    checkpointKey,
                    checkpoint.getStatus(),
                    lease.owner(),
                    resumed,
                    checkpoint.getLastProcessedRecursoId()
            );
        } catch (RuntimeException ex) {
            RecursalMeshReindexCheckpoint checkpoint = checkpointOf(checkpointKey).orElse(null);
            if (checkpoint != null) {
                checkpoint.setStatus("FAILED");
                checkpoint.setCompletedAt(Instant.now());
                checkpoint.setLockOwner(null);
                checkpoint.setLastError(trimError(ex));
                checkpointRepositorySave(checkpoint);
            }
            telemetry().ifPresent(it -> it.recordReindexFailed(checkpointKey));
            throw ex;
        }
    }

    private RecursalMeshReindexCheckpoint prepareCheckpoint(RecursalMeshReindexRequest request, String lockOwner) {
        RecursalMeshReindexCheckpoint checkpoint = checkpointOf(request.checkpointKey()).orElseGet(RecursalMeshReindexCheckpoint::new);
        checkpoint.setCheckpointKey(request.checkpointKey());
        checkpoint.setIndexName(RecursalMeshQueryModel.INDEX_NAME);
        checkpoint.setLockOwner(lockOwner);
        if (checkpoint.getStatus() == null) {
            checkpoint.setStatus("PENDING");
        }
        return checkpoint;
    }


    private <T> T executeIndexing(String target, java.util.function.Supplier<T> action) {
        RecursalMeshRetryExecutor retryExecutor = retryExecutorProvider == null ? null : retryExecutorProvider.getIfAvailable();
        if (retryExecutor == null) {
            return action.get();
        }
        return retryExecutor.execute("index", target, action);
    }

    private void executeIndexingVoid(String target, Runnable action) {
        RecursalMeshRetryExecutor retryExecutor = retryExecutorProvider == null ? null : retryExecutorProvider.getIfAvailable();
        if (retryExecutor == null) {
            action.run();
            return;
        }
        retryExecutor.executeVoid("index", target, action);
    }

    private Optional<RecursalMeshOperationalTelemetryService> telemetry() {
        return telemetryProvider == null ? Optional.empty() : Optional.ofNullable(telemetryProvider.getIfAvailable());
    }

    private Optional<RecursalMeshReindexCheckpoint> checkpointOf(String checkpointKey) {
        if (checkpointRepository == null) {
            return Optional.empty();
        }
        return checkpointRepository.findById(checkpointKey);
    }

    private void checkpointRepositorySave(RecursalMeshReindexCheckpoint checkpoint) {
        if (checkpointRepository != null && checkpoint != null) {
            checkpointRepository.save(checkpoint);
        }
    }

    private boolean prepareIndex(IndexOperations indexOperations, boolean recreateIndex) {
        if (recreateIndex) {
            if (indexOperations.exists()) {
                indexOperations.delete();
            }
            return indexOperations.createWithMapping();
        }
        if (!indexOperations.exists()) {
            return indexOperations.createWithMapping();
        }
        return false;
    }

    private static String lockKey(String checkpointKey) {
        return "recursal-mesh:index-reindex:" + checkpointKey;
    }

    private static String trimError(RuntimeException ex) {
        String message = ex == null ? null : ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex == null ? null : ex.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private record NoopLease(String key, String owner) implements PjbClusterLockService.Lease {
        @Override
        public void close() {
        }
    }
}
