package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RichTextDocumentSanitizerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RichTextDocumentSanitizer sanitizer =
            new RichTextDocumentSanitizer(mapper, new RichTextFormatCatalog());

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void documentoValidoPassaSemAlteracao() {
        JsonNode doc = parse("""
                {"type":"doc","content":[
                  {"type":"paragraph","attrs":{"textAlign":"justify"},"content":[
                    {"type":"text","text":"Excelentíssimo","marks":[{"type":"bold"}]}
                  ]}
                ]}""");

        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);

        assertThat(r.alterado()).isFalse();
        assertThat(r.remocoes()).isEmpty();
        assertThat(r.documento().get("content").get(0).get("content").get(0).get("marks").get(0).get("type").asText())
                .isEqualTo("bold");
    }

    @Test
    void noNaoPermitidoEhRemovido() {
        JsonNode doc = parse("""
                {"type":"doc","content":[
                  {"type":"script","content":[{"type":"text","text":"alert(1)"}]},
                  {"type":"paragraph","content":[{"type":"text","text":"ok"}]}
                ]}""");

        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);

        assertThat(r.alterado()).isTrue();
        assertThat(r.remocoes()).anyMatch(m -> m.contains("script"));
        assertThat(r.documento().get("content")).hasSize(1);
        assertThat(r.documento().get("content").get(0).get("type").asText()).isEqualTo("paragraph");
    }

    @Test
    void marcaNaoPermitidaEhRemovidaMantendoTexto() {
        JsonNode doc = parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"x","marks":[{"type":"blink"},{"type":"italic"}]}
                ]}]}""");

        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);

        JsonNode marks = r.documento().get("content").get(0).get("content").get(0).get("marks");
        assertThat(marks).hasSize(1);
        assertThat(marks.get(0).get("type").asText()).isEqualTo("italic");
        assertThat(r.remocoes()).anyMatch(m -> m.contains("blink"));
    }

    @Test
    void linkComJavascriptSchemeEhBloqueado() {
        JsonNode doc = parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"clique","marks":[{"type":"link","attrs":{"href":"javascript:alert(1)"}}]}
                ]}]}""");

        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);

        JsonNode linkAttrs = r.documento().get("content").get(0).get("content").get(0).get("marks").get(0).get("attrs");
        assertThat(linkAttrs == null || linkAttrs.get("href") == null).isTrue();
        assertThat(r.remocoes()).anyMatch(m -> m.contains("esquema bloqueado"));
    }

    @Test
    void imagemComDataUriEhBloqueadaMasHttpsPassa() {
        JsonNode bloqueada = parse("""
                {"type":"doc","content":[{"type":"image","attrs":{"src":"data:text/html;base64,PHNjcmlwdD4="}}]}""");
        RichTextDocumentSanitizer.SanitizeResult r1 = sanitizer.sanitize(bloqueada);
        JsonNode attrs1 = r1.documento().get("content").get(0).get("attrs");
        assertThat(attrs1 == null || attrs1.get("src") == null).isTrue();

        JsonNode ok = parse("""
                {"type":"doc","content":[{"type":"image","attrs":{"src":"https://cdn.pjb/x.png","alt":"foto"}}]}""");
        RichTextDocumentSanitizer.SanitizeResult r2 = sanitizer.sanitize(ok);
        assertThat(r2.documento().get("content").get(0).get("attrs").get("src").asText())
                .isEqualTo("https://cdn.pjb/x.png");
    }

    @Test
    void fonteNaoPermitidaEhDescartadaFontePermitidaFica() {
        JsonNode doc = parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"a","marks":[{"type":"textStyle","attrs":{"fontFamily":"Comic Sans MS","fontSize":"12pt"}}]},
                  {"type":"text","text":"b","marks":[{"type":"textStyle","attrs":{"fontFamily":"Arial"}}]}
                ]}]}""");

        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);

        JsonNode m0 = r.documento().get("content").get(0).get("content").get(0).get("marks").get(0).get("attrs");
        assertThat(m0.get("fontFamily")).isNull();
        assertThat(m0.get("fontSize").asText()).isEqualTo("12pt");
        JsonNode m1 = r.documento().get("content").get(0).get("content").get(1).get("marks").get(0).get("attrs");
        assertThat(m1.get("fontFamily").asText()).isEqualTo("Arial");
    }

    @Test
    void nivelDeTituloForaDoIntervaloEhDescartado() {
        JsonNode doc = parse("""
                {"type":"doc","content":[{"type":"heading","attrs":{"level":9},"content":[{"type":"text","text":"t"}]}]}""");
        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(doc);
        JsonNode attrs = r.documento().get("content").get(0).get("attrs");
        assertThat(attrs == null || attrs.get("level") == null).isTrue();
    }

    @Test
    void documentoNuloViraDocVazio() {
        RichTextDocumentSanitizer.SanitizeResult r = sanitizer.sanitize(null);
        assertThat(r.documento().get("type").asText()).isEqualTo("doc");
        assertThat(r.documento().get("content")).isEmpty();
        assertThat(r.alterado()).isTrue();
    }
}
