package com.tcc.pjb.backend.service.juiz.produtividade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.juiz.produtividade.JuizProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JuizProdutividadeServiceTest {

    private final MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
    private final JuizProdutividadeService service = new JuizProdutividadeService(movimentacaoRepository);

    private MovimentacaoProcessual movimentacao(Long processoId, String descricao, Instant data) {
        Processo processo = new Processo();
        processo.setId(processoId);
        return MovimentacaoProcessual.builder().id(processoId).processo(processo).descricao(descricao).dataMovimentacao(data).build();
    }

    @Test
    void classificaAtosPorTipoECalculaIntervaloMedio() {
        when(movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(eq(10L), any()))
                .thenReturn(List.of(
                        movimentacao(1L, "Sentença judicial proferida e documento assinado: DOC-3", Instant.parse("2026-04-10T12:00:00Z")),
                        movimentacao(2L, "Despacho judicial proferido e documento assinado: DOC-2", Instant.parse("2026-04-10T08:00:00Z")),
                        movimentacao(3L, "Decisão interlocutória proferida e documento assinado: DOC-1", Instant.parse("2026-04-09T08:00:00Z"))));

        JuizProdutividadePainelResponse painel = service.painel(10L, 30);

        assertThat(painel.total()).isEqualTo(3);
        assertThat(painel.porTipo()).containsEntry("SENTENCA", 1).containsEntry("DESPACHO", 1).containsEntry("DECISAO_INTERLOCUTORIA", 1);
        assertThat(painel.intervaloMedioHoras()).isEqualTo(14.0);
    }

    @Test
    void retornaIntervaloNuloComMenosDeDoisAtos() {
        when(movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(eq(10L), any()))
                .thenReturn(List.of(movimentacao(1L, "Despacho judicial proferido", Instant.now())));

        JuizProdutividadePainelResponse painel = service.painel(10L, 30);

        assertThat(painel.total()).isEqualTo(1);
        assertThat(painel.intervaloMedioHoras()).isNull();
    }

    @Test
    void retornaPainelVazioSemNenhumAto() {
        when(movimentacaoRepository.findByAtor_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoDesc(eq(10L), any()))
                .thenReturn(List.of());

        JuizProdutividadePainelResponse painel = service.painel(10L, 30);

        assertThat(painel.total()).isZero();
        assertThat(painel.porTipo()).isEmpty();
        assertThat(painel.intervaloMedioHoras()).isNull();
    }
}
