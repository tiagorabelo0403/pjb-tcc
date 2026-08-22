package com.tcc.pjb.backend.controller.processual.peticionamento;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.ValidarFormatoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.ValidarFormatoResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextFormatCatalog;
import org.junit.jupiter.api.Test;

class PeticaoEditorFormatoControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RichTextFormatCatalog catalog = new RichTextFormatCatalog();
    private final PeticaoEditorFormatoController controller =
            new PeticaoEditorFormatoController(catalog, new RichTextDocumentSanitizer(mapper, catalog));

    @Test
    void catalogoExpoeAllowlist() {
        var resp = controller.catalogo();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().get("model")).isEqualTo("TIPTAP_PROSEMIRROR_JSON");
    }

    @Test
    void validarSanitizaEReportaRemocoes() throws Exception {
        var doc = mapper.readTree("""
                {"type":"doc","content":[
                  {"type":"paragraph","content":[
                    {"type":"text","text":"x","marks":[{"type":"link","attrs":{"href":"javascript:alert(1)"}}]}
                  ]}
                ]}""");
        var out = controller.validar(new ValidarFormatoRequest(doc));
        assertThat(out.getStatusCode().is2xxSuccessful()).isTrue();
        ValidarFormatoResponse body = out.getBody();
        assertThat(body.alterado()).isTrue();
        assertThat(body.remocoes()).isNotEmpty();
    }
}
