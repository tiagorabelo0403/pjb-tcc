package com.tcc.pjb.backend.controller.processual.linkage;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageAnalysisRequest;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageAnalysisResponse;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageApplyRequest;
import com.tcc.pjb.backend.service.processual.linkage.ProcessoLinkageGovernanceService;

@RestController
@RequestMapping("/api/v1/processual/linkage")
public class ProcessoLinkageGovernanceController {

    private final ProcessoLinkageGovernanceService service;

    public ProcessoLinkageGovernanceController(ProcessoLinkageGovernanceService service) {
        this.service = service;
    }

    @PostMapping("/analisar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoLinkageAnalysisResponse> analisar(@Valid @RequestBody ProcessoLinkageAnalysisRequest request) {
        return ResponseEntity.ok(service.analisar(request));
    }

    @PostMapping("/aplicar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoLinkageAnalysisResponse> aplicar(@Valid @RequestBody ProcessoLinkageApplyRequest request) {
        return ResponseEntity.ok(service.aplicar(request));
    }
}
