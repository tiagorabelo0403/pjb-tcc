package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoFatalCalculatorTest {

    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    private final CalendarioForenseTribunalService calendarioForenseTribunalService = mock(CalendarioForenseTribunalService.class);
    private final PrazoFatalCalculator calculator = new PrazoFatalCalculator(calendarioForenseTribunalService);

    private SecretariaInstitucionalItem item(int prazoBaseDias, boolean prazoEmDobro) {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setPrazoBaseDias(prazoBaseDias);
        item.setPrazoEmDobro(prazoEmDobro);
        return item;
    }

    private CalendarioForenseTribunalService.PrazoCalculado prazoCalculado(LocalDate vencimento) {
        return new CalendarioForenseTribunalService.PrazoCalculado(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21), 15, vencimento,
                "TJCE", "CE", "Fortaleza", List.of(), "resumo", "fundamentacao");
    }

    @Test
    void prazoSimplesUsaPrazoBaseDiasDireto() {
        SecretariaInstitucionalItem item = item(15, false);
        Processo processo = Processo.builder().id(1L).tribunal("TJCE").uf("CE").comarca("Fortaleza").build();
        Instant marco = Instant.parse("2026-08-20T13:00:00Z");
        LocalDate vencimento = LocalDate.of(2026, 9, 10);
        when(calendarioForenseTribunalService.calcularPrazo(any(), eq(15), eq("TJCE"), eq("CE"), eq("Fortaleza")))
                .thenReturn(prazoCalculado(vencimento));

        Instant prazoFatal = calculator.calcular(item, processo, marco);

        assertThat(prazoFatal).isEqualTo(vencimento.atTime(LocalTime.MAX).atZone(FUSO_BR).toInstant());
        verify(calendarioForenseTribunalService).calcularPrazo(
                eq(marco.atZone(FUSO_BR).toLocalDate()), eq(15), eq("TJCE"), eq("CE"), eq("Fortaleza"));
    }

    @Test
    void prazoEmDobroDobraOsDiasUteisPassadosAoCalendario() {
        SecretariaInstitucionalItem item = item(15, true);
        Processo processo = Processo.builder().id(2L).tribunal("TJCE").uf("CE").comarca("Sobral").build();
        Instant marco = Instant.parse("2026-08-20T13:00:00Z");
        LocalDate vencimento = LocalDate.of(2026, 9, 25);
        when(calendarioForenseTribunalService.calcularPrazo(any(), eq(30), eq("TJCE"), eq("CE"), eq("Sobral")))
                .thenReturn(prazoCalculado(vencimento));

        Instant prazoFatal = calculator.calcular(item, processo, marco);

        assertThat(prazoFatal).isEqualTo(vencimento.atTime(LocalTime.MAX).atZone(FUSO_BR).toInstant());
        verify(calendarioForenseTribunalService).calcularPrazo(any(), eq(30), eq("TJCE"), eq("CE"), eq("Sobral"));
    }

    @Test
    void marcoConvertidoParaFusoBrasilAntesDeChamarCalendario() {
        SecretariaInstitucionalItem item = item(5, false);
        Processo processo = Processo.builder().id(3L).tribunal("TJSP").uf("SP").comarca("Sao Paulo").build();
        Instant marcoProximoDaMeiaNoiteUtc = Instant.parse("2026-08-21T02:30:00Z");
        LocalDate vencimento = LocalDate.of(2026, 8, 28);
        when(calendarioForenseTribunalService.calcularPrazo(any(), eq(5), eq("TJSP"), eq("SP"), eq("Sao Paulo")))
                .thenReturn(prazoCalculado(vencimento));

        calculator.calcular(item, processo, marcoProximoDaMeiaNoiteUtc);

        LocalDate esperado = marcoProximoDaMeiaNoiteUtc.atZone(FUSO_BR).toLocalDate();
        verify(calendarioForenseTribunalService).calcularPrazo(eq(esperado), eq(5), eq("TJSP"), eq("SP"), eq("Sao Paulo"));
    }
}
