package com.tcc.pjb.backend.model.entity.casefile;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

class CaseContinuityTrackTest {
    @Test
    void deveManterCumprimentoQuandoAcaoExplicitaInicioDeCumprimento() {
        CaseContinuityTrack track = CaseContinuityTrack.resolve(
                ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
                FaseProcessual.EXECUCAO,
                StatusProcesso.CUMPRIMENTO_SENTENCA
        );

        assertThat(track).isEqualTo(CaseContinuityTrack.CUMPRIMENTO);
    }

    @Test
    void devePriorizarExecucaoQuandoSincronizaSemAcaoMasJaEstaNaFaseExecutoriaPlena() {
        CaseContinuityTrack track = CaseContinuityTrack.resolve(
                null,
                FaseProcessual.EXECUCAO,
                StatusProcesso.CUMPRIMENTO_SENTENCA
        );

        assertThat(track).isEqualTo(CaseContinuityTrack.EXECUCAO);
    }
}
