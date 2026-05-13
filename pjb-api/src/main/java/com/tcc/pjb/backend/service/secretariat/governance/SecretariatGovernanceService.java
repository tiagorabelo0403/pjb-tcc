package com.tcc.pjb.backend.service.secretariat.governance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioResolver;
import com.tcc.pjb.backend.core.forum.routing.ForumInstance;
import com.tcc.pjb.backend.core.forum.routing.ForumLane;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganKind;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganRef;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatCoverageSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatExceptionDeskSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatFormalCatalogSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatGovernanceSnapshotDto;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.operational.catalog.OperationalCompetenceMatrixService;
import com.tcc.pjb.backend.service.operational.catalog.OperationalCoveragePlannerService;
import com.tcc.pjb.backend.service.operational.catalog.ProceduralFormalCatalogService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatGovernanceService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SecretariatQueueItemRepository repository;
    private final SecretariatInstitutionalVisibilityService visibilityService;
    private final ForumDeskPortfolioResolver portfolioResolver;
    private final OperationalCompetenceMatrixService competenceMatrixService;
    private final OperationalCoveragePlannerService coveragePlannerService;
    private final ProceduralFormalCatalogService formalCatalogService;
    private final ObjectMapper objectMapper;

    public SecretariatGovernanceService(SecretariatQueueItemRepository repository,
                                        SecretariatInstitutionalVisibilityService visibilityService,
                                        ForumDeskPortfolioResolver portfolioResolver,
                                        OperationalCompetenceMatrixService competenceMatrixService,
                                        OperationalCoveragePlannerService coveragePlannerService,
                                        ProceduralFormalCatalogService formalCatalogService,
                                        ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.visibilityService = Objects.requireNonNull(visibilityService);
        this.portfolioResolver = Objects.requireNonNull(portfolioResolver);
        this.competenceMatrixService = Objects.requireNonNull(competenceMatrixService);
        this.coveragePlannerService = Objects.requireNonNull(coveragePlannerService);
        this.formalCatalogService = Objects.requireNonNull(formalCatalogService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public SecretariatGovernanceSnapshotDto governance(String inboxKey, Collection<String> statuses) {
        GovernanceContext context = loadContext(inboxKey, statuses);
        OperationalCompetenceMatrixService.MatrixProjection projection = competenceMatrixService.resolveSecretariat(
                context.inboxKey(),
                context.profile(),
                context.portfolio(),
                context.items(),
                context.metadataByWorkItemId()
        );
        LinkedHashMap<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("inbox", context.inboxKey());
        ctx.put("descriptor", context.inboxDescriptor());
        ctx.put("institutionalVisibility", context.profile().toMap());
        ctx.put("portfolio", context.portfolio().toMap());
        ctx.put("statusFilter", context.statuses());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>(projection.metrics());
        metrics.put("totalItems", context.items().size());
        metrics.put("blockingItems", context.items().stream().filter(SecretariatQueueItem::isBlocking).count());
        metrics.put("sensitiveItems", context.items().stream().filter(item -> item.isSecrecyReviewRequired() || item.isHearingSensitive()).count());
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("governancePath", OperationalApiRoutes.secretariatQueueGovernance());
        routes.put("exceptionsPath", OperationalApiRoutes.secretariatQueueExceptions());
        routes.put("coveragePath", OperationalApiRoutes.secretariatQueueCoverage());
        routes.put("formalCatalogPath", OperationalApiRoutes.secretariatQueueFormalCatalog());
        return new SecretariatGovernanceSnapshotDto(
                Instant.now(),
                context.inboxKey(),
                context.inboxDescriptor(),
                Map.copyOf(ctx),
                Map.copyOf(metrics),
                projection.rules().stream().map(rule -> new SecretariatGovernanceSnapshotDto.Rule(
                        rule.actCode(),
                        rule.actLabel(),
                        rule.actAxis(),
                        rule.minimumRole(),
                        rule.delegatedFunction(),
                        rule.ritoAxis(),
                        rule.ramoAxis(),
                        rule.phaseAxis(),
                        rule.secrecyAxis(),
                        rule.urgencyAxis(),
                        rule.functionalCredentialRequired(),
                        rule.compatibleCategories(),
                        rule.institutionalScopes(),
                        rule.signals()
                )).toList(),
                projection.warnings(),
                Map.copyOf(routes)
        );
    }

    @Transactional(readOnly = true)
    public SecretariatExceptionDeskSnapshotDto exceptions(String inboxKey, Collection<String> statuses) {
        GovernanceContext context = loadContext(inboxKey, statuses);
        List<SecretariatExceptionDeskSnapshotDto.ExceptionCase> exceptions = new ArrayList<>();
        for (SecretariatQueueItem item : context.items()) {
            Map<String, Object> metadata = context.metadataByWorkItemId().getOrDefault(item.getWorkItemId(), Map.of());
            List<String> reasons = classifyExceptions(item, metadata);
            if (!reasons.isEmpty()) {
                exceptions.add(new SecretariatExceptionDeskSnapshotDto.ExceptionCase(
                        item.getWorkItemId(),
                        item.getProcessoId(),
                        item.getTitulo(),
                        item.getStatus(),
                        severity(item, reasons),
                        reasons.getFirst(),
                        labelOf(reasons.getFirst()),
                        item.getDueAt(),
                        item.isBlocking(),
                        requiredActions(reasons),
                        compactMetadata(metadata)
                ));
            }
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("exceptionCount", exceptions.size());
        metrics.put("blockingExceptions", exceptions.stream().filter(SecretariatExceptionDeskSnapshotDto.ExceptionCase::blocking).count());
        metrics.put("criticalExceptions", exceptions.stream().filter(item -> "CRITICO".equals(item.severity())).count());
        metrics.put("queueItems", context.items().size());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (exceptions.isEmpty()) {
            warnings.add("Mesa de exceções sem pendências explícitas nesta leitura; manter observação para drift silencioso e contingência.");
        }
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("exceptionsPath", OperationalApiRoutes.secretariatQueueExceptions());
        routes.put("governancePath", OperationalApiRoutes.secretariatQueueGovernance());
        return new SecretariatExceptionDeskSnapshotDto(Instant.now(), context.inboxKey(), context.inboxDescriptor(), Map.copyOf(metrics), List.copyOf(exceptions), List.copyOf(warnings), Map.copyOf(routes));
    }

    @Transactional(readOnly = true)
    public SecretariatCoverageSnapshotDto coverage(String inboxKey, Collection<String> statuses) {
        GovernanceContext context = loadContext(inboxKey, statuses);
        OperationalCoveragePlannerService.CoverageProjection projection = coveragePlannerService.resolveSecretariat(context.inboxKey(), context.items());
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("coveragePath", OperationalApiRoutes.secretariatQueueCoverage());
        routes.put("governancePath", OperationalApiRoutes.secretariatQueueGovernance());
        return new SecretariatCoverageSnapshotDto(
                Instant.now(),
                context.inboxKey(),
                projection.mode(),
                projection.metrics(),
                projection.slices().stream().map(slice -> new SecretariatCoverageSnapshotDto.Cell(
                        slice.sliceKey(),
                        slice.label(),
                        slice.totalItems(),
                        slice.overdueItems(),
                        slice.blockingItems(),
                        slice.unassignedItems(),
                        slice.loadBand(),
                        slice.nextDueAt(),
                        slice.substitutePool(),
                        slice.redistributionSuggestions(),
                        slice.metrics()
                )).toList(),
                projection.gaps(),
                projection.warnings(),
                Map.copyOf(routes)
        );
    }

    @Transactional(readOnly = true)
    public SecretariatFormalCatalogSnapshotDto formalCatalog(String inboxKey, Collection<String> statuses) {
        GovernanceContext context = loadContext(inboxKey, statuses);
        ProceduralFormalCatalogService.FormalCatalogProjection projection = formalCatalogService.resolveSecretariatCatalog(context.inboxKey(), context.items(), context.metadataByWorkItemId());
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("formalCatalogPath", OperationalApiRoutes.secretariatQueueFormalCatalog());
        routes.put("governancePath", OperationalApiRoutes.secretariatQueueGovernance());
        return new SecretariatFormalCatalogSnapshotDto(
                Instant.now(),
                context.inboxKey(),
                projection.ramoAxis(),
                projection.ritoAxis(),
                projection.instanceAxis(),
                projection.metrics(),
                projection.documents().stream().map(document -> new SecretariatFormalCatalogSnapshotDto.DocumentTemplate(
                        document.documentCode(),
                        document.title(),
                        document.actAxis(),
                        document.targetBranch(),
                        document.targetPhase(),
                        document.urgencyAxis(),
                        document.sensitive(),
                        document.tags(),
                        document.metadata()
                )).toList(),
                projection.warnings(),
                Map.copyOf(routes)
        );
    }

    private GovernanceContext loadContext(String inboxKey, Collection<String> statuses) {
        String normalizedInboxKey = visibilityService.requireInboxAccess(inboxKey);
        List<String> effectiveStatuses = normalizeStatuses(statuses);
        List<SecretariatQueueItem> items = repository.listInbox(normalizedInboxKey, effectiveStatuses, PageRequest.of(0, 200)).getContent();
        Map<Long, Map<String, Object>> metadataByWorkItemId = new LinkedHashMap<>();
        for (SecretariatQueueItem item : items) {
            metadataByWorkItemId.put(item.getWorkItemId(), parseJson(item.getMetadataJson()));
        }
        SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile profile = visibilityService.describeAuthorizedInbox(normalizedInboxKey);
        ForumDeskPortfolioProfile portfolio = resolvePortfolio(normalizedInboxKey);
        return new GovernanceContext(normalizedInboxKey, resolveInboxDescriptor(normalizedInboxKey, portfolio), effectiveStatuses, profile, portfolio, List.copyOf(items), Map.copyOf(metadataByWorkItemId));
    }

    private List<String> classifyExceptions(SecretariatQueueItem item, Map<String, Object> metadata) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (metadataFlag(metadata, "missingContact") || metadataMissing(metadata, "principalContact") || metadataMissing(metadata, "contactEnvelope")) {
            reasons.add("CONTATO_INCOMPLETO");
        }
        if (metadataFlag(metadata, "venuePending") || contains(item.getQueueCode(), "AUDIENCIA") && metadataMissing(metadata, "venue")) {
            reasons.add("LOCAL_LINK_PENDENTE");
        }
        if (metadataFlag(metadata, "notificationPending") || contains(item.getQueueCode(), "INTIM", "CITAC") && metadataMissing(metadata, "notificationText")) {
            reasons.add("INTIMACAO_PENDENTE");
        }
        if (item.isBlocking()) {
            reasons.add("DILIGENCIA_IMPOSSIVEL");
        }
        if (item.isSecrecyReviewRequired()) {
            reasons.add("REVISAO_SIGILO");
        }
        if (item.isEscalationRequired()) {
            reasons.add("ESCALONAMENTO_NECESSARIO");
        }
        if (metadataFlag(metadata, "representationInvalid")) {
            reasons.add("REPRESENTACAO_INVALIDA");
        }
        if (metadataFlag(metadata, "wrongTargetOrgan")) {
            reasons.add("ORGAO_DESTINATARIO_INCORRETO");
        }
        if (metadataFlag(metadata, "ritualConflict")) {
            reasons.add("CONFLITO_DE_RITO");
        }
        return List.copyOf(reasons);
    }

    private String severity(SecretariatQueueItem item, List<String> reasons) {
        if (item.isBlocking() || item.isEscalationRequired() || reasons.contains("DILIGENCIA_IMPOSSIVEL")) {
            return "CRITICO";
        }
        if (item.isSecrecyReviewRequired() || reasons.contains("REVISAO_SIGILO")) {
            return "ALTO";
        }
        return "MODERADO";
    }

    private List<String> requiredActions(List<String> reasons) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        for (String reason : reasons) {
            switch (reason) {
                case "CONTATO_INCOMPLETO" -> actions.add("ENRIQUECER_CONTATOS");
                case "LOCAL_LINK_PENDENTE" -> actions.add("CONFIRMAR_LOCAL_OU_LINK");
                case "INTIMACAO_PENDENTE" -> actions.add("MATERIALIZAR_TEXTO_FINAL");
                case "DILIGENCIA_IMPOSSIVEL" -> actions.add("MIGRAR_PARA_CONTINGENCIA_ASSISTIDA");
                case "REVISAO_SIGILO" -> actions.add("REVALIDAR_SEGREGACAO_OPERACIONAL");
                case "ESCALONAMENTO_NECESSARIO" -> actions.add("ESCALONAR_PARA_CHEFIA");
                case "REPRESENTACAO_INVALIDA" -> actions.add("SANEAR_REPRESENTACAO");
                case "ORGAO_DESTINATARIO_INCORRETO" -> actions.add("REENCAMINHAR_ORGAO_DESTINO");
                case "CONFLITO_DE_RITO" -> actions.add("AJUSTAR_CATALOGO_FORMAL_POR_RITO");
                default -> actions.add("TRATAR_NA_MESA_DE_EXCECOES");
            }
        }
        return List.copyOf(actions);
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(raw, MAP_TYPE);
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, Object> compactMetadata(Map<String, Object> metadata) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return Collections.unmodifiableMap(out);
    }

    private boolean metadataFlag(Map<String, Object> metadata, String key) {
        Object raw = metadata.get(key);
        return raw instanceof Boolean flag && flag || raw instanceof String text && "true".equalsIgnoreCase(text);
    }

    private boolean metadataMissing(Map<String, Object> metadata, String key) {
        Object raw = metadata.get(key);
        return raw == null || raw instanceof String text && text.isBlank() || raw instanceof Collection<?> collection && collection.isEmpty();
    }

    private List<String> normalizeStatuses(Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of("PENDENTE", "EM_EXECUCAO");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String status : statuses) {
            if (status != null && !status.isBlank()) {
                normalized.add(status.trim().toUpperCase(Locale.ROOT));
            }
        }
        return normalized.isEmpty() ? List.of("PENDENTE", "EM_EXECUCAO") : List.copyOf(normalized);
    }

    private ForumDeskPortfolioProfile resolvePortfolio(String inboxKey) {
        return SecretariatInboxKeyParser.parse(inboxKey)
                .map(parts -> portfolioResolver.resolve(new ForumDeskKey(
                        parts.normalized(),
                        new JudicialOrganRef(firstNonBlank(parts.org(), "UNKNOWN"), organKind(parts.org()), firstNonBlank(parts.org(), "UNKNOWN")),
                        instance(parts.instance()),
                        ForumLane.fromToken(parts.lane()).orElse(ForumLane.COMUM),
                        parts.uf(),
                        "-".equals(parts.comarca()) ? "" : parts.comarca(),
                        parts.jurisdicao()
                )))
                .orElseGet(() -> portfolioResolver.resolve(new ForumDeskKey(
                        inboxKey,
                        JudicialOrganRef.unknown(),
                        ForumInstance.FIRST,
                        ForumLane.COMUM,
                        "XX",
                        "",
                        ""
                )));
    }

    private String resolveInboxDescriptor(String inboxKey, ForumDeskPortfolioProfile portfolio) {
        return SecretariatInboxKeyParser.parse(inboxKey)
                .map(parts -> parts.org() + '/' + parts.instance() + '/' + parts.lane() + '/' + parts.uf() + '/' + firstNonBlank(parts.comarca(), parts.jurisdicao(), "BASE"))
                .orElse(portfolio.operationalDescriptor());
    }

    private static ForumInstance instance(String token) {
        String normalized = token == null ? "" : token.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "2G", "SECOND" -> ForumInstance.SECOND;
            case "SUP", "SUPERIOR" -> ForumInstance.SUPERIOR;
            default -> ForumInstance.FIRST;
        };
    }

    private static JudicialOrganKind organKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return JudicialOrganKind.UNKNOWN;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("TRF")) return JudicialOrganKind.TRF;
        if (normalized.startsWith("TRT")) return JudicialOrganKind.TRT;
        if (normalized.startsWith("TRE")) return JudicialOrganKind.TRE;
        if (normalized.startsWith("TJM")) return JudicialOrganKind.TJM;
        if (normalized.startsWith("STM")) return JudicialOrganKind.STM;
        if (normalized.startsWith("STJ")) return JudicialOrganKind.STJ;
        if (normalized.startsWith("STF")) return JudicialOrganKind.STF;
        if (normalized.startsWith("TST")) return JudicialOrganKind.TST;
        if (normalized.startsWith("TSE")) return JudicialOrganKind.TSE;
        if (normalized.startsWith("TJ")) return JudicialOrganKind.TJ;
        return JudicialOrganKind.UNKNOWN;
    }

    private static boolean contains(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String labelOf(String code) {
        return switch (code) {
            case "CONTATO_INCOMPLETO" -> "Contato incompleto";
            case "LOCAL_LINK_PENDENTE" -> "Local ou link pendente";
            case "INTIMACAO_PENDENTE" -> "Intimação pendente";
            case "DILIGENCIA_IMPOSSIVEL" -> "Diligência impossível";
            case "REVISAO_SIGILO" -> "Revisão de sigilo";
            case "ESCALONAMENTO_NECESSARIO" -> "Escalonamento necessário";
            case "REPRESENTACAO_INVALIDA" -> "Representação inválida";
            case "ORGAO_DESTINATARIO_INCORRETO" -> "Órgão destinatário incorreto";
            case "CONFLITO_DE_RITO" -> "Conflito de rito";
            default -> code;
        };
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

    private record GovernanceContext(
            String inboxKey,
            String inboxDescriptor,
            List<String> statuses,
            SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile profile,
            ForumDeskPortfolioProfile portfolio,
            List<SecretariatQueueItem> items,
            Map<Long, Map<String, Object>> metadataByWorkItemId
    ) {
    }
}
