package com.tcc.pjb.backend.service.analytics.celerity;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioRetrabalhoServiceTest {

    private final RelatorioRetrabalhoService service = new RelatorioRetrabalhoService();

    @Test
    void gerar_relatorio_comPadroes() {
        var padroes = List.of(
                new RelatorioRetrabalhoService.PadraoRetrabalhoAgregado(
                        "CITACAO_FRUSTRADA", 30L, 40.0, "Verificar endereços antes de expedir."),
                new RelatorioRetrabalhoService.PadraoRetrabalhoAgregado(
                        "MANDADO_DEVOLVIDO", 20L, 26.0, "Atualizar cadastro de partes."));
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", padroes, 120);
        assertThat(relatorio.padroes()).hasSize(2);
        assertThat(relatorio.totalProcessosComRetrabalho()).isEqualTo(120);
        assertThat(relatorio.observacao()).contains("sistêmicos");
    }

    @Test
    void gerar_relatorio_semPadroes() {
        var relatorio = service.gerar(UUID.randomUUID(), "2025-04", List.of(), 0);
        assertThat(relatorio.padroes()).isEmpty();
        assertThat(relatorio.totalProcessosComRetrabalho()).isEqualTo(0);
    }
}
