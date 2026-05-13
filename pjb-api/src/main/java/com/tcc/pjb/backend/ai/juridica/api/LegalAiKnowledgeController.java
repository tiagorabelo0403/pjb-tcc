package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCorpusRegistryService;
import com.tcc.pjb.backend.ai.juridica.knowledge.LegalKnowledgeCoverageService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCatalogManifestService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSourceView;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSyncSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai/legal/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class LegalAiKnowledgeController {

    private final LegalKnowledgeCoverageService coverageService;
    private final LegalKnowledgeCorpusRegistryService corpusRegistryService;
    private final LegalKnowledgeCatalogManifestService manifestService;

    public LegalAiKnowledgeController(LegalKnowledgeCoverageService coverageService,
                                      LegalKnowledgeCorpusRegistryService corpusRegistryService,
                                      LegalKnowledgeCatalogManifestService manifestService) {
        this.coverageService = Objects.requireNonNull(coverageService, "coverageService");
        this.corpusRegistryService = Objects.requireNonNull(corpusRegistryService, "corpusRegistryService");
        this.manifestService = Objects.requireNonNull(manifestService, "manifestService");
    }

    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> sources() {
        return ResponseEntity.ok(coverageService.catalogSummary());
    }


    @GetMapping("/catalog/health")
    public ResponseEntity<Map<String, Object>> catalogHealth() {
        return ResponseEntity.ok(manifestService.summary());
    }

    @GetMapping("/corpus")
    public ResponseEntity<Map<String, Object>> corpus() {
        return ResponseEntity.ok(corpusRegistryService.corpusSummary());
    }

    @PostMapping("/corpus/sync")
    public ResponseEntity<LegalKnowledgeCorpusSyncSnapshot> syncCorpus() {
        return ResponseEntity.ok(corpusRegistryService.syncCatalog());
    }

    @GetMapping("/corpus/sources/{sourceId}")
    public ResponseEntity<LegalKnowledgeCorpusSourceView> corpusSource(@PathVariable String sourceId) {
        return corpusRegistryService.sourceView(sourceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/coverage", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalKnowledgeCoverageSnapshot> coverage(@Valid @RequestBody LegalAiConversationRequest request,
                                                                  @RequestParam(required = false) String capability,
                                                                  @RequestParam(required = false) String version) {
        return ResponseEntity.ok(coverageService.inspect(request, capability, version));
    }
}
