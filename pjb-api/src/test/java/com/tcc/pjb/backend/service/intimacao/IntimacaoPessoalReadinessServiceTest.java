package com.tcc.pjb.backend.service.intimacao;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IntimacaoPessoalReadinessServiceTest {

    private final IntimacaoPessoalReadinessService service = new IntimacaoPessoalReadinessService();

    @Test
    void avaliar_apta_quandoOficialJusticaCompletoDisponivel() {
        var input = new IntimacaoPessoalReadinessService.IntimacaoPessoalInput(
                UUID.randomUUID(), IntimacaoModalidadeEspecial.PESSOAL_OFICIAL_JUSTICA,
                true, true, true, true, false, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isTrue();
    }

    @Test
    void avaliar_inapta_quandoEnderecoNaoLocalizado() {
        var input = new IntimacaoPessoalReadinessService.IntimacaoPessoalInput(
                UUID.randomUUID(), IntimacaoModalidadeEspecial.PESSOAL_OFICIAL_JUSTICA,
                false, true, true, true, false, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Endereço"));
    }

    @Test
    void avaliar_inapta_eletronicaSemDomicilio() {
        var input = new IntimacaoPessoalReadinessService.IntimacaoPessoalInput(
                UUID.randomUUID(), IntimacaoModalidadeEspecial.ELETRONICA_DOMICILIO_JUDICIAL,
                true, false, false, true, false, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Domicílio judicial"));
    }

    @Test
    void avaliar_inapta_semDestinatario() {
        var input = new IntimacaoPessoalReadinessService.IntimacaoPessoalInput(
                UUID.randomUUID(), IntimacaoModalidadeEspecial.PESSOAL_VIA_CORREIO_AR,
                true, false, false, false, false, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("destinatário"));
    }
}
