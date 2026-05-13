package com.tcc.pjb.backend.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.DecisionTrailResponse;
import com.tcc.pjb.backend.model.dto.governance.DecisionTrailSnapshotRequest;
import com.tcc.pjb.backend.service.governance.ProcessoDecisionTrailService;

@RestController
@RequestMapping("/api/v1/admin/governance/decision-trail")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
public class ProcessoDecisionTrailController {

    private final ProcessoDecisionTrailService service;

    public ProcessoDecisionTrailController(ProcessoDecisionTrailService service) {
        this.service = service;
    }

    @GetMapping("/{processoId}")
    public ResponseEntity<DecisionTrailResponse> timeline(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.timeline(processoId));
    }

    @PostMapping("/snapshot")
    public ResponseEntity<DecisionTrailResponse.DecisionTrailEntryView> snapshot(@Valid @RequestBody DecisionTrailSnapshotRequest request) {
        return ResponseEntity.ok(service.registrarSnapshot(
                request.processoId(),
                request.decisionType(),
                request.confidence(),
                request.reasonsJson(),
                request.citationsJson(),
                request.inputDigest(),
                request.outputDigest(),
                request.modelVersion(),
                request.metadataJson()
        ));
    }
}
