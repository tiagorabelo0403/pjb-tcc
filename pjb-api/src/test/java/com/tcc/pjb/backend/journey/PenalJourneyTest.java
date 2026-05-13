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

class PenalJourneyTest extends JourneyTestSupport {
    @Test
    void deveLevarRitoPenalParaExecucaoAposTransito() {
        Processo processo = processo(RitoProcessual.TRIBUNAL_JURI, StatusProcesso.SENTENCA_PROFERIDA, FaseProcessual.PRONUNCIA);
        ProcessoLifecycleMachine machine = new ProcessoLifecycleMachine(new AtoProcessualCatalogService());

        machine.apply(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.RECURSAL);

        machine.apply(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO);
        machine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);

        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.EXECUCAO);
    }

    @Test
    void deveSinalizarAlertaQuandoJuriNaoPassouPorPronunciaOuPlenario() {
        Processo processo = processo(RitoProcessual.TRIBUNAL_JURI, StatusProcesso.EM_ANDAMENTO, FaseProcessual.CONHECIMENTO);
        ProcessoLifecycleMachine machine = new ProcessoLifecycleMachine(new AtoProcessualCatalogService());

        var decision = machine.preview(processo, ProcessoLifecycleAction.PROFERIR_SENTENCA);

        assertThat(decision.alertas()).anyMatch(alerta -> alerta.contains("Júri sem marcos típicos"));
    }
}
