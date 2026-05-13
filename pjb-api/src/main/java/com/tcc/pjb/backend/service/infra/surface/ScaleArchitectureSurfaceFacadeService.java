package com.tcc.pjb.backend.service.infra.surface;

import com.tcc.pjb.backend.model.dto.infra.ScaleArchitectureCachePolicyRequest;
import com.tcc.pjb.backend.model.dto.infra.ScaleArchitecturePartitionPlanRequest;
import com.tcc.pjb.backend.model.dto.infra.ScaleArchitectureReadModelRecompositionRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.infra.ScaleArchitectureService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ScaleArchitectureSurfaceFacadeService {

    private final ScaleArchitectureService scaleArchitectureService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public ScaleArchitectureSurfaceFacadeService(ScaleArchitectureService scaleArchitectureService,
                                                 SurfaceProjectionSupport surfaceProjectionSupport) {
        this.scaleArchitectureService = Objects.requireNonNull(scaleArchitectureService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceCollectionResponse listarCachePolicies() {
        return surfaceProjectionSupport.collection("admin.scale-architecture.cache-policies", scaleArchitectureService.listarPoliticasCache());
    }

    public SurfaceSnapshotResponse salvarCachePolicy(ScaleArchitectureCachePolicyRequest request) {
        return surfaceProjectionSupport.snapshot(
                "admin.scale-architecture.cache-policies.salvar",
                scaleArchitectureService.salvarPoliticaCache(
                        new ScaleArchitectureService.CachePolicyRequest(
                                request.cacheName(),
                                request.roleName(),
                                request.ttlSeconds(),
                                request.staleWhileRevalidateSeconds(),
                                request.enabled(),
                                request.notes()
                        )
                )
        );
    }

    public SurfaceCollectionResponse listarPartitionPlans() {
        return surfaceProjectionSupport.collection("admin.scale-architecture.partition-plans", scaleArchitectureService.listarPlanosParticao());
    }

    public SurfaceSnapshotResponse adaptiveDataPlane() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.adaptive-data-plane", scaleArchitectureService.adaptiveDataPlaneView());
    }

    public SurfaceSnapshotResponse judicialScaleProfiles() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-scale-profiles", scaleArchitectureService.judicialScaleProfilesView());
    }

    public SurfaceSnapshotResponse judicialRuntimePolicies() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-runtime-policies", scaleArchitectureService.judicialRuntimePoliciesView());
    }

    public SurfaceSnapshotResponse judicialSecretariatModels() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-secretariat-models", scaleArchitectureService.judicialSecretariatModelsView());
    }

    public SurfaceSnapshotResponse judicialOperationalDesks() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-operational-desks", scaleArchitectureService.judicialOperationalDesksView());
    }

    public SurfaceSnapshotResponse judicialOperationalActions() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-operational-actions", scaleArchitectureService.judicialOperationalActionsView());
    }

    public SurfaceSnapshotResponse judicialOperationalTransactions() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-operational-transactions", scaleArchitectureService.judicialOperationalTransactionsView());
    }

    public SurfaceSnapshotResponse judicialProceduralCoverage() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-procedural-coverage", scaleArchitectureService.judicialProceduralCoverageView());
    }

    public SurfaceSnapshotResponse judicialProceduralCoverageDetail(String rito) {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-procedural-coverage.detail", scaleArchitectureService.judicialProceduralCoverageDetailView(rito));
    }


    public SurfaceSnapshotResponse judicialProceduralPlaybook() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-procedural-playbooks", scaleArchitectureService.judicialProceduralPlaybookView());
    }

    public SurfaceSnapshotResponse judicialProceduralPlaybookDetail(String rito) {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-procedural-playbooks.detail", scaleArchitectureService.judicialProceduralPlaybookDetailView(rito));
    }

    public SurfaceSnapshotResponse judicialTribunalVariations() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-tribunal-variations", scaleArchitectureService.judicialTribunalVariationView());
    }

    public SurfaceSnapshotResponse judicialTribunalVariationDetail(String tribunalCodigo, String rito, String unidadeCodigo, String tipoJustica) {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-tribunal-variations.detail", scaleArchitectureService.judicialTribunalVariationDetailView(tribunalCodigo, rito, unidadeCodigo, tipoJustica));
    }

    public SurfaceSnapshotResponse judicialInstitutionalAlignment() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.judicial-institutional-alignment", scaleArchitectureService.judicialInstitutionalAlignmentView());
    }

    public SurfaceSnapshotResponse databaseRuntimePosture() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.database-runtime-posture", scaleArchitectureService.databaseRuntimePostureView());
    }

    public SurfaceSnapshotResponse processualReadModels() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.processual-read-models", scaleArchitectureService.processualReadModelsView());
    }

    public SurfaceSnapshotResponse processualReadModelsPersistence() {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.processual-read-models.persistence", scaleArchitectureService.processualReadModelPersistenceView());
    }

    public SurfaceSnapshotResponse enqueueProcessualReadModelRecomposition(ScaleArchitectureReadModelRecompositionRequest request) {
        return surfaceProjectionSupport.snapshot(
                "admin.scale-architecture.processual-read-models.recomposition.enqueue",
                scaleArchitectureService.enqueueProcessualReadModelRecomposition(
                        new ScaleArchitectureService.ProcessualReadModelRecompositionRequest(
                                request.domain(),
                                request.tribunalCode(),
                                request.ramoCode(),
                                request.scopeKey(),
                                request.requestedBy(),
                                request.reason()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse salvarPartitionPlan(ScaleArchitecturePartitionPlanRequest request) {
        return surfaceProjectionSupport.snapshot(
                "admin.scale-architecture.partition-plans.salvar",
                scaleArchitectureService.salvarPlanoParticao(
                        new ScaleArchitectureService.PartitionPlanRequest(
                                request.tableName(),
                                request.partitionColumn(),
                                request.partitionPrefix(),
                                request.startYear(),
                                request.yearsAhead(),
                                request.notes()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse preview(String tableName) {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.partition-plans.preview", scaleArchitectureService.previewMaterializacao(tableName));
    }

    public SurfaceSnapshotResponse materialize(String tableName) {
        return surfaceProjectionSupport.snapshot("admin.scale-architecture.partition-plans.materialize", scaleArchitectureService.materializar(tableName));
    }
}
