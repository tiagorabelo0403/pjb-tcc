package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RichTextHtmlRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RichTextHtmlRenderer renderer = new RichTextHtmlRenderer();

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void marcasViramTagsSemânticas() {
        String html = renderer.toHtml(parse("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"textAlign":"justify"},"content":[
                  {"type":"text","text":"forte","marks":[{"type":"bold"}]},
                  {"type":"text","text":"it","marks":[{"type":"italic"}]},
                  {"type":"text","text":"sub","marks":[{"type":"underline"}]}
                ]}]}"""));
        assertThat(html).contains("<p style=\"text-align:justify\">");
        assertThat(html).contains("<strong>forte</strong>");
        assertThat(html).contains("<em>it</em>");
        assertThat(html).contains("<u>sub</u>");
    }

    @Test
    void tituloListaTabelaViramHtml() {
        String html = renderer.toHtml(parse("""
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"Fatos"}]},
                  {"type":"bulletList","content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"um"}]}]}]},
                  {"type":"table","content":[{"type":"tableRow","content":[{"type":"tableCell","content":[{"type":"paragraph","content":[{"type":"text","text":"c"}]}]}]}]}
                ]}"""));
        assertThat(html).contains("<h2>Fatos</h2>");
        assertThat(html).contains("<ul><li><p>um</p></li></ul>");
        assertThat(html).contains("<table><tr><td><p>c</p></td></tr></table>");
    }

    @Test
    void fonteTamanhoCorViramStyleInline() {
        String html = renderer.toHtml(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[
                  {"type":"text","text":"x","marks":[{"type":"textStyle","attrs":{"fontFamily":"Arial","fontSize":"12pt","color":"#0A3D62"}}]}
                ]}]}"""));
        assertThat(html).contains("font-family:Arial;");
        assertThat(html).contains("font-size:12pt;");
        assertThat(html).contains("color:#0A3D62;");
    }

    @Test
    void textoEhEscapadoNuncaEmiteHtmlBruto() {
        String html = renderer.toHtml(parse("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"<script>a & b"}]}]}"""));
        assertThat(html).contains("&lt;script&gt;a &amp; b");
        assertThat(html).doesNotContain("<script>");
    }
}
