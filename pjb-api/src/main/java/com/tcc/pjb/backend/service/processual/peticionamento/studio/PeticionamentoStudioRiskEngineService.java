package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioRiskEngineService {

    public Map<String, Object> build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> blockingIssues = new ArrayList<>(sanitize(safe.baseBlockingIssues()));
        ArrayList<String> alerts = new ArrayList<>(sanitize(safe.baseAlerts()));
        ArrayList<String> checklist = new ArrayList<>(sanitize(safe.baseChecklist()));

        if (safe.requestCount() == 0) {
            blockingIssues.add("A peça não possui pedidos estruturados; o protocolo não deve seguir sem capítulo petitório mínimo.");
        }
        if (safe.groundCount() == 0) {
            blockingIssues.add("A peça não possui fundamentação jurídica consolidada no workspace atual.");
        }
        if (safe.factCount() == 0) {
            alerts.add("A narrativa fática ainda não foi estruturada em linhas objetivas; isso reduz coerência e dialeticidade.");
        }
        if (safe.requestedUrgency() && safe.evidenceCount() == 0) {
            blockingIssues.add("Urgência declarada sem evidência materializada no dossiê; revisar pedido urgente antes do protocolo.");
        }
        if (safe.valueClaimMissing() && !isRecursalFamily(safe.petitionFamily())) {
            alerts.add("Valor da causa ausente ou não consolidado no workspace atual.");
        }
        if (isRecursalFamily(safe.petitionFamily()) && !safe.hasDecisionArtifact()) {
            blockingIssues.add("Fluxo recursal sem decisão/acórdão/ato impugnado identificado no dossiê documental.");
        }
        if (isRecursalFamily(safe.petitionFamily()) && !safe.hasIntimationArtifact()) {
            blockingIssues.add("Fluxo recursal sem prova visível de ciência ou intimação para fechamento da janela temporal.");
        }
        if ("EMBARGOS".equals(safe.petitionFamily()) && safe.embargosGrounds().isEmpty()) {
            blockingIssues.add("Embargos sem vício individualizado; é preciso apontar omissão, contradição, obscuridade ou erro material.");
        }

        ProcessMaterialDossierReport dossier = safe.materialDossier();
        if (dossier != null) {
            alerts.addAll(sanitize(dossier.proofGaps()));
            checklist.addAll(sanitize(dossier.protocolChecklist()));
        }
        ProcessMaterialStrategyReport strategy = safe.materialStrategy();
        if (strategy != null) {
            blockingIssues.addAll(sanitize(strategy.protocolBlockers()));
            alerts.addAll(sanitize(strategy.negotiationGuardrails()));
            checklist.addAll(sanitize(strategy.executionChecklist()));
        }

        List<Map<String, Object>> protocolItems = safe.protocolChecklistItems() == null ? List.of() : safe.protocolChecklistItems();
        long criticalProtocolPending = protocolItems.stream()
                .filter(item -> "CRITICAL".equals(item.get("severity")) && !"READY".equals(item.get("status")))
                .count();
        if (criticalProtocolPending > 0) {
            blockingIssues.add("Checklist procedimental mantém " + criticalProtocolPending + " pendência(s) crítica(s) antes da assinatura.");
        }

        String proofStrength = safe.proofMatrixOverallStrength();
        if ("CRITICO".equals(proofStrength)) {
            blockingIssues.add("A matriz prova x pedido está crítica; pedidos sem amarração mínima não devem ser protocolados.");
        } else if ("FRAGIL".equals(proofStrength)) {
            alerts.add("A matriz prova x pedido está frágil; reforçar lastro documental e fundamento jurídico antes do protocolo.");
        }

        ArrayList<String> nextActions = new ArrayList<>();
        if (!blockingIssues.isEmpty()) {
            nextActions.add("Resolver bloqueios estruturais antes da assinatura e do protocolo.");
        }
        if (safe.requestedUrgency()) {
            nextActions.add("Priorizar matriz de urgência, documentos de lastro e fechamento de pedido liminar/tutelar.");
        }
        if (isRecursalFamily(safe.petitionFamily())) {
            nextActions.add("Fechar cabimento, ciência/intimação e pacote documental obrigatório da trilha recursal.");
        }
        if ("EMBARGOS".equals(safe.petitionFamily())) {
            nextActions.add("Delimitar o vício integrativo e manter os embargos dentro da moldura própria da espécie.");
        }
        if (nextActions.isEmpty()) {
            nextActions.add("Executar revisão humana, conferir assinatura governada e seguir para o protocolo assistido.");
        }

        LinkedHashMap<String, Object> risk = new LinkedHashMap<>();
        risk.put("blocking", !blockingIssues.isEmpty());
        risk.put("blockingIssues", deduplicate(blockingIssues));
        risk.put("alerts", deduplicate(alerts));
        risk.put("checklist", deduplicate(checklist));
        risk.put("readyForProtocol", !(!blockingIssues.isEmpty() || criticalProtocolPending > 0 || "CRITICO".equals(proofStrength)));
        risk.put("riskBands", Map.of(
                "proofMatrix", proofStrength,
                "petitionFamily", safe.petitionFamily(),
                "criticalProtocolPending", criticalProtocolPending,
                "urgency", safe.requestedUrgency() ? "PRIORITY" : "STANDARD"
        ));
        risk.put("coverageSummary", Map.of(
                "facts", safe.factCount(),
                "grounds", safe.groundCount(),
                "requests", safe.requestCount(),
                "evidence", safe.evidenceCount()
        ));
        risk.put("nextActions", List.copyOf(nextActions));
        risk.put("proofMatrixOverallStrength", proofStrength);
        risk.put("petitionFamily", safe.petitionFamily());
        risk.put("canonicalAppealType", safe.canonicalAppealType());
        risk.put("recursalCounterReasons", safe.counterReasons());
        return Map.copyOf(risk);
    }

    private boolean isRecursalFamily(String family) {
        String normalized = trimToNull(family);
        return normalized != null && !"PETICAO_BASE".equals(normalized);
    }

    private List<String> deduplicate(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : sanitize(values)) {
            if (!out.contains(value)) {
                out.add(value);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> sanitize(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResolveRequest(String petitionFamily,
                                 String canonicalAppealType,
                                 boolean counterReasons,
                                 List<String> embargosGrounds,
                                 int factCount,
                                 int groundCount,
                                 int requestCount,
                                 int evidenceCount,
                                 boolean valueClaimMissing,
                                 boolean requestedUrgency,
                                 boolean hasDecisionArtifact,
                                 boolean hasIntimationArtifact,
                                 List<String> baseBlockingIssues,
                                 List<String> baseAlerts,
                                 List<String> baseChecklist,
                                 String proofMatrixOverallStrength,
                                 List<Map<String, Object>> protocolChecklistItems,
                                 ProcessMaterialDossierReport materialDossier,
                                 ProcessMaterialStrategyReport materialStrategy) {
        public ResolveRequest {
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily.trim();
            embargosGrounds = embargosGrounds == null ? List.of() : List.copyOf(embargosGrounds);
            baseBlockingIssues = baseBlockingIssues == null ? List.of() : List.copyOf(baseBlockingIssues);
            baseAlerts = baseAlerts == null ? List.of() : List.copyOf(baseAlerts);
            baseChecklist = baseChecklist == null ? List.of() : List.copyOf(baseChecklist);
            proofMatrixOverallStrength = proofMatrixOverallStrength == null || proofMatrixOverallStrength.isBlank() ? "CRITICO" : proofMatrixOverallStrength.trim();
            protocolChecklistItems = protocolChecklistItems == null ? List.of() : List.copyOf(protocolChecklistItems);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("PETICAO_BASE", null, false, List.of(), 0, 0, 0, 0, true, false, false, false, List.of(), List.of(), List.of(), "CRITICO", List.of(), null, null);
        }
    }
}
