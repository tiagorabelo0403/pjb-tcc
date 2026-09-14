package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.ExportarDocxRequest;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.DocxExportService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exportação da peça para .docx real (Word/LibreOffice), gerado sem dependência externa a partir do
 * documento JSON do editor — sempre sanitizado antes.
 */
@RestController
@RequestMapping("/api/v1/peticionamento/editor/exportar")
@PreAuthorize("isAuthenticated()")
public class PeticaoEditorExportController {

    private final DocxExportService docxExportService;

    public PeticaoEditorExportController(DocxExportService docxExportService) {
        this.docxExportService = Objects.requireNonNull(docxExportService, "docxExportService");
    }

    @PostMapping("/docx")
    public ResponseEntity<byte[]> exportarDocx(@Valid @RequestBody ExportarDocxRequest request) {
        byte[] docx = docxExportService.exportar(request.documento(), request.tituloCaso());
        String nome = "peticao.docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(docxExportService.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .body(docx);
    }
}
