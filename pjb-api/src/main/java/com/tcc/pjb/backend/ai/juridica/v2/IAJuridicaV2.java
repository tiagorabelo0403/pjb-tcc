package com.tcc.pjb.backend.ai.juridica.v2;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.juridica.v1.IAJuridicaV1;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IAJuridicaV2 implements IAService {

    private final IAJuridicaV1 juridicaV1;
    private final RitoPackService ritoPackService;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private final CanonicalRitoSelector canonicalRitoSelector;

    private IAResponse ultimaResposta;

    public IAJuridicaV2(IAJuridicaV1 juridicaV1,
                        RitoPackService ritoPackService,
                        JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                        JuridicaLegalAiSpineService juridicaLegalAiSpineService,
                        CanonicalRitoSelector canonicalRitoSelector) {
        this.juridicaV1 = juridicaV1;
        this.ritoPackService = ritoPackService;
        this.juridicaUnifiedMeshProfileService = juridicaUnifiedMeshProfileService;
        this.juridicaLegalAiSpineService = juridicaLegalAiSpineService;
        this.canonicalRitoSelector = canonicalRitoSelector;
    }


    @Override
    public String getTipo() {
        return "JURIDICA_V2";
    }

    @Override
    public IAResponse getUltimaResposta() {
        return ultimaResposta;
    }

    @Override
    public IAResponse processar(IARequest request) {
        IAPipelineContext ctx = new IAPipelineContext(request);
        juridicaV1.processar(ctx);
        return processar(ctx);
    }

    @Override
    public IAResponse processar(IAPipelineContext context) {
        if (!context.lembrarBoolean("juridica_v1_executada")) {
            IAResponse r1 = juridicaV1.processar(context);
            context.setUltimaResposta(r1);
            context.memorizar("juridica_v1_executada", true);
        }

        context.avancarEtapa("JURIDICA_V2");

        IARequest req = context.getRequestEntrada();
        Map<String, Object> payload = req == null ? Collections.emptyMap() : req.getPayload();

        IAResponse base = context.getUltimaResposta();
        Map<String, Object> baseMeta = base == null ? Collections.emptyMap() : Objects.requireNonNullElse(base.getMetadados(), Collections.emptyMap());
        Map<String, Object> canonicalPayload = buildCanonicalPayload(payload, baseMeta);
        var selectedRito = canonicalRitoSelector.select(canonicalPayload, extractHeuristicRito(payload, baseMeta), "ia_juridica_v2");
        var rito = selectedRito.rito();
        MateriaJurisdicao materia = inferMateria(payload, baseMeta);

        Optional<RitoDefinition> defOpt = ritoPackService.get(rito);
        String planoTexto = buildPlanoTexto(rito != null ? rito.name() : null, materia, defOpt.orElse(null));

        Map<String, Object> essence = new LinkedHashMap<>();
        essence.put("rito", rito != null ? rito.name() : null);
        essence.put("materia", materia != null ? materia.name() : null);
        essence.put("rito_selection", selectedRito.toMap());
        if (defOpt.isPresent()) {
            essence.put("rito_title", defOpt.get().getTitle());
            essence.put("stages", summarizeStages(defOpt.get().getStages()));
        }

        var mesh = juridicaUnifiedMeshProfileService.resolveForIa(
                req,
                com.tcc.pjb.backend.platform.versioning.ApiVersion.V2,
                getTipo(),
                java.util.Map.of(
                        "complexityScore", canonicalPayload.size() * 2,
                        "injectionRiskScore", 0,
                        "petitionDetected", canonicalPayload.containsKey("textoPeticaoLivre")
                ),
                java.util.Map.of(),
                java.util.Map.of("effectiveMode", "READ_ONLY")
        );

        var spine = juridicaLegalAiSpineService.resolveForIa(req, com.tcc.pjb.backend.platform.versioning.ApiVersion.V2, getTipo());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pipeline_stage", context.getEtapaAtual());
        meta.put("timestamp", Instant.now().toString());
        meta.put("v1_origem", base != null ? base.getOrigem() : null);
        meta.put("rito_selection", selectedRito.toMap());
        meta.put("juridica_mesh_profile", mesh.asMap());
        meta.put("juridica_mesh_tools", mesh.tools().stream().map(tool -> tool.id()).toList());
        meta.put("juridica_spine_profile", spine.asMap());
        meta.put("juridica_structured_outputs", spine.structuredOutputs().stream().map(output -> output.schemaId()).toList());
        meta.put("juridica_retrieval_stages", spine.retrieval().stages());
        meta.put("juridica_memory_scopes", spine.memory().enabledScopes());
        meta.put("juridica_symbolic_engines", spine.validation().symbolicEngines());
        meta.put("juridica_graph_enabled", spine.graph().enabled());
        meta.put("juridica_graph_traversals", spine.graph().traversalModes());
        meta.put("juridica_multimodal_modalities", spine.multimodal().enabledModalities());
        meta.put("juridica_eval_suites", spine.evaluation().evalSuites());
        meta.put("juridica_hallucination_guard", spine.hallucinationGuard().asMap());
        meta.put("juridica_unresolved_citation_placeholder", spine.hallucinationGuard().unresolvedCitationPlaceholder());
        meta.put("juridica_citation_emission_mode", spine.hallucinationGuard().citationEmissionMode());
        meta.put("juridica_trace_lane", spine.trace().lane());
        meta.put("juridica_research_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_RESEARCH_DOSSIER);
        meta.put("juridica_validation_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_VALIDATE_ENVELOPE);
        meta.put("juridica_hallucination_guard_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_HALLUCINATION_GUARD);
        meta.put("juridica_conversation_capability", com.tcc.pjb.backend.ai.juridica.spine.JuridicaSpineLabels.CAPABILITY_CONVERSATION);
        meta.put("flags", Map.of(
                "v1_executada", true,
                "rito_detectado", rito != null,
                "fallback_aplicado", selectedRito.fallbackApplied()
        ));

        IAResponse resposta = IAResponse.builder()
                .origem(getTipo())
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(0.84)
                .texto(planoTexto)
                .essence(essence)
                .metadados(meta)
                .dataGeracao(Instant.now())
                .build();

        this.ultimaResposta = resposta;
        context.setUltimaResposta(resposta);
        context.memorizar("juridica_v2_executada", true);
        context.memorizar("rito_processual", rito != null ? rito.name() : null);
        context.memorizar("rito_selection", selectedRito.toMap());
        return resposta;
    }

    private Map<String, Object> buildCanonicalPayload(Map<String, Object> payload, Map<String, Object> baseMeta) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (baseMeta != null) {
            out.putAll(baseMeta);
        }
        if (payload != null) {
            out.putAll(payload);
        }
        return out;
    }

    private String buildPlanoTexto(String ritoName, MateriaJurisdicao materia, RitoDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append("[V2] Plano processual (orientado a rito)\n\n");
        sb.append("Rito inferido: ").append(ritoName != null ? ritoName : "(não inferido)").append("\n");
        sb.append("Matéria inferida: ").append(materia != null ? materia.name() : "(não inferida)").append("\n\n");

        if (def == null || def.getStages() == null || def.getStages().isEmpty()) {
            sb.append("Workflow do rito ainda não carregado para esse tipo.\n");
            sb.append("Ações mínimas sugeridas:\n");
            sb.append("1) Confirmar competência e rito antes do protocolo.\n");
            sb.append("2) Validar legitimidade, interesse, documentos obrigatórios e valor da causa.\n");
            sb.append("3) Montar cronologia e mapa de prova/ônus.\n");
            sb.append("4) Identificar pedido principal e subsidiários; avaliar tutela de urgência.\n");
            sb.append("5) Preparar minuta inicial e plano de diligências/atos.\n");
            return sb.toString();
        }

        sb.append("Workflow canônico do rito (Pack 2026):\n");
        int index = 1;
        for (RitoStage stage : def.getStages()) {
            String fase = stage != null ? stage.getFase() : null;
            sb.append(index++).append(") ").append(fase != null ? fase : "(fase)");
            if (stage != null && stage.getAllowedNext() != null && !stage.getAllowedNext().isEmpty()) {
                sb.append("  → próximos: ").append(String.join(", ", stage.getAllowedNext()));
            }
            sb.append("\n");
        }

        sb.append("\nChecklist prático (V2):\n");
        sb.append("- Prova/ônus: documentos essenciais + plano de testemunhas/perícia.\n");
        sb.append("- Prazos: agenda (início/fim, contagem, feriados locais).\n");
        sb.append("- Riscos: prescrição/decadência, prevenção, litispendência, nulidades.\n");
        sb.append("- Estratégia: tutela provisória, pedidos alternativos, precedentes e acordos.\n");
        return sb.toString();
    }

    private static List<Map<String, Object>> summarizeStages(List<RitoStage> stages) {
        if (stages == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (RitoStage stage : stages) {
            if (stage == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fase", stage.getFase());
            item.put("allowedNext", stage.getAllowedNext() == null ? Collections.emptyList() : stage.getAllowedNext());
            out.add(item);
        }
        return out;
    }

    private String extractHeuristicRito(Map<String, Object> payload, Map<String, Object> baseMeta) {
        return firstNonBlank(
                str(payload.get("rito")),
                str(payload.get("rito_processual")),
                str(payload.get("procedimento")),
                str(baseMeta.get("rito")),
                str(baseMeta.get("rito_processual"))
        );
    }

    private MateriaJurisdicao inferMateria(Map<String, Object> payload, Map<String, Object> baseMeta) {
        String materiaTxt = firstNonBlank(
                str(payload.get("materia")),
                str(payload.get("materia_jurisdicao")),
                str(baseMeta.get("materia"))
        );
        if (materiaTxt != null) {
            return MateriaJurisdicao.fromString(materiaTxt);
        }

        String ambito = normalizeEnum(str(payload.get("ambito_direito")));
        if (ambito.contains("PENAL")) {
            return MateriaJurisdicao.PENAL;
        }
        if (ambito.contains("TRABALH")) {
            return MateriaJurisdicao.TRABALHISTA;
        }
        if (ambito.contains("PREVID")) {
            return MateriaJurisdicao.PREVIDENCIARIA;
        }
        if (ambito.contains("TRIBUT")) {
            return MateriaJurisdicao.TRIBUTARIA;
        }
        if (ambito.contains("ADMIN")) {
            return MateriaJurisdicao.ADMINISTRATIVO;
        }
        return MateriaJurisdicao.MULTIMATERIA;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String normalizeEnum(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
