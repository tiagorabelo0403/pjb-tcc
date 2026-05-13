package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

class RecursalMeshIndexDriftServiceTest {

    @Test
    void shouldDetectMissingAndOutdatedDocumentsInOperationalIndex() {
        RecursalProcessIntegrationStateRepository projectionRepository = mock(RecursalProcessIntegrationStateRepository.class);
        RecursalMeshQueryRepository queryRepository = mock(RecursalMeshQueryRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(queryRepository);
        when(projectionRepository.count()).thenReturn(2L);
        RecursalProcessIntegrationState first = projection("rec-1", 3, RecursalLifecycleState.PROVIDO, "2026-04-05T12:00:00Z");
        RecursalProcessIntegrationState second = projection("rec-2", 4, RecursalLifecycleState.IMPROVIDO, "2026-04-05T13:00:00Z");
        when(projectionRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(first, second)));
        when(queryRepository.count()).thenReturn(1L);
        when(queryRepository.findById("rec-1")).thenReturn(Optional.of(RecursalMeshQueryModel.builder()
                .recursoId("rec-1")
                .currentRevision(2)
                .currentState(RecursalLifecycleState.PROVIDO.name())
                .updatedAt(Instant.parse("2026-04-05T11:00:00Z"))
                .build()));
        when(queryRepository.findById("rec-2")).thenReturn(Optional.empty());

        RecursalMeshIndexDriftService service = new RecursalMeshIndexDriftService(projectionRepository, queryRepositoryProvider, telemetryProvider);

        var report = service.assess(20);

        assertThat(report.status()).isEqualTo("ASSESSED");
        assertThat(report.projectionCount()).isEqualTo(2L);
        assertThat(report.indexCount()).isEqualTo(1L);
        assertThat(report.sampled()).isEqualTo(2);
        assertThat(report.missingInIndex()).isEqualTo(1);
        assertThat(report.outdatedInIndex()).isEqualTo(1);
        assertThat(report.divergentRevision()).isEqualTo(1);
        assertThat(report.severity()).isNotBlank();
    }

    private RecursalProcessIntegrationState projection(String recursoId, int revision, RecursalLifecycleState state, String updatedAt) {
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId(recursoId);
        projection.setCurrentRevision(revision);
        projection.setCurrentState(state);
        projection.setUpdatedAt(Instant.parse(updatedAt));
        return projection;
    }
}
