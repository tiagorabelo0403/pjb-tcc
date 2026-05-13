package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioDocumentGapService {

    public DocumentGapReport build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> items = new ArrayList<>();
        ArrayList<String> nextActions = new ArrayList<>();

        items.add(item(
                "IDENTIFICACAO_PARTES",
                "Identificação mínima das partes",
                safe.hasParties(),
                safe.hasParties() ? "Qualificação mínima das partes já informada no dossiê." : "Qualificação das partes ainda incompleta para fechamento técnico da peça.",
                "CRITICAL",
                List.of("parteAutora", "parteRe", "dados mínimos de qualificação")
        ));

        items.add(item(
                "PROVAS_BASE",
                "Provas-base do caso",
                safe.evidenceCount() > 0,
                safe.evidenceCount() > 0 ? "O dossiê já contém material probatório referenciado." : "Ainda não há prova materializada suficiente para amarrar os fatos centrais.",
                "CRITICAL",
                List.of("documentosAnexados", "provasDocumentais", "mídia inline", "laudos", "comprovantes")
        ));

        boolean representationRequired = safe.requiresRepresentation();
        items.add(item(
                "REPRESENTACAO_PROCESSUAL",
                "Representação processual",
                !representationRequired || safe.hasRepresentationArtifact(),
                !representationRequired
                        ? "Fluxo atual não exige instrumento de representação explícito para o perfil informado."
                        : safe.hasRepresentationArtifact()
                        ? "Instrumento de representação já indicado no dossiê."
                        : "O protocolo exige instrumento de representação ou confirmação institucional equivalente.",
                representationRequired ? "CRITICAL" : "ATTENTION",
                List.of("procuração", "substabelecimento", "designação institucional", "credencial funcional")
        ));

        if (safe.requestedUrgency()) {
            items.add(item(
                    "LASTRO_URGENTE",
                    "Lastro documental da urgência",
                    safe.hasUrgencyEvidence(),
                    safe.hasUrgencyEvidence()
                            ? "Há material mínimo indicado para sustentar pedido urgente."
                            : "Urgência declarada sem documento, mídia ou marcador de prova diretamente vinculado.",
                    "CRITICAL",
                    List.of("comprovante contemporâneo", "laudo", "print qualificado", "documento médico", "ato iminente")
            ));
        }

        if (safe.isRecursalFamily()) {
            items.add(item(
                    "DECISAO_RECORRIDA",
                    "Decisão ou acórdão impugnado",
                    safe.hasDecisionArtifact(),
                    safe.hasDecisionArtifact()
                            ? "Decisão/acórdão impugnado identificado no dossiê."
                            : "Não há decisão ou acórdão claramente identificado para sustentar a peça recursal.",
                    "CRITICAL",
                    List.of("sentença", "decisão interlocutória", "acórdão", "decisão embargada")
            ));
            items.add(item(
                    "CIENCIA_INTIMACAO",
                    "Prova de ciência/intimação",
                    safe.hasIntimationArtifact(),
                    safe.hasIntimationArtifact()
                            ? "Há artefato de ciência/intimação para fechamento temporal da janela recursal."
                            : "Falta artefato claro de ciência/intimação/publicação para tempestividade.",
                    "CRITICAL",
                    List.of("certidão", "publicação", "intimação", "comprovante de ciência")
            ));
        }

        if ("EMBARGOS".equals(safe.petitionFamily())) {
            boolean embargosReady = !safe.embargosGrounds().isEmpty();
            items.add(item(
                    "VICIO_EMBARGOS",
                    "Vício individualizado dos embargos",
                    embargosReady,
                    embargosReady
                            ? "Os vícios integrativos já foram delimitados no workspace."
                            : "Embargos ainda sem vício individualizado; é preciso apontar omissão, contradição, obscuridade ou erro material.",
                    "CRITICAL",
                    List.of("omissão", "contradição", "obscuridade", "erro material")
            ));
        }

        ProcessMaterialDossierReport dossier = safe.materialDossier();
        if (dossier != null) {
            for (String gap : sanitize(dossier.proofGaps())) {
                items.add(item(
                        "GAP_MATERIAL_DOSSIER",
                        "Lacuna apontada pelo dossiê material",
                        false,
                        gap,
                        "ATTENTION",
                        List.of("prova complementar", "documento de lastro", "memória narrativa")
                ));
            }
        }
        ProcessMaterialStrategyReport strategy = safe.materialStrategy();
        if (strategy != null) {
            for (String blocker : sanitize(strategy.protocolBlockers())) {
                items.add(item(
                        "BLOQUEIO_ESTRATEGICO",
                        "Bloqueio estratégico do protocolo",
                        false,
                        blocker,
                        "CRITICAL",
                        List.of("revisão técnica", "complementação documental", "ajuste procedimental")
                ));
            }
        }

        long criticalMissing = items.stream()
                .filter(item -> Objects.equals("MISSING", item.get("status")) && Objects.equals("CRITICAL", item.get("severity")))
                .count();
        long attentionMissing = items.stream()
                .filter(item -> Objects.equals("MISSING", item.get("status")) && !Objects.equals("CRITICAL", item.get("severity")))
                .count();

        if (criticalMissing > 0) {
            nextActions.add("Completar primeiro as lacunas críticas do dossiê antes de avançar para assinatura e protocolo.");
        }
        if (attentionMissing > 0) {
            nextActions.add("Revisar as lacunas secundárias para elevar robustez argumentativa e documental da peça.");
        }
        if (safe.isRecursalFamily()) {
            nextActions.add("Na trilha recursal, fechar decisão atacada e prova de ciência é obrigatório para estabilidade do protocolo.");
        }
        if ("EMBARGOS".equals(safe.petitionFamily())) {
            nextActions.add("Nos embargos, manter a fundamentação vinculada ao vício integrativo sem ampliar indevidamente o objeto recursal.");
        }
        if (nextActions.isEmpty()) {
            nextActions.add("Dossiê documental suficiente para seguir com revisão humana e protocolo assistido.");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("overallStatus", criticalMissing > 0 ? "CRITICAL" : attentionMissing > 0 ? "ATTENTION" : "READY");
        workspace.put("items", List.copyOf(items));
        workspace.put("summary", Map.of(
                "criticalMissing", criticalMissing,
                "attentionMissing", attentionMissing,
                "totalItems", items.size()
        ));
        workspace.put("nextActions", List.copyOf(nextActions));
        workspace.put("petitionFamily", safe.petitionFamily());
        workspace.put("canonicalAppealType", safe.canonicalAppealType());
        return new DocumentGapReport(
                stringValue(workspace.get("overallStatus"), "CRITICAL"),
                List.copyOf(items),
                List.copyOf(nextActions),
                Map.copyOf(workspace)
        );
    }

    private Map<String, Object> item(String code,
                                     String label,
                                     boolean present,
                                     String summary,
                                     String severity,
                                     List<String> suggestedSources) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("label", label);
        out.put("status", present ? "READY" : "MISSING");
        out.put("present", present);
        out.put("summary", summary);
        out.put("severity", severity);
        out.put("suggestedSources", suggestedSources == null ? List.of() : List.copyOf(suggestedSources));
        return Collections.unmodifiableMap(out);
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
    }

    public record ResolveRequest(String petitionFamily,
                                 String canonicalAppealType,
                                 boolean requestedUrgency,
                                 boolean hasParties,
                                 int evidenceCount,
                                 boolean hasRepresentationArtifact,
                                 boolean requiresRepresentation,
                                 boolean hasDecisionArtifact,
                                 boolean hasIntimationArtifact,
                                 boolean hasUrgencyEvidence,
                                 List<String> embargosGrounds,
                                 ProcessMaterialDossierReport materialDossier,
                                 ProcessMaterialStrategyReport materialStrategy) {
        public ResolveRequest {
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily.trim();
            embargosGrounds = embargosGrounds == null ? List.of() : List.copyOf(embargosGrounds);
        }

        public boolean isRecursalFamily() {
            return petitionFamily != null && !petitionFamily.isBlank() && !"PETICAO_BASE".equals(petitionFamily);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("PETICAO_BASE", null, false, false, 0, false, false, false, false, false, List.of(), null, null);
        }
    }

    public record DocumentGapReport(String overallStatus,
                                    List<Map<String, Object>> items,
                                    List<String> nextActions,
                                    Map<String, Object> workspace) {
        public DocumentGapReport {
            overallStatus = overallStatus == null || overallStatus.isBlank() ? "CRITICAL" : overallStatus.trim().toUpperCase(Locale.ROOT);
            items = items == null ? List.of() : List.copyOf(items);
            nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
