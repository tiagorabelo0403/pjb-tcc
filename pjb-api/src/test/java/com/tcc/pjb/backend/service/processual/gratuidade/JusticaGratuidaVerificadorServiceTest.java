package com.tcc.pjb.backend.service.processual.gratuidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JusticaGratuidaVerificadorServiceTest {

    private static final UUID PROCESSO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PARTE_ID = "parte-001";
    private static final BigDecimal SALARIO_MINIMO_2026 = new BigDecimal("1621.00");

    private SalarioMinimoNacionalService salarioMinimoNacionalService;
    private JusticaGratuidaVerificadorService service;

    @BeforeEach
    void setUp() {
        salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoNacionalService.valorVigente()).thenReturn(SALARIO_MINIMO_2026);
        service = new JusticaGratuidaVerificadorService(salarioMinimoNacionalService);
    }

    @Test
    void representadoPorDefensoriaEhAptoSemConsultarSalarioMinimo() {
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, false, null, true, false, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isTrue();
        assertThat(resultado.orientacaoServidor()).contains("Isentar de custas");
        verifyNoInteractions(salarioMinimoNacionalService);
    }

    @Test
    void beneficioSocialEhAptoSemConsultarSalarioMinimo() {
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, false, null, false, true, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isTrue();
        verifyNoInteractions(salarioMinimoNacionalService);
    }

    @Test
    void declaracaoDeHipossuficienciaComRendaNoTetoUsaSalarioVigente() {
        BigDecimal rendaNoTeto = SALARIO_MINIMO_2026.multiply(new BigDecimal("5"));
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, true, rendaNoTeto, false, false, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isTrue();
        assertThat(resultado.alertas()).isEmpty();
        verify(salarioMinimoNacionalService).valorVigente();
    }

    @Test
    void declaracaoComRendaUmCentavoAcimaDoTetoNaoPresumeGratuidade() {
        BigDecimal umCentavoAcima = SALARIO_MINIMO_2026.multiply(new BigDecimal("5")).add(new BigDecimal("0.01"));
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, true, umCentavoAcima, false, false, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isFalse();
        assertThat(resultado.alertas()).anyMatch(a -> a.contains("5 salários mínimos"));
    }

    @Test
    void declaracaoComRendaNulaNaoPresumeGratuidade() {
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, true, null, false, false, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isFalse();
        assertThat(resultado.alertas()).anyMatch(a -> a.contains("5 salários mínimos"));
    }

    @Test
    void impugnacaoPelaParteContrariaMantemAlertaEBloqueiaOrientacaoDeIsencao() {
        BigDecimal rendaBaixa = new BigDecimal("500.00");
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, true, rendaBaixa, false, false, true);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isTrue();
        assertThat(resultado.alertas()).anyMatch(a -> a.contains("Gratuidade impugnada"));
        assertThat(resultado.orientacaoServidor()).contains("Aguardar pronunciamento judicial");
    }

    @Test
    void semDeclaracaoBeneficioOuDefensoriaNaoPresumeGratuidade() {
        JusticaGratuidaVerificadorService.GratuidadeInput input = new JusticaGratuidaVerificadorService.GratuidadeInput(
                PROCESSO_ID, PARTE_ID, false, null, false, false, false);

        JusticaGratuidaVerificadorService.GratuidadeSnapshot resultado = service.avaliar(input);

        assertThat(resultado.presumivelmenteApta()).isFalse();
        verifyNoInteractions(salarioMinimoNacionalService);
    }
}
