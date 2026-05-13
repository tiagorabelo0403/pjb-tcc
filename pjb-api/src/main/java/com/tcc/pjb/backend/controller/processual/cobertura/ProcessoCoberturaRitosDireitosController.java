package com.tcc.pjb.backend.controller.processual.cobertura;

import com.tcc.pjb.backend.model.dto.processual.cobertura.ProcessoProceduralCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.cobertura.ProcessoProceduralGuaranteeResponse;
import com.tcc.pjb.backend.service.processual.cobertura.ProcessoCoberturaRitosDireitosFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoCoberturaRitosDireitosController {

    private final ProcessoCoberturaRitosDireitosFacadeService facadeService;

    public ProcessoCoberturaRitosDireitosController(ProcessoCoberturaRitosDireitosFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/cobertura-ritos-direitos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoProceduralCoverageResponse> coberturaCompleta() {
        return ResponseEntity.ok(facadeService.coberturaCompleta());
    }

    @GetMapping("/cobertura-ritos-direitos/{rito}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoProceduralGuaranteeResponse> detalhar(@PathVariable String rito) {
        return ResponseEntity.ok(facadeService.detalhar(rito));
    }
}
