package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class RecursalMeshProjectionServiceTest {

    @Mock
    private RecursalProcessIntegrationStateRepository repository;

    @Mock
    private RecursalMeshSlaService slaService;

    @Test
    void deveMapearVisaoPorProcesso() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshSearchIndexerService> indexerProvider = (ObjectProvider<RecursalMeshSearchIndexerService>) org.mockito.Mockito.mock(ObjectProvider.class);
        RecursalMeshProjectionService service = new RecursalMeshProjectionService(repository, new RecursalMeshFingerprintService(), slaService, indexerProvider);
        Processo processo = new Processo();
        processo.setId(9L);
        processo.setNumeroProcesso("0000009-00.2026.8.06.0001");
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId("agint-9");
        projection.setProcesso(processo);
        projection.setNumeroProcesso(processo.getNumeroProcesso());
        projection.setSpeciesCode("AGINT");
        projection.setProfileName("TJCE_RULE_PROFILE");
        projection.setCurrentState(RecursalLifecycleState.JULGAMENTO_COLEGIADO);
        projection.setTribunalAtual(RecursalTribunal.TJ);
        projection.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.TJCE);
        projection.setInstanciaAtual(InstanceLevel.SECOND_INSTANCE);
        projection.setAutoridadeAtual(RecursalAuthority.CAMARA);
        projection.setLastEvent(RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        projection.setCurrentRevision(6);
        projection.setTotalTransitions(7);
        projection.setIteracoesEmbargos(1);
        projection.setTransitadoEmJulgado(false);
        projection.setLastActor("relator");
        projection.setLastTransitionAt(Instant.parse("2026-03-11T16:00:00Z"));
        projection.setSnapshotJson("{}");
        projection.setRoutePlanJson("{}");
        projection.setIntegrityFingerprint(new RecursalMeshFingerprintService().projectionFingerprint(projection));
        when(repository.findTop50ByProcesso_IdOrderByUpdatedAtDesc(9L)).thenReturn(List.of(projection));
        when(slaService.snapshot(any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.JULGAMENTO_COLEGIADO,
                RecursalTribunal.TJ,
                30,
                false,
                "Fundamento",
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 4, 22),
                false,
                0,
                "MONITORAR_INTERNO"
        )));

        List<RecursalMeshProcessLinkView> views = service.findByProcesso(9L);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().recursoId()).isEqualTo("agint-9");
        assertThat(views.getFirst().processoId()).isEqualTo(9L);
        assertThat(views.getFirst().speciesCode()).isEqualTo("AGINT");
        assertThat(views.getFirst().currentRevision()).isEqualTo(6);
        assertThat(views.getFirst().sla()).isNotNull();
    }
}
