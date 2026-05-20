package com.tcc.pjb.backend.core.frontend.delivery.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendBootstrapView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliverySummary;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDomainView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureBlockerView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PjbFrontendDeliveryApplicationService {

    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:public\\s+)?(?:record|class|interface|enum)\\s+(\\w+)");
    private static final Pattern REQUEST_MAPPING = Pattern.compile("@RequestMapping\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern HTTP_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final List<String> PRIORITY_PREFIXES = List.of(
            "/api/v1/ui",
            "/api/v1/auth",
            "/api/v1/publico",
            "/api/v1/consulta",
            "/api/v1/processual",
            "/api/v1/offline",
            "/api/v1/admin/final-closure",
            "/api/v1/admin/roadmap/closure",
            "/api/v1/frontend/delivery"
    );

    private final PjbFinalClosureApplicationService finalClosureApplicationService;
    private final PjbRoadmapClosureApplicationService roadmapClosureApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    @Autowired
    public PjbFrontendDeliveryApplicationService(PjbFinalClosureApplicationService finalClosureApplicationService,
                                                 PjbRoadmapClosureApplicationService roadmapClosureApplicationService,
                                                 PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                 AuditLedgerService auditLedgerService) {
        this(finalClosureApplicationService, roadmapClosureApplicationService, apiSurfaceSanityApplicationService, auditLedgerService, Path.of(""));
    }

    PjbFrontendDeliveryApplicationService(PjbFinalClosureApplicationService finalClosureApplicationService,
                                          PjbRoadmapClosureApplicationService roadmapClosureApplicationService,
                                          PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                          AuditLedgerService auditLedgerService,
                                          Path projectRoot) {
        this.finalClosureApplicationService = Objects.requireNonNull(finalClosureApplicationService);
        this.roadmapClosureApplicationService = Objects.requireNonNull(roadmapClosureApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    @Transactional(readOnly = true)
    public PjbFrontendDeliverySummary summary() {
        List<PjbFrontendRouteView> routes = routes();
        PjbFinalClosureSummary finalSummary = finalClosureApplicationService.summary();
        PjbRoadmapClosureSummary roadmapSummary = roadmapClosureApplicationService.summary();
        var apiSurface = apiSurfaceSanityApplicationService.auditar();
        int adminRoutes = (int) routes.stream().filter(PjbFrontendRouteView::adminSurface).count();
        int uiRoutes = (int) routes.stream().filter(PjbFrontendRouteView::uiSurface).count();
        int publicRoutes = routes.size() - adminRoutes;
        int controllers = (int) routes.stream().map(PjbFrontendRouteView::controller).distinct().count();
        boolean roadmapReady = roadmapSummary.total() > 0 && roadmapSummary.surfacedPartial() >= roadmapSummary.closed();
        boolean readyForFrontend = finalSummary.buildApproved()
                && apiSurface.limpo()
                && !routes.isEmpty()
                && controllers > 0
                && roadmapReady;
        PjbFrontendDeliverySummary summary = new PjbFrontendDeliverySummary(
                readyForFrontend,
                finalSummary.buildApproved(),
                apiSurface.limpo(),
                roadmapReady,
                routes.size(),
                adminRoutes,
                uiRoutes,
                publicRoutes,
                controllers,
                roadmapSummary.surfacedPartial(),
                blockers().size(),
                Instant.now());
        auditLedgerService.appendSafely("FRONTEND_DELIVERY_SUMMARY_QUERY", "FRONTEND", "DELIVERY", "routes=" + summary.totalRoutes() + " blockers=" + summary.blockerCount());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendRouteView> routes() {
        Path root = projectRoot.resolve("src/main/java/com/tcc/pjb/backend/controller");
        List<PjbFrontendRouteView> routes = scanRoutes(root);
        auditLedgerService.appendSafely("FRONTEND_DELIVERY_ROUTES_QUERY", "FRONTEND", "DELIVERY", "routes=" + routes.size());
        return routes;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendDomainView> domains() {
        LinkedHashMap<String, List<PjbFrontendRouteView>> grouped = new LinkedHashMap<>();
        for (PjbFrontendRouteView route : routes()) {
            grouped.computeIfAbsent(route.domain(), ignored -> new ArrayList<>()).add(route);
        }
        List<PjbFrontendDomainView> domains = grouped.entrySet().stream()
                .map(entry -> new PjbFrontendDomainView(
                        entry.getKey(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream().map(PjbFrontendRouteView::controller).distinct().count(),
                        entry.getValue().stream().allMatch(PjbFrontendRouteView::adminSurface),
                        entry.getValue().stream().map(PjbFrontendRouteView::path).distinct().sorted().limit(5).toList()))
                .sorted(Comparator.comparingInt(PjbFrontendDomainView::routeCount).reversed().thenComparing(PjbFrontendDomainView::domain))
                .toList();
        auditLedgerService.appendSafely("FRONTEND_DELIVERY_DOMAINS_QUERY", "FRONTEND", "DELIVERY", "domains=" + domains.size());
        return domains;
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendDeliveryBlockerView> blockers() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<PjbFrontendDeliveryBlockerView> blockers = new ArrayList<>();
        for (PjbFinalClosureBlockerView blocker : finalClosureApplicationService.blockers()) {
            String key = blocker.scope() + '|' + blocker.code() + '|' + blocker.summary();
            if (seen.add(key)) {
                blockers.add(new PjbFrontendDeliveryBlockerView(blocker.scope(), blocker.severity(), blocker.code(), blocker.summary()));
            }
            if (blockers.size() >= 25) {
                break;
            }
        }
        if (routes().isEmpty()) {
            blockers.add(new PjbFrontendDeliveryBlockerView("frontend", "CRITICO", "frontend.routes.empty", "Nenhuma rota HTTP detectada para consumo do frontend."));
        }
        auditLedgerService.appendSafely("FRONTEND_DELIVERY_BLOCKERS_QUERY", "FRONTEND", "DELIVERY", "blockers=" + blockers.size());
        return List.copyOf(blockers);
    }

    @Transactional(readOnly = true)
    public PjbFrontendBootstrapView bootstrap() {
        PjbFrontendDeliverySummary summary = summary();
        List<PjbFrontendDomainView> domains = domains().stream().limit(12).toList();
        List<PjbFrontendRouteView> priorityRoutes = priorityRoutes(routes());
        List<PjbFrontendDeliveryBlockerView> blockers = blockers().stream().limit(12).toList();
        List<String> nextSteps = List.of(
                "Consumir /api/v1/frontend/delivery/routes para mapear rotas utilizaveis pelo frontend.",
                "Consumir /api/v1/frontend/delivery/domains para organizar telas por dominio funcional.",
                "Consumir /api/v1/frontend/delivery/blockers antes de prometer fluxo fechado no frontend.",
                "Cruzar este bootstrap com /v3/api-docs e com os controllers UI ja existentes.",
                "Usar o resumo para saber se a base esta pronta para integracao ou ainda bloqueada por build/surface."
        );
        PjbFrontendBootstrapView bootstrap = new PjbFrontendBootstrapView(summary, domains, priorityRoutes, blockers, nextSteps, Instant.now());
        auditLedgerService.appendSafely("FRONTEND_DELIVERY_BOOTSTRAP_QUERY", "FRONTEND", "DELIVERY", "domains=" + domains.size() + " priorityRoutes=" + priorityRoutes.size());
        return bootstrap;
    }

    private List<PjbFrontendRouteView> priorityRoutes(List<PjbFrontendRouteView> routes) {
        Comparator<PjbFrontendRouteView> comparator = Comparator
                .comparingInt((PjbFrontendRouteView route) -> priorityIndex(route.path()))
                .thenComparing(PjbFrontendRouteView::path)
                .thenComparing(PjbFrontendRouteView::method);
        return routes.stream().sorted(comparator).limit(40).toList();
    }

    private int priorityIndex(String path) {
        for (int i = 0; i < PRIORITY_PREFIXES.size(); i++) {
            if (path.startsWith(PRIORITY_PREFIXES.get(i))) {
                return i;
            }
        }
        return PRIORITY_PREFIXES.size() + 1;
    }

    private List<PjbFrontendRouteView> scanRoutes(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .flatMap(path -> parseRoutes(path).stream())
                    .sorted(Comparator.comparing(PjbFrontendRouteView::path).thenComparing(PjbFrontendRouteView::method).thenComparing(PjbFrontendRouteView::controller))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private List<PjbFrontendRouteView> parseRoutes(Path file) {
        String source = read(file);
        String packageName = match(PACKAGE, source);
        String controller = match(TYPE, source);
        String prefix = normalizePath(match(REQUEST_MAPPING, source));
        ArrayList<PjbFrontendRouteView> routes = new ArrayList<>();
        Matcher matcher = HTTP_MAPPING.matcher(source);
        while (matcher.find()) {
            String method = httpVerb(matcher.group(1));
            String path = normalizePath(prefix + '/' + Objects.toString(matcher.group(2), ""));
            boolean adminSurface = path.startsWith("/api/v1/admin/");
            boolean uiSurface = path.startsWith("/api/v1/ui/") || packageName.contains(".controller.ui");
            routes.add(new PjbFrontendRouteView(method, path, controller, packageName, domainOf(path), adminSurface, uiSurface));
        }
        return routes;
    }

    private String domainOf(String path) {
        List<String> segments = Stream.of(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.size() < 3) {
            return "root";
        }
        if (segments.size() >= 4 && "api".equals(segments.get(0)) && "v1".equals(segments.get(1)) && "admin".equals(segments.get(2))) {
            return "admin/" + segments.get(3);
        }
        if ("api".equals(segments.get(0)) && "v1".equals(segments.get(1))) {
            return segments.get(2);
        }
        return segments.get(0).toLowerCase(Locale.ROOT);
    }

    private static String httpVerb(String annotation) {
        return switch (annotation) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            default -> annotation.toUpperCase(Locale.ROOT);
        };
    }

    private static String normalizePath(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isBlank()) {
            return "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = '/' + normalized;
        }
        normalized = normalized.replaceAll("/+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String match(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source == null ? "" : source);
        return matcher.find() ? Objects.toString(matcher.group(1), "") : "";
    }

    private static String read(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
