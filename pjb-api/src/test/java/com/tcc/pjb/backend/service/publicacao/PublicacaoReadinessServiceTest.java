package com.tcc.pjb.backend.service.publicacao;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PublicacaoReadinessServiceTest {

    private final PublicacaoReadinessService service = new PublicacaoReadinessService();

    private PublicacaoReadinessService.PublicacaoInput apto() {
        return new PublicacaoReadinessService.PublicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), "DESPACHO",
                MeioPublicacao.DIARIO_ELETRONICO_JUSTICA,
                true, true, true, true, true);
    }

    @Test
    void avaliar_apta_quandoTodasCondicoesOk() {
        var r = service.avaliar(apto());
        assertThat(r.apta()).isTrue();
        assertThat(r.bloqueios()).isEmpty();
    }

    @Test
    void avaliar_bloqueada_quandoDocumentoNaoAssinado() {
        var input = new PublicacaoReadinessService.PublicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), "SENTENCA",
                MeioPublicacao.DIARIO_ELETRONICO_JUSTICA,
                false, true, true, true, true);
        var r = service.avaliar(input);
        assertThat(r.apta()).isFalse();
        assertThat(r.bloqueios()).anyMatch(b -> b.contains("assinado"));
    }

    @Test
    void avaliar_bloqueada_quandoSigiloNaoVerificado() {
        var input = new PublicacaoReadinessService.PublicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), "DESPACHO",
                MeioPublicacao.DIARIO_ELETRONICO_JUSTICA,
                true, false, true, true, true);
        assertThat(service.avaliar(input).apta()).isFalse();
    }

    @Test
    void avaliar_alerta_quandoEnderecoEletronicoNaoValidado() {
        var r = service.avaliar(new PublicacaoReadinessService.PublicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), "DESPACHO",
                MeioPublicacao.DIARIO_ELETRONICO_JUSTICA,
                true, true, true, false, true));
        assertThat(r.alertas()).anyMatch(a -> a.contains("eletrônico"));
    }

    @Test
    void avaliar_meioSugerido_domicilioQuandoSemAdvogado() {
        var input = new PublicacaoReadinessService.PublicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(), "DESPACHO",
                MeioPublicacao.DIARIO_ELETRONICO_JUSTICA,
                true, true, false, true, true);
        var r = service.avaliar(input);
        assertThat(r.meioSugerido()).isEqualTo(MeioPublicacao.DOMICILIO_ELETRONICO_JUDICIAL);
    }
}
