package com.tcc.pjb.backend.ai.juridica.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeCatalogManifestService;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeResourcePaths;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSourceView;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCorpusSyncSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeSourceDescriptor;
import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusArtifact;
import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusRevision;
import com.tcc.pjb.backend.model.entity.ai.legal.LegalKnowledgeCorpusSource;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.repository.ai.legal.LegalKnowledgeCorpusArtifactRepository;
import com.tcc.pjb.backend.repository.ai.legal.LegalKnowledgeCorpusRevisionRepository;
import com.tcc.pjb.backend.repository.ai.legal.LegalKnowledgeCorpusSourceRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class LegalKnowledgeCorpusRegistryService {

    private static final Map<String, String> INTERNAL_PACK_BY_SOURCE = Map.of(
            "PJB_RITOS_PACK_2026", LegalKnowledgeResourcePaths.RITO_PACK,
            "PJB_PRECEDENTES_SEED_2026", LegalKnowledgeResourcePaths.PRECEDENT_PACK,
            "PJB_MATERIAL_PACK_2026", LegalKnowledgeResourcePaths.MATERIAL_PACK,
            "PJB_CONSTITUTION_PACK_2026", LegalKnowledgeResourcePaths.CONSTITUTION_PACK,
            "PJB_SUMULA_PACK_2026", LegalKnowledgeResourcePaths.SUMULA_PACK,
            "PJB_DOCTRINE_CATALOG_2026", LegalKnowledgeResourcePaths.DOCTRINE_PACK,
            "PJB_SPECIAL_LAW_PACK_2026", LegalKnowledgeResourcePaths.SPECIAL_LAW_PACK,
            "PJB_QUALIFIED_PRECEDENT_PACK_2026", LegalKnowledgeResourcePaths.QUALIFIED_PRECEDENT_PACK,
            "PJB_GOVERNANCE_ACT_PACK_2026", LegalKnowledgeResourcePaths.GOVERNANCE_ACT_PACK
    );

    private final LegalKnowledgeSourceCatalogService catalogService;
    private final LegalKnowledgeCorpusSourceRepository sourceRepository;
    private final LegalKnowledgeCorpusRevisionRepository revisionRepository;
    private final LegalKnowledgeCorpusArtifactRepository artifactRepository;
    private final ObjectMapper objectMapper;
    private final LegalKnowledgeJsonResourceLoader resourceLoader;
    private final LegalKnowledgeCatalogManifestService manifestService;

    public LegalKnowledgeCorpusRegistryService(LegalKnowledgeSourceCatalogService catalogService,
                                               LegalKnowledgeCorpusSourceRepository sourceRepository,
                                               LegalKnowledgeCorpusRevisionRepository revisionRepository,
                                               LegalKnowledgeCorpusArtifactRepository artifactRepository,
                                               ObjectMapper objectMapper,
                                               LegalKnowledgeJsonResourceLoader resourceLoader,
                                               LegalKnowledgeCatalogManifestService manifestService) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
        this.sourceRepository = Objects.requireNonNull(sourceRepository, "sourceRepository");
        this.revisionRepository = Objects.requireNonNull(revisionRepository, "revisionRepository");
        this.artifactRepository = Objects.requireNonNull(artifactRepository, "artifactRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.manifestService = Objects.requireNonNull(manifestService, "manifestService");
    }

    public void ensureCatalogSeeded() {
        manifestService.summary();
        if (sourceRepository.count() == 0L) {
            syncCatalog();
        }
    }

    @Transactional
    @PjbTransactionalBudget(operation = "legal-ai.knowledge.catalog-sync.persist", maxMillis = 2500, critical = true)
    public LegalKnowledgeCorpusSyncSnapshot syncCatalog() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ArrayList<String> changed = new ArrayList<>();
        int totalArtifacts = 0;
        for (LegalKnowledgeSourceDescriptor descriptor : catalogService.listAll()) {
            UpsertOutcome outcome = upsertSource(descriptor, now);
            totalArtifacts += outcome.artifactCount();
            if (outcome.changed()) {
                changed.add(descriptor.sourceId());
            }
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("officialSourceCount", sourceRepository.countByOfficialSourceTrue());
        diagnostics.put("doctrineSourceCount", sourceRepository.countByDoctrineSourceTrue());
        diagnostics.put("storageLanes", sourceRepository.findAllByOrderByOfficialSourceDescDoctrineSourceAscInstitutionAscTitleAsc().stream()
                .map(LegalKnowledgeCorpusSource::getStorageLane)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        diagnostics.put("internalPackCount", INTERNAL_PACK_BY_SOURCE.size());
        diagnostics.put("catalogManifest", manifestService.summary());
        return new LegalKnowledgeCorpusSyncSnapshot(
                changed.isEmpty() ? "UP_TO_DATE" : "SYNCED",
                now,
                (int) sourceRepository.count(),
                changed.size(),
                totalArtifacts,
                List.copyOf(changed),
                Map.copyOf(diagnostics)
        );
    }

    @Transactional
    @PjbTransactionalBudget(operation = "legal-ai.knowledge.catalog-metrics.read", maxMillis = 800, critical = false)
    public Map<String, Object> corpusMetrics() {
        ensureCatalogSeeded();
        List<LegalKnowledgeCorpusSource> sources = sourceRepository.findAllByOrderByOfficialSourceDescDoctrineSourceAscInstitutionAscTitleAsc();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("totalSources", sources.size());
        out.put("officialSources", sourceRepository.countByOfficialSourceTrue());
        out.put("doctrineSources", sourceRepository.countByDoctrineSourceTrue());
        out.put("totalArtifacts", sources.stream().mapToInt(LegalKnowledgeCorpusSource::getArtifactCount).sum());
        out.put("totalRevisions", sources.stream().mapToInt(LegalKnowledgeCorpusSource::getRevisionCount).sum());
        out.put("storageLanes", sources.stream().map(LegalKnowledgeCorpusSource::getStorageLane).filter(Objects::nonNull).distinct().toList());
        out.put("catalogManifest", manifestService.summary());
        return Collections.unmodifiableMap(out);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "legal-ai.knowledge.catalog-summary.read", maxMillis = 1200, critical = false)
    public Map<String, Object> corpusSummary() {
        ensureCatalogSeeded();
        List<LegalKnowledgeCorpusSource> sources = sourceRepository.findAllByOrderByOfficialSourceDescDoctrineSourceAscInstitutionAscTitleAsc();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("totalSources", sources.size());
        out.put("officialSources", sourceRepository.countByOfficialSourceTrue());
        out.put("doctrineSources", sourceRepository.countByDoctrineSourceTrue());
        out.put("totalArtifacts", sources.stream().mapToInt(LegalKnowledgeCorpusSource::getArtifactCount).sum());
        out.put("totalRevisions", sources.stream().mapToInt(LegalKnowledgeCorpusSource::getRevisionCount).sum());
        out.put("sources", sources.stream().map(this::toView).map(LegalKnowledgeCorpusSourceView::asMap).toList());
        out.put("catalogManifest", manifestService.summary());
        return Collections.unmodifiableMap(out);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "legal-ai.knowledge.source-detail.read", maxMillis = 1200, critical = false)
    public Optional<LegalKnowledgeCorpusSourceView> sourceView(String sourceId) {
        ensureCatalogSeeded();
        return sourceRepository.findBySourceId(normalize(sourceId)).map(this::toView);
    }

    private UpsertOutcome upsertSource(LegalKnowledgeSourceDescriptor descriptor, Instant now) {
        String normalizedSourceId = normalize(descriptor.sourceId());
        LegalKnowledgeCorpusSource entity = sourceRepository.findBySourceId(normalizedSourceId)
                .orElseGet(LegalKnowledgeCorpusSource::new);
        boolean existing = entity.getId() != null;
        entity.setSourceId(normalizedSourceId);
        entity.setTitle(trim(descriptor.title()));
        entity.setSourceKind(normalize(descriptor.sourceKind()));
        entity.setAuthorityLevel(normalize(descriptor.authorityLevel()));
        entity.setInstitution(trim(descriptor.institution()));
        entity.setStorageLane(normalize(descriptor.storageLane()));
        entity.setLicensingModel(normalize(descriptor.licensingModel()));
        entity.setBaseUrl(trim(descriptor.baseUrl()));
        entity.setRefreshStrategy(normalize(descriptor.refreshStrategy()));
        entity.setBranchCodesJson(writeJson(descriptor.branches()));
        entity.setArtifactTypesJson(writeJson(descriptor.artifactTypes()));
        entity.setRetrievalTagsJson(writeJson(descriptor.retrievalTags()));
        entity.setRestrictionsJson(writeJson(descriptor.restrictions()));
        entity.setMetadataJson(writeJson(buildSourceMetadata(descriptor)));
        entity.setOfficialSource(isOfficial(descriptor));
        entity.setDoctrineSource("DOCTRINE".equals(normalize(descriptor.sourceKind())));
        entity.setActive(true);
        entity.setNextRefreshAt(resolveNextRefreshAt(entity.getRefreshStrategy(), now));
        String contentHash = computeSourceHash(descriptor);
        boolean changed = !existing || !contentHash.equals(entity.getContentHash());
        entity.setContentHash(contentHash);
        entity.setVersionTag(buildVersionTag(contentHash, now));
        entity.setLastSyncedAt(now);
        sourceRepository.save(entity);
        int artifactCount = synchronizeArtifacts(entity, descriptor, now, contentHash, changed);
        entity.setArtifactCount(artifactCount);
        entity.setRevisionCount(countRevisions(entity));
        sourceRepository.save(entity);
        return new UpsertOutcome(changed, artifactCount);
    }

    private int synchronizeArtifacts(LegalKnowledgeCorpusSource source,
                                     LegalKnowledgeSourceDescriptor descriptor,
                                     Instant now,
                                     String contentHash,
                                     boolean changed) {
        ArrayList<LegalKnowledgeCorpusArtifact> artifacts = new ArrayList<>();
        String sourceId = source.getSourceId();
        if ("PJB_RITOS_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadRitoArtifacts(source));
        } else if ("PJB_PRECEDENTES_SEED_2026".equals(sourceId)) {
            artifacts.addAll(loadPrecedentArtifacts(source));
        } else if ("PJB_MATERIAL_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadMaterialArtifacts(source));
        } else if ("PJB_CONSTITUTION_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadConstitutionArtifacts(source));
        } else if ("PJB_SUMULA_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadSumulaArtifacts(source));
        } else if ("PJB_DOCTRINE_CATALOG_2026".equals(sourceId)) {
            artifacts.addAll(loadDoctrineArtifacts(source));
        } else if ("PJB_SPECIAL_LAW_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadSpecialLawArtifacts(source));
        } else if ("PJB_QUALIFIED_PRECEDENT_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadQualifiedPrecedentArtifacts(source));
        } else if ("PJB_GOVERNANCE_ACT_PACK_2026".equals(sourceId)) {
            artifacts.addAll(loadGovernanceActArtifacts(source));
        }
        artifactRepository.deleteBySource_Id(source.getId());
        if (!artifacts.isEmpty()) {
            artifactRepository.saveAll(artifacts);
        }
        int artifactCount = artifacts.size();
        Optional<LegalKnowledgeCorpusRevision> latestRevision = revisionRepository.findTopBySource_IdOrderByHarvestedAtDesc(source.getId());
        if (changed || latestRevision.isEmpty() || latestRevision.get().getArtifactCount() != artifactCount) {
            LegalKnowledgeCorpusRevision revision = new LegalKnowledgeCorpusRevision();
            revision.setSource(source);
            revision.setRevisionKey(buildRevisionKey(contentHash, now));
            revision.setContentHash(contentHash);
            revision.setRevisionStatus("READY");
            revision.setArtifactCount(artifactCount);
            revision.setManifestJson(writeJson(buildRevisionManifest(descriptor, artifactCount, now)));
            revision.setHarvestedAt(now);
            revisionRepository.save(revision);
        }
        source.setRevisionCount(countRevisions(source));
        return artifactCount;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        return null;
    }

    private Map<String, Object> materializeMetadata(JsonNode item, String kind) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("kind", kind);
        forEachObjectField(item, (fieldName, value) -> {
            if ("title".equals(fieldName) || "titulo".equals(fieldName) || "summary".equals(fieldName)
                    || "ementaResumo".equals(fieldName) || "descricao".equals(fieldName) || "referenceUrl".equals(fieldName)
                    || "urlReferencia".equals(fieldName) || "id".equals(fieldName) || "branchCode".equals(fieldName)
                    || "ramoSugerido".equals(fieldName) || "effectiveDate".equals(fieldName) || "dataPublicacao".equals(fieldName)
                    || "editionDate".equals(fieldName)) {
                return;
            }
            if (value.isArray()) {
                metadata.put(fieldName, readArray(value));
            } else if (value.isBoolean()) {
                metadata.put(fieldName, value.asBoolean());
            } else if (value.isNumber()) {
                metadata.put(fieldName, value.numberValue());
            } else {
                String textValue = trim(value.asText());
                if (textValue != null) {
                    metadata.put(fieldName, textValue);
                }
            }
        });
        return Collections.unmodifiableMap(metadata);
    }

    private int countRevisions(LegalKnowledgeCorpusSource source) {
        return (int) revisionRepository.countBySource_Id(source.getId());
    }

    private List<LegalKnowledgeCorpusArtifact> loadRitoArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.RITO_PACK);
        JsonNode definitions = root.path("definitions");
        forEachObjectField(definitions, (fieldName, definition) -> {
            String rito = normalize(fieldName);
            String branchCode = normalize(definition.path("ramoSugerido").asText());
            String title = trim(definition.path("title").asText());
            addArtifact(out, source, rito, rito, "PROCEDURAL_RITE", branchCode, title,
                    summarizeRitoDefinition(definition), null, null,
                    Map.of("kind", "RITO_DEFINITION", "stageCount", definition.path("stages").size()));
            for (JsonNode stage : definition.path("stages")) {
                String fase = normalize(stage.path("fase").asText());
                addArtifact(out, source, rito + ':' + fase, fase, "PROCEDURAL_STAGE", branchCode,
                        title + " — " + fase.replace('_', ' '), summarizeStage(stage), null, null,
                        Map.of("kind", "RITO_STAGE", "allowedNext", readArray(stage.path("allowedNext"))));
                for (JsonNode work : stage.path("work")) {
                    String code = trim(work.path("code").asText());
                    addArtifact(out, source, code, code, normalize(work.path("type").asText("WORK_ITEM")), branchCode,
                            trim(work.path("title").asText()), trim(work.path("description").asText()), null, null,
                            Map.of(
                                    "kind", "RITO_WORK_ITEM",
                                    "fase", fase,
                                    "actorRole", normalize(work.path("actorRole").asText()),
                                    "priority", work.path("priority").asInt(0),
                                    "slaDays", work.path("slaDays").asInt(0),
                                    "blocking", work.path("blocking").asBoolean(false),
                                    "legalBases", readArray(work.path("legalBases"))
                            ));
                }
            }
        });
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadPrecedentArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.PRECEDENT_PACK);
        for (JsonNode item : root) {
            String key = normalize(item.path("fonte").asText()) + '_' + normalize(item.path("identificador").asText());
            String branchCode = normalize(item.path("ramoSugerido").asText());
            LocalDate effectiveDate = parseDate(item.path("dataPublicacao").asText());
            addArtifact(out, source, key, trim(item.path("identificador").asText()), normalize(item.path("tipo").asText()),
                    branchCode, trim(item.path("titulo").asText()), trim(item.path("ementaResumo").asText()),
                    trim(item.path("urlReferencia").asText()), effectiveDate,
                    Map.of(
                            "kind", "PRECEDENT_SEED",
                            "fonte", trim(item.path("fonte").asText()),
                            "tese", trim(item.path("tese").asText()),
                            "ritoSugerido", normalize(item.path("ritoSugerido").asText())
                    ));
        }
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadMaterialArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.MATERIAL_PACK);
        JsonNode byRamo = root.path("byRamo");
        forEachObjectField(byRamo, (fieldName, item) -> {
            String branchCode = normalize(fieldName);
            addArtifact(out, source, "RAMO_" + branchCode, branchCode, "INTERNAL_MATERIAL_BRANCH", branchCode,
                    branchCode.replace('_', ' ') + " — material interno controlado", summarizeMaterial(item), null, null,
                    Map.of(
                            "kind", "MATERIAL_BRANCH",
                            "requiredDocuments", readArray(item.path("requiredDocuments")),
                            "proofChecklist", readArray(item.path("proofChecklist")),
                            "legalBases", readArray(item.path("legalBases")),
                            "warnings", readArray(item.path("warnings"))
                    ));
        });
        JsonNode byRito = root.path("byRito");
        forEachObjectField(byRito, (fieldName, item) -> {
            String rito = normalize(fieldName);
            addArtifact(out, source, "RITO_" + rito, rito, "INTERNAL_MATERIAL_RITE", normalize(item.path("branchCode").asText("JUIZADOS")),
                    rito.replace('_', ' ') + " — material interno controlado", summarizeMaterial(item), null, null,
                    Map.of(
                            "kind", "MATERIAL_RITE",
                            "requiredDocuments", readArray(item.path("requiredDocuments")),
                            "proofChecklist", readArray(item.path("proofChecklist")),
                            "legalBases", readArray(item.path("legalBases")),
                            "warnings", readArray(item.path("warnings"))
                    ));
        });
        return out;
    }

    private void forEachObjectField(JsonNode node, java.util.function.BiConsumer<String, JsonNode> consumer) {
        if (node == null || !node.isObject()) {
            return;
        }
        var fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            consumer.accept(fieldName, node.path(fieldName));
        }
    }

    private List<LegalKnowledgeCorpusArtifact> loadConstitutionArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.CONSTITUTION_PACK);
        for (JsonNode item : root) {
            String key = normalize(item.path("tema").asText()) + '_' + normalize(item.path("titulo").asText());
            addArtifact(out, source, key, trim(item.path("tema").asText()), "CONSTITUTION_TOPIC",
                    normalize(item.path("branchCode").asText()), trim(item.path("titulo").asText()), trim(item.path("summary").asText()), null, null,
                    Map.of(
                            "kind", "CONSTITUTION_TOPIC",
                            "tema", trim(item.path("tema").asText()),
                            "artigos", readArray(item.path("artigos")),
                            "keywords", readArray(item.path("keywords")),
                            "relatedRitos", readArray(item.path("relatedRitos"))
                    ));
        }
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadSumulaArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.SUMULA_PACK);
        for (JsonNode item : root) {
            String externalId = trim(item.path("identificador").asText());
            String key = normalize(item.path("tribunal").asText()) + '_' + normalize(externalId);
            addArtifact(out, source, key, externalId, normalize(item.path("tipo").asText()),
                    normalize(item.path("branchCode").asText()), trim(item.path("titulo").asText()), trim(item.path("summary").asText()), null, null,
                    Map.of(
                            "kind", "CURATED_SUMULA_ENTRY",
                            "tribunal", trim(item.path("tribunal").asText()),
                            "keywords", readArray(item.path("keywords")),
                            "authorityLevel", normalize(item.path("authorityLevel").asText())
                    ));
        }
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadDoctrineArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.DOCTRINE_PACK);
        for (JsonNode item : root) {
            String catalogId = trim(item.path("catalogId").asText());
            addArtifact(out, source, catalogId, catalogId, normalize(item.path("artifactType").asText("LICENSED_DOCTRINE_GUIDE")),
                    normalize(item.path("branchCode").asText()), trim(item.path("title").asText()), trim(item.path("summary").asText()), null, null,
                    Map.of(
                            "kind", "DOCTRINE_CATALOG_ENTRY",
                            "authors", readArray(item.path("authors")),
                            "editionOrVersion", trim(item.path("editionOrVersion").asText()),
                            "keywords", readArray(item.path("keywords")),
                            "priority", item.path("priority").asInt(0),
                            "licensingModel", normalize(item.path("licensingModel").asText())
                    ));
        }
        return out;
    }
    private List<LegalKnowledgeCorpusArtifact> loadSpecialLawArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.SPECIAL_LAW_PACK);
        for (JsonNode item : root) {
            String lawId = trim(item.path("lawId").asText());
            addArtifact(out, source, lawId, trim(item.path("officialId").asText()), normalize(item.path("artifactType").asText("SPECIAL_LAW")),
                    normalize(item.path("branchCode").asText()), trim(item.path("title").asText()), trim(item.path("summary").asText()), trim(item.path("sourceUrl").asText()), null,
                    Map.of(
                            "kind", "SPECIAL_LAW_ENTRY",
                            "keywords", readArray(item.path("keywords")),
                            "relatedRitos", readArray(item.path("relatedRitos")),
                            "authorityLevel", normalize(item.path("authorityLevel").asText())
                    ));
        }
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadQualifiedPrecedentArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.QUALIFIED_PRECEDENT_PACK);
        for (JsonNode item : root) {
            String identifier = trim(item.path("identifier").asText());
            String tribunal = normalize(item.path("tribunal").asText());
            String artifactKey = tribunal + '_' + normalize(identifier);
            addArtifact(out, source, artifactKey, identifier, normalize(item.path("trackType").asText("QUALIFIED_PRECEDENT")),
                    normalize(item.path("branchCode").asText()), trim(item.path("title").asText()), trim(item.path("summary").asText()), trim(item.path("officialUrl").asText()), null,
                    Map.of(
                            "kind", "QUALIFIED_PRECEDENT_ENTRY",
                            "tribunal", trim(item.path("tribunal").asText()),
                            "trackType", normalize(item.path("trackType").asText()),
                            "keywords", readArray(item.path("keywords")),
                            "authorityLevel", normalize(item.path("authorityLevel").asText())
                    ));
        }
        return out;
    }

    private List<LegalKnowledgeCorpusArtifact> loadGovernanceActArtifacts(LegalKnowledgeCorpusSource source) {
        ArrayList<LegalKnowledgeCorpusArtifact> out = new ArrayList<>();
        JsonNode root = readTree(LegalKnowledgeResourcePaths.GOVERNANCE_ACT_PACK);
        for (JsonNode item : root) {
            String actId = trim(item.path("actId").asText());
            addArtifact(out, source, actId, actId, normalize(item.path("artifactType").asText("GOVERNANCE_ACT")),
                    normalize(item.path("branchCode").asText()), trim(item.path("title").asText()), trim(item.path("summary").asText()), trim(item.path("sourceUrl").asText()), null,
                    Map.of(
                            "kind", "GOVERNANCE_ACT_ENTRY",
                            "keywords", readArray(item.path("keywords")),
                            "authorityLevel", normalize(item.path("authorityLevel").asText())
                    ));
        }
        return out;
    }


    private void addArtifact(List<LegalKnowledgeCorpusArtifact> out,
                             LegalKnowledgeCorpusSource source,
                             String artifactKey,
                             String externalId,
                             String artifactType,
                             String branchCode,
                             String title,
                             String excerpt,
                             String sourceUrl,
                             LocalDate effectiveDate,
                             Map<String, Object> metadata) {
        LegalKnowledgeCorpusArtifact artifact = new LegalKnowledgeCorpusArtifact();
        artifact.setSource(source);
        artifact.setArtifactKey(normalize(artifactKey));
        artifact.setExternalId(blankToNull(externalId));
        artifact.setArtifactType(normalize(artifactType));
        artifact.setBranchCode(blankToNull(normalize(branchCode)));
        artifact.setStorageLane(source.getStorageLane());
        artifact.setAuthorityLevel(source.getAuthorityLevel());
        artifact.setTitle(trim(title));
        artifact.setExcerpt(trim(excerpt));
        artifact.setSourceUrl(blankToNull(trim(sourceUrl)));
        artifact.setEffectiveDate(effectiveDate);
        artifact.setMetadataJson(writeJson(metadata));
        artifact.setContentHash(sha256(artifact.getArtifactKey() + '|' + artifact.getTitle() + '|' + artifact.getMetadataJson()));
        out.add(artifact);
    }

    private LegalKnowledgeCorpusSourceView toView(LegalKnowledgeCorpusSource source) {
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("contentHash", source.getContentHash());
        diagnostics.put("baseUrl", source.getBaseUrl());
        diagnostics.put("sampleArtifacts", artifactRepository.findTop10BySource_IdOrderByTitleAsc(source.getId()).stream().map(item -> Map.of(
                "artifactKey", item.getArtifactKey(),
                "artifactType", item.getArtifactType(),
                "title", item.getTitle(),
                "branchCode", item.getBranchCode() == null ? "" : item.getBranchCode()
        )).toList());
        return new LegalKnowledgeCorpusSourceView(
                source.getSourceId(),
                source.getTitle(),
                source.getSourceKind(),
                source.getAuthorityLevel(),
                source.getInstitution(),
                source.getStorageLane(),
                source.getLicensingModel(),
                source.getRefreshStrategy(),
                source.getVersionTag(),
                source.isOfficialSource(),
                source.isDoctrineSource(),
                source.isActive(),
                source.getArtifactCount(),
                source.getRevisionCount(),
                source.getLastSyncedAt(),
                source.getNextRefreshAt(),
                readArray(source.getBranchCodesJson()),
                readArray(source.getArtifactTypesJson()),
                readArray(source.getRetrievalTagsJson()),
                readArray(source.getRestrictionsJson()),
                Map.copyOf(diagnostics)
        );
    }

    private JsonNode readTree(String resourcePath) {
        return resourceLoader.readTree(resourcePath);
    }

    private boolean isOfficial(LegalKnowledgeSourceDescriptor descriptor) {
        String authorityLevel = normalize(descriptor.authorityLevel());
        return "PRIMARY_OFFICIAL".equals(authorityLevel) || "OFFICIAL_CURATED".equals(authorityLevel);
    }

    private Map<String, Object> buildSourceMetadata(LegalKnowledgeSourceDescriptor descriptor) {
        TreeMap<String, Object> metadata = new TreeMap<>();
        metadata.put("sourceId", normalize(descriptor.sourceId()));
        metadata.put("sourceKind", normalize(descriptor.sourceKind()));
        metadata.put("authorityLevel", normalize(descriptor.authorityLevel()));
        metadata.put("institution", trim(descriptor.institution()));
        metadata.put("storageLane", normalize(descriptor.storageLane()));
        metadata.put("licensingModel", normalize(descriptor.licensingModel()));
        metadata.put("baseUrl", trim(descriptor.baseUrl()));
        metadata.put("refreshStrategy", normalize(descriptor.refreshStrategy()));
        metadata.put("branches", descriptor.branches() == null ? List.of() : List.copyOf(descriptor.branches()));
        metadata.put("artifactTypes", descriptor.artifactTypes() == null ? List.of() : List.copyOf(descriptor.artifactTypes()));
        metadata.put("retrievalTags", descriptor.retrievalTags() == null ? List.of() : List.copyOf(descriptor.retrievalTags()));
        metadata.put("restrictions", descriptor.restrictions() == null ? List.of() : List.copyOf(descriptor.restrictions()));
        return metadata;
    }

    private Map<String, Object> buildRevisionManifest(LegalKnowledgeSourceDescriptor descriptor, int artifactCount, Instant now) {
        TreeMap<String, Object> manifest = new TreeMap<>();
        manifest.put("sourceId", normalize(descriptor.sourceId()));
        manifest.put("harvestedAt", now.toString());
        manifest.put("artifactCount", artifactCount);
        manifest.put("storageLane", normalize(descriptor.storageLane()));
        manifest.put("refreshStrategy", normalize(descriptor.refreshStrategy()));
        manifest.put("branches", descriptor.branches() == null ? List.of() : List.copyOf(descriptor.branches()));
        return manifest;
    }

    private Instant resolveNextRefreshAt(String refreshStrategy, Instant now) {
        return switch (normalize(refreshStrategy)) {
            case "OFFICIAL_PULL_HTML", "OFFICIAL_PULL_SEARCH" -> now.plus(1, ChronoUnit.DAYS);
            case "INTERNAL_CURATED_REFRESH", "LICENSED_DOCTRINE_SYNC" -> now.plus(7, ChronoUnit.DAYS);
            default -> now.plus(3, ChronoUnit.DAYS);
        };
    }

    private String computeSourceHash(LegalKnowledgeSourceDescriptor descriptor) {
        TreeMap<String, Object> canonical = new TreeMap<>(buildSourceMetadata(descriptor));
        String packPath = INTERNAL_PACK_BY_SOURCE.get(normalize(descriptor.sourceId()));
        if (packPath != null) {
            canonical.put("internalPackHash", resourceHash(packPath));
        }
        return sha256(writeJson(canonical));
    }

    private String resourceHash(String path) {
        return sha256(resourceLoader.readUtf8(path));
    }

    private String buildVersionTag(String contentHash, Instant now) {
        return now.truncatedTo(ChronoUnit.SECONDS) + "-" + contentHash.substring(0, 12);
    }

    private String buildRevisionKey(String contentHash, Instant now) {
        return now.truncatedTo(ChronoUnit.SECONDS) + "-" + contentHash.substring(0, 16);
    }

    private String summarizeRitoDefinition(JsonNode definition) {
        return trim(definition.path("title").asText()) + " | stages=" + definition.path("stages").size();
    }

    private String summarizeStage(JsonNode stage) {
        return normalize(stage.path("fase").asText()) + " | allowedNext=" + readArray(stage.path("allowedNext")).size();
    }

    private String summarizeMaterial(JsonNode item) {
        return "docs=" + item.path("requiredDocuments").size() + " | provas=" + item.path("proofChecklist").size() + " | bases=" + item.path("legalBases").size();
    }

    private List<String> readArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return readArray(node);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> readArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        node.forEach(item -> {
            String value = trim(item.asText());
            if (!value.isBlank()) {
                out.add(value);
            }
        });
        return List.copyOf(out);
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                out.append(String.format(Locale.ROOT, "%02x", item));
            }
            return out.toString();
        } catch (Exception e) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String normalized = trim(value);
        return normalized.isBlank() ? null : normalized;
    }

    private record UpsertOutcome(boolean changed, int artifactCount) {
    }
}
