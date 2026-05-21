package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acaoconstitucional.MandadoInjuncaoReadinessService.EfeitoMandadoInjuncao;
import com.tcc.pjb.backend.service.acaoconstitucional.MandadoInjuncaoReadinessService.MandadoInjuncaoInput;
import com.tcc.pjb.backend.service.acaoconstitucional.MandadoInjuncaoReadinessService.MandadoInjuncaoResult;
import org.junit.jupiter.api.Test;

class MandadoInjuncaoReadinessServiceTest {

    private final MandadoInjuncaoReadinessService service = new MandadoInjuncaoReadinessService();

    @Test
    void deveSugerirEfeitoErgaOmnesQuandoInteresseForColetivo() {
        MandadoInjuncaoInput input = new MandadoInjuncaoInput(
                "P-001", true, true, true, true);

        MandadoInjuncaoResult result = service.avaliar(input);

        assertThat(result.efeitoSugerido()).isEqualTo(EfeitoMandadoInjuncao.ERGA_OMNES);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("Sem pendências");
    }

    @Test
    void deveSugerirEfeitoInterPartesQuandoInteresseForIndividual() {
        MandadoInjuncaoInput input = new MandadoInjuncaoInput(
                "P-002", true, true, false, true);

        MandadoInjuncaoResult result = service.avaliar(input);

        assertThat(result.efeitoSugerido()).isEqualTo(EfeitoMandadoInjuncao.INTER_PARTES);
    }

    @Test
    void deveAdicionarPendenciaQuandoOmissaoNormativaNaoConfigurada() {
        MandadoInjuncaoInput input = new MandadoInjuncaoInput(
                "P-003", true, false, false, true);

        MandadoInjuncaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("omissão normativa"));
    }

    @Test
    void deveAdicionarPendenciaQuandoNormaConstitucionalNaoDependerDeRegulamentacao() {
        MandadoInjuncaoInput input = new MandadoInjuncaoInput(
                "P-004", false, true, false, true);

        MandadoInjuncaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("norma constitucional"));
    }
}
