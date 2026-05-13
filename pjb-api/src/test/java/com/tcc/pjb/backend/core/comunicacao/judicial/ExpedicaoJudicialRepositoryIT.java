package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExpedicaoJudicialRepositoryIT extends PjbIntegrationTestBase {

    @Autowired
    private ExpedicaoJudicialRepository repository;

    @Test
    void devePromoverPresuncaoERecuperarHistoricoDigitalConfirmado() {
        ExpedicaoJudicial presumida = repository.save(new ExpedicaoJudicial(
                91L,
                "0001111-22.2026.8.06.0001",
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH,
                ExpedicaoJudicial.TipoDestinatario.ADVOGADO_OAB,
                "Advogada Teste",
                "123.456.789-01",
                "CIVIL",
                "PRIMEIRO_GRAU",
                null,
                "hash-presumida-1",
                "CPC art. 272"
        ));
        presumida.setStatus(ExpedicaoJudicial.StatusExpedicao.EXPEDIDA);
        presumida.setInstanciaExpedidora("TJCE");
        presumida.setCanalDigitalUtilizado("GOVBR_PUSH");
        repository.save(presumida);

        assertThat(repository.atualizarPresuncoesPendentes(Instant.now().plusSeconds(60))).isGreaterThanOrEqualTo(1);

        ExpedicaoJudicial atualizada = repository.findById(presumida.getId()).orElseThrow();
        assertThat(atualizada.getStatus()).isEqualTo(ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE);
        assertThat(atualizada.getPrazoRespostaInicioEm()).isNotNull();
        assertThat(repository.jaFoiServadoDigitalmente("12345678901")).isTrue();
        assertThat(repository.findUltimaEntregaDigitalConfirmada("12345678901"))
                .isPresent()
                .get()
                .extracting(ExpedicaoJudicial::getId)
                .isEqualTo(atualizada.getId());
    }

    @Test
    void deveFiltrarEvasoesNaoEscalonadasSemMisturarCanceladas() {
        ExpedicaoJudicial pendente = repository.save(new ExpedicaoJudicial(
                92L,
                "0002222-33.2026.8.06.0001",
                TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA,
                ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Parte Ré",
                "987.654.321-00",
                "CIVIL",
                "PRIMEIRO_GRAU",
                null,
                "hash-evasao-1",
                "CPC art. 238"
        ));
        pendente.registrarEvasao("domicilio fechado em tres diligencias");
        repository.save(pendente);

        ExpedicaoJudicial cancelada = repository.save(new ExpedicaoJudicial(
                93L,
                "0003333-44.2026.8.06.0001",
                TipoComunicacaoJudicial.CITACAO_INICIAL,
                ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA,
                ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA,
                "Parte Ré Cancelada",
                "111.222.333-44",
                "CIVIL",
                "PRIMEIRO_GRAU",
                null,
                "hash-evasao-2",
                "CPC art. 238"
        ));
        cancelada.registrarEvasao("rota invalida");
        cancelada.setStatus(ExpedicaoJudicial.StatusExpedicao.CANCELADA);
        repository.save(cancelada);

        assertThat(repository.findEvasoesNaoEscalonadas())
                .extracting(ExpedicaoJudicial::getId)
                .contains(pendente.getId())
                .doesNotContain(cancelada.getId());
    }
}
