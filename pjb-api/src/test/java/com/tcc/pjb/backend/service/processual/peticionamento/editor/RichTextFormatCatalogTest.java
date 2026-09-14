package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RichTextFormatCatalogTest {

    private final RichTextFormatCatalog catalog = new RichTextFormatCatalog();

    @Test
    void allowlistCobreOsBlocosEMarcasEsperados() {
        assertThat(catalog.nodePermitido("paragraph")).isTrue();
        assertThat(catalog.nodePermitido("table")).isTrue();
        assertThat(catalog.nodePermitido("script")).isFalse();
        assertThat(catalog.markPermitida("bold")).isTrue();
        assertThat(catalog.markPermitida("underline")).isTrue();
        assertThat(catalog.markPermitida("blink")).isFalse();
    }

    @Test
    void esquemaUrlAceitaHttpsMailtoRelativoEBloqueiaJavascriptEData() {
        assertThat(catalog.esquemaUrlPermitido("https://x")).isTrue();
        assertThat(catalog.esquemaUrlPermitido("mailto:a@b")).isTrue();
        assertThat(catalog.esquemaUrlPermitido("/api/v1/x")).isTrue();
        assertThat(catalog.esquemaUrlPermitido("javascript:alert(1)")).isFalse();
        assertThat(catalog.esquemaUrlPermitido("data:text/html,x")).isFalse();
    }

    @Test
    void fontesTamanhosEAlinhamentosCurados() {
        assertThat(catalog.fontePermitida("Arial")).isTrue();
        assertThat(catalog.fontePermitida("Comic Sans MS")).isFalse();
        assertThat(catalog.tamanhoPermitido("12pt")).isTrue();
        assertThat(catalog.tamanhoPermitido("99pt")).isFalse();
        assertThat(catalog.alinhamentoPermitido("justify")).isTrue();
        assertThat(catalog.alinhamentoPermitido("diagonal")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void blueprintMapExpoeCatalogoParaOEditor() {
        Map<String, Object> map = catalog.toBlueprintMap();
        assertThat(map.get("model")).isEqualTo("TIPTAP_PROSEMIRROR_JSON");
        assertThat(map.get("enforcement")).isEqualTo("BACKEND_SANITIZE_JSON");
        assertThat((java.util.List<String>) map.get("fonts")).contains("Times New Roman");
        assertThat((java.util.List<String>) map.get("marks")).contains("bold", "italic");
    }
}
