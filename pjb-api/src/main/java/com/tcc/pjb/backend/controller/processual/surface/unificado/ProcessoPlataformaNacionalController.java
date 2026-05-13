package com.tcc.pjb.backend.controller.processual.surface.unificado;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoPlataformaNacionalController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoPlataformaNacionalController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/operacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> operacao(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.operacao(processoId));
    }

    @GetMapping("/busca")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> busca(@RequestParam(required = false) String cpf,
                                                                  @RequestParam(required = false) String nome,
                                                                  @RequestParam(required = false) String numero,
                                                                  @RequestParam(required = false) String uf,
                                                                  @RequestParam(required = false) String comarca,
                                                                  @RequestParam(required = false) String ramo,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String tribunal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(facadeService.busca(cpf, nome, numero, uf, comarca, ramo, status, tribunal, page, size));
    }

    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> analytics(@RequestParam(required = false) String ramo,
                                                                      @RequestParam(required = false) String tribunal,
                                                                      @RequestParam(required = false) String uf,
                                                                      @RequestParam(required = false) String comarca) {
        return ResponseEntity.ok(facadeService.analytics(ramo, tribunal, uf, comarca));
    }

    @GetMapping("/{processoId}/encaixe-final")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> encaixeFinal(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.encaixeFinal(processoId));
    }

    @GetMapping("/encaixe-final")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> encaixeFinalCarteira(@RequestParam(defaultValue = "25") int limit) {
        return ResponseEntity.ok(facadeService.encaixeFinalCarteira(limit));
    }
}
