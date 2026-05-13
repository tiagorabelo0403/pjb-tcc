package com.tcc.pjb.backend.ai.sync;

import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.service.procedural.NationalForumMeshGovernanceService;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("!test")
@Service
@ConditionalOnProperty(prefix = "pjb.sync.cnj", name = "enabled", havingValue = "true")
public class CnjSyncService {

    private final CnjTpuSyncService cnjTpuSyncService;
    private final NationalForumMeshGovernanceService nationalForumMeshGovernanceService;

    public CnjSyncService(CnjTpuSyncService cnjTpuSyncService,
                          NationalForumMeshGovernanceService nationalForumMeshGovernanceService) {
        this.cnjTpuSyncService = Objects.requireNonNull(cnjTpuSyncService);
        this.nationalForumMeshGovernanceService = Objects.requireNonNull(nationalForumMeshGovernanceService);
    }

    @PostConstruct
    public void carregarTribunais() {
        log.info("CNJ sync: iniciando sincronização operacional do catálogo TPU e malha nacional...");
        CnjTpuSyncService.TpuCatalogSnapshot snapshot = cnjTpuSyncService.forceSync();
        NationalForumMeshGovernanceService.MeshGovernanceReport report = nationalForumMeshGovernanceService.reconcile();
        log.info(
                "CNJ sync: snapshot={} classes={} assuntos={} origemCNJ={} unidades={} atualizadas={} classesAdicionadas={} assuntosAdicionados={} endpointsAjustados={} alertas={}",
                snapshot != null ? snapshot.snapshotId() : null,
                snapshot != null ? snapshot.totalClasses() : 0,
                snapshot != null ? snapshot.totalAssuntos() : 0,
                snapshot != null && snapshot.fromCnj(),
                report.totalUnits(),
                report.updatedUnits(),
                report.classesAdded(),
                report.assuntosAdded(),
                report.digitalEndpointsAdjusted(),
                report.alerts().size()
        );
        if (!report.alerts().isEmpty()) {
            log.warn("CNJ sync: alertas de governança da malha nacional: {}", report.alerts());
        }
    }
}
