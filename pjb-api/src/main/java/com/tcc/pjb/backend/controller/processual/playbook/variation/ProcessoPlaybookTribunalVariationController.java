package com.tcc.pjb.backend.controller.processual.playbook.variation;

import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoPlaybookTribunalVariationController {

    private final NationalProceduralTribunalVariationService tribunalVariationService;

    public ProcessoPlaybookTribunalVariationController(NationalProceduralTribunalVariationService tribunalVariationService) {
        this.tribunalVariationService = tribunalVariationService;
    }

    @GetMapping("/variacoes-tribunal-unidade/{tribunalCodigo}/{rito}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalProceduralTribunalVariationRow> detalharVariacao(@PathVariable String tribunalCodigo,
                                                                                    @PathVariable String rito,
                                                                                    @RequestParam(required = false) String unidadeCodigo,
                                                                                    @RequestParam(required = false) String tipoJustica) {
        return ResponseEntity.ok(tribunalVariationService.describe(tribunalCodigo, unidadeCodigo, rito, tipoJustica));
    }
}
