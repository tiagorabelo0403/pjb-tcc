package com.tcc.pjb.backend.service.financeiro.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SalarioMinimoStalenessWatchdogServiceTest {

    private final int anoAtual = LocalDate.now().getYear();

    @Test
    void defasagemAbaixoDoLimiarNaoDisparaAlerta() {
        SalarioMinimoNacionalService salarioService = mock(SalarioMinimoNacionalService.class);
        when(salarioService.anoMaisRecenteConhecido()).thenReturn(anoAtual);
        SalarioMinimoStalenessWatchdogService watchdog = new SalarioMinimoStalenessWatchdogService(salarioService, 1);

        boolean disparou = watchdog.verificarDefasagem();

        assertThat(disparou).isFalse();
        verify(salarioService, times(1)).anoMaisRecenteConhecido();
    }

    @Test
    void defasagemExatamenteNoLimiarNaoDisparaAlerta() {
        SalarioMinimoNacionalService salarioService = mock(SalarioMinimoNacionalService.class);
        when(salarioService.anoMaisRecenteConhecido()).thenReturn(anoAtual - 1);
        SalarioMinimoStalenessWatchdogService watchdog = new SalarioMinimoStalenessWatchdogService(salarioService, 1);

        boolean disparou = watchdog.verificarDefasagem();

        assertThat(disparou).isFalse();
    }

    @Test
    void cenarioRealDeFallbackDefasadoAlemDoLimiarDisparaAlerta() {
        SalarioMinimoNacionalService salarioService = mock(SalarioMinimoNacionalService.class);
        when(salarioService.anoMaisRecenteConhecido()).thenReturn(anoAtual - 3);
        SalarioMinimoStalenessWatchdogService watchdog = new SalarioMinimoStalenessWatchdogService(salarioService, 1);

        boolean disparou = watchdog.verificarDefasagem();

        assertThat(disparou).isTrue();
    }
}
