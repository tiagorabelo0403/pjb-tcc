package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TemaRepercussaoGeralServiceTest {

    private final TemaRepercussaoGeralService service = new TemaRepercussaoGeralService();

    @Test
    void avaliarTema_semSobrestamento_quandoSemRepercussao() {
        var tema = input("999", false, false, false, null, null);
        var status = service.avaliarTema(tema);
        assertThat(status.sobrestamentoObrigatorio()).isFalse();
        assertThat(status.liberadoParaCumprimento()).isTrue();
        assertThat(status.situacao()).isEqualTo("SEM_REPERCUSSAO_GERAL");
    }

    @Test
    void avaliarTema_liberado_quandoJulgamentoConcluido() {
        var julgamento = LocalDate.of(2024, 6, 1);
        var tema = input("100", true, true, true, LocalDate.of(2023, 1, 1), julgamento);
        var status = service.avaliarTema(tema);
        assertThat(status.sobrestamentoObrigatorio()).isFalse();
        assertThat(status.liberadoParaCumprimento()).isTrue();
        assertThat(status.situacao()).isEqualTo("JULGADO");
        assertThat(status.orientacao()).contains("precedente vinculante");
    }

    @Test
    void avaliarTema_sobrestamentoObrigatorio_quandoAfetadoPendente() {
        var tema = input("200", true, true, false, LocalDate.of(2024, 1, 1), null);
        var status = service.avaliarTema(tema);
        assertThat(status.sobrestamentoObrigatorio()).isTrue();
        assertThat(status.liberadoParaCumprimento()).isFalse();
        assertThat(status.situacao()).isEqualTo("AFETADO_PENDENTE");
    }

    @Test
    void avaliarTemas_processaLista() {
        var temas = List.of(
                input("1", false, false, false, null, null),
                input("2", true, true, true, LocalDate.now().minusYears(1), LocalDate.now().minusMonths(6)));
        var resultados = service.avaliarTemas(temas);
        assertThat(resultados).hasSize(2);
    }

    private TemaRepercussaoGeralService.TemaInput input(
            String numero, boolean repercussaoReconhecida, boolean afetado, boolean julgado,
            LocalDate dataAfetacao, LocalDate dataJulgamento) {
        return new TemaRepercussaoGeralService.TemaInput(
                numero, "Tema de teste " + numero,
                repercussaoReconhecida, afetado, julgado,
                dataAfetacao, dataJulgamento, "STF");
    }
}
