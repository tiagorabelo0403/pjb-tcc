package com.tcc.pjb.backend.service.ajuizamento.federal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
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
        // origem: 2 ativos com 120d e 110d → totalAtivos=2, atrasoEstrutural=2
        // destino: 1 ativo com 10d → totalAtivos=1, atrasoEstrutural=0
        when(processoRepository.computeLoadByJurisdicao(any(LocalDateTime.class))).thenReturn(List.of(
                projection(1L, 2L, 2L),
                projection(2L, 1L, 0L)
        ));
        FederalismoRedistribuicaoService service = new FederalismoRedistribuicaoService(jurisdicaoRepository, processoRepository);

        var first = service.sugerir(0.50d);
        var second = service.sugerir(0.50d);

        assertThat(second).isSameAs(first);
        verify(jurisdicaoRepository, times(1)).findAll();
        verify(processoRepository, times(1)).computeLoadByJurisdicao(any(LocalDateTime.class));
    }

    @Test
    void shouldRankCandidatesUsingPrecomputedLoad() {
        JurisdicaoRepository jurisdicaoRepository = Mockito.mock(JurisdicaoRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        Jurisdicao origem = jurisdicao(1L, "J1", "1ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        Jurisdicao melhorDestino = jurisdicao(2L, "J2", "2ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        Jurisdicao piorDestino = jurisdicao(3L, "J3", "3ª Vara", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);
        when(jurisdicaoRepository.findAll()).thenReturn(List.of(origem, melhorDestino, piorDestino));
        // origem: 3 ativos, todos > 90d → totalAtivos=3, atrasoEstrutural=3, indice=1.0
        // melhorDestino: 2 ativos, nenhum atraso → totalAtivos=2, atrasoEstrutural=0, indice=0.0
        // piorDestino: 2 ativos, 1 com 91d → totalAtivos=2, atrasoEstrutural=1, indice=0.5
        when(processoRepository.computeLoadByJurisdicao(any(LocalDateTime.class))).thenReturn(List.of(
                projection(1L, 3L, 3L),
                projection(2L, 2L, 0L),
                projection(3L, 2L, 1L)
        ));
        FederalismoRedistribuicaoService service = new FederalismoRedistribuicaoService(jurisdicaoRepository, processoRepository);

        var report = service.sugerir(0.50d);

        assertThat(report.varasCriticas()).hasSize(1);
        assertThat(report.varasCriticas().getFirst().candidatasRedistribuicao())
                .extracting(FederalismoRedistribuicaoService.CandidataRedistribuicao::jurisdicaoId)
                .containsExactly(2L, 3L);
    }

    @Test
    void devePreservarSemanticaDaAgregacaoJavaNosValoresDaProjection() {
        JurisdicaoRepository jurisdicaoRepository = Mockito.mock(JurisdicaoRepository.class);
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);

        // j1: projection(totalAtivos=2, atrasoEstrutural=3)
        //   Replica comportamento Java original: atrasoEstrutural não filtra status — qualquer
        //   processo com dataUltimaMovimentacao < threshold conta, inclusive ARQUIVADO.
        //   Este teste prova PRESERVAÇÃO da lógica anterior, não corretude da métrica.
        //   Dívida D15: avaliar se ARQUIVADO deveria ser excluído do atrasoEstrutural.
        //   indice = 3/2 = 1.5 → crítico (> 0.50)
        Jurisdicao j1 = jurisdicao(1L, "J1", "Vara Civil", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);

        // j2: 1 EM_ANDAMENTO com dataUltimaMovimentacao nula
        //   totalAtivos=1, atrasoEstrutural=0 (nulo não conta como atraso)
        //   indice = 0.0 → não crítico
        Jurisdicao j2 = jurisdicao(2L, "J2", "Vara Cível", "CE", "Fortaleza", MateriaJurisdicao.CIVIL, true);

        // j3: apenas ARQUIVADOS com 120 dias
        //   totalAtivos=0 → indice=0.0 (fórmula: totalAtivos==0 ? 0.0 : atraso/ativos)
        //   independente do atrasoEstrutural, não entra em varasCriticas
        Jurisdicao j3 = jurisdicao(3L, "J3", "Vara Penal", "CE", "Fortaleza", MateriaJurisdicao.PENAL, true);

        when(jurisdicaoRepository.findAll()).thenReturn(List.of(j1, j2, j3));
        when(processoRepository.computeLoadByJurisdicao(any(LocalDateTime.class))).thenReturn(List.of(
                projection(1L, 2L, 3L),
                projection(2L, 1L, 0L),
                projection(3L, 0L, 2L)
        ));

        FederalismoRedistribuicaoService service = new FederalismoRedistribuicaoService(jurisdicaoRepository, processoRepository);
        var report = service.sugerir(0.50d);

        assertThat(report.varasCriticas()).hasSize(1);
        FederalismoRedistribuicaoService.VaraAnalise vara = report.varasCriticas().getFirst();
        assertThat(vara.jurisdicaoId()).isEqualTo(1L);
        assertThat(vara.totalAtivos()).isEqualTo(2L);
        assertThat(vara.atrasoEstrutural()).isEqualTo(3L);
        assertThat(vara.indiceSobrecarga()).isEqualTo(1.5d);
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

    private static ProcessoRepository.JurisdicaoLoadProjection projection(long jurisdicaoId,
                                                                          long totalAtivos,
                                                                          long atrasoEstrutural) {
        return new ProcessoRepository.JurisdicaoLoadProjection() {
            @Override public Long getJurisdicaoId() { return jurisdicaoId; }
            @Override public Long getTotalAtivos() { return totalAtivos; }
            @Override public Long getAtrasoEstrutural() { return atrasoEstrutural; }
        };
    }
}
