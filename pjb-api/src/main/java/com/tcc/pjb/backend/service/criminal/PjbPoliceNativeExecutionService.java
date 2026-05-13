package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.model.dto.criminal.PoliceLocalSnapshotRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceNativeCautelarDispatchRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceNativeIntimationMirrorRequest;
import com.tcc.pjb.backend.model.dto.criminal.PoliceTransactionalContingencyRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbPoliceNativeExecutionService {

    private final PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService;
    private final PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService;
    private final PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService;

    public PjbPoliceNativeExecutionService(PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService,
                                           PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService,
                                           PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService,
                                           PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService) {
        this.pjbPoliceNativeToolbeltService = Objects.requireNonNull(pjbPoliceNativeToolbeltService, "pjbPoliceNativeToolbeltService");
        this.policeTransactionalAdapterMeshService = Objects.requireNonNull(policeTransactionalAdapterMeshService, "policeTransactionalAdapterMeshService");
        this.policeSovereignOperationalWorkbenchService = Objects.requireNonNull(policeSovereignOperationalWorkbenchService, "policeSovereignOperationalWorkbenchService");
        this.policeTraceableExecutionLedgerService = Objects.requireNonNull(policeTraceableExecutionLedgerService, "policeTraceableExecutionLedgerService");
    }

    public Map<String, Object> executableCatalog(TipoUsuario tipoUsuario) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "PJB_POLICE_NATIVE_EXECUTABLE_CATALOG");
        out.put("actorLane", actorLane(tipoUsuario));
        out.put("executionOrder", List.of("PJB_NATIVE_FIRST", "PARTNER_TRANSACTION_SECOND", "CONTINGENCY_THIRD"));
        out.put("tools", toolCatalog());
        out.put("mandatoryFunctionFamilies", List.of(
                "remessa_cautelar_nativa",
                "espelho_soberano_eventos_intimacoes",
                "contingencia_transacional_policial",
                "snapshot_local_reconciliavel",
                "ledger_operacional_rastreavel",
                "reconciliacao_partner_por_execucao"
        ));
        out.put("securityBackbone", List.of(
                "hash_de_custodia",
                "assinatura_digital",
                "native_first_strict",
                "ledger_local_reconciliavel",
                "observabilidade_transacional",
                "execution_id_estavel",
                "audit_hash_sha256"
        ));
        out.put("traceableExecutionBackbone", policeTraceableExecutionLedgerService.operationalLedgerBlueprint(tipoUsuario));
        out.put("workbench", nativeExecutionWorkbench(tipoUsuario));
        return immutableMap(out);
    }

    public Map<String, Object> detail(String code, TipoUsuario tipoUsuario) {
        String normalized = normalize(code);
        for (Map<String, Object> tool : (List<Map<String, Object>>) executableCatalog(tipoUsuario).get("tools")) {
            if (normalized.equals(tool.get("code"))) {
                return tool;
            }
        }
        return Map.of(
                "code", normalized,
                "found", false,
                "availableCodes", ((List<Map<String, Object>>) executableCatalog(tipoUsuario).get("tools")).stream().map(item -> String.valueOf(item.get("code"))).toList()
        );
    }

    public Map<String, Object> nativeExecutionWorkbench(TipoUsuario tipoUsuario) {
        Map<String, Object> nativeToolbelt = pjbPoliceNativeToolbeltService.nativeWorkbench(tipoUsuario);
        Map<String, Object> transactionalAdapterMesh = policeTransactionalAdapterMeshService.sovereignMesh(tipoUsuario);
        Map<String, Object> sovereignWorkbench = policeSovereignOperationalWorkbenchService.compose(tipoUsuario);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "PJB_POLICE_NATIVE_EXECUTION_WORKBENCH");
        out.put("actorLane", actorLane(tipoUsuario));
        out.put("nativeFirst", Boolean.TRUE);
        out.put("nativeExecutionTools", toolCatalog());
        out.put("transactionFamilies", transactionalAdapterMesh.get("transactionFamilies"));
        out.put("partnerSystems", transactionalAdapterMesh.get("partnerSystems"));
        out.put("nativeToolbelt", nativeToolbelt);
        out.put("transactionalAdapterMesh", transactionalAdapterMesh);
        out.put("sovereignWorkbench", sovereignWorkbench);
        out.put("traceableOperationalLedger", policeTraceableExecutionLedgerService.operationalLedgerBlueprint(tipoUsuario));
        out.put("recentTraceableExecutions", policeTraceableExecutionLedgerService.recentExecutions(tipoUsuario, 8));
        out.put("guardrails", List.of(
                "sem_dependencia_exclusiva_de_parceiro",
                "fallback_so_se_native_nao_confirmar",
                "retentativa_com_backoff_e_limite",
                "snapshot_reconciliavel_com_hash_local",
                "espelho_de_eventos_sem_sobrescrever_origem",
                "status_real_por_execucao"
        ));
        out.put("mustBeImplemented", List.of(
                "motor_nativo_de_cautelares",
                "espelho_local_de_eventos_e_intimacoes",
                "fila_soberana_de_contingencia",
                "snapshot_local_reconciliavel_com_diff",
                "ledger_operacional_por_execucao",
                "fila_de_confirmacao_e_erro_por_parceiro"
        ));
        return immutableMap(out);
    }


    private List<Map<String, Object>> toolCatalog() {
        return List.of(
                executable("REMESSA_CAUTELAR_NATIVA", "Remessa cautelar nativa", List.of("cautelares", "representacoes", "protetivas"), List.of("draft_nativo", "assinatura_hash", "remessa_native_first", "fallback_parceiro_condicionado")),
                executable("ESPELHO_SOBERANO_INTIMACOES_EVENTOS", "Espelho soberano de intimações e eventos", List.of("intimacoes", "eventos", "andamentos", "ciencia_operacional"), List.of("mirror_pull", "deduplicacao_hash", "janela_reconciliacao", "timeline_local")),
                executable("FILA_CONTINGENCIA_TRANSACIONAL", "Fila de contingência transacional", List.of("retentativas", "degradacao_controlada", "backoff", "fallback"), List.of("enqueue_native", "partner_retry", "strict_native_first", "manual_override")),
                executable("SNAPSHOT_LOCAL_RECONCILIAVEL", "Snapshot local reconciliável", List.of("snapshot", "timeline", "mandados", "anexos"), List.of("captura_hash", "reconcile_partner", "ledger_local", "diff_operacional"))
        );
    }

    public Map<String, Object> dispatchCautelar(TipoUsuario tipoUsuario, PoliceNativeCautelarDispatchRequest request) {
        PoliceNativeCautelarDispatchRequest safe = request == null ? new PoliceNativeCautelarDispatchRequest(null, null, "MEDIDA_CAUTELAR", "Fundamento operacional não informado", null, Boolean.FALSE, null, Boolean.TRUE, Boolean.FALSE) : request;
        List<String> partnerFallbackOrder = partnerFallbackOrder(safe.tribunalAlvo());
        LinkedHashMap<String, Object> route = new LinkedHashMap<>();
        route.put("mode", "REMESSA_CAUTELAR_NATIVA");
        route.put("nativeFirst", Boolean.TRUE);
        route.put("partnerDispatchAllowed", safe.permitirRemessaParceiraResolvido());
        route.put("partnerFallbackOrder", partnerFallbackOrder);
        route.put("signatureModel", "ICP_BRASIL_E_HASH_DE_CUSTODIA");
        route.put("transactionFamily", "REMESSA_CAUTELAR");
        route.put("sigilo", safe.sigiloResolvido());
        route.put("priorityLane", safe.prioridadeAltaResolvida() ? "CRITICAL" : "HIGH");
        route.put("requiredGates", List.of("draft_nativo", "hash_manifesto", "assinatura_valida", "quota_operacional", "roteamento_confirmado"));
        Map<String, Object> traceableExecution = policeTraceableExecutionLedgerService.registerExecution(
                tipoUsuario,
                "REMESSA_CAUTELAR_NATIVA",
                safe.inqueritoId(),
                safe.processoId(),
                partnerFallbackOrder.getFirst(),
                "REMESSA_CAUTELAR",
                true,
                safe.prioridadeAltaResolvida() ? "CRITICAL" : "HIGH",
                route,
                List.of("assinar_manifesto", "submeter_remessa_nativa", "acompanhar_confirmacao_tribunal", "acionar_fallback_parceiro_se_necessario"),
                partnerFallbackOrder
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REMESSA_CAUTELAR_PREPARADA");
        out.put("executedAt", Instant.now().toString());
        out.put("actorLane", actorLane(tipoUsuario));
        putIfNotNull(out, "inqueritoId", safe.inqueritoId());
        putIfNotNull(out, "processoId", safe.processoId());
        out.put("tipoMedida", safe.tipoMedida());
        out.put("fundamento", safe.fundamento());
        out.put("referenciasEvidencia", safe.referenciasEvidencia());
        out.put("route", immutableMap(route));
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("traceableExecution", traceableExecution);
        out.put("nextActions", List.of("assinar_manifesto", "submeter_remessa_nativa", "acompanhar_confirmacao_tribunal", "acionar_fallback_parceiro_se_necessario"));
        return immutableMap(out);
    }

    public Map<String, Object> mirrorIntimacoesEventos(TipoUsuario tipoUsuario, PoliceNativeIntimationMirrorRequest request) {
        PoliceNativeIntimationMirrorRequest safe = request == null ? new PoliceNativeIntimationMirrorRequest(null, null, null, null, null, null, null, null) : request;
        LinkedHashMap<String, Object> mirror = new LinkedHashMap<>();
        mirror.put("mode", "ESPELHO_SOBERANO_INTIMACOES_EVENTOS");
        mirror.put("sourcePartner", safe.sistemaParceiro());
        mirror.put("windowHours", safe.janelaHoras());
        mirror.put("includeEventos", safe.incluirEventosResolvido());
        mirror.put("includeIntimacoes", safe.incluirIntimacoesResolvido());
        mirror.put("includeAnexos", safe.incluirAnexosResolvido());
        mirror.put("reconcileWithSnapshot", safe.reconciliarComSnapshotResolvido());
        mirror.put("dedupStrategy", "HASH_EVENTO_E_CHAVE_EXTERNAS");
        Map<String, Object> traceableExecution = policeTraceableExecutionLedgerService.registerExecution(
                tipoUsuario,
                "ESPELHO_SOBERANO_INTIMACOES_EVENTOS",
                safe.inqueritoId(),
                safe.processoId(),
                safe.sistemaParceiro(),
                "ESPELHO_INTIMACOES_EVENTOS",
                true,
                "HIGH",
                mirror,
                List.of("capturar_lote_eventos", "deduplicar_por_hash", "gerar_timeline_local", "marcar_diferencas_reconciliacao"),
                List.of(safe.sistemaParceiro())
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ESPELHO_OPERACIONAL_PREPARADO");
        out.put("executedAt", Instant.now().toString());
        out.put("actorLane", actorLane(tipoUsuario));
        putIfNotNull(out, "inqueritoId", safe.inqueritoId());
        putIfNotNull(out, "processoId", safe.processoId());
        out.put("mirrorPlan", immutableMap(mirror));
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("traceableExecution", traceableExecution);
        out.put("nextActions", List.of("capturar_lote_eventos", "deduplicar_por_hash", "gerar_timeline_local", "marcar_diferencas_reconciliacao"));
        return immutableMap(out);
    }

    public Map<String, Object> enqueueContingencia(TipoUsuario tipoUsuario, PoliceTransactionalContingencyRequest request) {
        PoliceTransactionalContingencyRequest safe = request == null ? new PoliceTransactionalContingencyRequest(null, null, null, null, null, null, null) : request;
        LinkedHashMap<String, Object> queue = new LinkedHashMap<>();
        queue.put("mode", "FILA_CONTINGENCIA_TRANSACIONAL");
        queue.put("transactionFamily", safe.familiaTransacao());
        queue.put("partnerSystem", safe.sistemaParceiro());
        queue.put("retryLimit", safe.limiteTentativas());
        queue.put("nativeFirstStrict", safe.nativeFirstEstritoResolvido());
        queue.put("backoffPolicy", "EXPONENCIAL_COM_TETO");
        queue.put("deadLetterLane", "POLICE_TRANSACTIONAL_CONTINGENCY_DLT");
        queue.put("reason", safe.motivoOperacional());
        Map<String, Object> traceableExecution = policeTraceableExecutionLedgerService.registerExecution(
                tipoUsuario,
                "FILA_CONTINGENCIA_TRANSACIONAL",
                safe.inqueritoId(),
                safe.processoId(),
                safe.sistemaParceiro(),
                safe.familiaTransacao(),
                safe.nativeFirstEstritoResolvido(),
                "HIGH",
                queue,
                List.of("enfileirar_retentativa", "monitorar_confirmacao_externa", "escalar_para_operador_se_limite_exceder"),
                List.of(safe.sistemaParceiro())
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CONTINGENCIA_ENFILEIRADA");
        out.put("executedAt", Instant.now().toString());
        out.put("actorLane", actorLane(tipoUsuario));
        putIfNotNull(out, "inqueritoId", safe.inqueritoId());
        putIfNotNull(out, "processoId", safe.processoId());
        out.put("queuePlan", immutableMap(queue));
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("traceableExecution", traceableExecution);
        out.put("nextActions", List.of("enfileirar_retentativa", "monitorar_confirmacao_externa", "escalar_para_operador_se_limite_exceder"));
        return immutableMap(out);
    }

    public Map<String, Object> buildSnapshot(TipoUsuario tipoUsuario, PoliceLocalSnapshotRequest request) {
        PoliceLocalSnapshotRequest safe = request == null ? new PoliceLocalSnapshotRequest(null, null, null, null, null, null, null, null, null) : request;
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mode", "SNAPSHOT_LOCAL_RECONCILIAVEL");
        snapshot.put("scope", safe.escopoSnapshot());
        snapshot.put("partnerSystem", safe.sistemaParceiro());
        snapshot.put("reconcile", safe.reconciliarResolvido());
        snapshot.put("includeTimeline", safe.incluirTimelineResolvido());
        snapshot.put("includeAnexos", safe.incluirAnexosResolvido());
        snapshot.put("includeMandados", safe.incluirMandadosResolvido());
        snapshot.put("freezeHash", safe.congelarHashResolvido());
        snapshot.put("diffModel", "NATIVE_LEDGER_VS_PARTNER_SNAPSHOT");
        Map<String, Object> traceableExecution = policeTraceableExecutionLedgerService.registerExecution(
                tipoUsuario,
                "SNAPSHOT_LOCAL_RECONCILIAVEL",
                safe.inqueritoId(),
                safe.processoId(),
                safe.sistemaParceiro(),
                "SNAPSHOT_LOCAL_RECONCILIACAO",
                true,
                "HIGH",
                snapshot,
                List.of("capturar_estado_local", "coletar_estado_parceiro", "comparar_hashes", "publicar_reconciliacao"),
                List.of(safe.sistemaParceiro())
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SNAPSHOT_LOCAL_PREPARADO");
        out.put("executedAt", Instant.now().toString());
        out.put("actorLane", actorLane(tipoUsuario));
        putIfNotNull(out, "inqueritoId", safe.inqueritoId());
        putIfNotNull(out, "processoId", safe.processoId());
        out.put("snapshotPlan", immutableMap(snapshot));
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("traceableExecution", traceableExecution);
        out.put("nextActions", List.of("capturar_estado_local", "coletar_estado_parceiro", "comparar_hashes", "publicar_reconciliacao"));
        return immutableMap(out);
    }

    private static Map<String, Object> executable(String code, String displayName, List<String> scope, List<String> executionBackbone) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("displayName", displayName);
        out.put("found", true);
        out.put("scope", scope);
        out.put("executionBackbone", executionBackbone);
        out.put("safetyBackbone", List.of("native_first", "hash_guard", "fallback_controlado", "observabilidade", "traceable_execution"));
        return immutableMap(out);
    }

    private static List<String> partnerFallbackOrder(String tribunalAlvo) {
        String normalized = tribunalAlvo == null ? "TRIBUNAL_PADRAO" : tribunalAlvo.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("FEDERAL")) {
            return List.of("PJE_MNI", "EPROC", "EPOL");
        }
        return List.of("PJE_MNI", "ESAJ", "EPROC", "SINESP_PPE");
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value != null) {
                safe.put(key, value);
            }
        });
        return Map.copyOf(safe);
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        return tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL ? "POLICIA_FEDERAL" : "POLICIA_CIVIL";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
