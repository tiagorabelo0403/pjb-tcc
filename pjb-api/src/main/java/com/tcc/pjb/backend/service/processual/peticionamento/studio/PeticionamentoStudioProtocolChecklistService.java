package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioProtocolChecklistService {

    public ProtocolChecklistReport build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> items = new ArrayList<>();

        add(items, "QUALIFICACAO_PARTES", "Qualificação mínima das partes", hasText(safe.parteAutora()) && hasText(safe.parteRe()) ? "READY" : "NEEDS_REVIEW", "HIGH",
                "A peça deve identificar polo ativo e passivo com mínimo operacional antes do protocolo.");
        add(items, "FATOS_ESTRUTURADOS", "Fatos estruturados", safe.factCount() > 0 ? "READY" : "NEEDS_INPUT", "HIGH",
                "A narrativa precisa ter fatos organizados antes da assinatura.");
        add(items, "FUNDAMENTACAO", "Fundamentação jurídica", safe.groundCount() > 0 ? "READY" : "NEEDS_INPUT", "CRITICAL",
                "A peça precisa de base normativa ou tese jurídica minimamente consolidada.");
        add(items, "PEDIDOS", "Pedidos definidos", safe.requestCount() > 0 ? "READY" : "NEEDS_INPUT", "CRITICAL",
                "Sem pedido estruturado o protocolo não deve avançar.");

        if (!"EMBARGOS".equals(safe.petitionFamily())) {
            add(items, "VALOR_CAUSA", "Valor da causa consolidado", safe.valueClaimPresent() ? "READY" : "NEEDS_REVIEW", "MEDIUM",
                    "Conferir valor da causa, sobretudo em iniciais e incidentes que o exigem expressamente.");
        }

        if (safe.hasRepresentationDocument() || hasText(safe.tipoInstrumentoRepresentacao())) {
            add(items, "REPRESENTACAO", "Representação processual", "READY", "HIGH",
                    "Há indício de instrumento de representação ou configuração equivalente no dossiê.");
        } else {
            add(items, "REPRESENTACAO", "Representação processual", "NEEDS_REVIEW", "HIGH",
                    "Conferir procuração, substabelecimento, designação institucional ou equivalente antes do protocolo.");
        }

        if (safe.requestedUrgency()) {
            add(items, "LASTRO_URGENCIA", "Lastro de urgência", safe.evidenceCount() > 0 ? "READY" : "NEEDS_REVIEW", "CRITICAL",
                    "Pedido urgente sem suporte material visível aumenta risco de indeferimento liminar.");
        }

        if (isRecursalFamily(safe.petitionFamily())) {
            add(items, "DECISAO_RECORRIDA", "Decisão ou ato recorrido", safe.hasDecisionArtifact() ? "READY" : "NEEDS_INPUT", "CRITICAL",
                    "A trilha recursal precisa da decisão/acórdão ou ato embargado claramente identificado.");
            add(items, "CIENCIA_RECURSAL", "Prova da ciência/intimação", safe.hasIntimationArtifact() ? "READY" : "NEEDS_REVIEW", "CRITICAL",
                    "A janela recursal deve ser ancorada em prova de ciência, intimação ou marco temporal equivalente.");
            add(items, "CABIMENTO", "Cabimento e espécie", hasText(safe.canonicalAppealType()) ? "READY" : "NEEDS_REVIEW", "CRITICAL",
                    "A espécie recursal canônica deve estar fechada antes da assinatura.");
        }

        if (safe.counterReasons()) {
            add(items, "CAPITULOS_RECURSO_ADVERSO", "Capítulos do recurso adverso", safe.factCount() > 0 ? "READY" : "NEEDS_INPUT", "HIGH",
                    "Contrarrazões exigem enfrentamento dialético dos capítulos impugnados.");
        }

        if ("EMBARGOS".equals(safe.petitionFamily())) {
            add(items, "VICIO_EMBARGADO", "Vício da decisão embargada", safe.embargosGrounds().isEmpty() ? "NEEDS_INPUT" : "READY", "CRITICAL",
                    "Embargos exigem omissão, contradição, obscuridade ou erro material individualizado.");
        }

        ProcessMaterialDossierReport dossier = safe.materialDossier();
        if (dossier != null) {
            for (String item : sanitize(dossier.protocolChecklist())) {
                add(items, "DOSSIER_" + items.size(), item, "READY", "MEDIUM",
                        "Checkpoint herdado do dossiê material do caso.");
            }
            for (String gap : sanitize(dossier.proofGaps())) {
                add(items, "GAP_" + items.size(), gap, "NEEDS_REVIEW", "HIGH",
                        "Lacuna probatória mapeada pelo dossiê material.");
            }
        }

        ProcessMaterialStrategyReport strategy = safe.materialStrategy();
        if (strategy != null) {
            for (String blocker : sanitize(strategy.protocolBlockers())) {
                add(items, "BLOCKER_" + items.size(), blocker, "NEEDS_INPUT", "CRITICAL",
                        "Bloqueio herdado da estratégia material do caso.");
            }
            for (String item : sanitize(strategy.executionChecklist())) {
                add(items, "EXEC_" + items.size(), item, "READY", "LOW",
                        "Checklist operacional derivado da estratégia material.");
            }
        }

        List<String> summary = summarize(items);
        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", "PETITION_PROTOCOL_CHECKLIST_V2");
        workspace.put("petitionFamily", safe.petitionFamily());
        workspace.put("items", List.copyOf(items));
        workspace.put("summary", summary);
        workspace.put("blocking", items.stream().anyMatch(item -> "CRITICAL".equals(item.get("severity")) && !"READY".equals(item.get("status"))));
        return new ProtocolChecklistReport(List.copyOf(items), summary, Map.copyOf(workspace));
    }

    private void add(List<Map<String, Object>> target,
                     String code,
                     String label,
                     String status,
                     String severity,
                     String rationale) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        item.put("status", status);
        item.put("severity", severity);
        item.put("rationale", rationale);
        target.add(Map.copyOf(item));
    }

    private List<String> summarize(List<Map<String, Object>> items) {
        ArrayList<String> out = new ArrayList<>();
        long criticalPending = items.stream().filter(item -> "CRITICAL".equals(item.get("severity")) && !"READY".equals(item.get("status"))).count();
        long reviewPending = items.stream().filter(item -> "NEEDS_REVIEW".equals(item.get("status"))).count();
        long inputPending = items.stream().filter(item -> "NEEDS_INPUT".equals(item.get("status"))).count();
        if (criticalPending > 0) {
            out.add("Há " + criticalPending + " checkpoint(s) críticos impedindo avanço seguro para assinatura e protocolo.");
        }
        if (inputPending > 0) {
            out.add("Há " + inputPending + " checkpoint(s) exigindo preenchimento material antes do fechamento da peça.");
        }
        if (reviewPending > 0) {
            out.add("Há " + reviewPending + " checkpoint(s) em revisão assistida antes do protocolo.");
        }
        if (out.isEmpty()) {
            out.add("Checklist procedimental sem pendências críticas visíveis na janela atual.");
        }
        return List.copyOf(out);
    }

    private List<String> sanitize(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private boolean isRecursalFamily(String family) {
        String normalized = trimToNull(family);
        return normalized != null && !"PETICAO_BASE".equals(normalized);
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
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
                                 String parteAutora,
                                 String parteRe,
                                 int factCount,
                                 int groundCount,
                                 int requestCount,
                                 boolean valueClaimPresent,
                                 boolean requestedUrgency,
                                 int evidenceCount,
                                 boolean hasRepresentationDocument,
                                 String tipoInstrumentoRepresentacao,
                                 boolean hasDecisionArtifact,
                                 boolean hasIntimationArtifact,
                                 ProcessMaterialDossierReport materialDossier,
                                 ProcessMaterialStrategyReport materialStrategy) {
        public ResolveRequest {
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily.trim();
            embargosGrounds = embargosGrounds == null ? List.of() : List.copyOf(embargosGrounds);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("PETICAO_BASE", null, false, List.of(), null, null, 0, 0, 0, false, false, 0, false, null, false, false, null, null);
        }
    }

    public record ProtocolChecklistReport(List<Map<String, Object>> items,
                                          List<String> summary,
                                          Map<String, Object> workspace) {
        public ProtocolChecklistReport {
            items = items == null ? List.of() : List.copyOf(items);
            summary = summary == null ? List.of() : List.copyOf(summary);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
