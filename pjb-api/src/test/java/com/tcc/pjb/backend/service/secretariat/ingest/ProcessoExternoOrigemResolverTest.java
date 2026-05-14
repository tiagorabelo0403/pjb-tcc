package com.tcc.pjb.backend.service.secretariat.ingest;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoExternoOrigemResolverTest {

    private final ProcessoExternoOrigemResolver resolver = new ProcessoExternoOrigemResolver();

    @Test
    void resolver_identificaPJE_quandoSistemaDeclarado() {
        assertThat(resolver.resolver("PJE", null)).isEqualTo(SistemaProcessualOrigem.PJE);
    }

    @Test
    void resolver_identificaESAJ_quandoTJSP() {
        assertThat(resolver.resolver("TJSP", null)).isEqualTo(SistemaProcessualOrigem.ESAJ);
    }

    @Test
    void resolver_identificaMNI_peloFormato() {
        assertThat(resolver.resolver(null, "MNI-envelope")).isEqualTo(SistemaProcessualOrigem.MNI);
    }

    @Test
    void resolver_retornaOUTRO_quandoNaoReconhecido() {
        assertThat(resolver.resolver("SISTEMA_DESCONHECIDO", null))
                .isEqualTo(SistemaProcessualOrigem.OUTRO);
    }

    @Test
    void resolver_retornaOUTRO_quandoAmbosNulos() {
        assertThat(resolver.resolver(null, null)).isEqualTo(SistemaProcessualOrigem.OUTRO);
    }

    @Test
    void resolver_identificaEPROC_quandoTRF4() {
        assertThat(resolver.resolver("TRF4", null)).isEqualTo(SistemaProcessualOrigem.EPROC);
    }
}
