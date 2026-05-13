package com.tcc.pjb.backend.service.ajuizamento.federal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

class FederalismoRedistribuicaoServiceTest {

    @Test
    void shouldReuseCachedReportForSameThreshold() {
        JurisdicaoRepository jurisdicaoRepository = Mockito.mock(JurisdicaoRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        Jurisdicao origem = jurisdicao(1L, "J1", "1ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        Jurisdicao destino = jurisdicao(2L, "J2", "2ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        when(jurisdicaoRepository.findAll()).thenReturn(List.of(origem, destino));
        when(processoRepository.findAll()).thenReturn(List.of(
                processo(origem, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(120)),
                processo(origem, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(110)),
                processo(destino, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(10))
        ));
        FederalismoRedistribuicaoService service = new FederalismoRedistribuicaoService(jurisdicaoRepository, processoRepository);

        var first = service.sugerir(0.50d);
        var second = service.sugerir(0.50d);

        assertThat(second).isSameAs(first);
        verify(jurisdicaoRepository, times(1)).findAll();
        verify(processoRepository, times(1)).findAll();
    }

    @Test
    void shouldRankCandidatesUsingPrecomputedLoad() {
        JurisdicaoRepository jurisdicaoRepository = Mockito.mock(JurisdicaoRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        Jurisdicao origem = jurisdicao(1L, "J1", "1ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        Jurisdicao melhorDestino = jurisdicao(2L, "J2", "2ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        Jurisdicao piorDestino = jurisdicao(3L, "J3", "3ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        when(jurisdicaoRepository.findAll()).thenReturn(List.of(origem, melhorDestino, piorDestino));
        when(processoRepository.findAll()).thenReturn(List.of(
                processo(origem, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(150)),
                processo(origem, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(140)),
                processo(origem, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(130)),
                processo(melhorDestino, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(2)),
                processo(melhorDestino, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(1)),
                processo(piorDestino, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(91)),
                processo(piorDestino, StatusProcesso.EM_ANDAMENTO, LocalDateTime.now().minusDays(2))
        ));
        FederalismoRedistribuicaoService service = new FederalismoRedistribuicaoService(jurisdicaoRepository, processoRepository);

        var report = service.sugerir(0.50d);

        assertThat(report.varasCriticas()).hasSize(1);
        assertThat(report.varasCriticas().getFirst().candidatasRedistribuicao())
                .extracting(FederalismoRedistribuicaoService.CandidataRedistribuicao::jurisdicaoId)
                .containsExactly(2L, 3L);
    }

    private static Jurisdicao jurisdicao(Long id,
                                         String sigla,
                                         String nome,
                                         String uf,
                                         String comarca,
                                         MateriaJurisdicao materia,
                                         boolean ativa) {
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setId(id);
        jurisdicao.setSigla(sigla);
        jurisdicao.setNome(nome);
        jurisdicao.setUf(uf);
        jurisdicao.setComarca(comarca);
        jurisdicao.setMateria(materia);
        jurisdicao.setAtivo(ativa);
        return jurisdicao;
    }

    private static Processo processo(Jurisdicao jurisdicao,
                                     StatusProcesso status,
                                     LocalDateTime ultimaMovimentacao) {
        Processo processo = new Processo();
        processo.setJurisdicao(jurisdicao);
        processo.setStatusProcesso(status);
        processo.setDataUltimaMovimentacao(ultimaMovimentacao);
        return processo;
    }
}
