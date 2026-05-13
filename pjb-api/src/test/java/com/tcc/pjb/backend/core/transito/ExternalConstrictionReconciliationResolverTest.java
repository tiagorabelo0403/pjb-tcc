package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExternalConstrictionReconciliationResolverTest {

    private final ExternalConstrictionReconciliationResolver resolver = new ExternalConstrictionReconciliationResolver();

    @Test
    void resolvePartialSuccessForSisbajud() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExternalConstrictionReconciliationProfile profile = resolver.resolve(processo, "dinheiro", "sisbajud", "partial_success", "PROTO-1", 50000D);

        assertEquals("SISBAJUD", profile.gatewayCode());
        assertEquals("PARTIAL_SUCCESS", profile.externalStatus());
        assertTrue(profile.reconciliationStatus().contains("PARCIAL"));
    }
}
