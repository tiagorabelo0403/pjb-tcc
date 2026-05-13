package com.tcc.pjb.backend.ai.juridica.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCorpusRegistryService;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCoverageService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCatalogManifestService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSourceView;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSyncSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeSourceDescriptor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LegalAiKnowledgeControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalKnowledgeCoverageService coverageService = mock(LegalKnowledgeCoverageService.class);
    private final LegalKnowledgeCorpusRegistryService corpusRegistryService = mock(LegalKnowledgeCorpusRegistryService.class);
    private final LegalKnowledgeCatalogManifestService manifestService = mock(LegalKnowledgeCatalogManifestService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalAiKnowledgeController(
                coverageService,
                corpusRegistryService,
                manifestService
        )).build();
    }

    @Test
    void knowledgeSurfaceMustExposeCatalogCoverageAndCorpusLifecycle() throws Exception {
        when(coverageService.catalogSummary()).thenReturn(Map.of(
                "status", "READY",
                "officialSourceCount", 5,
                "doctrineSourceCount", 2
        ));
        when(manifestService.summary()).thenReturn(Map.of(
                "status", "READY",
                "manifestVersion", "2026"
        ));
        when(corpusRegistryService.corpusSummary()).thenReturn(Map.of(
                "status", "READY",
                "totalSources", 7,
                "activeSources", 6
        ));
        when(corpusRegistryService.syncCatalog()).thenReturn(new LegalKnowledgeCorpusSyncSnapshot(
                "SYNCED",
                Instant.parse("2026-04-21T18:00:00Z"),
                7,
                2,
                15,
                List.of("planalto_constituicao", "stf_sumulas_vinculantes"),
                Map.of("mode", "INCREMENTAL")
        ));
        when(corpusRegistryService.sourceView("planalto_constituicao")).thenReturn(Optional.of(new LegalKnowledgeCorpusSourceView(
                "planalto_constituicao",
                "Constituição Federal",
                "NORMATIVE_TEXT",
                "MAXIMUM",
                "Planalto",
                "NORMATIVE_TEXT",
                "PUBLIC_OFFICIAL",
                "DAILY",
                "2026.04",
                true,
                false,
                true,
                3,
                2,
                Instant.parse("2026-04-21T17:00:00Z"),
                Instant.parse("2026-04-22T17:00:00Z"),
                List.of("CONSTITUCIONAL"),
                List.of("CONSTITUTION"),
                List.of("CF88"),
                List.of(),
                Map.of("checksum", "abc123")
        )));
        when(coverageService.inspect(any(), any(), any())).thenReturn(new LegalKnowledgeCoverageSnapshot(
                "READY",
                "BRANCH_GUIDED",
                List.of("CIVEL", "CONSTITUCIONAL"),
                List.of(new LegalKnowledgeSourceDescriptor(
                        "planalto_constituicao",
                        "Constituição Federal",
                        "NORMATIVE_TEXT",
                        "MAXIMUM",
                        "Planalto",
                        "NORMATIVE_TEXT",
                        "PUBLIC_OFFICIAL",
                        "https://www.planalto.gov.br",
                        "DAILY",
                        List.of("CONSTITUCIONAL"),
                        List.of("CONSTITUTION"),
                        List.of("CF88"),
                        List.of()
                )),
                List.of(),
                List.of("CONSTITUICAO", "CODIGOS"),
                List.of("OFFICIAL_ONLY"),
                List.of("branch-match"),
                Map.of("capability", "LEGAL_GENERAL_ASSIST_V3")
        ));

        LegalAiConversationRequest request = new LegalAiConversationRequest(
                "conv-knowledge",
                "PROC-9",
                "Quero base constitucional e rito aplicável.",
                "ADVOGADO",
                List.of(),
                List.of(),
                Map.of("sourceSystem", "CNJ")
        );

        mockMvc.perform(get("/api/ai/legal/knowledge/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.officialSourceCount").value(5));

        mockMvc.perform(get("/api/ai/legal/knowledge/catalog/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifestVersion").value("2026"));

        mockMvc.perform(get("/api/ai/legal/knowledge/corpus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSources").value(7));

        mockMvc.perform(post("/api/ai/legal/knowledge/corpus/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SYNCED"))
                .andExpect(jsonPath("$.changedSourceIds[0]").value("planalto_constituicao"));

        mockMvc.perform(get("/api/ai/legal/knowledge/corpus/sources/planalto_constituicao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value("planalto_constituicao"))
                .andExpect(jsonPath("$.officialSource").value(true));

        mockMvc.perform(post("/api/ai/legal/knowledge/coverage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("capability", "LEGAL_GENERAL_ASSIST_V3")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageMode").value("BRANCH_GUIDED"))
                .andExpect(jsonPath("$.officialSources[0].sourceId").value("planalto_constituicao"))
                .andExpect(jsonPath("$.diagnostics.capability").value("LEGAL_GENERAL_ASSIST_V3"));
    }
}
