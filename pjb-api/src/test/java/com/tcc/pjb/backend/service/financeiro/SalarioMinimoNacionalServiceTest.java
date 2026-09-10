package com.tcc.pjb.backend.service.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SalarioMinimoNacionalServiceTest {

    private final int anoAtual = LocalDate.now().getYear();

    @Test
    void anoMaisRecenteConhecidoSemPersistenciaRetornaMaximoDoFallback() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(anoAtual))
                .thenReturn(Optional.empty());
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        assertThat(service.anoMaisRecenteConhecido())
                .isEqualTo(Collections.max(SalarioMinimoNacionalService.FALLBACK_OFICIAL.keySet()));
    }

    @Test
    void anoMaisRecenteConhecidoComPersistenciaMaisAntigaQueOFallbackReportaAnoRealDaPersistencia() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        SalarioMinimoNacional registroAntigo = new SalarioMinimoNacional();
        registroAntigo.setAnoReferencia(2023);
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(anoAtual))
                .thenReturn(Optional.of(registroAntigo));
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        assertThat(service.anoMaisRecenteConhecido()).isEqualTo(2023);
    }

    @Test
    void anoMaisRecenteConhecidoComPersistenciaNoAnoAtualReportaOAnoAtual() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        SalarioMinimoNacional registroAtual = new SalarioMinimoNacional();
        registroAtual.setAnoReferencia(anoAtual);
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(anoAtual))
                .thenReturn(Optional.of(registroAtual));
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        assertThat(service.anoMaisRecenteConhecido()).isEqualTo(anoAtual);
    }

    @Test
    void valorPorAnoFuturoDevolveOMaiorAnoConhecidoDeFormaDeterministica() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        int maiorAnoConhecido = Collections.max(SalarioMinimoNacionalService.FALLBACK_OFICIAL.keySet());
        java.math.BigDecimal esperado = SalarioMinimoNacionalService.FALLBACK_OFICIAL.get(maiorAnoConhecido);

        assertThat(service.valorPorAno(maiorAnoConhecido + 4))
                .as("o fallback iterava Map.copyOf, cuja ordem e indefinida; precisa ser o maior ano, nao o ultimo iterado")
                .isEqualByComparingTo(esperado);
    }
}
