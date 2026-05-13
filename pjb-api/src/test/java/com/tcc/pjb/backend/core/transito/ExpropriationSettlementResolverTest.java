package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExpropriationSettlementResolverTest {

    private final ExpropriationSettlementResolver resolver = new ExpropriationSettlementResolver();

    @Test
    void resolvePartialSettlementWithResidualBalance() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExpropriationSettlementProfile profile = resolver.resolve(processo, "veiculo", "deposito judicial", "trabalhista", "nao", 65000D, 35000D, 100000D);

        assertEquals("VEICULO", profile.assetKind());
        assertEquals("LIQUIDACAO_PARCIAL_COM_SALDO_REMANESCENTE", profile.settlementMode());
        assertTrue(profile.preferenceMode().contains("TRABALHISTA"));
        assertEquals("BAIXA_PARCIAL_COM_SALDO", profile.terminalDispositionHint());
    }
}
