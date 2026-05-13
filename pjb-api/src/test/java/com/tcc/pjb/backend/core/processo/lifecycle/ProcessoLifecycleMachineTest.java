package com.tcc.pjb.backend.core.processo.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ProcessoLifecycleMachineTest {

    private final ProcessoLifecycleMachine machine = new ProcessoLifecycleMachine(new AtoProcessualCatalogService());

    @Test
    void distributesIntoConhecimentoAndDistributedStatus() {
        Processo processo = baseProcesso();
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setFaseAtual((com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual) null);

        ProcessoLifecycleDecision decision = machine.apply(processo, ProcessoLifecycleAction.DISTRIBUIR);

        assertTrue(decision.permitida());
        assertEquals(StatusProcesso.DISTRIBUIDO, processo.getStatusProcesso());
        assertEquals(FaseProcessual.CONHECIMENTO, processo.getFaseAtual());
        assertEquals("Distribuição", decision.atoProcessual().titulo());
    }

    @Test
    void sentenceMovesToSentencaProferida() {
        Processo processo = baseProcesso();
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setFaseAtual(FaseProcessual.INSTRUTORIA);

        ProcessoLifecycleDecision decision = machine.apply(processo, ProcessoLifecycleAction.PROFERIR_SENTENCA);

        assertTrue(decision.permitida());
        assertEquals(StatusProcesso.SENTENCA_PROFERIDA, processo.getStatusProcesso());
        assertEquals(FaseProcessual.CONHECIMENTO, processo.getFaseAtual());
    }

    @Test
    void recursoOpensRecursalPhase() {
        Processo processo = baseProcesso();
        processo.setStatusProcesso(StatusProcesso.SENTENCA_PROFERIDA);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);

        ProcessoLifecycleDecision decision = machine.apply(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);

        assertTrue(decision.permitida());
        assertEquals(StatusProcesso.RECURSO_INTERPOSTO, processo.getStatusProcesso());
        assertEquals(FaseProcessual.RECURSAL, processo.getFaseAtual());
    }

    @Test
    void cumprimentoRequiresDecisionOrTransit() {
        Processo processo = baseProcesso();
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);

        ProcessoLifecycleDecision rejected = machine.preview(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);

        assertFalse(rejected.permitida());
        assertEquals(StatusProcesso.EM_ANDAMENTO, rejected.statusDestino());
    }

    @Test
    void desarquivamentoOnlyFromArchivedStatus() {
        Processo processo = baseProcesso();
        processo.setStatusProcesso(StatusProcesso.ARQUIVADO);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);

        ProcessoLifecycleDecision decision = machine.apply(processo, ProcessoLifecycleAction.DESARQUIVAR);

        assertTrue(decision.permitida());
        assertEquals(StatusProcesso.EM_ANDAMENTO, processo.getStatusProcesso());
        assertEquals(FaseProcessual.CUMPRIMENTO_SENTENCA, processo.getFaseAtual());
    }

    private Processo baseProcesso() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0000001-00.2026.8.06.0001");
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        return processo;
    }
}
