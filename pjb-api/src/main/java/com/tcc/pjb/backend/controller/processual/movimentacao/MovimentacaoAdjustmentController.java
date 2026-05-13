package com.tcc.pjb.backend.controller.processual.movimentacao;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.processual.movimentacao.MovimentacaoAdjustmentRequest;
import com.tcc.pjb.backend.model.dto.processual.movimentacao.MovimentacaoAdjustmentResponse;
import com.tcc.pjb.backend.service.processual.movimentacao.MovimentacaoAdjustmentService;

@RestController
@RequestMapping("/api/v1/processual/movimentacoes")
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_SERVIDOR_FORUM','ROLE_MAGISTRADO','ROLE_JUIZ','ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class MovimentacaoAdjustmentController {

    private final MovimentacaoAdjustmentService service;

    public MovimentacaoAdjustmentController(MovimentacaoAdjustmentService service) {
        this.service = service;
    }

    @PostMapping("/processos/{processoId}/{movimentacaoId}/ajuste")
    public ResponseEntity<MovimentacaoAdjustmentResponse> ajustar(@PathVariable Long processoId,
                                                                  @PathVariable Long movimentacaoId,
                                                                  @RequestBody MovimentacaoAdjustmentRequest request) {
        return ResponseEntity.ok(service.ajustar(processoId, movimentacaoId, request));
    }

    @GetMapping("/processos/{processoId}/ajustes")
    public ResponseEntity<List<MovimentacaoAdjustmentResponse>> historico(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.historico(processoId));
    }
}
