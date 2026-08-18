package com.tcc.pjb.backend.journey;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachineTestFactory;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

class TrabalhistaJourneyTest extends JourneyTestSupport {
    @Test
    void deveExecutarRitoTrabalhistaComDeskEspecializado() {
        Processo processo = processo(RitoProcessual.TRABALHISTA_EXECUCAO, StatusProcesso.TRANSITO_EM_JULGADO, FaseProcessual.RECURSAL);
        ProcessoLifecycleMachine machine = ProcessoLifecycleMachineTestFactory.standalone();

        var preview = machine.preview(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        assertThat(preview.responsavelSugerido()).isEqualTo("MAGISTRATURA_TRABALHISTA");

        machine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.EXECUCAO);
    }
}
