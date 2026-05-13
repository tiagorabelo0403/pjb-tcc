package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExpropriationAuctionCycleResolverTest {

    private final ExpropriationAuctionCycleResolver resolver = new ExpropriationAuctionCycleResolver();

    @Test
    void resolveSecondRoundElectronicAuction() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExpropriationAuctionCycleProfile profile = resolver.resolve(processo, "hasta publica", "imovel", "eletronica", 2, 320000D);

        assertEquals("HASTA_PUBLICA", profile.actType());
        assertTrue(profile.cycleMode().contains("SEGUNDA_RODADA_ELETRONICA"));
        assertEquals("SEGUNDA_PRACA_COM_REDUCAO_CONTROLADA", profile.pracaMode());
    }
}
