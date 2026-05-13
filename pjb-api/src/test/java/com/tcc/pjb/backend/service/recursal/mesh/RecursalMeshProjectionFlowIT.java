package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RecursalMeshProjectionFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private RecursalMeshProjectionService projectionService;

    @Autowired
    private RecursalProcessIntegrationStateRepository projectionRepository;

    @MockitoBean
    private RecursalMeshSearchIndexerService searchIndexerService;

    @MockitoBean
    private RecursalMeshRetryExecutor retryExecutor;

    @Test
    void devePersistirProjecaoComFingerprintESlaSemDerivaDeWorkflow() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(retryExecutor).executeVoid(any(), any(), any(Runnable.class));

        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("REC-2026-1")
                .numeroUnificado("0004444-55.2026.8.06.0001")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .build());

        RecursalAggregateState aggregate = new RecursalAggregateState();
        aggregate.setRecursoId("ap-0001");
        aggregate.setProcesso(processo);
        aggregate.setNumeroProcesso(processo.getNumeroUnificado());
        aggregate.setSpeciesCode("APELACAO");
        aggregate.setSpeciesName("Apelação");
        aggregate.setProfileName("CIVEL_ORDINARIO");
        aggregate.setCurrentState(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM);
        aggregate.setTribunalAtual(RecursalTribunal.TJ);
        aggregate.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.TJCE);
        aggregate.setInstanciaAtual(InstanceLevel.SECOND_INSTANCE);
        aggregate.setAutoridadeAtual(RecursalAuthority.SECRETARIA_JUDICIARIA);
        aggregate.setSnapshotJson("{\"state\":\"AGUARDANDO_ADMISSIBILIDADE\"}");
        aggregate.setRoutePlanJson("{\"next\":[\"CONTRARRAZOES\",\"COLEGIADO\"]}");
        aggregate.setContextJson("{\"ritual\":\"civil\"}");
        aggregate.setIteracoesEmbargos(0);

        projectionService.sync(
                aggregate,
                RecursalTransitionEvent.PROTOCOLAR,
                "gabinete-recursal",
                Instant.parse("2026-04-16T12:00:00Z"),
                3
        );

        var projection = projectionRepository.findById("ap-0001").orElseThrow();
        assertThat(projection.getProcesso()).isNotNull();
        assertThat(projection.getNumeroProcesso()).isEqualTo("0004444-55.2026.8.06.0001");
        assertThat(projection.getCurrentRevision()).isEqualTo(2);
        assertThat(projection.getTotalTransitions()).isEqualTo(3);
        assertThat(projection.getIntegrityFingerprint()).isNotBlank();
        assertThat(projectionService.findByProcesso(processo.getId()))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.recursoId()).isEqualTo("ap-0001");
                    assertThat(view.sla()).isNotNull();
                    assertThat(view.sla().fundamentoLegal()).isNotBlank();
                });
        verify(searchIndexerService).index(any());
    }
}
