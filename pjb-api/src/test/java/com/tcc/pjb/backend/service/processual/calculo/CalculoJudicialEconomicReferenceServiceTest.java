package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoReferenciaAnual;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CalculoJudicialEconomicReferenceServiceTest {

    private static final ZoneId FUSO_LEGAL = ZoneId.of("America/Sao_Paulo");

    @Test
    void deveExporSalarioMinimoETetoInssOficiais() {
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService(relogioEm("2026-10-09"));

        var response = service.current();

        assertThat(response.salarioMinimoNacional().vigente()).isEqualByComparingTo(new BigDecimal("1621.00"));
        assertThat(response.inss().tetoBeneficio2026()).isEqualByComparingTo(new BigDecimal("8475.55"));
        assertThat(response.fontesOficiais()).containsKeys("salarioMinimo2026", "salarioMinimo2025", "inss2026");
    }

    @Test
    void janelaComparativaRotulaCadaValorComOAnoDeQueEleVeio() {
        SalarioMinimoNacionalService salarioMock = mock(SalarioMinimoNacionalService.class);
        when(salarioMock.referenciaEm(LocalDate.of(2026, 10, 9))).thenReturn(new SalarioMinimoReferenciaAnual(
                2026, new BigDecimal("2222.00"), LocalDate.of(2026, 1, 1), "Norma 2026", "https://fonte/2026"));
        when(salarioMock.referenciaAte(2025)).thenReturn(new SalarioMinimoReferenciaAnual(
                2025, new BigDecimal("1111.00"), LocalDate.of(2025, 1, 1), "Norma 2025", "https://fonte/2025"));
        CalculoJudicialEconomicReferenceService service = new CalculoJudicialEconomicReferenceService(
                salarioMock, new PjbTimeService(relogioEm("2026-10-09"), FUSO_LEGAL));

        var response = service.current();
        var salario = response.salarioMinimoNacional();

        assertThat(salario.vigente()).isEqualByComparingTo(new BigDecimal("2222.00"));
        assertThat(salario.vigenteEm()).isEqualTo("2026-01-01");
        assertThat(salario.anoVigente()).isEqualTo(2026);
        assertThat(salario.anoReferenciaAnterior()).isEqualTo(2025);
        assertThat(salario.referenciaAnterior()).isEqualByComparingTo(new BigDecimal("1111.00"));
        assertThat(salario.normaReferencia()).isEqualTo("Norma 2026");
        assertThat(salario.fonteOficial()).isEqualTo("https://fonte/2026");
        assertThat(response.fontesOficiais())
                .containsEntry("salarioMinimo2026", "https://fonte/2026")
                .containsEntry("salarioMinimo2025", "https://fonte/2025");
    }

    @Test
    void anoSemDecretoCadastradoNaoRotulaOValorDoAnoAnteriorComoSeFosseDoAnoNovo() {
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService(relogioEm("2027-01-15"));

        var salario = service.current().salarioMinimoNacional();

        assertThat(salario.anoVigente()).isEqualTo(2026);
        assertThat(salario.vigente()).isEqualByComparingTo(new BigDecimal("1621.00"));
        assertThat(salario.vigenteEm()).isEqualTo("2026-01-01");
        assertThat(salario.normaReferencia()).isEqualTo("Decreto 12.797/2025");
        assertThat(salario.anoReferenciaAnterior()).isEqualTo(2025);
        assertThat(salario.referenciaAnterior()).isEqualByComparingTo(new BigDecimal("1518.00"));
    }

    @Test
    void registrosNoFormatoDaSeedDoBancoChegamAoPainelComNormaEFonteDoRegistro() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        SalarioMinimoNacional registro2026 = registro(2026, "1621.00", "Decreto 12.797/2025",
                "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/decreto/d12797.htm");
        SalarioMinimoNacional registro2025 = registro(2025, "1518.00", "Decreto 12.342/2024",
                "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2024/decreto/d12342.htm");
        when(repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(any())).thenReturn(Optional.of(registro2026));
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(anyInt())).thenReturn(Optional.empty());
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(2025)).thenReturn(Optional.of(registro2025));
        CalculoJudicialEconomicReferenceService service = new CalculoJudicialEconomicReferenceService(
                new SalarioMinimoNacionalService(repository), new PjbTimeService(relogioEm("2026-10-09"), FUSO_LEGAL));

        var salario = service.current().salarioMinimoNacional();

        assertThat(salario.anoVigente()).isEqualTo(2026);
        assertThat(salario.normaReferencia()).isEqualTo("Decreto 12.797/2025");
        assertThat(salario.fonteOficial()).isEqualTo("https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/decreto/d12797.htm");
        assertThat(salario.anoReferenciaAnterior()).isEqualTo(2025);
        assertThat(salario.referenciaAnterior()).isEqualByComparingTo(new BigDecimal("1518.00"));
    }

    @Test
    void dataDeReferenciaDoPainelSegueOFusoJuridicoNaViradaDoAno() {
        Instant ultimaMeiaHoraDe2026EmBrasilia = LocalDate.of(2026, 12, 31).atTime(23, 30).atZone(FUSO_LEGAL).toInstant();
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService(
                Clock.fixed(ultimaMeiaHoraDe2026EmBrasilia, ZoneOffset.UTC));

        var response = service.current();

        assertThat(response.referenciaTemporal()).isEqualTo("2026-12-31");
    }

    @Test
    void semAnoAnteriorConhecidoAReferenciaAnteriorFicaDeFora() {
        CalculoJudicialEconomicReferenceService service = TestEconomicReferenceSupport.economicReferenceService(relogioEm("2023-06-01"));

        var response = service.current();

        assertThat(response.salarioMinimoNacional().anoVigente()).isEqualTo(2023);
        assertThat(response.salarioMinimoNacional().anoReferenciaAnterior()).isNull();
        assertThat(response.salarioMinimoNacional().referenciaAnterior()).isNull();
        assertThat(response.fontesOficiais()).containsKey("salarioMinimo2023").doesNotContainKey("salarioMinimo2026");
    }

    private static SalarioMinimoNacional registro(int ano, String valor, String norma, String fonte) {
        SalarioMinimoNacional registro = new SalarioMinimoNacional();
        registro.setAnoReferencia(ano);
        registro.setValorMensal(new BigDecimal(valor));
        registro.setVigenteDesde(LocalDate.of(ano, 1, 1));
        registro.setNormaReferencia(norma);
        registro.setFonteOficial(fonte);
        registro.setAtivo(true);
        return registro;
    }

    private static Clock relogioEm(String data) {
        return Clock.fixed(LocalDate.parse(data).atTime(12, 0).atZone(FUSO_LEGAL).toInstant(), ZoneOffset.UTC);
    }
}
