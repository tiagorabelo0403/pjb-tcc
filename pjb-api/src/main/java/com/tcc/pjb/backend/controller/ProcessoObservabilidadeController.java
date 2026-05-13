package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoVisibilidadeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processos")
@PreAuthorize("isAuthenticated()")
public class ProcessoObservabilidadeController {

    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final ProcessoObservabilidadeAcessoService observabilidadeAcessoService;

    public ProcessoObservabilidadeController(ProcessoAccessApplicationService processoAccessApplicationService,
                                             ProcessoObservabilidadeAcessoService observabilidadeAcessoService) {
        this.processoAccessApplicationService = Objects.requireNonNull(processoAccessApplicationService);
        this.observabilidadeAcessoService = Objects.requireNonNull(observabilidadeAcessoService);
    }

    @GetMapping("/{processoId}/acompanhamento/acessos")
    public ResponseEntity<ProcessoAcessoVisibilidadeResponse> acompanhamentoAcessos(@PathVariable Long processoId) {
        Processo processo = processoAccessApplicationService.loadCompletoAndRequireRead(processoId);
        return ResponseEntity.ok(observabilidadeAcessoService.resumir(processo));
    }
}
