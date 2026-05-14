package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DessobrestamentoTematicoPainelServiceTest {

    private final DessobrestamentoTematicoPainelService service =
            new DessobrestamentoTematicoPainelService();

    @Test
    void analisar_manterSobrestamento_quandoTemaNaoJulgado() {
        var processo = processoSobrestado(false, null, false);
        var result = service.analisar(List.of(processo));
        assertThat(result.totalDessobrestar()).isZero();
        assertThat(result.totalManter()).isEqualTo(1);
    }

    @Test
    void analisar_manterSobrestamento_quandoAcordaoNaoPublicado() {
        var processo = processoSobrestado(true, LocalDate.now().minusMonths(1), false);
        var result = service.analisar(List.of(processo));
        assertThat(result.totalDessobrestar()).isZero();
        assertThat(result.recomendacoes().getFirst().acaoRecomendada()).contains("publicação");
    }

    @Test
    void analisar_dessobrestar_quandoTemaJulgadoEAcordaoPublicado() {
        var processo = processoSobrestado(true, LocalDate.now().minusMonths(1), true);
        var result = service.analisar(List.of(processo));
        assertThat(result.totalDessobrestar()).isEqualTo(1);
        assertThat(result.recomendacoes().getFirst().deveDessobrestar()).isTrue();
        assertThat(result.recomendacoes().getFirst().motivacao()).contains("acórdão publicado");
    }

    @Test
    void analisar_resultadoAgregado() {
        var processos = List.of(
                processoSobrestado(true, LocalDate.now().minusMonths(2), true),
                processoSobrestado(false, null, false));
        var result = service.analisar(processos);
        assertThat(result.recomendacoes()).hasSize(2);
        assertThat(result.totalDessobrestar()).isEqualTo(1);
        assertThat(result.totalManter()).isEqualTo(1);
    }

    @Test
    void analisar_listaVazia_retornaResultadoVazio() {
        var result = service.analisar(List.of());
        assertThat(result.recomendacoes()).isEmpty();
        assertThat(result.totalDessobrestar()).isZero();
        assertThat(result.totalManter()).isZero();
    }

    private DessobrestamentoTematicoPainelService.ProcessoSobrestado processoSobrestado(
            boolean temaJulgado, LocalDate dataJulgamento, boolean acordaoPublicado) {
        return new DessobrestamentoTematicoPainelService.ProcessoSobrestado(
                UUID.randomUUID(), "0001234-56.2024.8.26.0001", "1000",
                LocalDate.now().minusMonths(6),
                temaJulgado, dataJulgamento, acordaoPublicado, false);
    }
}
