package com.tcc.pjb.backend.ai.juridica.knowledge;

import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCatalogManifestService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeSurfaceTextCatalogService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeSourceDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeCoverageService {

    private final LegalKnowledgeSourceCatalogService catalogService;
    private final LegalKnowledgeCorpusRegistryService corpusRegistryService;
    private final LegalKnowledgeCatalogManifestService manifestService;
    private final LegalKnowledgeSurfaceTextCatalogService surfaceTextCatalogService;

    public LegalKnowledgeCoverageService(LegalKnowledgeSourceCatalogService catalogService,
                                         LegalKnowledgeCorpusRegistryService corpusRegistryService,
                                         LegalKnowledgeCatalogManifestService manifestService,
                                         LegalKnowledgeSurfaceTextCatalogService surfaceTextCatalogService) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
        this.corpusRegistryService = Objects.requireNonNull(corpusRegistryService, "corpusRegistryService");
        this.manifestService = Objects.requireNonNull(manifestService, "manifestService");
        this.surfaceTextCatalogService = Objects.requireNonNull(surfaceTextCatalogService, "surfaceTextCatalogService");
    }

    public LegalKnowledgeCoverageSnapshot inspect(LegalAiConversationRequest request,
                                                  String capability,
                                                  String version) {
        corpusRegistryService.ensureCatalogSeeded();
        List<String> matchedBranches = catalogService.inferBranches(
                request == null ? null : request.message(),
                request == null ? null : request.context()
        );
        List<LegalKnowledgeSourceDescriptor> officialSources = catalogService.selectForBranches(matchedBranches, false);
        List<LegalKnowledgeSourceDescriptor> doctrineSources = catalogService.listDoctrineSources().stream()
                .filter(item -> intersects(item.branches(), matchedBranches))
                .toList();
        LinkedHashSet<String> policies = new LinkedHashSet<>(catalogService.ingestionPolicies());
        if (!doctrineSources.isEmpty()) {
            policies.add(surfaceTextCatalogService.doctrineSeparateLanePolicyNote());
        }
        ArrayList<String> reasons = new ArrayList<>(catalogService.selectionReasons(!doctrineSources.isEmpty()));
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("selectedCapability", capability == null ? "" : capability);
        diagnostics.put("selectedVersion", version == null ? "" : version);
        diagnostics.put("matchedBranches", matchedBranches);
        diagnostics.put("officialSourceCount", officialSources.size());
        diagnostics.put("doctrineSourceCount", doctrineSources.size());
        diagnostics.put("doctrinePolicy", catalogService.doctrinePolicy());
        diagnostics.put("storageLanes", resolveStorageLanes(officialSources, doctrineSources));
        diagnostics.put("persistentCorpus", corpusRegistryService.corpusMetrics());
        diagnostics.put("catalogManifest", manifestService.summary());
        boolean hasDoctrine = !doctrineSources.isEmpty();
        return new LegalKnowledgeCoverageSnapshot(
                hasDoctrine ? surfaceTextCatalogService.officialPlusDoctrineReadyStatus() : surfaceTextCatalogService.officialReadyStatus(),
                hasDoctrine ? surfaceTextCatalogService.officialPlusDoctrineMode() : surfaceTextCatalogService.officialPrimaryMode(),
                matchedBranches,
                officialSources,
                doctrineSources,
                catalogService.priorityOrder(),
                List.copyOf(policies),
                List.copyOf(reasons),
                Map.copyOf(diagnostics)
        );
    }

    public Map<String, Object> catalogSummary() {
        corpusRegistryService.ensureCatalogSeeded();
        List<LegalKnowledgeSourceDescriptor> all = catalogService.listAll();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("totalSources", all.size());
        out.put("officialSources", catalogService.listOfficialSources().stream().map(LegalKnowledgeSourceDescriptor::asMap).toList());
        out.put("doctrineSources", catalogService.listDoctrineSources().stream().map(LegalKnowledgeSourceDescriptor::asMap).toList());
        out.put("doctrinePolicy", catalogService.doctrinePolicy());
        out.put("catalogManifest", manifestService.summary());
        out.put("corpus", corpusRegistryService.corpusSummary());
        return Collections.unmodifiableMap(out);
    }

    private List<String> resolveStorageLanes(List<LegalKnowledgeSourceDescriptor> officialSources,
                                             List<LegalKnowledgeSourceDescriptor> doctrineSources) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        officialSources.forEach(item -> out.add(item.storageLane()));
        doctrineSources.forEach(item -> out.add(item.storageLane()));
        return List.copyOf(out);
    }

    private boolean intersects(List<String> sourceBranches, List<String> requestedBranches) {
        if (sourceBranches == null || sourceBranches.isEmpty() || requestedBranches == null || requestedBranches.isEmpty()) {
            return true;
        }
        for (String item : sourceBranches) {
            if (requestedBranches.contains(item)) {
                return true;
            }
        }
        return false;
    }
}
