package com.tcc.pjb.backend.service.delegado;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeExecutionService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeToolbeltService;
import com.tcc.pjb.backend.service.criminal.PoliceInvestigationSystemLandscapeService;
import com.tcc.pjb.backend.service.criminal.PoliceSovereignOperationalWorkbenchService;
import com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService;
import com.tcc.pjb.backend.service.criminal.PoliceTransactionalAdapterMeshService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de DelegadoPainelService: agrupa os 6 colaboradores police* que
 * compõem o landscape investigativo e o workbench operacional soberano do painel
 * do delegado. Cada método público expõe um bloco pronto para o painel, ou o
 * bundle inteiro (composeInvestigativeWorkstation) que espelha a montagem original
 * do bootstrapPainel().
 */
@Service
public class DelegadoInvestigativeWorkbenchOrchestrator {

    private final PoliceInvestigationSystemLandscapeService landscapeService;
    private final PjbPoliceNativeToolbeltService nativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService transactionalAdapterMeshService;
    private final PoliceSovereignOperationalWorkbenchService sovereignOperationalWorkbenchService;
    private final PjbPoliceNativeExecutionService nativeExecutionService;
    private final PoliceTraceableExecutionLedgerService traceableExecutionLedgerService;

    public DelegadoInvestigativeWorkbenchOrchestrator(PoliceInvestigationSystemLandscapeService landscapeService,
                                                       PjbPoliceNativeToolbeltService nativeToolbeltService,
                                                       PoliceTransactionalAdapterMeshService transactionalAdapterMeshService,
                                                       PoliceSovereignOperationalWorkbenchService sovereignOperationalWorkbenchService,
                                                       PjbPoliceNativeExecutionService nativeExecutionService,
                                                       PoliceTraceableExecutionLedgerService traceableExecutionLedgerService) {
        this.landscapeService = Objects.requireNonNull(landscapeService);
        this.nativeToolbeltService = Objects.requireNonNull(nativeToolbeltService);
        this.transactionalAdapterMeshService = Objects.requireNonNull(transactionalAdapterMeshService);
        this.sovereignOperationalWorkbenchService = Objects.requireNonNull(sovereignOperationalWorkbenchService);
        this.nativeExecutionService = Objects.requireNonNull(nativeExecutionService);
        this.traceableExecutionLedgerService = Objects.requireNonNull(traceableExecutionLedgerService);
    }

    public Map<String, Object> landscapeFor(TipoUsuario tipoUsuario) {
        return landscapeService.landscapeFor(tipoUsuario);
    }

    /**
     * Compõe o `investigativeWorkstation` completo do painel do delegado, na mesma
     * ordem original do bootstrapPainel() -- começa pela workbench soberana e vai
     * empilhando os blocos (nativeToolbelt, transactionalAdapterMesh,
     * nativeExecutionWorkbench, traceableOperationalLedger, recentTraceableExecutions).
     */
    public Map<String, Object> composeInvestigativeWorkstation(TipoUsuario tipoUsuario) {
        Map<String, Object> workstation = new LinkedHashMap<>(sovereignOperationalWorkbenchService.compose(tipoUsuario));
        workstation.put("nativeToolbelt", nativeToolbeltService.nativeWorkbench(tipoUsuario));
        workstation.put("transactionalAdapterMesh", transactionalAdapterMeshService.sovereignMesh(tipoUsuario));
        workstation.put("nativeExecutionWorkbench", nativeExecutionService.nativeExecutionWorkbench(tipoUsuario));
        workstation.put("traceableOperationalLedger", traceableExecutionLedgerService.operationalLedgerBlueprint(tipoUsuario));
        workstation.put("recentTraceableExecutions", traceableExecutionLedgerService.recentExecutions(tipoUsuario, 8));
        return workstation;
    }
}
