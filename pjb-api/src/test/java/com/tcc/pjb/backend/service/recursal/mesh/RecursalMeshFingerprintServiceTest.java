package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;

class RecursalMeshFingerprintServiceTest {

    @Test
    void deveProduzirHashDeterministicoEVariarQuandoEstadoMuda() {
        RecursalMeshFingerprintService service = new RecursalMeshFingerprintService();
        RecursalAggregateState aggregate = new RecursalAggregateState();
        aggregate.setRecursoId("re-1");
        aggregate.setSpeciesCode("RE");
        aggregate.setProfileName("STF_RULE_PROFILE");
        aggregate.setCurrentState(RecursalLifecycleState.INTERPOSTO);
        aggregate.setTribunalAtual(RecursalTribunal.STF);
        aggregate.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.STF);
        aggregate.setInstanciaAtual(InstanceLevel.EXTRAORDINARY);
        aggregate.setAutoridadeAtual(RecursalAuthority.PRESIDENCIA);
        aggregate.setSnapshotJson("{\"revision\":1}");
        aggregate.setRoutePlanJson("{\"profileName\":\"STF_RULE_PROFILE\"}");
        aggregate.setContextJson("{\"processoId\":1}");

        String first = service.aggregateFingerprint(aggregate);
        String second = service.aggregateFingerprint(aggregate);
        aggregate.setCurrentState(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM);
        String third = service.aggregateFingerprint(aggregate);

        assertThat(first).hasSize(64);
        assertThat(second).isEqualTo(first);
        assertThat(third).isNotEqualTo(first);
    }
}
