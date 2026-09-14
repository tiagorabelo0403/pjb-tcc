package com.tcc.pjb.backend.controller.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.AutosaveRascunhoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.DraftVersaoPreviewResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.DraftVersaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho.RascunhoConteudoResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.rascunho.PeticaoDraftVersionamentoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/peticionamento/inicial/rascunhos/{draftId}")
@PreAuthorize("isAuthenticated()")
public class PeticaoRascunhoVersaoController {

    private final PeticaoDraftVersionamentoService service;

    public PeticaoRascunhoVersaoController(PeticaoDraftVersionamentoService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @PutMapping("/autosave")
    public ResponseEntity<RascunhoConteudoResponse> autosalvar(@PathVariable Long draftId,
                                                               @Valid @RequestBody AutosaveRascunhoRequest request) {
        return ResponseEntity.ok(service.autosalvar(draftId, request));
    }

    @GetMapping("/versoes")
    public ResponseEntity<List<DraftVersaoResponse>> versoes(@PathVariable Long draftId) {
        return ResponseEntity.ok(service.listarVersoes(draftId));
    }

    @GetMapping("/versoes/{versaoSeq}")
    public ResponseEntity<DraftVersaoPreviewResponse> previsualizarVersao(@PathVariable Long draftId,
                                                                          @PathVariable int versaoSeq) {
        return ResponseEntity.ok(service.previsualizarVersao(draftId, versaoSeq));
    }

    @PostMapping("/versoes/{versaoSeq}/restaurar")
    public ResponseEntity<RascunhoConteudoResponse> restaurar(@PathVariable Long draftId,
                                                              @PathVariable int versaoSeq) {
        return ResponseEntity.ok(service.restaurar(draftId, versaoSeq));
    }
}
