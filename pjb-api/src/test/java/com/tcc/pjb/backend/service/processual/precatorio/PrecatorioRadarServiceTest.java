package com.tcc.pjb.backend.service.processual.precatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrecatorioRadarServiceTest {

    private static final LocalDate TRANSITO = LocalDate.of(2026, 3, 10);
    private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1518.00");

    private SalarioMinimoNacionalService salarioMinimoService;
    private PrecatorioRadarService service;

    @BeforeEach
    void setUp() {
        salarioMinimoService = mock(SalarioMinimoNacionalService.class);
        when(salarioMinimoService.multiplicar(any(), any()))
                .thenAnswer(inv -> ((BigDecimal) inv.getArgument(0)).multiply(SALARIO_MINIMO));
        service = new PrecatorioRadarService(salarioMinimoService);
    }

    private PrecatorioRadarService.PrecatorioInput entrada(BigDecimal valor,
                                                          PrecatorioRadarService.TipoObrigacaoFazenda fazenda,
                                                          boolean alimentar) {
        return new PrecatorioRadarService.PrecatorioInput(
                UUID.randomUUID(), valor, fazenda, true, alimentar, TRANSITO);
    }

    @Test
    void limiteFederalUsaSessentaSalariosMinimosDaDataDoTransito() {
        service.avaliar(entrada(new BigDecimal("1000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, false));

        org.mockito.Mockito.verify(salarioMinimoService)
                .multiplicar(eq(new BigDecimal("60")), eq(TRANSITO));
    }

    @Test
    void limiteEstadualUsaQuarentaSalariosMinimos() {
        service.avaliar(entrada(new BigDecimal("1000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.ESTADUAL, false));

        org.mockito.Mockito.verify(salarioMinimoService)
                .multiplicar(eq(new BigDecimal("40")), eq(TRANSITO));
    }

    @Test
    void valorAbaixoDoLimiteFederalEhRpv() {
        var snapshot = service.avaliar(entrada(new BigDecimal("50000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, false));

        assertThat(snapshot.aptaRpv()).isTrue();
        assertThat(snapshot.requerPrecatorio()).isFalse();
        assertThat(snapshot.tipoExpedicao()).isEqualTo("RPV");
        assertThat(snapshot.valorLimiteRpv()).isEqualByComparingTo(new BigDecimal("91080.00"));
    }

    @Test
    void valorNoLimiteExatoAindaEhRpv() {
        var snapshot = service.avaliar(entrada(new BigDecimal("91080.00"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, false));

        assertThat(snapshot.aptaRpv()).isTrue();
    }

    @Test
    void valorAcimaDoLimiteExigePrecatorio() {
        var snapshot = service.avaliar(entrada(new BigDecimal("91080.01"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, false));

        assertThat(snapshot.aptaRpv()).isFalse();
        assertThat(snapshot.requerPrecatorio()).isTrue();
        assertThat(snapshot.tipoExpedicao()).isEqualTo("PRECATORIO");
    }

    @Test
    void naturezaAlimentarNaoTransformaValorAcimaDoLimiteEmRpv() {
        var snapshot = service.avaliar(entrada(new BigDecimal("5000000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, true));

        assertThat(snapshot.aptaRpv())
                .as("natureza alimentar da preferencia na ordem de pagamento, nao dispensa precatorio pelo valor")
                .isFalse();
        assertThat(snapshot.tipoExpedicao()).isEqualTo("PRECATORIO");
    }

    @Test
    void naturezaAlimentarAcimaDoLimiteRegistraPreferenciaNasProvidencias() {
        var snapshot = service.avaliar(entrada(new BigDecimal("5000000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, true));

        assertThat(snapshot.providencias())
                .anyMatch(p -> p.contains("natureza alimentar") && p.contains("art. 100"));
    }

    @Test
    void semTransitoEmJulgadoNaoClassificaEExigeAguardar() {
        var input = new PrecatorioRadarService.PrecatorioInput(
                UUID.randomUUID(), new BigDecimal("1000"),
                PrecatorioRadarService.TipoObrigacaoFazenda.FEDERAL, false, true, null);

        var snapshot = service.avaliar(input);

        assertThat(snapshot.tipoExpedicao()).isEqualTo("AGUARDANDO_TRANSITO");
        assertThat(snapshot.aptaRpv()).isFalse();
        assertThat(snapshot.requerPrecatorio()).isFalse();
        assertThat(snapshot.valorLimiteRpv()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
