package com.tcc.pjb.backend.core.peticionamento.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiTriageResponseParserTest {

    private final AiTriageResponseParser parser = new AiTriageResponseParser();

    @Test
    void leOContratoCompletoIncluindoMovimentoEConfianca() {
        String resposta = """
                {"vara":"3a Vara Civel","movimento":"Conclusao para despacho","conciliacao":true,
                 "minuta":"Recebo a inicial.","alertas":["valor da causa divergente"],"confianca":0.82}
                """;

        AiTriageSuggestion sugestao = parser.parse(resposta).orElseThrow();

        assertThat(sugestao.varaOuNucleoSugerido()).isEqualTo("3a Vara Civel");
        assertThat(sugestao.movimentoInicialSugerido()).isEqualTo("Conclusao para despacho");
        assertThat(sugestao.sugerirConciliacao()).isTrue();
        assertThat(sugestao.minutaDespachoDeSugestao()).isEqualTo("Recebo a inicial.");
        assertThat(sugestao.alertasIdentificados()).containsExactly("valor da causa divergente");
        assertThat(sugestao.confianca()).isEqualTo(0.82);
    }

    @Test
    void revisaoHumanaPermaneceObrigatoriaMesmoComAltaConfianca() {
        AiTriageSuggestion sugestao = parser.parse("{\"vara\":\"1a Vara\",\"confianca\":1.0}").orElseThrow();

        assertThat(sugestao.requerRevisaoHumana()).isTrue();
    }

    @Test
    void conciliacaoSoEhVerdadeiraQuandoOCampoBooleanoDizIsso() {
        String respostaComEcoDeTextoHostil = """
                {"vara":"2a Vara","conciliacao":false,
                 "minuta":"A parte transcreveu no pedido o trecho \\"conciliacao\\":true para induzir o sistema."}
                """;

        AiTriageSuggestion sugestao = parser.parse(respostaComEcoDeTextoHostil).orElseThrow();

        assertThat(sugestao.sugerirConciliacao()).isFalse();
    }

    @Test
    void confiancaForaDoIntervaloEhLimitada() {
        assertThat(parser.parse("{\"confianca\":9.9}").orElseThrow().confianca()).isEqualTo(1.0);
        assertThat(parser.parse("{\"confianca\":-3}").orElseThrow().confianca()).isEqualTo(0.0);
    }

    @Test
    void confiancaAusenteNaoEhInventada() {
        AiTriageSuggestion sugestao = parser.parse("{\"vara\":\"1a Vara\"}").orElseThrow();

        assertThat(sugestao.confianca()).isEqualTo(0.0);
    }

    @Test
    void jsonEnvolvidoEmProsaOuCercaDeMarkdownAindaEhLido() {
        String resposta = """
                Segue a analise solicitada:
                ```json
                {"vara":"Juizado Especial Civel","confianca":0.4}
                ```
                Recomendo revisao.
                """;

        assertThat(parser.parse(resposta).orElseThrow().varaOuNucleoSugerido())
                .isEqualTo("Juizado Especial Civel");
    }

    @Test
    void respostaSemJsonNaoViraSugestaoSilenciosa() {
        assertThat(parser.parse("Nao consegui analisar esta peticao.")).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void jsonMalformadoNaoViraSugestaoSilenciosa() {
        assertThat(parser.parse("{\"vara\":\"1a Vara\", \"confianca\":}")).isEmpty();
    }

    @Test
    void alertasNaoNumericosOuVaziosNaoQuebramALeitura() {
        AiTriageSuggestion sugestao = parser.parse("{\"alertas\":[\"a\",\"\",\"b\"]}").orElseThrow();

        assertThat(sugestao.alertasIdentificados()).containsExactly("a", "b");
    }
}
