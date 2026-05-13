package com.tcc.pjb.backend.ai.juridica.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhase;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.ai.juridica.policy.LegalSafetyGate;
import com.tcc.pjb.backend.core.util.SafeMaps;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@Component
public class JuridicaRelatorPhase implements CognitivePhase {

    private final IAService v1;
    private final IAService v2;
    private final IAService v3;

    public JuridicaRelatorPhase(com.tcc.pjb.backend.ai.juridica.v1.IAJuridicaV1 v1,
                               com.tcc.pjb.backend.ai.juridica.v2.IAJuridicaV2 v2,
                               com.tcc.pjb.backend.ai.juridica.v3.IAJuridicaV3 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    @Override
    public CognitivePhaseName name() {
        return CognitivePhaseName.RESPOND;
    }

    @Override
    public void execute(AgentExecutionContext ctx) {
        if (ctx.isFailFast()) return;

        IARequest req = ctx.request();
        IAService svc = pick(ctx.version());
        IAResponse base = svc.processar(new IAPipelineContext(req));

        String texto = LegalSafetyGate.apply(base != null ? base.getTexto() : "");
        int evCount = ctx.evidences().size();
        if (evCount > 0) {
            texto = (texto == null ? "" : texto) + "\n\n[Fontes (RAG)]\nEvidências recuperadas: " + evCount + ".";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> quality = (Map<String, Object>) ctx.facts().get("evidenceQuality");
        @SuppressWarnings("unchecked")
        Map<String, Object> contradiction = (Map<String, Object>) ctx.facts().get("evidenceContradiction");
        @SuppressWarnings("unchecked")
        Map<String, Object> resolution = (Map<String, Object>) ctx.facts().get("contradictionResolution");
        @SuppressWarnings("unchecked")
        Map<String, Object> plan = (Map<String, Object>) ctx.facts().get("sufficiencyPlan");

        boolean conflictRisk = quality != null && Boolean.TRUE.equals(quality.get("conflictRisk"));

        Double suff = null;
        if (quality != null) {
            Object sObj = quality.get("sufficiencyScore");
            if (sObj instanceof Number n) suff = n.doubleValue();
        }
        Double incons = null;
        if (contradiction != null) {
            Object iObj = contradiction.get("inconsistencyScore");
            if (iObj instanceof Number n) incons = n.doubleValue();
        }
        Double residual = null;
        boolean resolved = false;
        if (resolution != null) {
            Object rObj = resolution.get("residualInconsistencyScore");
            if (rObj instanceof Number n) residual = n.doubleValue();
            resolved = Boolean.TRUE.equals(resolution.get("resolved"));
        }

        boolean needUncertainty = conflictRisk
                || (residual != null && residual >= 0.55)
                || (incons != null && incons >= 0.55)
                || (suff != null && suff < (ctx.version().isAtLeast(ApiVersion.V3) ? 0.95 : 0.80));

        if (needUncertainty) {
            StringBuilder sb = new StringBuilder(texto == null ? "" : texto);
            sb.append("\n\n[Incertezas e Próximos Dados Necessários]\n")
              .append("- Evitar conclusões absolutas: validar com documentação e decisões do caso.\n");

            if (plan != null) {
                Object minQs = plan.get("minQuestions");
                if (minQs instanceof Iterable<?> it) {
                    for (Object h : it) {
                        if (h != null) sb.append("- ").append(h).append('\n');
                    }
                }
            }

            if (quality != null) {
                Object hints = quality.get("missingDataHints");
                if (hints instanceof Iterable<?> it) {
                    for (Object h : it) {
                        if (h != null) sb.append("- ").append(h).append('\n');
                    }
                }
            }

            if (conflictRisk) {
                sb.append("- Sinais de ambiguidade entre fontes: refine tribunal/UF/assunto e amplie precedentes qualificados.\n");
            }
            if ((residual != null && residual >= 0.55) || (incons != null && incons >= 0.55)) {
                if (resolved) {
                    sb.append("- Houve conflito, mitigado por fonte âncora (maior autoridade/aderência); ainda requer validação.\n");
                } else {
                    sb.append("- Conflito/inconsistência relevante entre fontes: delimite recorte temporal e órgão julgador.\n");
                }
            }
            texto = sb.toString();
        }

        Map<String, Object> pipelineMeta = new LinkedHashMap<>();
        pipelineMeta.put("pipeline", SafeMaps.of(
                "plan", SafeMaps.ofNullable(ctx.plan()),
                "facts", SafeMaps.ofNullable(ctx.facts()),
                "failFast", false,
                "version", ctx.version().name(),
                "capability", ctx.capability(),
                "trace", SafeMaps.ofNullable(ctx.traceMeta())
        ));

        IAResponse out = (base != null ? base.toBuilder() : IAResponse.builder())
                .origem("JURIDICA_PIPELINE_" + ctx.version().name())
                .texto(texto)
                .metadados(mergeMaps(base != null ? base.getMetadados() : null, pipelineMeta))
                .evidencias(ctx.evidences())
                .dataGeracao(ctx.now())
                .build();

        ctx.setDraft(out.getTexto());
        ctx.putMemory("finalResponse", out);
    }

    private IAService pick(ApiVersion v) {
        if (v != null && v.isAtLeast(ApiVersion.V3)) return v3;
        if (v != null && v.isAtLeast(ApiVersion.V2)) return v2;
        return v1;
    }

    private static Map<String, Object> mergeMaps(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b != null) out.putAll(b);
        return out;
    }
}
