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

class ExecucaoFiscalJourneyTest extends JourneyTestSupport {
    @Test
    void deveNascerEmExecucaoNoFluxoFiscal() {
        Processo processo = processo(RitoProcessual.EXECUCAO_FISCAL, StatusProcesso.DISTRIBUIDO, null);
        ProcessoLifecycleMachine machine = ProcessoLifecycleMachineTestFactory.standalone();

        var decision = machine.preview(processo, ProcessoLifecycleAction.DISTRIBUIR);

        assertThat(decision.faseDestino()).isEqualTo(FaseProcessual.EXECUCAO);
        assertThat(decision.responsavelSugerido()).isEqualTo("SECRETARIA");
    }
}
