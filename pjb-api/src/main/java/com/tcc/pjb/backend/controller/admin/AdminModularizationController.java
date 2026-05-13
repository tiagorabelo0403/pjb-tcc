package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbAggregatorActivationApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbCoreExtractionPlannerApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbCoreSeedExtractionApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleScaffoldApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/modularization")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminModularizationController {

    private final PjbModuleBoundaryReadinessApplicationService applicationService;
    private final PjbCoreExtractionPlannerApplicationService coreExtractionPlannerApplicationService;
    private final PjbModuleScaffoldApplicationService moduleScaffoldApplicationService;
    private final PjbCoreSeedExtractionApplicationService coreSeedExtractionApplicationService;
    private final PjbAggregatorActivationApplicationService aggregatorActivationApplicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminModularizationController(PjbModuleBoundaryReadinessApplicationService applicationService,
                                         PjbCoreExtractionPlannerApplicationService coreExtractionPlannerApplicationService,
                                         PjbModuleScaffoldApplicationService moduleScaffoldApplicationService,
                                         PjbCoreSeedExtractionApplicationService coreSeedExtractionApplicationService,
                                         PjbAggregatorActivationApplicationService aggregatorActivationApplicationService,
                                         ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.coreExtractionPlannerApplicationService = coreExtractionPlannerApplicationService;
        this.moduleScaffoldApplicationService = moduleScaffoldApplicationService;
        this.coreSeedExtractionApplicationService = coreSeedExtractionApplicationService;
        this.aggregatorActivationApplicationService = aggregatorActivationApplicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> snapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.snapshot(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }

    @GetMapping("/packages")
    public ResponseEntity<ApiQueryResponse<?>> packages() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.packages(), List.of()));
    }

    @GetMapping("/phases")
    public ResponseEntity<ApiQueryResponse<?>> phases() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.phases(), List.of()));
    }

    @GetMapping("/core-extraction/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> coreExtractionSnapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreExtractionPlannerApplicationService.snapshot(), List.of()));
    }

    @GetMapping("/core-extraction/candidates")
    public ResponseEntity<ApiQueryResponse<?>> coreExtractionCandidates() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreExtractionPlannerApplicationService.candidates(), List.of()));
    }

    @GetMapping("/core-extraction/dependencies")
    public ResponseEntity<ApiQueryResponse<?>> coreExtractionDependencies() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreExtractionPlannerApplicationService.dependencies(), List.of()));
    }

    @GetMapping("/core-extraction/pom-preview")
    public ResponseEntity<ApiQueryResponse<?>> coreExtractionPomPreview() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreExtractionPlannerApplicationService.pomPreview(), List.of()));
    }

    @GetMapping("/core-extraction/move-plan")
    public ResponseEntity<ApiQueryResponse<?>> coreExtractionMovePlan() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreExtractionPlannerApplicationService.movePlan(), List.of()));
    }

    @GetMapping("/scaffold/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> scaffoldSnapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(moduleScaffoldApplicationService.snapshot(), List.of()));
    }

    @GetMapping("/scaffold/module-poms")
    public ResponseEntity<ApiQueryResponse<?>> scaffoldModulePoms() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(moduleScaffoldApplicationService.modulePoms(), List.of()));
    }

    @GetMapping("/scaffold/directories")
    public ResponseEntity<ApiQueryResponse<?>> scaffoldDirectories() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(moduleScaffoldApplicationService.directories(), List.of()));
    }

    @GetMapping("/scaffold/build-order")
    public ResponseEntity<ApiQueryResponse<?>> scaffoldBuildOrder() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(moduleScaffoldApplicationService.buildOrder(), List.of()));
    }


    @GetMapping("/core-seed/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> coreSeedSnapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreSeedExtractionApplicationService.snapshot(), List.of()));
    }

    @GetMapping("/core-seed/mirrors")
    public ResponseEntity<ApiQueryResponse<?>> coreSeedMirrors() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreSeedExtractionApplicationService.mirrors(), List.of()));
    }

    @GetMapping("/core-seed/drift")
    public ResponseEntity<ApiQueryResponse<?>> coreSeedDrift() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreSeedExtractionApplicationService.drift(), List.of()));
    }

    @GetMapping("/core-seed/parity")
    public ResponseEntity<ApiQueryResponse<?>> coreSeedParity() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(coreSeedExtractionApplicationService.parity(), List.of()));
    }

    @GetMapping("/aggregator/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> aggregatorSnapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(aggregatorActivationApplicationService.snapshot(), List.of()));
    }

    @GetMapping("/aggregator/module-links")
    public ResponseEntity<ApiQueryResponse<?>> aggregatorModuleLinks() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(aggregatorActivationApplicationService.moduleLinks(), List.of()));
    }

    @GetMapping("/aggregator/checklist")
    public ResponseEntity<ApiQueryResponse<?>> aggregatorChecklist() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(aggregatorActivationApplicationService.checklist(), List.of()));
    }

    @GetMapping("/aggregator/pom-patch")
    public ResponseEntity<ApiQueryResponse<?>> aggregatorPomPatch() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(aggregatorActivationApplicationService.pomPatch(), List.of()));
    }
}
