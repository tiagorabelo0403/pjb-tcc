package com.tcc.pjb.backend.ai.juridica.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.juridica.philosophy.LegalPhilosopher;
import com.tcc.pjb.backend.ai.juridica.philosophy.LegalPhilosophyCompass;
import com.tcc.pjb.backend.ai.juridica.v2.IAJuridicaV2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IAJuridicaV3 implements IAService {

    private final IAJuridicaV2 juridicaV2;
    private final LegalPhilosophyCompass philosophyCompass;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private IAResponse ultimaResposta;

    public IAJuridicaV3(IAJuridicaV2 juridicaV2,
                        LegalPhilosophyCompass philosophyCompass,
                        JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                        JuridicaLegalAiSpineService juridicaLegalAiSpineService) {
        this.juridicaV2 = juridicaV2;
        this.philosophyCompass = philosophyCompass;
        this.juridicaUnifiedMeshProfileService = juridicaUnifiedMeshProfileService;
        this.juridicaLegalAiSpineService = juridicaLegalAiSpineService;
    }


    @Override
    public String getTipo() {
        return "JURIDICA_V3";
    }

    @Override
    public IAResponse getUltimaResposta() {
        return ultimaResposta;
    }

    @Override
    public IAResponse processar(IARequest request) {
        IAPipelineContext ctx = new IAPipelineContext(request);
        return processar(ctx);
    }

    @Override
    public IAResponse processar(IAPipelineContext context) {
        
        IAResponse base = juridicaV2.processar(context);

        String textoBase = base != null ? base.getTexto() : "";
        String textoV3 = gerarConsolidacao(textoBase, context);

        var mesh = juridicaUnifiedMeshProfileService.resolveForIa(
                context.getRequestEntrada(),
                com.tcc.pjb.backend.platform.versioning.ApiVersion.V3,
                getTipo(),
                java.util.Map.of(
                        "complexityScore", context.getFacts() != null ? context.getFacts().size() * 3 : 0,
                        "injectionRiskScore", 0,
                        "petitionDetected", context.getRequestEntrada() != null && context.getRequestEntrada().getPayload().containsKey("textoPeticaoLivre")
                ),
                java.util.Map.of(),
                java.util.Map.of("effectiveMode", "READ_ONLY")
        );

        var spine = juridicaLegalAiSpineService.resolveForIa(context.getRequestEntrada(), com.tcc.pjb.backend.platform.versioning.ApiVersion.V3, getTipo());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versao", 3);
        meta.put("base_origem", base != null ? base.getOrigem() : null);
        meta.put("etapas", context.getStageHistory());
        meta.put("recomendacao", "Consolidar fundamentos + próximos atos + prova/ônus.");
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
        meta.put("juridica_approval_required", spine.approval().approvalRequired());

        IAResponse resposta = IAResponse.builder()
                .origem(getTipo())
                .status(base != null ? base.getStatus() : IAResponse.StatusIA.ALERTA)
                .confianca(base != null ? base.getConfianca() : 0.6)
                .texto(textoV3)
                .metadados(meta)
                .evidencias(base != null ? base.getEvidencias() : null)
                .dataGeracao(Instant.now())
                .build();

        this.ultimaResposta = resposta;
        context.setUltimaResposta(resposta);
        context.memorizar("juridica_v3_executada", true);
        context.avancarEtapa("JURIDICA_V3_CONSOLIDACAO");
        return resposta;
    }

    private String gerarConsolidacao(String textoBase, IAPipelineContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(textoBase == null ? "" : textoBase.trim());
        if (sb.length() > 0) sb.append("\n\n");

        sb.append("[V3 - Consolidação]\n");
        sb.append("1) **Tese e enquadramento**: confirme classe/matéria/rito e competência antes do protocolo.\n");
        sb.append("2) **Ônus e prova**: liste documentos indispensáveis (contratos, comunicações, boletins, laudos, prints) e identifique prova testemunhal/pericial.\n");
        sb.append("3) **Riscos**: avalie prescrição/decadência, legitimidade, interesse de agir, litispendência e possibilidade de tutela de urgência.\n");
        sb.append("4) **Próximos passos**: (i) validar número/cadastro; (ii) montar cronologia; (iii) calcular valores; (iv) definir pedido principal + subsidiários; (v) preparar minuta com fundamentação.\n");
        sb.append("5) **Qualidade**: use linguagem objetiva, cite precedentes pertinentes e alinhe pedidos ao rito.\n");

        
        try {
            if (context.getUltimaResposta() != null && context.getUltimaResposta().getEssence() != null
                    && !context.getUltimaResposta().getEssence().isEmpty()) {
                sb.append("\nEssence (extraído): ").append(context.getUltimaResposta().getEssence()).append("\n");
            }
        } catch (Exception ignored) {
        }


        
        try {
            sb.append("\n\n[Hermenêutica – checklist]\n");
            for (String lens : philosophyCompass.lenses()) {
                sb.append("- ").append(lens).append("\n");
            }

            sb.append("\n[Referencial doutrinário sugerido]\n");
            Object payloadObj = context.getRequestEntrada() != null ? context.getRequestEntrada().getPayload() : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = payloadObj instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
            for (LegalPhilosopher p : philosophyCompass.suggest(payload)) {
                sb.append("- ").append(p.nome()).append(" — ").append(p.area()).append(": ").append(p.porQue()).append("\n");
            }
        } catch (Exception ignored) {
            
        }

        return sb.toString();
    }
}
