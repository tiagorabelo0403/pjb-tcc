package com.tcc.pjb.backend.controller.mni;

import com.tcc.pjb.backend.integration.mni.application.MniDocumentoIngestaoService;
import com.tcc.pjb.backend.model.dto.mni.ConfirmarClassificacaoDocumentoRequest;
import com.tcc.pjb.backend.model.dto.mni.DocumentoClassificadoResponse;
import com.tcc.pjb.backend.model.dto.mni.DocumentoPendenteClassificacaoDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fila de documentos importados via MNI cujo tipoDocumento não pôde ser resolvido
 * automaticamente por palavra-chave — um servidor confirma a classificação antes do documento
 * ser tratado como peça oficial classificada. Ver {@link MniDocumentoIngestaoService}.
 */
@RestController
@RequestMapping("/api/v1/mni/documentos")
@PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','ADMINISTRADOR')")
public class MniDocumentoClassificacaoController {

    private final MniDocumentoIngestaoService documentoIngestaoService;

    public MniDocumentoClassificacaoController(MniDocumentoIngestaoService documentoIngestaoService) {
        this.documentoIngestaoService = Objects.requireNonNull(documentoIngestaoService);
    }

    @GetMapping("/pendentes-classificacao")
    public ResponseEntity<List<DocumentoPendenteClassificacaoDto>> listarPendentes() {
        List<DocumentoPendenteClassificacaoDto> pendentes = documentoIngestaoService.listarPendentesDeClassificacao()
                .stream()
                .map(DocumentoPendenteClassificacaoDto::from)
                .toList();
        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/{documentoId}/classificacao")
    public ResponseEntity<DocumentoClassificadoResponse> confirmarClassificacao(
            @PathVariable UUID documentoId,
            @Valid @RequestBody ConfirmarClassificacaoDocumentoRequest request) {
        var documento = documentoIngestaoService.confirmarClassificacao(documentoId, request.tipoDocumento());
        return ResponseEntity.ok(DocumentoClassificadoResponse.from(documento));
    }
}
