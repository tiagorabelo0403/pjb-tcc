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

class CivelJourneyTest extends JourneyTestSupport {
    @Test
    void devePercorrerJornadaCivelDoConhecimentoAoArquivamentoEDesarquivamento() {
        Processo processo = processo(RitoProcessual.COMUM_ORDINARIO, StatusProcesso.DISTRIBUIDO, FaseProcessual.CONHECIMENTO);
        ProcessoLifecycleMachine machine = new ProcessoLifecycleMachine(new AtoProcessualCatalogService());

        machine.apply(processo, ProcessoLifecycleAction.EXPEDIR_INTIMACAO);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.CITACAO_REALIZADA);

        machine.apply(processo, ProcessoLifecycleAction.PROFERIR_SENTENCA);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.SENTENCA_PROFERIDA);

        machine.apply(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.RECURSAL);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.RECURSO_INTERPOSTO);

        machine.apply(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.TRANSITO_EM_JULGADO);

        machine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.CUMPRIMENTO_SENTENCA);

        machine.apply(processo, ProcessoLifecycleAction.ARQUIVAR);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.ARQUIVADO);

        machine.apply(processo, ProcessoLifecycleAction.DESARQUIVAR);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.EM_ANDAMENTO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.CONHECIMENTO);
    }
}
