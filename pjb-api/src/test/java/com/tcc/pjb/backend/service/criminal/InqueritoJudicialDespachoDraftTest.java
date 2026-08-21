package com.tcc.pjb.backend.service.criminal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InqueritoJudicialDespachoDraftTest {

    @Test
    void gerar_interpolaNumeroDoProcedimentoRealNoTexto() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setNumeroProcedimento("2026.001.INQ.000123");
        inquerito.setOrgaoApuracao("1ª Delegacia de Polícia Civil");

        Optional<InqueritoJudicialDespachoDraft.Minuta> minuta = InqueritoJudicialDespachoDraft.gerar(inquerito);

        assertThat(minuta).isPresent();
        assertThat(minuta.get().conteudo())
                .contains("2026.001.INQ.000123")
                .contains("1ª Delegacia de Polícia Civil")
                .contains("Vistos.")
                .contains("Ministério Público")
                .contains("Cumpra-se.");
    }

    @Test
    void gerar_separaConteudoDeFundamentacao() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setNumeroProcedimento("2026.001.INQ.000123");

        InqueritoJudicialDespachoDraft.Minuta minuta = InqueritoJudicialDespachoDraft.gerar(inquerito).orElseThrow();

        assertThat(minuta.conteudo()).doesNotContain("CPP art. 28");
        assertThat(minuta.fundamentacao())
                .contains("CPP art. 28")
                .contains("Lei nº 13.964/2019");
    }

    @Test
    void gerar_semOrgaoApuracao_usaRotuloGenerico() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setNumeroProcedimento("2026.001.INQ.000999");

        InqueritoJudicialDespachoDraft.Minuta minuta = InqueritoJudicialDespachoDraft.gerar(inquerito).orElseThrow();

        assertThat(minuta.conteudo()).contains("autoridade policial");
    }

    @Test
    void gerar_tipoDiferenteDeInqueritoPolicial_naoGeraMinuta() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("TERMO_CIRCUNSTANCIADO");
        inquerito.setNumeroProcedimento("2026.001.TC.000001");

        Optional<InqueritoJudicialDespachoDraft.Minuta> minuta = InqueritoJudicialDespachoDraft.gerar(inquerito);

        assertThat(minuta).isEmpty();
    }

    @Test
    void gerar_semNumeroDeProcedimento_naoGeraMinuta() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setNumeroProcedimento(null);

        Optional<InqueritoJudicialDespachoDraft.Minuta> minuta = InqueritoJudicialDespachoDraft.gerar(inquerito);

        assertThat(minuta).isEmpty();
    }

    @Test
    void gerar_inqueritoNulo_naoGeraMinuta() {
        assertThat(InqueritoJudicialDespachoDraft.gerar(null)).isEmpty();
    }

    @Test
    void gerar_origemAcimaDoLimiteDaColuna_ehTruncada() {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setNumeroProcedimento("2026.001.INQ.000123");
        inquerito.setOrgaoApuracao("A".repeat(300));

        InqueritoJudicialDespachoDraft.Minuta minuta = InqueritoJudicialDespachoDraft.gerar(inquerito).orElseThrow();

        assertThat(minuta.conteudo()).contains("A".repeat(120));
        assertThat(minuta.conteudo()).doesNotContain("A".repeat(121));
    }
}
