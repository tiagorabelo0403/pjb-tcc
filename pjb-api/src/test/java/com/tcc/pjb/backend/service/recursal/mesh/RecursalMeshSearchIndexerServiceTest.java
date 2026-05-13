package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
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
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

class RecursalMeshSearchIndexerServiceTest {

    @Test
    void shouldBuildOperationalSearchDocumentWithSlaTagsAndPrecedentTrace() throws Exception {
        RecursalMeshQueryRepository repository = mock(RecursalMeshQueryRepository.class);
        RecursalMeshSlaService slaService = mock(RecursalMeshSlaService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RecursalMeshSearchIndexerService service = new RecursalMeshSearchIndexerService(repository, slaService, objectMapper);

        Processo processo = Processo.builder()
                .id(9L)
                .numeroUnificado("0001234-55.2026.8.06.0001")
                .tribunal("TJCE")
                .vara("2a Vara Cível")
                .comarca("Fortaleza")
                .uf("CE")
                .assunto("Responsabilidade civil")
                .nivelSigilo(NivelSigilo.SEGREDO_JUSTICA)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .classeProcessual("Ação de indenização")
                .objetoProcessual("Indenização por danos morais")
                .pedidoPrincipal("Condenação ao pagamento")
                .build();

        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId("resp-2026-1");
        projection.setProcesso(processo);
        projection.setNumeroProcesso("0001234-55.2026.8.06.0001");
        projection.setSpeciesCode("RESP");
        projection.setProfileName("TRIBUNAL_JUSTICA");
        projection.setCurrentState(RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE);
        projection.setTribunalAtual(RecursalTribunal.STJ);
        projection.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.STJ);
        projection.setInstanciaAtual(InstanceLevel.SUPERIOR);
        projection.setAutoridadeAtual(RecursalAuthority.RELATOR);
        projection.setLastEvent(RecursalTransitionEvent.SOBRESTAR);
        projection.setCurrentRevision(3);
        projection.setTotalTransitions(4);
        projection.setIteracoesEmbargos(1);
        projection.setTransitadoEmJulgado(false);
        projection.setLastActor("adv-rabelo");
        projection.setLastTransitionAt(Instant.parse("2026-04-04T12:00:00Z"));
        projection.setSnapshotJson(objectMapper.writeValueAsString(snapshot()));

        when(slaService.snapshot(
                RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                Instant.parse("2026-04-04T12:00:00Z"),
                "CE",
                "Fortaleza"
        )).thenReturn(java.util.Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE,
                RecursalTribunal.STJ,
                30,
                true,
                "Monitoramento de tema vinculante e retomada recursal",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 5, 20),
                true,
                4,
                "CRITICO_PARTES"
        )));

        var document = service.toDocument(projection);

        assertThat(document.getRecursoId()).isEqualTo("resp-2026-1");
        assertThat(document.getTribunalAtual()).isEqualTo("STJ");
        assertThat(document.getSlaSeveridade()).isEqualTo("CRITICO_PARTES");
        assertThat(document.getSlaVencido()).isTrue();
        assertThat(document.getSobrestadoPrecedente()).isTrue();
        assertThat(document.getPrecedenteCodigo()).isEqualTo("TEMA-1102");
        assertThat(document.getPrecedenteTribunal()).isEqualTo("STJ");
        assertThat(document.getPrecedenteTema()).contains("repetitivo");
        assertThat(document.getTags()).contains("RESP", "STJ", "SLA_VENCIDO", "SLA_FATAL_PARTES", "SOBRESTADO_PRECEDENTE", "TEMA-1102");
        assertThat(document.getSearchableText()).contains("Responsabilidade civil", "Ação de indenização", "TEMA-1102");
    }

    private static RecursalStateSnapshot snapshot() {
        return new RecursalStateSnapshot(
                "resp-2026-1",
                RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE,
                3,
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
                true,
                false,
                false,
                false,
                1,
                false,
                false,
                RecursalRemessaTrace.empty(),
                RecursalSustentacaoOralTrace.empty(),
                new RecursalPrecedentTrace(true, "TEMA-1102", "STJ", "Tema repetitivo 1102", false, false, null, null, null),
                RecursalCompetenciaTrace.empty(),
                RecursalPublicPaymentTrace.empty(),
                Instant.parse("2026-04-04T12:00:00Z")
        );
    }
}
