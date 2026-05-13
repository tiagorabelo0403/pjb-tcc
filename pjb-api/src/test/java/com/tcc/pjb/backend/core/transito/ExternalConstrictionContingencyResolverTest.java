package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExternalConstrictionContingencyResolverTest {

    private final ExternalConstrictionContingencyResolver resolver = new ExternalConstrictionContingencyResolver();

    @Test
    void resolveTimeoutFinancialContingency() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExternalConstrictionContingencyProfile profile = resolver.resolve(processo, "dinheiro", "sisbajud", "timeout", null, 280000D);

        assertEquals("SISBAJUD", profile.gatewayCode());
        assertTrue(profile.contingencyMode().contains("CONTINGENCIA_FINANCEIRA"));
        assertEquals("LACUNA_PROBATORIA_CRITICA", profile.proofGapMode());
    }
}
