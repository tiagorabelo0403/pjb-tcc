package com.tcc.pjb.backend.service.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.repository.ComarcaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Cobre a resolucao acento-insensivel depois que ela deixou de ser uma query nativa com
 * {@code unaccent} (extensao PostgreSQL) e passou a rodar em Java — mudanca necessaria porque o
 * servico e chamado tambem em boot (ApplicationReadyEvent), que sobe sobre H2 nos testes.
 */
class ComarcaResolutionServiceTest {

    @Test
    void resolvePorNomeComAcentoEcaixaDiferentesQuandoUfInformada() {
        ComarcaRepository repository = mock(ComarcaRepository.class);
        Comarca saoGoncalo = new Comarca("São Gonçalo do Amarante", "CE", "2312908", null);
        when(repository.findAllByUfIgnoreCase("ce"))
                .thenReturn(List.of(new Comarca("Fortaleza", "CE", "2304400", null), saoGoncalo));

        Comarca resolvida = new ComarcaResolutionService(repository).resolver("sao goncalo do amarante", "ce").orElseThrow();

        assertThat(resolvida).isSameAs(saoGoncalo);
    }

    @Test
    void naoResolveQuandoNenhumaComarcaDaUfCasaPeloNome() {
        ComarcaRepository repository = mock(ComarcaRepository.class);
        when(repository.findAllByUfIgnoreCase("CE"))
                .thenReturn(List.of(new Comarca("Fortaleza", "CE", "2304400", null)));

        assertThat(new ComarcaResolutionService(repository).resolver("Comarca Inexistente", "CE")).isEmpty();
    }

    @Test
    void resolveSemUfQuandoHaExatamenteUmaCandidataInequivoca() {
        ComarcaRepository repository = mock(ComarcaRepository.class);
        Comarca unica = new Comarca("Quixadá", "CE", "2311306", null);
        when(repository.findAll()).thenReturn(List.of(unica, new Comarca("Fortaleza", "CE", "2304400", null)));

        assertThat(new ComarcaResolutionService(repository).resolver("quixada", null)).containsSame(unica);
    }

    @Test
    void naoResolveSemUfQuandoNomeEAmbiguoEntreUfsDistintas() {
        ComarcaRepository repository = mock(ComarcaRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new Comarca("Boa Vista", "CE", "2301000", null),
                new Comarca("Boa Vista", "MG", "3101000", null)));

        assertThat(new ComarcaResolutionService(repository).resolver("Boa Vista", null)).isEmpty();
    }

    @Test
    void naoResolveNemConsultaRepositorioQuandoNomeEmBranco() {
        ComarcaRepository repository = mock(ComarcaRepository.class);

        assertThat(new ComarcaResolutionService(repository).resolver("  ", "CE")).isEmpty();

        org.mockito.Mockito.verifyNoInteractions(repository);
    }
}
