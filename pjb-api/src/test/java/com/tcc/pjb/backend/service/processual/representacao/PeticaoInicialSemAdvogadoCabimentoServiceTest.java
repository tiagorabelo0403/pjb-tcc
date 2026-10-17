package com.tcc.pjb.backend.service.processual.representacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PeticaoInicialSemAdvogadoCabimentoServiceTest {

    private static final BigDecimal VINTE = new BigDecimal("20");
    private static final BigDecimal LIMITE = new BigDecimal("30360.00");

    private final SalarioMinimoNacionalService salarioMinimo = mock(SalarioMinimoNacionalService.class);

    private PeticaoInicialSemAdvogadoCabimentoService servicoEm(String instanteUtc) {
        Clock clock = Clock.fixed(Instant.parse(instanteUtc), ZoneOffset.UTC);
        when(salarioMinimo.multiplicar(eq(VINTE), any(LocalDate.class))).thenReturn(LIMITE);
        return new PeticaoInicialSemAdvogadoCabimentoService(salarioMinimo, new PjbTimeService(clock, ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void juizadoEspecialCivelAteVinteSalariosMinimosEAdmitido() {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-10-17T15:00:00Z");

        assertThatCode(() -> servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("3000.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void valorIgualAVinteSalariosMinimosAindaEAdmitido() {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-10-17T15:00:00Z");

        assertThatCode(() -> servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, LIMITE)).doesNotThrowAnyException();
    }

    @Test
    void valorAcimaDeVinteSalariosMinimosExigeAdvogado() {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-10-17T15:00:00Z");

        assertThatThrownBy(() -> servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("30360.01")))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("20 salários mínimos")
                .hasMessageContaining("art. 9º");
    }

    @Test
    void semValorDaCausaNaoHaComoConferirOLimite() {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-10-17T15:00:00Z");

        assertThatThrownBy(() -> servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("valor da causa");
        assertThatThrownBy(() -> servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, BigDecimal.ZERO))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @ParameterizedTest
    @EnumSource(value = RitoProcessual.class, names = {
            "COMUM_ORDINARIO", "JUIZADO_ESPECIAL_FEDERAL", "PREVIDENCIARIO_JEF", "TRABALHISTA_ORDINARIO", "TRABALHISTA_SUMARISSIMO"
    })
    void foraDoJuizadoEspecialCivelOCidadaoAindaNaoPeticionaSemAdvogado(RitoProcessual rito) {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-10-17T15:00:00Z");

        assertThat(servico.admiteRito(rito)).isFalse();
        assertThatThrownBy(() -> servico.exigirCabimento(rito, new BigDecimal("1000.00")))
                .isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void salarioMinimoDeReferenciaEODaDataNoFusoLegal() {
        PeticaoInicialSemAdvogadoCabimentoService servico = servicoEm("2026-01-01T02:00:00Z");

        servico.exigirCabimento(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, new BigDecimal("1000.00"));

        verify(salarioMinimo).multiplicar(VINTE, LocalDate.of(2025, 12, 31));
    }
}
