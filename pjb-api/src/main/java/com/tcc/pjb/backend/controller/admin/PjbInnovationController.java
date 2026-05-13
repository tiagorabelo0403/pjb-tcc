package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewRequest;
import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.innovation.PjbInnovationCapabilityService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/innovation")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','MAGISTRADO','MINISTRO','DESEMBARGADOR')")
public class PjbInnovationController {

    private final PjbInnovationCapabilityService service;
    private final SurfaceProjectionSupport projectionSupport;

    public PjbInnovationController(PjbInnovationCapabilityService service,
                                   SurfaceProjectionSupport projectionSupport) {
        this.service = Objects.requireNonNull(service);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    @GetMapping("/superiority-capabilities")
    public ResponseEntity<SurfaceSnapshotResponse> superiorityCapabilities() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.superiority-capabilities", service.superiorityCapabilities()));
    }

    @GetMapping("/interoperability-mesh")
    public ResponseEntity<SurfaceSnapshotResponse> interoperabilityMesh() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.interoperability-mesh", service.interoperabilityMesh()));
    }

    @GetMapping("/legacy-superiority-delta")
    public ResponseEntity<SurfaceSnapshotResponse> legacySuperiorityDelta() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.legacy-superiority-delta", service.legacySuperiorityDelta()));
    }

    @GetMapping("/migration-hygiene-rules")
    public ResponseEntity<SurfaceSnapshotResponse> migrationHygieneRules() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.migration-hygiene-rules", service.migrationHygieneRules()));
    }

    @GetMapping("/digital-hearing-intelligence")
    public ResponseEntity<SurfaceSnapshotResponse> digitalHearingIntelligence() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.digital-hearing-intelligence", service.digitalHearingIntelligence()));
    }

    @GetMapping("/connector-secretariat-governance")
    public ResponseEntity<SurfaceSnapshotResponse> connectorSecretariatGovernance() {
        return ResponseEntity.ok(projectionSupport.snapshot("admin.innovation.connector-secretariat-governance", service.connectorSecretariatGovernance()));
    }

    @PostMapping("/migration-hygiene/preview")
    public ResponseEntity<PjbMigrationHygienePreviewResponse> migrationHygienePreview(@Valid @RequestBody PjbMigrationHygienePreviewRequest request) {
        return ResponseEntity.ok(service.previewMigrationHygiene(request));
    }
}
