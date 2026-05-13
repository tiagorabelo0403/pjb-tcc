package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import org.junit.jupiter.api.Test;

class ExternalConstrictionResolverTest {

    private final ExternalConstrictionResolver resolver = new ExternalConstrictionResolver();

    @Test
    void resolveSisbajudIntegration() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExternalConstrictionProfile profile = resolver.resolve(processo, "penhora", "dinheiro", "sisbajud", 15000D);

        assertEquals("SISBAJUD", profile.gatewayCode());
        assertEquals("ACCEPTED", profile.statusTarget());
        assertTrue(profile.blocking());
    }

    @Test
    void resolveOficioEletronicoForFaturamento() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.EXECUCAO_FISCAL);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExternalConstrictionProfile profile = resolver.resolve(processo, "penhora", "faturamento", null, 300000D);

        assertEquals("OFICIO_ELETRONICO", profile.gatewayCode());
        assertEquals("PENDING", profile.statusTarget());
        assertTrue(profile.reconciliationMode().contains("RECONCILIACAO"));
    }
}
