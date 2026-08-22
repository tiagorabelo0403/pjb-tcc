package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.EditorBootstrapResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.editor.PeticaoEditorBootstrapService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato único de abertura do editor de peça para o frontend: uma chamada devolve, tipado, tudo
 * o que o editor precisa para renderizar (formatação permitida, identidade visual resolvida do ator,
 * endpoints/limites de rascunho e mídia).
 */
@RestController
@RequestMapping("/api/v1/peticionamento/editor")
@PreAuthorize("isAuthenticated()")
public class PeticaoEditorBootstrapController {

    private final PeticaoEditorBootstrapService service;

    public PeticaoEditorBootstrapController(PeticaoEditorBootstrapService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<EditorBootstrapResponse> bootstrap() {
        return ResponseEntity.ok(service.bootstrap());
    }
}
