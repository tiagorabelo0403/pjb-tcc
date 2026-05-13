package com.tcc.pjb.backend.controller.processual.painel.detalhe;

import com.tcc.pjb.backend.model.dto.processual.painel.trabalhista.ProcessoPainelBndtResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.fonte.ProcessoPainelFonteOficialResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.previdenciario.ProcessoPainelPrevidenciarioTrilhoResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.rota.ProcessoPainelRotaTaticaResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.telemetria.ProcessoPainelTelemetriaConectorResponse;
import com.tcc.pjb.backend.service.processual.painel.ProcessoPainelContextualFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoPainelContextualDetalheController {

    private final ProcessoPainelContextualFacadeService facadeService;

    public ProcessoPainelContextualDetalheController(ProcessoPainelContextualFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/painel-contextual/telemetria-conectores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelTelemetriaConectorResponse> telemetria(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.telemetria(processoId));
    }

    @GetMapping("/{processoId}/painel-contextual/fontes-oficiais")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelFonteOficialResponse> fontesOficiais(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.fontesOficiais(processoId));
    }

    @GetMapping("/{processoId}/painel-contextual/trabalhista/bndt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelBndtResponse> bndt(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.bndt(processoId));
    }

    @GetMapping("/{processoId}/painel-contextual/previdenciario/trilho")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelPrevidenciarioTrilhoResponse> trilhoPrevidenciario(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.trilhoPrevidenciario(processoId));
    }

    @GetMapping("/{processoId}/painel-contextual/rota-tatica")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoPainelRotaTaticaResponse> rotaTatica(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.rotaTatica(processoId));
    }
}
