package com.tcc.pjb.backend.service.institutional.produtividade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.institutional.produtividade.InstitutionalProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalProdutividadeServiceTest {

    private final MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
    private final InstitutionalProdutividadeService service = new InstitutionalProdutividadeService(movimentacaoRepository);

    private MovimentacaoProcessual movimentacao(Long id, String descricao, Instant data) {
        Processo processo = new Processo();
        processo.setId(id);
        return MovimentacaoProcessual.builder().id(id).processo(processo).descricao(descricao).dataMovimentacao(data).build();
    }

    @Test
    void classificaAtosDoMinisterioPublicoEDaDefensoriaEDaProcuradoria() {
        when(movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(eq(30L), any()))
                .thenReturn(List.of(
                        movimentacao(1L, "Manifestação do Ministério Público registrada.", Instant.parse("2026-04-10T12:00:00Z")),
                        movimentacao(2L, "Petição da Defensoria Pública registrada.", Instant.parse("2026-04-10T08:00:00Z")),
                        movimentacao(3L, "Contestação da Procuradoria apresentada.", Instant.parse("2026-04-09T08:00:00Z")),
                        movimentacao(4L, "Ato de origem desconhecida.", Instant.parse("2026-04-08T08:00:00Z"))));

        InstitutionalProdutividadePainelResponse painel = service.painel(30L, 30);

        assertThat(painel.total()).isEqualTo(4);
        assertThat(painel.porTipo()).containsEntry("MANIFESTACAO_MP", 1)
                .containsEntry("PETICAO_DEFENSORIA", 1)
                .containsEntry("CONTESTACAO_PROCURADORIA", 1)
                .containsEntry("OUTRO", 1);
    }

    @Test
    void retornaPainelVazioSemNenhumaMovimentacao() {
        when(movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(eq(30L), any()))
                .thenReturn(List.of());

        InstitutionalProdutividadePainelResponse painel = service.painel(30L, 30);

        assertThat(painel.total()).isZero();
        assertThat(painel.porTipo()).isEmpty();
        assertThat(painel.intervaloMedioHoras()).isNull();
    }
}
