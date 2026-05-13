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
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;
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
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;

class RecursalMeshSearchServiceTest {

    @Test
    void shouldFallbackToRelationalProjectionWhenSearchIndexIsUnavailable() throws Exception {
        RecursalProcessIntegrationStateRepository repository = mock(RecursalProcessIntegrationStateRepository.class);
        RecursalMeshProjectionService projectionService = mock(RecursalMeshProjectionService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.data.elasticsearch.core.ElasticsearchOperations> operationsProvider = (ObjectProvider<org.springframework.data.elasticsearch.core.ElasticsearchOperations>) mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider = (ObjectProvider<RecursalMeshQueryRepository>) mock(ObjectProvider.class);
        when(operationsProvider.getIfAvailable()).thenReturn(null);
        when(queryRepositoryProvider.getIfAvailable()).thenReturn(null);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RecursalMeshSearchService service = new RecursalMeshSearchService(repository, projectionService, operationsProvider, queryRepositoryProvider, objectMapper);

        Processo processo = Processo.builder().id(44L).tribunal("STJ").comarca("Fortaleza").uf("CE").assunto("Tema repetitivo").build();
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId("resp-44");
        projection.setProcesso(processo);
        projection.setNumeroProcesso("00044");
        projection.setSpeciesCode("RESP");
        projection.setProfileName("STJ_PROFILE");
        projection.setCurrentState(RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE);
        projection.setTribunalAtual(RecursalTribunal.STJ);
        projection.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.STJ);
        projection.setInstanciaAtual(InstanceLevel.SUPERIOR);
        projection.setAutoridadeAtual(RecursalAuthority.RELATOR);
        projection.setLastEvent(RecursalTransitionEvent.RETOMAR);
        projection.setLastActor("adv-rabelo");
        projection.setLastTransitionAt(Instant.parse("2026-04-04T12:00:00Z"));
        projection.setUpdatedAt(Instant.parse("2026-04-05T12:00:00Z"));
        projection.setSnapshotJson(objectMapper.writeValueAsString(snapshot()));

        RecursalProcessIntegrationState projectionOther = new RecursalProcessIntegrationState();
        projectionOther.setRecursoId("resp-99");
        projectionOther.setProcesso(Processo.builder().id(99L).tribunal("STJ").comarca("Fortaleza").uf("CE").assunto("Tema repetitivo").build());
        projectionOther.setNumeroProcesso("00099");
        projectionOther.setSpeciesCode("RESP");
        projectionOther.setProfileName("STJ_PROFILE");
        projectionOther.setCurrentState(RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE);
        projectionOther.setTribunalAtual(RecursalTribunal.STJ);
        projectionOther.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.STJ);
        projectionOther.setInstanciaAtual(InstanceLevel.SUPERIOR);
        projectionOther.setAutoridadeAtual(RecursalAuthority.RELATOR);
        projectionOther.setLastEvent(RecursalTransitionEvent.RETOMAR);
        projectionOther.setLastActor("adv-outro");
        projectionOther.setLastTransitionAt(Instant.parse("2026-04-04T11:00:00Z"));
        projectionOther.setUpdatedAt(Instant.parse("2026-04-05T11:00:00Z"));
        projectionOther.setSnapshotJson(objectMapper.writeValueAsString(snapshot()));

        when(repository.findByProcesso_IdIn(org.mockito.ArgumentMatchers.eq(List.of(44L)), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection, projectionOther)));
        when(projectionService.slaSnapshotOf(projection)).thenReturn(java.util.Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
                RecursalTribunal.STJ,
                5,
                false,
                "Aplicação ou distinguishing fundamentado do precedente vinculante",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 11),
                true,
                2,
                "ALERTA_INTERNO"
        )));
        when(projectionService.viewOf(projection)).thenReturn(new com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView(
                "resp-44",
                44L,
                "00044",
                "RESP",
                "STJ_PROFILE",
                RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.RELATOR,
                RecursalTransitionEvent.RETOMAR,
                2,
                3,
                0,
                false,
                "adv-rabelo",
                Instant.parse("2026-04-04T12:00:00Z"),
                new RecursalSlaSnapshot(
                        RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
                        RecursalTribunal.STJ,
                        5,
                        false,
                        "Aplicação ou distinguishing fundamentado do precedente vinculante",
                        LocalDate.of(2026, 4, 4),
                        LocalDate.of(2026, 4, 11),
                        true,
                        2,
                        "ALERTA_INTERNO"
                ),
                Instant.parse("2026-04-01T12:00:00Z"),
                Instant.parse("2026-04-05T12:00:00Z")
        ));

        var response = service.search(new RecursalMeshSearchRequest(
                "tema 1102",
                null,
                List.of(44L),
                "RESP",
                RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
                RecursalTribunal.STJ,
                null,
                null,
                "TEMA-1102",
                "STJ",
                "tema repetitivo",
                false,
                false,
                false,
                false,
                true,
                false,
                20
        ));

        assertThat(response.source()).isEqualTo("RELATIONAL_FALLBACK");
        assertThat(response.totalReturned()).isEqualTo(1);
        assertThat(response.items()).singleElement().extracting(item -> item.recursoId()).isEqualTo("resp-44");
    }

    private static RecursalStateSnapshot snapshot() {
        return new RecursalStateSnapshot(
                "resp-44",
                RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE,
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
                false,
                false,
                false,
                false,
                0,
                false,
                false,
                RecursalRemessaTrace.empty(),
                RecursalSustentacaoOralTrace.empty(),
                new RecursalPrecedentTrace(false, "TEMA-1102", "STJ", "Tema repetitivo 1102", false, false, null, Instant.parse("2026-04-04T12:00:00Z"), null),
                RecursalCompetenciaTrace.empty(),
                RecursalPublicPaymentTrace.empty(),
                Instant.parse("2026-04-04T12:00:00Z")
        );
    }
}
