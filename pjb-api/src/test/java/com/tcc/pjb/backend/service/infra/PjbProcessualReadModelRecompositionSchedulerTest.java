package com.tcc.pjb.backend.service.infra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbProcessualReadModelRecompositionSchedulerTest {

    @Test
    void shouldNotClaimJobsWhenPersistenceIsDisabled() {
        PjbProcessualReadModelRecompositionQueueService queueService = mock(PjbProcessualReadModelRecompositionQueueService.class);
        PjbProcessualReadModelPersistenceService persistenceService = mock(PjbProcessualReadModelPersistenceService.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getProcessualReadModels().setPersistenceEnabled(false);

        PjbProcessualReadModelRecompositionScheduler scheduler = new PjbProcessualReadModelRecompositionScheduler(queueService, persistenceService, properties);
        scheduler.replay();

        verify(queueService, never()).claimBatch();
        verify(persistenceService, never()).recompose(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCompleteJobWhenRecompositionSucceeds() {
        PjbProcessualReadModelRecompositionQueueService queueService = mock(PjbProcessualReadModelRecompositionQueueService.class);
        PjbProcessualReadModelPersistenceService persistenceService = mock(PjbProcessualReadModelPersistenceService.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelRecompositionJob job = new ProcessualReadModelRecompositionJob();
        job.setId(7L);
        job.setDomain("PROCESSO_TIMELINE_HOT");
        when(queueService.claimBatch()).thenReturn(List.of(job));
        when(persistenceService.recompose(any(), any(), any(), any(), any(), any())).thenReturn(
                new PjbProcessualReadModelPersistenceService.RecompositionResult("PROCESSO_TIMELINE_HOT", 3, "RECOMPOSED")
        );

        PjbProcessualReadModelRecompositionScheduler scheduler = new PjbProcessualReadModelRecompositionScheduler(queueService, persistenceService, properties);
        scheduler.replay();

        verify(queueService).claimBatch();
        verify(queueService).complete(eq(7L), eq("RECOMPOSED:3"));
        verify(queueService, never()).fail(eq(7L), any());
    }

    @Test
    void shouldFailJobWhenRecompositionThrows() {
        PjbProcessualReadModelRecompositionQueueService queueService = mock(PjbProcessualReadModelRecompositionQueueService.class);
        PjbProcessualReadModelPersistenceService persistenceService = mock(PjbProcessualReadModelPersistenceService.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelRecompositionJob job = new ProcessualReadModelRecompositionJob();
        job.setId(9L);
        job.setDomain("PROCESSO_TIMELINE_HOT");
        when(queueService.claimBatch()).thenReturn(List.of(job));
        when(persistenceService.recompose(any(), any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("recomposition-failure"));

        PjbProcessualReadModelRecompositionScheduler scheduler = new PjbProcessualReadModelRecompositionScheduler(queueService, persistenceService, properties);
        scheduler.replay();

        verify(queueService).fail(9L, "recomposition-failure");
        verify(queueService, never()).complete(eq(9L), any());
    }

    @Test
    void shouldIgnoreNullJobsAndJobsWithoutId() {
        PjbProcessualReadModelRecompositionQueueService queueService = mock(PjbProcessualReadModelRecompositionQueueService.class);
        PjbProcessualReadModelPersistenceService persistenceService = mock(PjbProcessualReadModelPersistenceService.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelRecompositionJob withoutId = new ProcessualReadModelRecompositionJob();
        withoutId.setDomain("PROCESSO_TIMELINE_HOT");
        when(queueService.claimBatch()).thenReturn(Arrays.asList(null, withoutId));

        PjbProcessualReadModelRecompositionScheduler scheduler = new PjbProcessualReadModelRecompositionScheduler(queueService, persistenceService, properties);
        scheduler.replay();

        verify(persistenceService, never()).recompose(any(), any(), any(), any(), any(), any());
        verify(queueService, never()).complete(any(), any());
        verify(queueService, never()).fail(any(), any());
    }
}
