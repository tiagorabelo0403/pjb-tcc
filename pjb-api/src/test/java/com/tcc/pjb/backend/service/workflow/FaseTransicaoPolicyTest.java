package com.tcc.pjb.backend.service.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import org.junit.jupiter.api.Test;

class FaseTransicaoPolicyTest {

    @Test
    void devePermitirTransicaoDeAutuacaoParaDistribuicao() {
        FaseTransicaoPolicy.TransicaoInput input = new FaseTransicaoPolicy.TransicaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM,
                FaseProcessual.AUTUACAO,
                FaseProcessual.DISTRIBUICAO,
                List.of());
        FaseTransicaoPolicy.TransicaoResult result = FaseTransicaoPolicy.avaliar(input);
        assertThat(result.permitida()).isTrue();
        assertThat(result.bloqueios()).isEmpty();
    }

    @Test
    void deveNegarTransicaoInvalidaDiretaDeAutuacaoParaJulgamento() {
        FaseTransicaoPolicy.TransicaoInput input = new FaseTransicaoPolicy.TransicaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM,
                FaseProcessual.AUTUACAO,
                FaseProcessual.JULGAMENTO,
                List.of());
        FaseTransicaoPolicy.TransicaoResult result = FaseTransicaoPolicy.avaliar(input);
        assertThat(result.permitida()).isFalse();
        assertThat(result.bloqueios()).isNotEmpty();
    }

    @Test
    void deveBloquerTransicaoDeCitacaoSemMandadoCumprido() {
        FaseTransicaoPolicy.TransicaoInput input = new FaseTransicaoPolicy.TransicaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM,
                FaseProcessual.CITACAO,
                FaseProcessual.RESPOSTA,
                List.of());
        FaseTransicaoPolicy.TransicaoResult result = FaseTransicaoPolicy.avaliar(input);
        assertThat(result.permitida()).isFalse();
        assertThat(result.bloqueios()).anyMatch(b -> b.contains("Citação"));
    }

    @Test
    void devePermitirTransicaoDeCitacaoComMandadoCumprido() {
        FaseTransicaoPolicy.TransicaoInput input = new FaseTransicaoPolicy.TransicaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM,
                FaseProcessual.CITACAO,
                FaseProcessual.RESPOSTA,
                List.of("MANDADO_CUMPRIDO"));
        FaseTransicaoPolicy.TransicaoResult result = FaseTransicaoPolicy.avaliar(input);
        assertThat(result.permitida()).isTrue();
    }

    @Test
    void devePermitirTransicaoDeJulgamentoParaArquivamento() {
        FaseTransicaoPolicy.TransicaoInput input = new FaseTransicaoPolicy.TransicaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM,
                FaseProcessual.JULGAMENTO,
                FaseProcessual.ARQUIVAMENTO,
                List.of());
        FaseTransicaoPolicy.TransicaoResult result = FaseTransicaoPolicy.avaliar(input);
        assertThat(result.permitida()).isTrue();
    }
}
