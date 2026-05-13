package com.tcc.pjb.backend.controller.processual.playbook.operational;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoPlaybookOperacionalController {

    private final NationalProceduralOperationalPlaybookService playbookService;

    public ProcessoPlaybookOperacionalController(NationalProceduralOperationalPlaybookService playbookService) {
        this.playbookService = playbookService;
    }

    @GetMapping("/playbook-operacional-ritos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalProceduralOperationalPlaybookSnapshot> listarPlaybooks() {
        return ResponseEntity.ok(playbookService.snapshot());
    }

    @GetMapping("/playbook-operacional-ritos/{rito}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalProceduralOperationalPlaybookRow> detalharPlaybook(@PathVariable String rito) {
        return ResponseEntity.ok(playbookService.describe(rito));
    }
}
