package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCompetenciaTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPrecedentTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPublicPaymentTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRemessaTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSustentacaoOralTrace;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

class RecursalMeshDashboardServiceTest {

    @Test
    void shouldBuildDashboardFromFallbackProjectionWithGargalosAndTemas() throws Exception {
        RecursalProcessIntegrationStateRepository repository = mock(RecursalProcessIntegrationStateRepository.class);
        RecursalMeshProjectionService projectionService = mock(RecursalMeshProjectionService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(null);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RecursalMeshDashboardService service = new RecursalMeshDashboardService(repository, projectionService, queryRepositoryProvider, objectMapper);

        RecursalProcessIntegrationState one = projection("rec-1", 1L, "RESP", RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE, "TEMA-1102", "STJ", "Tema repetitivo 1102", true, false, false);
        RecursalProcessIntegrationState two = projection("rec-2", 2L, "ARESP", RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE, "TEMA-1102", "STJ", "Tema repetitivo 1102", false, true, false);
        when(repository.findByProcesso_IdIn(org.mockito.ArgumentMatchers.eq(List.of(1L)), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(one, two)));
        when(projectionService.slaSnapshotOf(one)).thenReturn(java.util.Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE,
                RecursalTribunal.STJ,
                30,
                true,
                "Monitoramento de tema vinculante e retomada recursal",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 5, 10),
                true,
                5,
                "CRITICO_PARTES"
        )));
        when(projectionService.slaSnapshotOf(two)).thenReturn(java.util.Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
                RecursalTribunal.STJ,
                5,
                false,
                "Aplicação ou distinguishing fundamentado do precedente vinculante",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 11),
                false,
                0,
                "MONITORAR"
        )));

        var response = service.dashboard(new RecursalMeshDashboardRequest(
                null,
                null,
                List.of(1L),
                null,
                null,
                null,
                null,
                null,
                null,
                "STJ",
                "tema repetitivo",
                null,
                null,
                null,
                null,
                null,
                null,
                200,
                5
        ));

        assertThat(response.source()).isEqualTo("RELATIONAL_FALLBACK");
        assertThat(response.totalItens()).isEqualTo(1);
        assertThat(response.totalSobrestadosPorPrecedente()).isEqualTo(1);
        assertThat(response.totalPrecedenteAplicado()).isEqualTo(0);
        assertThat(response.gargalosPorEstado()).isNotEmpty();
        assertThat(response.gargalosPorTribunal()).isNotEmpty();
        assertThat(response.gargalosPorAutoridadeAtual()).isNotEmpty();
        assertThat(response.porTemaPrecedente()).first().extracting(bucket -> bucket.key()).isEqualTo("TEMA-1102 — Tema repetitivo 1102");
    }

    private RecursalProcessIntegrationState projection(String recursoId,
                                                       Long processoId,
                                                       String speciesCode,
                                                       RecursalLifecycleState state,
                                                       String precedenteCodigo,
                                                       String precedenteTribunal,
                                                       String precedenteTema,
                                                       boolean sobrestado,
                                                       boolean aplicado,
                                                       boolean distinguido) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Processo processo = Processo.builder().id(processoId).tribunal("STJ").comarca("Fortaleza").uf("CE").assunto("Tema repetitivo").build();
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId(recursoId);
        projection.setProcesso(processo);
        projection.setSpeciesCode(speciesCode);
        projection.setProfileName("SUPERIOR");
        projection.setCurrentState(state);
        projection.setTribunalAtual(RecursalTribunal.STJ);
        projection.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.STJ);
        projection.setInstanciaAtual(InstanceLevel.SUPERIOR);
        projection.setAutoridadeAtual(RecursalAuthority.RELATOR);
        projection.setLastTransitionAt(Instant.parse("2026-04-05T12:00:00Z"));
        projection.setUpdatedAt(Instant.parse("2026-04-05T12:00:00Z"));
        projection.setSnapshotJson(objectMapper.writeValueAsString(new RecursalStateSnapshot(
                recursoId,
                state,
                2,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.RELATOR,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                sobrestado,
                false,
                false,
                false,
                0,
                false,
                false,
                RecursalRemessaTrace.empty(),
                RecursalSustentacaoOralTrace.empty(),
                new RecursalPrecedentTrace(sobrestado, precedenteCodigo, precedenteTribunal, precedenteTema, aplicado, distinguido, distinguido ? "Distinção fundamentada" : null, Instant.parse("2026-04-05T12:00:00Z"), aplicado || distinguido ? Instant.parse("2026-04-05T12:00:00Z") : null),
                RecursalCompetenciaTrace.empty(),
                RecursalPublicPaymentTrace.empty(),
                Instant.parse("2026-04-05T12:00:00Z")
        )));
        return projection;
    }
}
