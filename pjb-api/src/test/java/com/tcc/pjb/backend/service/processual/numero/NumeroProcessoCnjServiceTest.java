package com.tcc.pjb.backend.service.processual.numero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.domain.valueobject.NumeroProcesso;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;

class NumeroProcessoCnjServiceTest {

    @Test
    void deveGerarNumeroCnjUnicoConsultandoRepositorio() {
        ProcessoRepository repository = mock(ProcessoRepository.class);
        NumeroProcessoCnjService service = new NumeroProcessoCnjService(repository);

        String primeiro = service.gerarParaAjuizamento(processo());
        String segundo = service.gerarParaAjuizamento(processo());

        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(NumeroProcesso.validar(primeiro)).isTrue();
        assertThat(NumeroProcesso.validar(segundo)).isTrue();
        verify(repository, atLeast(2)).existsByNumeroUnificado(anyString());
        verify(repository, atLeast(2)).existsByNumeroProcesso(anyString());
    }

    @Test
    void devePularNumeroExistenteNoRepositorio() {
        ProcessoRepository repository = mock(ProcessoRepository.class);
        when(repository.existsByNumeroUnificado(anyString())).thenReturn(true, false);
        NumeroProcessoCnjService service = new NumeroProcessoCnjService(repository);

        String numero = service.gerarParaAjuizamento(processo());

        assertThat(NumeroProcesso.validar(numero)).isTrue();
        verify(repository, atLeast(2)).existsByNumeroUnificado(anyString());
    }

    private Processo processo() {
        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        return processo;
    }
}
