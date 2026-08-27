package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class RichTextPlainTextExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RichTextPlainTextExtractor extractor = new RichTextPlainTextExtractor();

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void extraiParagrafosETitulosComoLinhasSeparadas() {
        List<String> linhas = extractor.extract(parse("""
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"Dos Fatos"}]},
                  {"type":"paragraph","content":[{"type":"text","text":"O réu descumpriu o contrato."}]}
                ]}"""));

        assertThat(linhas).containsExactly("Dos Fatos", "O réu descumpriu o contrato.");
    }

    @Test
    void marcasNaoAlteramOTextoExtraido() {
        List<String> linhas = extractor.extract(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"forte","marks":[{"type":"bold"}]},
                  {"type":"text","text":" normal"}
                ]}]}"""));

        assertThat(linhas).containsExactly("forte normal");
    }

    @Test
    void listaViraLinhasComPrefixoDeMarcador() {
        List<String> linhas = extractor.extract(parse("""
                {"type":"doc","content":[{"type":"bulletList","content":[
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"um"}]}]},
                  {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"dois"}]}]}
                ]}]}"""));

        assertThat(linhas).containsExactly("- um", "- dois");
    }

    @Test
    void tabelaViraUmaLinhaPorLinhaComCelulasSeparadasPorPipe() {
        List<String> linhas = extractor.extract(parse("""
                {"type":"doc","content":[{"type":"table","content":[{"type":"tableRow","content":[
                  {"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"a"}]}]},
                  {"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"b"}]}]}
                ]}]}]}"""));

        assertThat(linhas).containsExactly("a | b");
    }

    @Test
    void blockquoteRecebePrefixoDeCitacao() {
        List<String> linhas = extractor.extract(parse("""
                {"type":"doc","content":[{"type":"blockquote","content":[
                  {"type":"paragraph","content":[{"type":"text","text":"citação"}]}
                ]}]}"""));

        assertThat(linhas).containsExactly("> citação");
    }

    @Test
    void documentoNuloOuVazioResultaEmListaVazia() {
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract(parse("{\"type\":\"doc\",\"content\":[]}"))).isEmpty();
    }
}
