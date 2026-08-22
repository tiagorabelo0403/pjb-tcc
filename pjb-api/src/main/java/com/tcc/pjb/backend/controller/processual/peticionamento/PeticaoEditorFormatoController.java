package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.ValidarFormatoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.ValidarFormatoResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextDocumentSanitizer;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.RichTextFormatCatalog;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Formatação rica da peça: catálogo do que o editor pode oferecer (negrito, fonte, tamanho, cor,
 * alinhamento, tabela, etc.) e validação/sanitização do documento JSON do editor contra esse
 * catálogo antes de salvar/publicar.
 */
@RestController
@RequestMapping("/api/v1/peticionamento/editor/formato")
@PreAuthorize("isAuthenticated()")
public class PeticaoEditorFormatoController {

    private final RichTextFormatCatalog catalog;
    private final RichTextDocumentSanitizer sanitizer;

    public PeticaoEditorFormatoController(RichTextFormatCatalog catalog, RichTextDocumentSanitizer sanitizer) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    @GetMapping("/catalogo")
    public ResponseEntity<Map<String, Object>> catalogo() {
        return ResponseEntity.ok(catalog.toBlueprintMap());
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidarFormatoResponse> validar(@Valid @RequestBody ValidarFormatoRequest request) {
        RichTextDocumentSanitizer.SanitizeResult result = sanitizer.sanitize(request.documento());
        return ResponseEntity.ok(new ValidarFormatoResponse(result.documento(), result.alterado(), result.remocoes()));
    }
}
