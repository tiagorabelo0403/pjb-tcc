package com.tcc.pjb.backend.service.processo.autuacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetificacaoAutuacaoDiffServiceTest {

    private final RetificacaoAutuacaoDiffService service = new RetificacaoAutuacaoDiffService();

    @Test
    void calcular_semDiferencas_quandoCamposIguais() {
        var input = new RetificacaoAutuacaoDiffService.DiffInput(
                UUID.randomUUID(),
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao Civil", "Acao Civil",
                null, null);
        var resultado = service.calcular(input);
        assertThat(resultado.diferencas()).isEmpty();
        assertThat(resultado.exigeDecisaoJudicial()).isFalse();
        assertThat(resultado.resumo()).contains("Nenhuma diferença");
    }

    @Test
    void calcular_detectaAlteracaoDeRito_comImpactoJudicial() {
        var input = new RetificacaoAutuacaoDiffService.DiffInput(
                UUID.randomUUID(),
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                "Acao", "Acao",
                null, null);
        var resultado = service.calcular(input);
        assertThat(resultado.diferencas()).hasSize(1);
        assertThat(resultado.diferencas().getFirst().campo()).isEqualTo("rito");
        assertThat(resultado.diferencas().getFirst().critico()).isTrue();
        assertThat(resultado.exigeDecisaoJudicial()).isTrue();
    }

    @Test
    void calcular_detectaAlteracaoDeClasse() {
        var input = new RetificacaoAutuacaoDiffService.DiffInput(
                UUID.randomUUID(),
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao Civil", "Acao Civil Publica",
                null, null);
        var resultado = service.calcular(input);
        assertThat(resultado.diferencas()).anyMatch(d -> d.campo().equals("classe"));
    }

    @Test
    void calcular_detectaAlteracaoDeValorCausa() {
        var input = new RetificacaoAutuacaoDiffService.DiffInput(
                UUID.randomUUID(),
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao", "Acao",
                BigDecimal.valueOf(5000), BigDecimal.valueOf(15000));
        var resultado = service.calcular(input);
        assertThat(resultado.diferencas()).anyMatch(d -> d.campo().equals("valorCausa"));
    }
}
