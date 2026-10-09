package com.tcc.pjb.backend.service.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;
import java.math.BigDecimal;
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

    @Test
    void referenciaDeAnoAindaNaoCadastradoVemDoUltimoRegistroComOAnoENormaDeOrigem() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        SalarioMinimoNacional registro2026 = new SalarioMinimoNacional();
        registro2026.setAnoReferencia(2026);
        registro2026.setValorMensal(new BigDecimal("1621.00"));
        registro2026.setVigenteDesde(LocalDate.of(2026, 1, 1));
        registro2026.setNormaReferencia("Decreto cadastrado");
        registro2026.setFonteOficial("https://fonte.cadastrada");
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(2027))
                .thenReturn(Optional.of(registro2026));
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        SalarioMinimoReferenciaAnual referencia = service.referenciaAte(2027);

        assertThat(referencia.ano()).isEqualTo(2026);
        assertThat(referencia.valor()).isEqualByComparingTo(new BigDecimal("1621.00"));
        assertThat(referencia.vigenteDesde()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(referencia.normaReferencia()).isEqualTo("Decreto cadastrado");
        assertThat(referencia.fonteOficial()).isEqualTo("https://fonte.cadastrada");
        assertThat(service.valorPorAno(2027)).isEqualByComparingTo(new BigDecimal("1621.00"));
    }

    @Test
    void semRegistroAReferenciaVemDaTabelaOficialEmbarcadaComOAnoDeOrigem() {
        SalarioMinimoNacionalRepository repository = mock(SalarioMinimoNacionalRepository.class);
        when(repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(2027))
                .thenReturn(Optional.empty());
        SalarioMinimoNacionalService service = new SalarioMinimoNacionalService(repository);

        SalarioMinimoReferenciaAnual referencia = service.referenciaAte(2027);

        assertThat(referencia.ano()).isEqualTo(2026);
        assertThat(referencia.valor()).isEqualByComparingTo(new BigDecimal("1621.00"));
        assertThat(referencia.normaReferencia()).isEqualTo("Decreto 12.797/2025");
    }
}
