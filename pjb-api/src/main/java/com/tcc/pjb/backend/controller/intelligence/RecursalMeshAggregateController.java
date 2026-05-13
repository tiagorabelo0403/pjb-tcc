package com.tcc.pjb.backend.controller.intelligence;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshAggregateView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshConsistencyView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshLedgerView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSearchResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshReindexRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshReindexResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshOperationalAlertReport;
import com.tcc.pjb.backend.service.recursal.RecursalContextualAccessService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshTransactionalService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshDashboardService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshIndexDriftService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshSearchReindexService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshSearchService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshOperationalAlertService;

@RestController
@RequestMapping("/api/v1/intelligence/recursal/mesh/aggregate")
@Validated
@PreAuthorize("isAuthenticated()")
public class RecursalMeshAggregateController {

    private static final java.util.Set<String> RECURSAL_INDEX_ADMIN_AUTHORITIES = java.util.Set.of(
            "ROLE_ADMINISTRADOR",
            "ROLE_ADMIN",
            "ROLE_SERVIDOR"
    );

    private final NationalRecursalMeshTransactionalService service;
    private final RecursalContextualAccessService accessService;
    private final RecursalMeshSearchService searchService;
    private final RecursalMeshDashboardService dashboardService;
    private final RecursalMeshSearchReindexService reindexService;
    private final RecursalMeshIndexDriftService driftService;
    private final RecursalMeshOperationalAlertService operationalAlertService;

    public RecursalMeshAggregateController(NationalRecursalMeshTransactionalService service,
                                           RecursalContextualAccessService accessService,
                                           RecursalMeshSearchService searchService,
                                           RecursalMeshDashboardService dashboardService,
                                           RecursalMeshSearchReindexService reindexService,
                                           RecursalMeshIndexDriftService driftService,
                                           RecursalMeshOperationalAlertService operationalAlertService) {
        this.service = service;
        this.accessService = accessService;
        this.searchService = searchService;
        this.dashboardService = dashboardService;
        this.reindexService = reindexService;
        this.driftService = driftService;
        this.operationalAlertService = operationalAlertService;
    }

    @PostMapping("/open")
    public ResponseEntity<RecursalMeshAggregateView> open(
            @Valid @RequestBody RecursalMeshPlanRequest request,
            @RequestHeader(value = "X-Recursal-Actor", required = false) String actor) {
        accessService.requireWriteProcesso(request.context().processoId());
        return ResponseEntity.ok(service.openAggregate(request, actor));
    }

    @PostMapping("/transition")
    public ResponseEntity<RecursalTransitionResult> transition(@Valid @RequestBody RecursalMeshTransitionRequest request) {
        accessService.requireWriteProcesso(request.context().processoId());
        return ResponseEntity.ok(service.transition(request));
    }

    @GetMapping("/{recursoId}")
    public ResponseEntity<RecursalMeshAggregateView> find(@PathVariable String recursoId) {
        return service.findAggregate(recursoId)
                .map(view -> {
                    accessService.requireReadProcesso(view.processoId());
                    return ResponseEntity.ok(view);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{recursoId}/ledger")
    public ResponseEntity<List<RecursalMeshLedgerView>> ledger(@PathVariable String recursoId) {
        service.findAggregate(recursoId).ifPresent(view -> accessService.requireReadProcesso(view.processoId()));
        return ResponseEntity.ok(service.findLedger(recursoId));
    }

    @GetMapping("/{recursoId}/consistency")
    public ResponseEntity<RecursalMeshConsistencyView> consistency(@PathVariable String recursoId) {
        service.findAggregate(recursoId).ifPresent(view -> accessService.requireReadProcesso(view.processoId()));
        return service.verifyConsistency(recursoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/processo/{processoId}")
    public ResponseEntity<List<RecursalMeshProcessLinkView>> byProcesso(@PathVariable Long processoId) {
        accessService.requireReadProcesso(processoId);
        return ResponseEntity.ok(service.findByProcesso(processoId));
    }

    @PostMapping("/search")
    public ResponseEntity<RecursalMeshSearchResponse> search(@Valid @RequestBody RecursalMeshSearchRequest request) {
        return ResponseEntity.ok(searchService.search(accessService.scopeSearchRequest(request)));
    }

    @PostMapping("/dashboard")
    public ResponseEntity<RecursalMeshDashboardResponse> dashboard(@Valid @RequestBody RecursalMeshDashboardRequest request) {
        return ResponseEntity.ok(dashboardService.dashboard(accessService.scopeDashboardRequest(request)));
    }

    @PostMapping("/search/reindex")
    public ResponseEntity<RecursalMeshReindexResponse> reindex(@Valid @RequestBody RecursalMeshReindexRequest request) {
        requireRecursalIndexAdministration();
        return ResponseEntity.ok(reindexService.reindex(request));
    }

    @GetMapping("/search/drift")
    public ResponseEntity<RecursalMeshIndexDriftReport> drift(@RequestParam(name = "sampleSize", required = false) Integer sampleSize) {
        requireRecursalIndexAdministration();
        return ResponseEntity.ok(driftService.assess(sampleSize));
    }

    @GetMapping("/operations/alerts")
    public ResponseEntity<RecursalMeshOperationalAlertReport> alerts(@RequestParam(name = "scanLimit", required = false) Integer scanLimit,
                                                                     @RequestParam(name = "bucketLimit", required = false) Integer bucketLimit) {
        requireRecursalIndexAdministration();
        return ResponseEntity.ok(operationalAlertService.report(scanLimit, bucketLimit));
    }

    private void requireRecursalIndexAdministration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AccessDeniedException("Administração de índice recursal exige autoridade operacional elevada.");
        }
        boolean granted = authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .anyMatch(RECURSAL_INDEX_ADMIN_AUTHORITIES::contains);
        if (!granted) {
            throw new AccessDeniedException("Administração de índice recursal exige autoridade operacional elevada.");
        }
    }
}
