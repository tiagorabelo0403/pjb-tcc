package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.infra.ScaleArchitectureCachePolicyRequest;
import com.tcc.pjb.backend.model.dto.infra.ScaleArchitecturePartitionPlanRequest;
import com.tcc.pjb.backend.model.dto.infra.ScaleArchitectureReadModelRecompositionRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.infra.surface.ScaleArchitectureSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scale-architecture")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','MAGISTRADO','MINISTRO','DESEMBARGADOR')")
public class ScaleArchitectureController {

    private final ScaleArchitectureSurfaceFacadeService facadeService;

    public ScaleArchitectureController(ScaleArchitectureSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/cache-policies")
    public ResponseEntity<SurfaceCollectionResponse> listarCachePolicies() {
        return ResponseEntity.ok(facadeService.listarCachePolicies());
    }

    @PostMapping("/cache-policies")
    public ResponseEntity<SurfaceSnapshotResponse> salvarCachePolicy(@Valid @RequestBody ScaleArchitectureCachePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.salvarCachePolicy(request));
    }

    @GetMapping("/partition-plans")
    public ResponseEntity<SurfaceCollectionResponse> listarPartitionPlans() {
        return ResponseEntity.ok(facadeService.listarPartitionPlans());
    }

    @GetMapping("/adaptive-data-plane")
    public ResponseEntity<SurfaceSnapshotResponse> adaptiveDataPlane() {
        return ResponseEntity.ok(facadeService.adaptiveDataPlane());
    }

    @GetMapping("/judicial-scale-profiles")
    public ResponseEntity<SurfaceSnapshotResponse> judicialScaleProfiles() {
        return ResponseEntity.ok(facadeService.judicialScaleProfiles());
    }

    @GetMapping("/judicial-runtime-policies")
    public ResponseEntity<SurfaceSnapshotResponse> judicialRuntimePolicies() {
        return ResponseEntity.ok(facadeService.judicialRuntimePolicies());
    }

    @GetMapping("/judicial-secretariat-models")
    public ResponseEntity<SurfaceSnapshotResponse> judicialSecretariatModels() {
        return ResponseEntity.ok(facadeService.judicialSecretariatModels());
    }

    @GetMapping("/judicial-operational-desks")
    public ResponseEntity<SurfaceSnapshotResponse> judicialOperationalDesks() {
        return ResponseEntity.ok(facadeService.judicialOperationalDesks());
    }

    @GetMapping("/judicial-operational-actions")
    public ResponseEntity<SurfaceSnapshotResponse> judicialOperationalActions() {
        return ResponseEntity.ok(facadeService.judicialOperationalActions());
    }

    @GetMapping("/judicial-operational-transactions")
    public ResponseEntity<SurfaceSnapshotResponse> judicialOperationalTransactions() {
        return ResponseEntity.ok(facadeService.judicialOperationalTransactions());
    }

    @GetMapping("/judicial-procedural-coverage")
    public ResponseEntity<SurfaceSnapshotResponse> judicialProceduralCoverage() {
        return ResponseEntity.ok(facadeService.judicialProceduralCoverage());
    }

    @GetMapping("/judicial-procedural-coverage/{rito}")
    public ResponseEntity<SurfaceSnapshotResponse> judicialProceduralCoverageDetail(@PathVariable String rito) {
        return ResponseEntity.ok(facadeService.judicialProceduralCoverageDetail(rito));
    }


    @GetMapping("/judicial-procedural-playbooks")
    public ResponseEntity<SurfaceSnapshotResponse> judicialProceduralPlaybooks() {
        return ResponseEntity.ok(facadeService.judicialProceduralPlaybook());
    }

    @GetMapping("/judicial-procedural-playbooks/{rito}")
    public ResponseEntity<SurfaceSnapshotResponse> judicialProceduralPlaybookDetail(@PathVariable String rito) {
        return ResponseEntity.ok(facadeService.judicialProceduralPlaybookDetail(rito));
    }

    @GetMapping("/judicial-tribunal-variations")
    public ResponseEntity<SurfaceSnapshotResponse> judicialTribunalVariations() {
        return ResponseEntity.ok(facadeService.judicialTribunalVariations());
    }

    @GetMapping("/judicial-tribunal-variations/{tribunalCodigo}/{rito}")
    public ResponseEntity<SurfaceSnapshotResponse> judicialTribunalVariationDetail(@PathVariable String tribunalCodigo,
                                                                                   @PathVariable String rito,
                                                                                   @RequestParam(required = false) String unidadeCodigo,
                                                                                   @RequestParam(required = false) String tipoJustica) {
        return ResponseEntity.ok(facadeService.judicialTribunalVariationDetail(tribunalCodigo, rito, unidadeCodigo, tipoJustica));
    }

    @GetMapping("/judicial-institutional-alignment")
    public ResponseEntity<SurfaceSnapshotResponse> judicialInstitutionalAlignment() {
        return ResponseEntity.ok(facadeService.judicialInstitutionalAlignment());
    }

    @GetMapping("/database-runtime-posture")
    public ResponseEntity<SurfaceSnapshotResponse> databaseRuntimePosture() {
        return ResponseEntity.ok(facadeService.databaseRuntimePosture());
    }

    @GetMapping("/processual-read-models")
    public ResponseEntity<SurfaceSnapshotResponse> processualReadModels() {
        return ResponseEntity.ok(facadeService.processualReadModels());
    }

    @GetMapping("/processual-read-models/persistence")
    public ResponseEntity<SurfaceSnapshotResponse> processualReadModelsPersistence() {
        return ResponseEntity.ok(facadeService.processualReadModelsPersistence());
    }

    @PostMapping("/processual-read-models/recomposition")
    public ResponseEntity<SurfaceSnapshotResponse> enqueueProcessualReadModelRecomposition(@Valid @RequestBody ScaleArchitectureReadModelRecompositionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.enqueueProcessualReadModelRecomposition(request));
    }

    @PostMapping("/partition-plans")
    public ResponseEntity<SurfaceSnapshotResponse> salvarPartitionPlan(@Valid @RequestBody ScaleArchitecturePartitionPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.salvarPartitionPlan(request));
    }

    @GetMapping("/partition-plans/{tableName}/preview")
    public ResponseEntity<SurfaceSnapshotResponse> preview(@PathVariable String tableName) {
        return ResponseEntity.ok(facadeService.preview(tableName));
    }

    @PostMapping("/partition-plans/{tableName}/materialize")
    public ResponseEntity<SurfaceSnapshotResponse> materialize(@PathVariable String tableName) {
        return ResponseEntity.ok(facadeService.materialize(tableName));
    }
}
