package com.tcc.pjb.backend.controller.processual.painel.contextual;

import com.tcc.pjb.backend.model.dto.processual.painel.contextual.ProcessoPainelContextualResponse;
import com.tcc.pjb.backend.service.processual.painel.ProcessoPainelContextualFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoPainelContextualController {

    private final ProcessoPainelContextualFacadeService facadeService;

    public ProcessoPainelContextualController(ProcessoPainelContextualFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/painel-contextual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelContextualResponse> painel(@PathVariable Long processoId,
                                                                    @RequestParam(name = "profileCode", required = false) String profileCode) {
        return ResponseEntity.ok(facadeService.painel(processoId, profileCode));
    }
}
