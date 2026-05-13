package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshReindexRequest;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalMeshReindexCheckpoint;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalMeshReindexCheckpointRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.platform.cluster.PjbClusterLockService;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

class RecursalMeshSearchReindexServiceTest {

    @Test
    void shouldReindexProjectionBatchesIntoOperationalIndex() {
        RecursalProcessIntegrationStateRepository projectionRepository = mock(RecursalProcessIntegrationStateRepository.class);
        RecursalMeshSearchIndexerService indexerService = mock(RecursalMeshSearchIndexerService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshSearchIndexerService> indexerProvider = (ObjectProvider<RecursalMeshSearchIndexerService>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ElasticsearchOperations> operationsProvider = (ObjectProvider<ElasticsearchOperations>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        RecursalMeshQueryRepository queryRepository = mock(RecursalMeshQueryRepository.class);
        RecursalMeshReindexCheckpointRepository checkpointRepository = mock(RecursalMeshReindexCheckpointRepository.class);
        PjbClusterLockService lockService = mock(PjbClusterLockService.class);
        PjbClusterLockService.Lease lease = new PjbClusterLockService.Lease() {
            @Override public String key() { return "recursal"; }
            @Override public String owner() { return "node-a"; }
            @Override public void close() { }
        };

        when(indexerProvider.getIfAvailable()).thenReturn(indexerService);
        when(operationsProvider.getIfAvailable()).thenReturn(operations);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(queryRepository);
        when(operations.indexOps(RecursalMeshQueryModel.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false, false);
        when(indexOperations.createWithMapping()).thenReturn(true);
        when(lockService.tryAcquire(eq("recursal-mesh:index-reindex:default"), any(Duration.class))).thenReturn(Optional.of(lease));
        when(checkpointRepository.findById("default")).thenReturn(Optional.empty());
        when(projectionRepository.findNextForReindex(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(projection("rec-1"), projection("rec-2")))
                .thenReturn(List.of());
        when(indexerService.toDocument(any(RecursalProcessIntegrationState.class))).thenAnswer(invocation ->
                RecursalMeshQueryModel.builder().recursoId(((RecursalProcessIntegrationState) invocation.getArgument(0)).getRecursoId()).build());
        when(queryRepository.saveAll(any(Iterable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.save(any(RecursalMeshReindexCheckpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecursalMeshSearchReindexService service = new RecursalMeshSearchReindexService(
                projectionRepository,
                indexerProvider,
                operationsProvider,
                queryRepositoryProvider,
                checkpointRepository,
                lockService,
                telemetryProvider
        );

        var response = service.reindex(new RecursalMeshReindexRequest(true, 50, 2, true));

        assertThat(response.status()).isEqualTo("REINDEXED");
        assertThat(response.indexRecreated()).isTrue();
        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.indexed()).isEqualTo(2);
        assertThat(response.skipped()).isEqualTo(0);
        assertThat(response.indexName()).isEqualTo("pjb-recursal-mesh");
        assertThat(response.checkpointStatus()).isEqualTo("COMPLETED");
        assertThat(response.lockOwner()).isEqualTo("node-a");
        verify(checkpointRepository).save(any(RecursalMeshReindexCheckpoint.class));
    }

    @Test
    void shouldReturnLockContendedWhenAnotherNodeAlreadyOwnsReindex() {
        RecursalProcessIntegrationStateRepository projectionRepository = mock(RecursalProcessIntegrationStateRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshSearchIndexerService> indexerProvider = (ObjectProvider<RecursalMeshSearchIndexerService>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ElasticsearchOperations> operationsProvider = (ObjectProvider<ElasticsearchOperations>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        RecursalMeshQueryRepository queryRepository = mock(RecursalMeshQueryRepository.class);
        RecursalMeshSearchIndexerService indexerService = mock(RecursalMeshSearchIndexerService.class);
        RecursalMeshReindexCheckpointRepository checkpointRepository = mock(RecursalMeshReindexCheckpointRepository.class);
        PjbClusterLockService lockService = mock(PjbClusterLockService.class);
        RecursalMeshReindexCheckpoint checkpoint = new RecursalMeshReindexCheckpoint();
        checkpoint.setCheckpointKey("default");
        checkpoint.setStatus("RUNNING");
        checkpoint.setLockOwner("node-b");
        checkpoint.setProcessedCount(11);
        checkpoint.setIndexedCount(9);
        checkpoint.setSkippedCount(2);
        checkpoint.setLastProcessedRecursoId("rec-11");

        when(indexerProvider.getIfAvailable()).thenReturn(indexerService);
        when(operationsProvider.getIfAvailable()).thenReturn(operations);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(queryRepository);
        when(lockService.tryAcquire(eq("recursal-mesh:index-reindex:default"), any(Duration.class))).thenReturn(Optional.empty());
        when(checkpointRepository.findById("default")).thenReturn(Optional.of(checkpoint));

        RecursalMeshSearchReindexService service = new RecursalMeshSearchReindexService(
                projectionRepository,
                indexerProvider,
                operationsProvider,
                queryRepositoryProvider,
                checkpointRepository,
                lockService,
                telemetryProvider
        );

        var response = service.reindex(new RecursalMeshReindexRequest(false, 100, 1000, true));

        assertThat(response.status()).isEqualTo("LOCK_CONTENDED");
        assertThat(response.processed()).isEqualTo(11);
        assertThat(response.indexed()).isEqualTo(9);
        assertThat(response.skipped()).isEqualTo(2);
        assertThat(response.lastProcessedRecursoId()).isEqualTo("rec-11");
        assertThat(response.lockOwner()).isEqualTo("node-b");
    }


    @Test
    void shouldAccumulateSkippedItemsWithoutCorruptingBatchSnapshots() {
        RecursalProcessIntegrationStateRepository projectionRepository = mock(RecursalProcessIntegrationStateRepository.class);
        RecursalMeshSearchIndexerService indexerService = mock(RecursalMeshSearchIndexerService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshSearchIndexerService> indexerProvider = (ObjectProvider<RecursalMeshSearchIndexerService>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ElasticsearchOperations> operationsProvider = (ObjectProvider<ElasticsearchOperations>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        RecursalMeshQueryRepository queryRepository = mock(RecursalMeshQueryRepository.class);
        RecursalMeshReindexCheckpointRepository checkpointRepository = mock(RecursalMeshReindexCheckpointRepository.class);
        PjbClusterLockService lockService = mock(PjbClusterLockService.class);
        PjbClusterLockService.Lease lease = new PjbClusterLockService.Lease() {
            @Override public String key() { return "recursal"; }
            @Override public String owner() { return "node-a"; }
            @Override public void close() { }
        };
        RecursalProcessIntegrationState blank = projection("");
        RecursalProcessIntegrationState valid = projection("rec-2");

        when(indexerProvider.getIfAvailable()).thenReturn(indexerService);
        when(operationsProvider.getIfAvailable()).thenReturn(operations);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(queryRepository);
        when(operations.indexOps(RecursalMeshQueryModel.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);
        when(lockService.tryAcquire(eq("recursal-mesh:index-reindex:default"), any(Duration.class))).thenReturn(Optional.of(lease));
        when(checkpointRepository.findById("default")).thenReturn(Optional.empty());
        when(projectionRepository.findNextForReindex(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(blank, valid))
                .thenReturn(List.of());
        when(indexerService.toDocument(valid)).thenReturn(RecursalMeshQueryModel.builder().recursoId("rec-2").build());
        when(queryRepository.saveAll(any(Iterable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.save(any(RecursalMeshReindexCheckpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecursalMeshSearchReindexService service = new RecursalMeshSearchReindexService(
                projectionRepository,
                indexerProvider,
                operationsProvider,
                queryRepositoryProvider,
                checkpointRepository,
                lockService,
                telemetryProvider
        );

        var response = service.reindex(new RecursalMeshReindexRequest(false, 50, 2, false));

        assertThat(response.processed()).isEqualTo(2);
        assertThat(response.indexed()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        verify(queryRepository).saveAll(any(Iterable.class));
    }

    private RecursalProcessIntegrationState projection(String recursoId) {
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId(recursoId);
        projection.setUpdatedAt(Instant.parse("2026-04-05T12:00:00Z"));
        return projection;
    }
}
