package com.tcc.pjb.backend.service.intimacao;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CartaPrecatoriaCumprimentoServiceTest {

    private final CartaPrecatoriaCumprimentoService service = new CartaPrecatoriaCumprimentoService();

    @Test
    void avaliar_apta_quandoTodosRequisitosExpedicao() {
        var input = inputExpedicao(true, true, true, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.aptaExpedicao()).isTrue();
    }

    @Test
    void avaliar_inapta_semCompetenciaDelegada() {
        var input = inputExpedicao(false, true, true, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.aptaExpedicao()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("competência"));
    }

    @Test
    void avaliar_inapta_semPecasInstruidas() {
        var input = inputExpedicao(true, false, true, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.aptaExpedicao()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("art. 262"));
    }

    @Test
    void avaliar_inapta_semDespacho() {
        var input = inputExpedicao(true, true, false, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.aptaExpedicao()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Despacho"));
    }

    @Test
    void avaliar_cumprida_quandoCumpridaEDevolvida() {
        var input = inputExpedicao(true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.cumprimentoConfirmado()).isTrue();
        assertThat(result.fundamentacao()).contains("cumprida");
    }

    private CartaPrecatoriaCumprimentoService.CartaPrecatoriaInput inputExpedicao(
            boolean competencia, boolean pecas, boolean despacho,
            boolean protocolada, boolean cumprida, boolean devolvida) {
        return new CartaPrecatoriaCumprimentoService.CartaPrecatoriaInput(
                UUID.randomUUID(), UUID.randomUUID(),
                "1ª Vara Cível - SP", "2ª Vara Cível - RJ", "RJ",
                competencia, pecas, despacho, protocolada, cumprida, devolvida);
    }
}
