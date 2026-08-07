package com.tcc.pjb.backend.service.transito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.transito.ArquivamentoPainelResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class ArquivamentoPainelServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final ArquivamentoPainelService service = new ArquivamentoPainelService(processoRepository);

    private Processo processo(Long id, String numero) {
        Processo p = new Processo();
        p.setId(id);
        p.setNumeroProcesso(numero);
        p.setClasseProcessual("Procedimento Comum Cível");
        p.setDataUltimaMovimentacao(LocalDateTime.now().minusDays(3));
        return p;
    }

    @Test
    void listaCandidatosAArquivamentoComTransitoEmJulgadoNaVara() {
        Page<Processo> pagina = new PageImpl<>(List.of(processo(1L, "PROC-1"), processo(2L, "PROC-2")));
        when(processoRepository.findByVaraAndStatusProcesso(eq("1ª Vara Cível"), eq(StatusProcesso.TRANSITO_EM_JULGADO), any()))
                .thenReturn(pagina);

        ArquivamentoPainelResponse resultado = service.candidatosPorVara("1ª Vara Cível");

        assertThat(resultado.vara()).isEqualTo("1ª Vara Cível");
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.candidatos()).extracting("numeroProcesso").containsExactly("PROC-1", "PROC-2");
    }

    @Test
    void retornaPainelVazioQuandoVaraNaoTemCandidatos() {
        when(processoRepository.findByVaraAndStatusProcesso(eq("Vara Vazia"), eq(StatusProcesso.TRANSITO_EM_JULGADO), any()))
                .thenReturn(new PageImpl<>(List.of()));

        ArquivamentoPainelResponse resultado = service.candidatosPorVara("Vara Vazia");

        assertThat(resultado.total()).isZero();
        assertThat(resultado.candidatos()).isEmpty();
    }
}
