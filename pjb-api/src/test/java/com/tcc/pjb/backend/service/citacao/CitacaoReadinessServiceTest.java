package com.tcc.pjb.backend.service.citacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitacaoReadinessServiceTest {

    private final CitacaoReadinessService service = new CitacaoReadinessService();

    @Test
    void deveAprovarCitacaoPostalComTodosRequisitos() {
        CitacaoReadinessService.CitacaoInput input = new CitacaoReadinessService.CitacaoInput(
                UUID.randomUUID(), ModalidadeCitacao.POSTAL,
                true, false, false, false, true, true);
        CitacaoReadinessService.CitacaoReadiness result = service.avaliar(input);
        assertThat(result.apta()).isTrue();
        assertThat(result.bloqueios()).isEmpty();
    }

    @Test
    void deveBloquerCitacaoSemDespachoPublicado() {
        CitacaoReadinessService.CitacaoInput input = new CitacaoReadinessService.CitacaoInput(
                UUID.randomUUID(), ModalidadeCitacao.POSTAL,
                true, false, false, false, true, false);
        CitacaoReadinessService.CitacaoReadiness result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.bloqueios()).anyMatch(b -> b.contains("Despacho"));
    }

    @Test
    void deveBloquerCitacaoMandadoSemAssinatura() {
        CitacaoReadinessService.CitacaoInput input = new CitacaoReadinessService.CitacaoInput(
                UUID.randomUUID(), ModalidadeCitacao.MANDADO,
                true, false, false, false, true, true);
        CitacaoReadinessService.CitacaoReadiness result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.bloqueios()).anyMatch(b -> b.contains("Mandado"));
    }

    @Test
    void deveSugerirIntimacaoPessoalParaFazendaPublica() {
        CitacaoReadinessService.CitacaoInput input = new CitacaoReadinessService.CitacaoInput(
                UUID.randomUUID(), ModalidadeCitacao.POSTAL,
                true, false, true, false, true, true);
        CitacaoReadinessService.CitacaoReadiness result = service.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("pessoal"));
    }
}
