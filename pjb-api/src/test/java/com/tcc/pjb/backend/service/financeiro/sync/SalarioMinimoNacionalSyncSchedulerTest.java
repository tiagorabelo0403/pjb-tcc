package com.tcc.pjb.backend.service.financeiro.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SalarioMinimoNacionalSyncSchedulerTest {

    private static final LocalDate DATA_2026 = LocalDate.of(2026, 1, 1);
    private static final BigDecimal VALOR_2026 = new BigDecimal("1621.00");
    private static final BigDecimal VALOR_ANTIGO = new BigDecimal("1518.00");

    private SalarioMinimoBcbClient bcbClient;
    private SalarioMinimoNacionalService salarioService;
    private SalarioMinimoNacionalSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        bcbClient = mock(SalarioMinimoBcbClient.class);
        salarioService = mock(SalarioMinimoNacionalService.class);
        scheduler = new SalarioMinimoNacionalSyncScheduler(bcbClient, salarioService);
    }

    @Test
    void valorRecebidoDiferenteDoPersistidoDisparaSalvarOuAtualizar() {
        when(bcbClient.buscarUltimoValor())
                .thenReturn(Optional.of(new SalarioMinimoBcbClient.SnapshotSalarioMinimo(DATA_2026, VALOR_2026)));
        when(salarioService.valorPorAno(2026)).thenReturn(VALOR_ANTIGO);
        when(salarioService.salvarOuAtualizar(eq(2026), any(), any(), any()))
                .thenReturn(new SalarioMinimoNacional());

        scheduler.sincronizar();

        ArgumentCaptor<BigDecimal> valorCap = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<String> normaCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> fonteCap = ArgumentCaptor.forClass(String.class);
        verify(salarioService).salvarOuAtualizar(eq(2026), valorCap.capture(), normaCap.capture(), fonteCap.capture());
        assertThat(valorCap.getValue()).isEqualByComparingTo(VALOR_2026);
        assertThat(normaCap.getValue()).contains("Banco Central");
        assertThat(fonteCap.getValue()).contains("bcb.gov.br");
    }

    @Test
    void valorRecebidoIgualAoPersistidoEhNoOp() {
        when(bcbClient.buscarUltimoValor())
                .thenReturn(Optional.of(new SalarioMinimoBcbClient.SnapshotSalarioMinimo(DATA_2026, VALOR_2026)));
        when(salarioService.valorPorAno(2026)).thenReturn(VALOR_2026);

        scheduler.sincronizar();

        verify(salarioService, never()).salvarOuAtualizar(any(Integer.class), any(), any(), any());
    }

    @Test
    void snapshotVazioNaoDisparaSalvarOuAtualizar() {
        when(bcbClient.buscarUltimoValor()).thenReturn(Optional.empty());

        scheduler.sincronizar();

        verify(salarioService, never()).valorPorAno(any(Integer.class));
        verify(salarioService, never()).salvarOuAtualizar(any(Integer.class), any(), any(), any());
    }

    @Test
    void diferencaDeCentavosNoScaleAindaConfiguraIgualdade() {
        BigDecimal valorPersistidoComScaleDiferente = new BigDecimal("1621");
        when(bcbClient.buscarUltimoValor())
                .thenReturn(Optional.of(new SalarioMinimoBcbClient.SnapshotSalarioMinimo(DATA_2026, VALOR_2026)));
        when(salarioService.valorPorAno(2026)).thenReturn(valorPersistidoComScaleDiferente);

        scheduler.sincronizar();

        verify(salarioService, never()).salvarOuAtualizar(any(Integer.class), any(), any(), any());
    }

    @Test
    void falhaInesperadaDoServicoNaoPropagaExcecao() {
        when(bcbClient.buscarUltimoValor())
                .thenReturn(Optional.of(new SalarioMinimoBcbClient.SnapshotSalarioMinimo(DATA_2026, VALOR_2026)));
        when(salarioService.valorPorAno(2026)).thenThrow(new RuntimeException("banco fora"));

        scheduler.sincronizar();

        verify(salarioService, never()).salvarOuAtualizar(any(Integer.class), any(), any(), any());
    }
}
