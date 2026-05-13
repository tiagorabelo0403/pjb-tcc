package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.FunctionalAuthorityEvaluationRequest;
import com.tcc.pjb.backend.model.dto.governance.FunctionalAuthorityEvaluationResponse;
import com.tcc.pjb.backend.service.governance.SegregacaoFuncionalProcessualService;

@RestController
@RequestMapping("/api/v1/admin/governance/functional-authority")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
public class FunctionalAuthorityMatrixController {

    private final SegregacaoFuncionalProcessualService service;

    public FunctionalAuthorityMatrixController(SegregacaoFuncionalProcessualService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<FunctionalAuthorityEvaluationResponse> evaluate(@Valid @RequestBody FunctionalAuthorityEvaluationRequest request) {
        return ResponseEntity.ok(service.avaliar(
                request.processoId(),
                request.operacao(),
                request.stepUpAtivo(),
                request.duplaAprovacaoAtiva(),
                request.revisaoIndependenteAtiva(),
                request.justificativaRegistrada(),
                request.finalidadeDeclarada()
        ));
    }
}
