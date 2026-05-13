package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.SensitiveDataAccessRequest;
import com.tcc.pjb.backend.model.dto.governance.SensitiveDataAccessResponse;
import com.tcc.pjb.backend.service.governance.SensitiveDataAccessControlService;

@RestController
@RequestMapping("/api/v1/admin/governance/sensitive-access")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM','ADVOGADO','DEFENSOR_PUBLICO','PROCURADOR','MEMBRO_MINISTERIO_PUBLICO')")
public class SensitiveDataAccessControlController {

    private final SensitiveDataAccessControlService service;

    public SensitiveDataAccessControlController(SensitiveDataAccessControlService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<SensitiveDataAccessResponse> evaluate(@Valid @RequestBody SensitiveDataAccessRequest request) {
        return ResponseEntity.ok(service.avaliar(
                request.processoId(),
                request.acessoExcepcional(),
                request.stepUpAtivo(),
                request.justificativaRegistrada(),
                request.duplaAprovacaoAtiva(),
                request.finalidadeDeclarada()
        ));
    }
}
