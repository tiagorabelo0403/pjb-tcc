package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalResearchDossierResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaResearchDossierService {

    private final VectorSearchService vectorSearchService;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private final LegalAiStructuredSchemaCatalog structuredSchemaCatalog;

    public JuridicaResearchDossierService(VectorSearchService vectorSearchService,
                                          JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                                          JuridicaLegalAiSpineService juridicaLegalAiSpineService,
                                          LegalAiStructuredSchemaCatalog structuredSchemaCatalog) {
        this.vectorSearchService = Objects.requireNonNull(vectorSearchService, "vectorSearchService");
        this.juridicaUnifiedMeshProfileService = Objects.requireNonNull(juridicaUnifiedMeshProfileService, "juridicaUnifiedMeshProfileService");
        this.juridicaLegalAiSpineService = Objects.requireNonNull(juridicaLegalAiSpineService, "juridicaLegalAiSpineService");
        this.structuredSchemaCatalog = Objects.requireNonNull(structuredSchemaCatalog, "structuredSchemaCatalog");
    }

    public LegalResearchDossierResponse build(LegalResearchDossierRequest request) {
        int topK = request != null && request.topK() != null && request.topK() > 0 ? request.topK() : 8;
        Map<String, Object> filtros = request != null && request.filtros() != null ? request.filtros() : Map.of();
        String query = normalizeQuery(request);

        var search = vectorSearchService.searchSimilarV2(query, filtros, topK);
        var mesh = juridicaUnifiedMeshProfileService.resolveForSurface(JuridicaSpineLabels.CAPABILITY_RESEARCH_DOSSIER, ApiVersion.V2);
        var spine = juridicaLegalAiSpineService.resolveForSurface(JuridicaSpineLabels.CAPABILITY_RESEARCH_DOSSIER, ApiVersion.V2);
        var recommendedSchema = structuredSchemaCatalog.recommend(ApiVersion.V2, JuridicaSpineLabels.CAPABILITY_RESEARCH_DOSSIER, null);

        List<Map<String, Object>> findings = search == null || search.resultados() == null
                ? List.of()
                : search.resultados().stream().map(item -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("docId", item.docId());
                    row.put("titulo", item.titulo());
                    row.put("ramo", item.ramo());
                    row.put("score", item.score());
                    row.put("cosine", item.cosine());
                    row.put("boost", item.boost());
                    return Map.copyOf(row);
                }).toList();

        LinkedHashMap<String, Object> trace = new LinkedHashMap<>();
        trace.put("lane", spine.trace().lane());
        trace.put("auditFields", spine.trace().requiredAuditFields());
        trace.put("meshRuntime", mesh.runtime());
        trace.put("query", query);
        trace.put("topK", topK);
        trace.put("vectorVersion", search == null ? "V2" : search.iaVersion());
        trace.put("citationEmissionMode", spine.hallucinationGuard().citationEmissionMode());
        trace.put("unresolvedCitationPlaceholder", spine.hallucinationGuard().unresolvedCitationPlaceholder());
        trace.put("recommendedStructuredSchema", recommendedSchema == null ? Map.of() : recommendedSchema.asMap());
        trace.put("structuredSchemaCatalog", structuredSchemaCatalog.resolve(ApiVersion.V2).stream().map(schema -> schema.asMap()).toList());

        return new LegalResearchDossierResponse(
                spine.profileCode(),
                spine.version(),
                spine.capability(),
                spine.retrieval().stages(),
                authorityLanes(spine),
                spine.graph().traversalModes(),
                mesh.tools().stream().map(tool -> tool.id()).toList(),
                spine.structuredOutputs().stream().map(output -> output.schemaId()).toList(),
                findings,
                Map.copyOf(trace)
        );
    }

    private String normalizeQuery(LegalResearchDossierRequest request) {
        if (request == null) {
            return "pesquisa juridica geral";
        }
        String assunto = value(request.assunto());
        String materia = value(request.materia());
        String contexto = value(request.contextoJuridico());
        String ramo = value(request.ramo());
        String rito = value(request.rito());
        String merged = String.join(" | ", List.of(assunto, materia, contexto, ramo, rito).stream().filter(value -> !value.isBlank()).toList());
        return merged.isBlank() ? "pesquisa juridica geral" : merged;
    }

    private List<String> authorityLanes(com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse spine) {
        Object lanes = spine.retrieval().retrievalPolicy().get("authorityLanes");
        if (lanes instanceof Iterable<?> iterable) {
            return java.util.stream.StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> item == null ? null : String.valueOf(item))
                    .filter(item -> item != null && !item.isBlank())
                    .toList();
        }
        return List.of();
    }

    private String value(String input) {
        return input == null ? "" : input.trim();
    }
}
