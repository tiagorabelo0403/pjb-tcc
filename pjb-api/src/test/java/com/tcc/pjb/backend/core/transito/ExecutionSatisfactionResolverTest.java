package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExecutionSatisfactionResolverTest {

    private final ExecutionSatisfactionResolver resolver = new ExecutionSatisfactionResolver();

    @Test
    void resolveIntegralPaymentClosure() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionSatisfactionProfile profile = resolver.resolve(processo, "pagamento integral", 100D, 0D, "quitacao integral");

        assertEquals("SATISFACAO_TOTAL", profile.satisfactionMode());
        assertTrue(profile.terminalDisposition().contains("EXTINCAO") || profile.terminalDisposition().contains("BAIXA"));
        assertEquals("BAIXA_COM_EXTINCAO_E_LIBERACAO_TOTAL", profile.baixaMode());
    }

    @Test
    void resolveSuspensaoSemBens() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.EXECUCAO_FISCAL);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionSatisfactionProfile profile = resolver.resolve(processo, "suspensao sem bens", 0D, 10000D, null);

        assertEquals("SATISFACAO_INSUFICIENTE", profile.satisfactionMode());
        assertEquals("BAIXA_COM_RESERVA_DE_REATIVACAO", profile.baixaMode());
        assertTrue(profile.reopenMode().contains("REABERTURA"));
    }
}
