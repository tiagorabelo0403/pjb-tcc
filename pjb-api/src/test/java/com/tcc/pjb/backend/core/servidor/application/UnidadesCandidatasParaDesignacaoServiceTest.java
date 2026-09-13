package com.tcc.pjb.backend.core.servidor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnidadesCandidatasParaDesignacaoServiceTest {

    private final UnidadeJudiciariaCompetenciaRepository repositorio =
            mock(UnidadeJudiciariaCompetenciaRepository.class);

    private final UnidadesCandidatasParaDesignacaoService service =
            new UnidadesCandidatasParaDesignacaoService(repositorio);

    private static UnidadeJudiciariaCompetencia unidade(Long id, String codigo, String comarca, String uf) {
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getId()).thenReturn(id);
        when(unidade.getCodigo()).thenReturn(codigo);
        when(unidade.getComarca()).thenReturn(comarca);
        when(unidade.getUf()).thenReturn(uf);
        return unidade;
    }

    @Test
    void consultaPelaComarcaEUfInformadasEDevolveOContratoDeResposta() {
        // Os mocks das unidades sao montados ANTES do when() externo: stub dentro de thenReturn deixa o
        // Mockito com um when() em aberto e estoura UnfinishedStubbing.
        var primeira = unidade(5L, "VARA-1", "Fortaleza", "CE");
        var segunda = unidade(6L, "VARA-2", "Fortaleza", "CE");
        when(repositorio.findAllByUfIgnoreCaseAndComarcaIgnoreCase("CE", "Fortaleza"))
                .thenReturn(List.of(primeira, segunda));

        var candidatas = service.naComarca("CE", "Fortaleza");

        assertThat(candidatas)
                .extracting(c -> c.id() + ":" + c.codigo() + ":" + c.comarca() + ":" + c.uf())
                .containsExactly("5:VARA-1:Fortaleza:CE", "6:VARA-2:Fortaleza:CE");
        verify(repositorio).findAllByUfIgnoreCaseAndComarcaIgnoreCase("CE", "Fortaleza");
    }

    @Test
    void comarcaSemUnidadeDevolveListaVaziaEmVezDeNulo() {
        when(repositorio.findAllByUfIgnoreCaseAndComarcaIgnoreCase("CE", "Comarca Inexistente"))
                .thenReturn(List.of());

        assertThat(service.naComarca("CE", "Comarca Inexistente")).isEmpty();
    }
}
