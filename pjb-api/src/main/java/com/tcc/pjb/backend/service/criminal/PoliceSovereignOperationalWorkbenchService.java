package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PoliceSovereignOperationalWorkbenchService {

    private final PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService;
    private final PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService;
    private final PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService;
    private final PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService;

    public PoliceSovereignOperationalWorkbenchService(PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService,
                                                      PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService,
                                                      PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService,
                                                      PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService,
                                                      PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService) {
        this.policeInvestigationSystemLandscapeService = Objects.requireNonNull(policeInvestigationSystemLandscapeService, "policeInvestigationSystemLandscapeService");
        this.policeInteroperabilityAdapterBlueprintService = Objects.requireNonNull(policeInteroperabilityAdapterBlueprintService, "policeInteroperabilityAdapterBlueprintService");
        this.pjbPoliceNativeToolbeltService = Objects.requireNonNull(pjbPoliceNativeToolbeltService, "pjbPoliceNativeToolbeltService");
        this.policeTransactionalAdapterMeshService = Objects.requireNonNull(policeTransactionalAdapterMeshService, "policeTransactionalAdapterMeshService");
        this.policeTraceableExecutionLedgerService = Objects.requireNonNull(policeTraceableExecutionLedgerService, "policeTraceableExecutionLedgerService");
    }

    public Map<String, Object> compose(TipoUsuario tipoUsuario) {
        Map<String, Object> landscape = policeInvestigationSystemLandscapeService.landscapeFor(tipoUsuario);
        Map<String, Object> workstation = policeInvestigationSystemLandscapeService.workstationBlueprint(tipoUsuario);
        Map<String, Object> operationalMesh = policeInteroperabilityAdapterBlueprintService.operationalMesh(tipoUsuario);
        Map<String, Object> nativeToolbelt = pjbPoliceNativeToolbeltService.nativeWorkbench(tipoUsuario);
        Map<String, Object> transactionalAdapterMesh = policeTransactionalAdapterMeshService.sovereignMesh(tipoUsuario);
        Map<String, Object> traceableLedger = policeTraceableExecutionLedgerService.operationalLedgerBlueprint(tipoUsuario);
        LinkedHashSet<String> mandatory = new LinkedHashSet<>();
        mandatory.addAll(strings(operationalMesh.get("mandatoryFunctionFamilies")));
        mandatory.addAll(strings(nativeToolbelt.get("mandatoryFunctionFamilies")));
        mandatory.addAll(strings(transactionalAdapterMesh.get("transactionFamilies")));
        mandatory.addAll(strings(traceableLedger.get("operationalLedger")));
        LinkedHashSet<String> security = new LinkedHashSet<>();
        security.addAll(strings(workstation.get("signatureAndAuthenticity") instanceof Map<?, ?> map ? map.get("model") : null));
        security.addAll(strings(operationalMesh.get("securityBackbone")));
        security.addAll(strings(nativeToolbelt.get("securityBackbone")));
        security.addAll(strings(transactionalAdapterMesh.get("operationalGuarantees")));
        security.addAll(strings(traceableLedger.get("auditBackbone")));
        LinkedHashSet<String> sovereign = new LinkedHashSet<>();
        sovereign.addAll(strings(nativeToolbelt.get("pjbSovereignCapabilities")));
        sovereign.addAll(strings(transactionalAdapterMesh.get("pjbNativeFallback")));
        sovereign.addAll(strings(traceableLedger.get("reconciliationModes")));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(workstation);
        out.put("mode", "POLICE_SOVEREIGN_OPERATIONAL_WORKBENCH");
        out.put("policeSystemsLandscape", landscape);
        out.put("adapterOperationalMesh", operationalMesh);
        out.put("nativeToolbelt", nativeToolbelt);
        out.put("transactionalAdapterMesh", transactionalAdapterMesh);
        out.put("traceableOperationalLedger", traceableLedger);
        out.put("mandatoryFunctionFamilies", List.copyOf(mandatory));
        out.put("securityBackbone", List.copyOf(security));
        out.put("pjbSovereignCapabilities", List.copyOf(sovereign));
        out.put("nativeFirst", Boolean.TRUE);
        out.put("executionOrder", List.of("PJB_NATIVE_FIRST", "PARTNER_TRANSACTION_SECOND", "CONTINGENCY_THIRD", "TRACEABLE_LEDGER_ALWAYS_ON"));
        out.put("mustBeImplemented", List.of(
                "cartorio_policial_nato_digital",
                "evidence_studio_e_custodia",
                "motor_nativo_de_cautelares_e_representacoes",
                "hub_soberano_de_remessa_e_contingencia",
                "snapshot_local_com_reconciliacao",
                "intimacoes_e_eventos_com_espelho_nativo",
                "ledger_operacional_por_execucao",
                "fila_confirmacao_erro_reconciliacao_por_parceiro"
        ));
        return Collections.unmodifiableMap(out);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<String>) list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }
}
