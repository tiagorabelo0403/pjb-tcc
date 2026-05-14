package com.tcc.pjb.backend.service.search.processual;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PesquisaTextualOrientadaAtoServiceTest {

    private final PesquisaTextualOrientadaAtoService service =
            new PesquisaTextualOrientadaAtoService();

    @Test
    void pesquisar_listaVazia_quandoConsultaNula() {
        var input = new PesquisaTextualOrientadaAtoService.PesquisaInput(
                null, RitoProcessual.PROCEDIMENTO_COMUM, "srv-001", false);
        assertThat(service.pesquisar(input)).isEmpty();
    }

    @Test
    void pesquisar_listaVazia_quandoConsultaVazia() {
        var input = new PesquisaTextualOrientadaAtoService.PesquisaInput(
                "", RitoProcessual.PROCEDIMENTO_COMUM, "srv-001", false);
        assertThat(service.pesquisar(input)).isEmpty();
    }

    @Test
    void normalizarConsulta_removePalavrasDeParagem() {
        var normalizada = service.normalizarConsulta("como preciso quero citar");
        assertThat(normalizada).doesNotContain("como").doesNotContain("preciso").doesNotContain("quero");
        assertThat(normalizada.trim()).isEqualTo("citar");
    }

    @Test
    void normalizarConsulta_retornaVazio_quandoNulo() {
        assertThat(service.normalizarConsulta(null)).isEmpty();
    }

    @Test
    void consultaEnvolveAtoJurisdicional_verdadeiro_quandoContemSentenca() {
        assertThat(service.consultaEnvolveAtoJurisdicional("proferir sentença")).isTrue();
    }

    @Test
    void consultaEnvolveAtoJurisdicional_falso_quandoNaoEnvolve() {
        assertThat(service.consultaEnvolveAtoJurisdicional("expedir mandado")).isFalse();
    }
}
