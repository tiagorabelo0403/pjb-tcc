package com.tcc.pjb.backend.service.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelRecompositionJobRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

class PjbProcessualReadModelRecompositionQueueServiceTest {

    @Test
    void shouldDeduplicateOpenJobWithNullSafeScopeComparison() {
        ProcessualReadModelRecompositionJobRepository repository = Mockito.mock(ProcessualReadModelRecompositionJobRepository.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelRecompositionJob existing = new ProcessualReadModelRecompositionJob();
        existing.setId(10L);
        existing.setDomain("PROCESSO_TIMELINE_HOT");
        existing.setStatus("PENDENTE");
        when(repository.findTop50ByStatusInOrderByCreatedAtDesc(List.of("PENDENTE", "EM_PROCESSAMENTO"))).thenReturn(List.of(existing));

        JudicialScaleRuntimePolicyService runtimePolicyService = Mockito.mock(JudicialScaleRuntimePolicyService.class);
        PjbProcessualReadModelRecompositionQueueService service = new PjbProcessualReadModelRecompositionQueueService(repository, properties, runtimePolicyService);
        PjbProcessualReadModelRecompositionQueueService.JobView job = service.enqueue("processo_timeline_hot", null, null, null, null, "teste");

        assertThat(job.id()).isEqualTo(10L);
    }

    @Test
    void shouldClaimConfiguredBatch() {
        ProcessualReadModelRecompositionJobRepository repository = Mockito.mock(ProcessualReadModelRecompositionJobRepository.class);
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        ProcessualReadModelRecompositionJob job = new ProcessualReadModelRecompositionJob();
        job.setId(1L);
        job.setDomain("PROCESSO_TIMELINE_HOT");
        job.setStatus("PENDENTE");
        when(repository.findByStatusAndNotBeforeAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Pageable.class))).thenReturn(List.of(job));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JudicialScaleRuntimePolicyService runtimePolicyService = Mockito.mock(JudicialScaleRuntimePolicyService.class);
        when(runtimePolicyService.recompositionClaimBatch(any())).thenReturn(1);
        PjbProcessualReadModelRecompositionQueueService service = new PjbProcessualReadModelRecompositionQueueService(repository, properties, runtimePolicyService);
        List<ProcessualReadModelRecompositionJob> claimed = service.claimBatch();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getStatus()).isEqualTo("EM_PROCESSAMENTO");
        assertThat(claimed.get(0).getAttemptCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectBlankDomain() {
        ProcessualReadModelRecompositionJobRepository repository = Mockito.mock(ProcessualReadModelRecompositionJobRepository.class);
        JudicialScaleRuntimePolicyService runtimePolicyService = Mockito.mock(JudicialScaleRuntimePolicyService.class);
        PjbProcessualReadModelRecompositionQueueService service = new PjbProcessualReadModelRecompositionQueueService(repository, new PjbDataSourceRoutingProperties(), runtimePolicyService);

        assertThatThrownBy(() -> service.enqueue("  ", null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
