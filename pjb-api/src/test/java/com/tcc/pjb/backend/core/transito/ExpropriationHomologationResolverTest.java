package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExpropriationHomologationResolverTest {

    private final ExpropriationHomologationResolver resolver = new ExpropriationHomologationResolver();

    @Test
    void resolveHomologationForRealEstateAdjudication() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExpropriationHomologationProfile profile = resolver.resolve(processo, "adjudicacao", "imovel", "direta", "Credor exequente", 280000D);

        assertEquals("ADJUDICACAO", profile.actType());
        assertEquals("IMOVEL", profile.assetKind());
        assertTrue(profile.homologationMode().contains("HOMOLOGACAO_ADJUDICACAO_IMOVEL"));
        assertEquals("TRIGGER_LIQUIDACAO_PRODUTO_EXPROPRIACAO", profile.settlementTriggerMode());
    }
}
