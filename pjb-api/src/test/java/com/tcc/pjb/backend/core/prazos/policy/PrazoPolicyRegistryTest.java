package com.tcc.pjb.backend.core.prazos.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class PrazoPolicyRegistryTest {

    private final PrazoPolicyRegistry registry = new PrazoPolicyRegistry();

    @Test
    void deveResolverPrazoEmDobroParaDefensoria() {
        assertThat(registry.resolveByPartyProfile(RamoDireito.CIVIL, RitoProcessual.PROCEDIMENTO_COMUM, true, false, false))
                .isEqualTo(PrazoRegime.DOBRO_UTEIS);
    }

    @Test
    void deveResolverEcaETrabalhista() {
        assertThat(registry.resolveByPartyProfile(RamoDireito.INFANCIA_JUVENTUDE, RitoProcessual.PROCEDIMENTO_COMUM, false, false, false))
                .isEqualTo(PrazoRegime.ECA);
        assertThat(registry.resolveByPartyProfile(RamoDireito.TRABALHISTA, RitoProcessual.PROCEDIMENTO_COMUM, false, false, false))
                .isEqualTo(PrazoRegime.CLT_HORAS_UTEIS);
    }
}
