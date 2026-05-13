package com.tcc.pjb.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

class JuizadoJourneyTest extends JourneyTestSupport {
    @Test
    void deveEncaminharJuizadoParaTurmaRecursal() {
        Processo processo = processo(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, StatusProcesso.SENTENCA_PROFERIDA, FaseProcessual.CONHECIMENTO);
        ProcessoLifecycleMachine machine = new ProcessoLifecycleMachine(new AtoProcessualCatalogService());

        var recurso = machine.preview(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);
        assertThat(recurso.alertas()).anyMatch(alerta -> alerta.contains("turma recursal"));

        machine.apply(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);
        var voto = machine.preview(processo, ProcessoLifecycleAction.PROFERIR_VOTO);
        assertThat(voto.responsavelSugerido()).isEqualTo("TURMA_RECURSAL");
    }
}
