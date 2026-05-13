package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExecutionClosureGovernanceResolverTest {

    private final ExecutionClosureGovernanceResolver resolver = new ExecutionClosureGovernanceResolver();

    @Test
    void resolveResidualClosureWithArchiveRestriction() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionClosureGovernanceProfile profile = resolver.resolve(
                processo,
                "fechamento parcial",
                "trabalhista",
                "sim",
                65D,
                1200D,
                "saldo ainda ativo");

        assertEquals("FECHAMENTO_EXECUTIVO_PARCIAL_COM_SALDO_REMANESCENTE", profile.closureMode());
        assertEquals("ARQUIVO_PARCIAL_COM_RESTRICAO_DE_SALDO", profile.archiveReadiness());
        assertTrue(profile.preferenceClosureMode().contains("TRABALHISTA"));
        assertTrue(profile.subrogationClosureMode().contains("SUBROGACAO_FINAL_CONFIRMADA"));
    }
}
