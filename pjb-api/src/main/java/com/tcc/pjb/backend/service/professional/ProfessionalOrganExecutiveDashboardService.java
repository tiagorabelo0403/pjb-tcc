package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAnalyticMetricCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAvatarCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartPointView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartSeriesView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganUnitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalProcessSpotlightView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleSegmentView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendWorkspaceBoardColumnView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalOrganExecutiveDashboardService {

    private static final String BRAZIL_GREEN = "#009C3B";
    private static final String BRAZIL_GREEN_DARK = "#006B2D";
    private static final String BRAZIL_YELLOW = "#FFDF00";
    private static final String BRAZIL_BLUE = "#002776";
    private static final String BRAZIL_SURFACE = "#12315E";
    private static final String BRAZIL_SURFACE_ALT = "#0C2247";
    private static final String BRAZIL_TEXT = "#F5F8FF";

    private final CurrentUserService currentUserService;
    private final ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService;
    private final ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService;
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository;

    public ProfessionalOrganExecutiveDashboardService(CurrentUserService currentUserService,
                                                      ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService,
                                                      ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService,
                                                      ProfessionalInstitutionalAccessGrantRepository grantRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.professionalForensicExecutiveDashboardService = Objects.requireNonNull(professionalForensicExecutiveDashboardService);
        this.professionalRoleExecutiveDashboardService = Objects.requireNonNull(professionalRoleExecutiveDashboardService);
        this.grantRepository = Objects.requireNonNull(grantRepository);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalOrganExecutiveDashboardView dashboard(Authentication authentication,
                                                                        LocalDate from,
                                                                        LocalDate to) {
        return build(authentication, from, to, OrganFlavor.from(currentUserService.getRequired()));
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalOrganExecutiveDashboardView magistratureDashboard(Authentication authentication,
                                                                                    LocalDate from,
                                                                                    LocalDate to) {
        return build(authentication, from, to, OrganFlavor.MAGISTRATURE);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalOrganExecutiveDashboardView defensoriaDashboard(Authentication authentication,
                                                                                  LocalDate from,
                                                                                  LocalDate to) {
        return build(authentication, from, to, OrganFlavor.DEFENSORIA);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalOrganExecutiveDashboardView procuradoriaDashboard(Authentication authentication,
                                                                                    LocalDate from,
                                                                                    LocalDate to) {
        return build(authentication, from, to, OrganFlavor.PROCURADORIA);
    }

    private PjbFrontendProfessionalOrganExecutiveDashboardView build(Authentication authentication,
                                                                     LocalDate from,
                                                                     LocalDate to,
                                                                     OrganFlavor flavor) {
        Usuario usuario = currentUserService.getRequired();
        BaseSurface base = baseSurface(authentication, from, to, flavor);
        List<ProfessionalInstitutionalAccessGrant> grants = loadGrants(usuario, flavor);
        List<PjbFrontendProfessionalOrganUnitView> organizationalUnits = buildOrganizationalUnits(grants, flavor);
        List<PjbFrontendProfessionalRoleSegmentView> segments = buildSegments(base, grants, organizationalUnits, flavor);
        List<PjbFrontendAnalyticMetricCardView> headlineCards = buildHeadlineCards(base, grants, organizationalUnits, flavor);
        LinkedHashSet<String> linkedModules = new LinkedHashSet<>(base.linkedModules());
        linkedModules.add("PROFESSIONAL_ORGAN_EXECUTIVE_2026");
        linkedModules.add(flavor.moduleKey);
        LinkedHashSet<String> quickRoutes = new LinkedHashSet<>(base.quickRoutes());
        quickRoutes.add("/api/v1/frontend/app/professional/workspace/organizational-executive-dashboard");
        quickRoutes.add(flavor.endpoint);
        quickRoutes.add("/api/v1/professional/access-grants/governance-dashboard");
        quickRoutes.add("/api/v1/professional/access-grants/operational-dashboard");
        LinkedHashSet<String> warnings = new LinkedHashSet<>(base.warnings());
        if (organizationalUnits.isEmpty()) {
            warnings.add("SEM_ANCORAS_ORGANIZACIONAIS_FORMALIZADAS");
        }
        if (grants.stream().anyMatch(ProfessionalInstitutionalAccessGrant::isPending)) {
            warnings.add("HA_GRANTS_INSTITUCIONAIS_PENDENTES");
        }
        if (grants.stream().anyMatch(this::isExpiringSoon)) {
            warnings.add("HA_GRANTS_ORGANIZACIONAIS_COM_EXPIRACAO_IMINENTE");
        }
        return new PjbFrontendProfessionalOrganExecutiveDashboardView(
                Instant.now(),
                base.actorClass(),
                flavor.dashboardKind,
                flavor.title,
                deriveOrganizationalLens(usuario, flavor, organizationalUnits),
                base.visualTheme(),
                headlineCards,
                segments,
                base.profileGallery(),
                base.board(),
                base.spotlightProcesses(),
                organizationalUnits,
                List.copyOf(linkedModules),
                List.copyOf(quickRoutes),
                List.copyOf(warnings)
        );
    }

    private BaseSurface baseSurface(Authentication authentication,
                                    LocalDate from,
                                    LocalDate to,
                                    OrganFlavor flavor) {
        return switch (flavor) {
            case MAGISTRATURE -> fromRole(professionalRoleExecutiveDashboardService.magistratureDashboard(authentication, from, to));
            case DEFENSORIA -> fromRole(professionalRoleExecutiveDashboardService.defensoriaDashboard(authentication, from, to));
            case PROCURADORIA -> fromRole(professionalRoleExecutiveDashboardService.procuradoriaDashboard(authentication, from, to));
            case PROFESSIONAL -> fromProfessional(professionalForensicExecutiveDashboardService.dashboard(authentication, from, to));
        };
    }

    private BaseSurface fromRole(PjbFrontendProfessionalRoleExecutiveDashboardView view) {
        return new BaseSurface(
                view.actorClass(),
                view.title(),
                view.organizationalLens(),
                view.visualTheme(),
                view.profileGallery(),
                view.board(),
                view.spotlightProcesses(),
                view.linkedModules(),
                view.quickRoutes(),
                view.warnings(),
                view.segments(),
                view.headlineCards()
        );
    }

    private BaseSurface fromProfessional(PjbFrontendProfessionalWorkspaceExecutiveDashboardView view) {
        return new BaseSurface(
                view.actorClass(),
                view.displayRole(),
                view.territorialAnchor(),
                view.visualTheme(),
                view.profileGallery(),
                view.board(),
                view.spotlightProcesses(),
                view.linkedModules(),
                view.quickRoutes(),
                view.warnings(),
                List.of(),
                view.headlineCards()
        );
    }

    private List<ProfessionalInstitutionalAccessGrant> loadGrants(Usuario usuario, OrganFlavor flavor) {
        return grantRepository.findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(usuario.getId()).stream()
                .filter(grant -> flavor.supportedActors.contains(grant.getActorClass()))
                .toList();
    }

    private List<PjbFrontendProfessionalOrganUnitView> buildOrganizationalUnits(List<ProfessionalInstitutionalAccessGrant> grants,
                                                                                OrganFlavor flavor) {
        if (grants.isEmpty()) {
            return List.of();
        }
        Map<String, List<ProfessionalInstitutionalAccessGrant>> grouped = new LinkedHashMap<>();
        for (ProfessionalInstitutionalAccessGrant grant : grants) {
            String key = normalizeKey(organizationalKey(grant, flavor));
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(grant);
        }
        ArrayList<PjbFrontendProfessionalOrganUnitView> units = new ArrayList<>();
        for (Map.Entry<String, List<ProfessionalInstitutionalAccessGrant>> entry : grouped.entrySet()) {
            List<ProfessionalInstitutionalAccessGrant> bucket = entry.getValue();
            long activeProcesses = bucket.stream().map(ProfessionalInstitutionalAccessGrant::getProcesso).filter(Objects::nonNull).map(p -> p.getId()).distinct().count();
            long activeGrants = bucket.stream().filter(grant -> grant.isApproved() && grant.isAtivoNaJanela(LocalDateTime.now())).count();
            long criticalQueue = bucket.stream().filter(grant -> grant.isPending() || grant.requiresStepUp() || isExpiringSoon(grant)).count();
            ProfessionalInstitutionalAccessGrant primary = bucket.stream().max(Comparator.comparing(ProfessionalInstitutionalAccessGrant::getId)).orElse(null);
            String label = labelForUnit(entry.getKey(), primary, flavor);
            String subtitle = subtitleForUnit(primary, activeProcesses, activeGrants, criticalQueue);
            units.add(new PjbFrontendProfessionalOrganUnitView(
                    entry.getKey(),
                    label,
                    subtitle,
                    entry.getKey(),
                    activeProcesses,
                    activeGrants,
                    criticalQueue,
                    accentFor(flavor, primary),
                    BRAZIL_SURFACE,
                    "/api/v1/professional/access-grants/governance-dashboard"
            ));
        }
        units.sort(Comparator.comparingLong(PjbFrontendProfessionalOrganUnitView::criticalQueue).reversed()
                .thenComparingLong(PjbFrontendProfessionalOrganUnitView::activeGrants).reversed());
        return List.copyOf(units.stream().limit(8).toList());
    }

    private List<PjbFrontendAnalyticMetricCardView> buildHeadlineCards(BaseSurface base,
                                                                       List<ProfessionalInstitutionalAccessGrant> grants,
                                                                       List<PjbFrontendProfessionalOrganUnitView> units,
                                                                       OrganFlavor flavor) {
        ArrayList<PjbFrontendAnalyticMetricCardView> cards = new ArrayList<>();
        cards.addAll(base.headlineCards().stream().limit(4).toList());
        long activeUnits = units.size();
        long activeGrants = grants.stream().filter(grant -> grant.isApproved() && grant.isAtivoNaJanela(LocalDateTime.now())).count();
        long critical = grants.stream().filter(grant -> grant.isPending() || grant.requiresStepUp() || isExpiringSoon(grant)).count();
        long protectedScope = grants.stream().filter(this::touchesProtectedScope).count();
        cards.add(metricCard("ORG_UNITS", flavor.unitLabelPlural, Long.toString(activeUnits), activeUnits == 0 ? "sem cobertura" : "cobertura ativa", "PRIMARY", accentFor(flavor, null), flavor.endpoint));
        cards.add(metricCard("ORG_GRANTS", "Grants organizacionais", Long.toString(activeGrants), grants.size() + " totais", "SUCCESS", BRAZIL_GREEN, "/api/v1/professional/access-grants/governance-dashboard"));
        cards.add(metricCard("ORG_CRITICAL", "Fila crítica institucional", Long.toString(critical), critical == 0 ? "janela limpa" : "ação recomendada", "WARNING", BRAZIL_YELLOW, "/api/v1/professional/access-grants/operational-dashboard"));
        cards.add(metricCard("ORG_PROTECTED", "Escopo protegido", Long.toString(protectedScope), flavor.protectedLabel, "INFO", BRAZIL_BLUE, "/api/v1/professional/forensic-panel/workspace"));
        return List.copyOf(cards.stream().limit(8).toList());
    }

    private List<PjbFrontendProfessionalRoleSegmentView> buildSegments(BaseSurface base,
                                                                       List<ProfessionalInstitutionalAccessGrant> grants,
                                                                       List<PjbFrontendProfessionalOrganUnitView> units,
                                                                       OrganFlavor flavor) {
        ArrayList<PjbFrontendProfessionalRoleSegmentView> segments = new ArrayList<>(base.inheritedSegments());
        segments.add(new PjbFrontendProfessionalRoleSegmentView(
                "ORGAN_COVERAGE",
                flavor.segmentCoverageTitle,
                flavor.segmentCoverageSubtitle,
                accentFor(flavor, null),
                List.of(
                        metricCard("UNIT_COUNT", flavor.unitLabelPlural, Long.toString(units.size()), coverageSummary(units), "PRIMARY", accentFor(flavor, null), flavor.endpoint),
                        metricCard("PROCESS_SCOPED", "Processos ancorados", Long.toString(countProcessScoped(grants)), "via grants formais", "SUCCESS", BRAZIL_GREEN, "/api/v1/professional/forensic-panel/institutional-overview"),
                        metricCard("TERRITORIAL", "Cobertura territorial", Long.toString(countTerritorialAnchors(grants)), flavor.territorialLabel, "INFO", BRAZIL_BLUE, "/api/v1/professional/forensic-panel/workspace"),
                        metricCard("ENTE_UNIT", flavor.organAnchorLabel, Long.toString(countDistinctOrganAnchors(grants, flavor)), "âncoras distintas", "SECONDARY", BRAZIL_GREEN_DARK, "/api/v1/professional/access-grants/governance-dashboard")
                ),
                List.of(
                        chartSeries("ORGAN_TOP", "Unidades e âncoras", "bar", List.of(BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_BLUE), units.stream().limit(6)
                                .map(unit -> new PjbFrontendChartPointView(unit.key(), unit.label(), unit.activeGrants(), "PRIMARY", unit.accentHex()))
                                .toList())
                ),
                List.of(flavor.endpoint, "/api/v1/professional/access-grants/governance-dashboard")
        ));
        segments.add(new PjbFrontendProfessionalRoleSegmentView(
                "ORGAN_CRITICAL_QUEUE",
                "Fila crítica institucional",
                "pendências, expiração e reautenticação forte",
                BRAZIL_YELLOW,
                List.of(
                        metricCard("PENDING", "Pendentes", Long.toString(grants.stream().filter(ProfessionalInstitutionalAccessGrant::isPending).count()), "aguardando decisão", "WARNING", BRAZIL_YELLOW, "/api/v1/professional/access-grants/workspace"),
                        metricCard("EXPIRING", "Expiração iminente", Long.toString(grants.stream().filter(this::isExpiringSoon).count()), "próximos 7 dias", "WARNING", BRAZIL_YELLOW, "/api/v1/professional/access-grants/operational-dashboard"),
                        metricCard("STEP_UP", "Step-up forte", Long.toString(grants.stream().filter(ProfessionalInstitutionalAccessGrant::requiresStepUp).count()), "reautenticação obrigatória", "INFO", BRAZIL_BLUE, "/api/v1/professional/access-grants/operational-dashboard"),
                        metricCard("INACTIVE", "Inativos ou revogados", Long.toString(grants.stream().filter(grant -> !grant.isApproved() || !Boolean.TRUE.equals(grant.getAtivo())).count()), "governança e saneamento", "NEUTRAL", BRAZIL_SURFACE_ALT, "/api/v1/professional/access-grants/governance-dashboard")
                ),
                List.of(
                        chartSeries("CRITICAL_PROFILE", "Perfil crítico", "donut", List.of(BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_SURFACE_ALT), List.of(
                                new PjbFrontendChartPointView("PENDING", "Pendentes", grants.stream().filter(ProfessionalInstitutionalAccessGrant::isPending).count(), "WARNING", BRAZIL_YELLOW),
                                new PjbFrontendChartPointView("STEP_UP", "Step-up", grants.stream().filter(ProfessionalInstitutionalAccessGrant::requiresStepUp).count(), "INFO", BRAZIL_BLUE),
                                new PjbFrontendChartPointView("EXPIRING", "Expiração", grants.stream().filter(this::isExpiringSoon).count(), "SECONDARY", BRAZIL_GREEN_DARK)
                        ))
                ),
                List.of("/api/v1/professional/access-grants/operational-dashboard")
        ));
        segments.add(new PjbFrontendProfessionalRoleSegmentView(
                "ORGAN_GRANT_PROFILE",
                "Perfil de grants e competência",
                flavor.profileSubtitle,
                BRAZIL_BLUE,
                List.of(
                        metricCard("RELATORIA", flavor.relatoriaLabel, Long.toString(countByType(grants, ProfessionalAccessGrantType.RELATORIA_PROCESSO)), "competência processual", "INFO", BRAZIL_BLUE, "/api/v1/professional/access-grants/governance-dashboard"),
                        metricCard("COLEGIADO", flavor.colegiadoLabel, Long.toString(countByType(grants, ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO)), "órgão colegiado", "SECONDARY", BRAZIL_BLUE, "/api/v1/professional/access-grants/governance-dashboard"),
                        metricCard("DESIGNACAO", flavor.designationLabel, Long.toString(countDesignationLike(grants, flavor)), "base institucional", "SUCCESS", BRAZIL_GREEN, "/api/v1/professional/access-grants/workspace"),
                        metricCard("PLANTAO", flavor.plantaoLabel, Long.toString(countByType(grants, ProfessionalAccessGrantType.PLANTAO) + countByType(grants, ProfessionalAccessGrantType.SUBSTITUICAO) + countByType(grants, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL)), "contingência e cobertura", "PRIMARY", BRAZIL_GREEN_DARK, "/api/v1/professional/access-grants/operational-dashboard")
                ),
                List.of(
                        chartSeries("GRANT_TYPES", "Tipos de grants", "bar", List.of(BRAZIL_BLUE, BRAZIL_GREEN, BRAZIL_YELLOW), List.of(
                                new PjbFrontendChartPointView("PROCESS", "Processuais", countProcessScoped(grants), "INFO", BRAZIL_BLUE),
                                new PjbFrontendChartPointView("TERRITORIAL", "Territoriais", countTerritorialAnchors(grants), "SUCCESS", BRAZIL_GREEN),
                                new PjbFrontendChartPointView("PROTECTED", "Escopo protegido", grants.stream().filter(this::touchesProtectedScope).count(), "WARNING", BRAZIL_YELLOW)
                        ))
                ),
                List.of("/api/v1/professional/forensic-panel/institutional-overview", "/api/v1/professional/access-grants/governance-dashboard")
        ));
        return List.copyOf(segments);
    }

    private PjbFrontendAnalyticMetricCardView metricCard(String key,
                                                         String label,
                                                         String value,
                                                         String secondaryValue,
                                                         String accentTone,
                                                         String accentHex,
                                                         String route) {
        return new PjbFrontendAnalyticMetricCardView(key, label, value, secondaryValue, accentTone, accentHex, BRAZIL_SURFACE, BRAZIL_TEXT, route);
    }

    private PjbFrontendChartSeriesView chartSeries(String key,
                                                   String label,
                                                   String chartType,
                                                   List<String> palette,
                                                   List<PjbFrontendChartPointView> points) {
        return new PjbFrontendChartSeriesView(key, label, chartType, "BRAZIL_EXECUTIVE_2026", palette, List.copyOf(points));
    }

    private String deriveOrganizationalLens(Usuario usuario,
                                            OrganFlavor flavor,
                                            List<PjbFrontendProfessionalOrganUnitView> units) {
        if (!units.isEmpty()) {
            return units.get(0).label() + " · " + units.get(0).subtitle();
        }
        StringBuilder builder = new StringBuilder(flavor.title);
        if (hasText(usuario.getUf())) {
            builder.append(" · ").append(usuario.getUf().toUpperCase(Locale.ROOT));
        }
        if (hasText(usuario.getComarca())) {
            builder.append(" · ").append(usuario.getComarca().toUpperCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private long countProcessScoped(List<ProfessionalInstitutionalAccessGrant> grants) {
        return grants.stream().filter(grant -> grant.getProcesso() != null).map(ProfessionalInstitutionalAccessGrant::getProcesso).filter(Objects::nonNull).map(processo -> processo.getId()).distinct().count();
    }

    private long countTerritorialAnchors(List<ProfessionalInstitutionalAccessGrant> grants) {
        return grants.stream().map(this::territorialKey).filter(this::hasText).distinct().count();
    }

    private long countDistinctOrganAnchors(List<ProfessionalInstitutionalAccessGrant> grants,
                                           OrganFlavor flavor) {
        return grants.stream().map(grant -> organizationalKey(grant, flavor)).filter(this::hasText).distinct().count();
    }

    private long countByType(List<ProfessionalInstitutionalAccessGrant> grants,
                             ProfessionalAccessGrantType type) {
        return grants.stream().filter(grant -> grant.getGrantType() == type).count();
    }

    private long countDesignationLike(List<ProfessionalInstitutionalAccessGrant> grants,
                                      OrganFlavor flavor) {
        return switch (flavor) {
            case MAGISTRATURE -> countByType(grants, ProfessionalAccessGrantType.DELEGACAO_GABINETE);
            case DEFENSORIA -> countByType(grants, ProfessionalAccessGrantType.DESIGNACAO_PROCESSO)
                    + countByType(grants, ProfessionalAccessGrantType.DESIGNACAO_TERRITORIAL)
                    + countByType(grants, ProfessionalAccessGrantType.LOTACAO_UNIDADE);
            case PROCURADORIA -> countByType(grants, ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO)
                    + countByType(grants, ProfessionalAccessGrantType.REPRESENTACAO_ENTE)
                    + countByType(grants, ProfessionalAccessGrantType.LOTACAO_UNIDADE);
            case PROFESSIONAL -> countByType(grants, ProfessionalAccessGrantType.LOTACAO_UNIDADE);
        };
    }

    private String coverageSummary(List<PjbFrontendProfessionalOrganUnitView> units) {
        if (units.isEmpty()) {
            return "sem cobertura formal";
        }
        long critical = units.stream().filter(unit -> unit.criticalQueue() > 0).count();
        return critical == 0 ? "sem fila crítica" : critical + " âncoras críticas";
    }

    private String organizationalKey(ProfessionalInstitutionalAccessGrant grant,
                                     OrganFlavor flavor) {
        if (grant == null) {
            return "GENERIC";
        }
        return switch (flavor) {
            case MAGISTRATURE -> firstNonBlank(grant.getOrgaoColegiadoCodigo(), grant.getUnidadeJudiciariaCodigo(), grant.getTribunal(), territorialKey(grant), "MAGISTRATURE");
            case DEFENSORIA -> firstNonBlank(grant.getUnidadeJudiciariaCodigo(), territorialKey(grant), grant.getSourceLabel(), "DEFENSORIA");
            case PROCURADORIA -> firstNonBlank(grant.getEnteCode(), grant.getUnidadeJudiciariaCodigo(), territorialKey(grant), "PROCURADORIA");
            case PROFESSIONAL -> firstNonBlank(grant.getUnidadeJudiciariaCodigo(), grant.getOrgaoColegiadoCodigo(), grant.getEnteCode(), territorialKey(grant), "PROFESSIONAL");
        };
    }

    private String labelForUnit(String key,
                                ProfessionalInstitutionalAccessGrant primary,
                                OrganFlavor flavor) {
        if (primary == null) {
            return key;
        }
        return switch (flavor) {
            case MAGISTRATURE -> firstNonBlank(primary.getSourceLabel(), primary.getOrgaoColegiadoCodigo(), primary.getUnidadeJudiciariaCodigo(), key);
            case DEFENSORIA -> firstNonBlank(primary.getSourceLabel(), "Unidade " + firstNonBlank(primary.getUnidadeJudiciariaCodigo(), key), key);
            case PROCURADORIA -> firstNonBlank(primary.getSourceLabel(), "Ente " + firstNonBlank(primary.getEnteCode(), key), key);
            case PROFESSIONAL -> firstNonBlank(primary.getSourceLabel(), key);
        };
    }

    private String subtitleForUnit(ProfessionalInstitutionalAccessGrant primary,
                                   long activeProcesses,
                                   long activeGrants,
                                   long criticalQueue) {
        ArrayList<String> parts = new ArrayList<>();
        if (primary != null && hasText(primary.getUf())) {
            parts.add(primary.getUf().toUpperCase(Locale.ROOT));
        }
        if (primary != null && hasText(primary.getComarca())) {
            parts.add(primary.getComarca().toUpperCase(Locale.ROOT));
        }
        parts.add(activeProcesses + " processos");
        parts.add(activeGrants + " grants");
        if (criticalQueue > 0) {
            parts.add(criticalQueue + " críticos");
        }
        return String.join(" · ", parts);
    }

    private String accentFor(OrganFlavor flavor,
                             ProfessionalInstitutionalAccessGrant grant) {
        if (grant != null && grant.requiresStepUp()) {
            return BRAZIL_YELLOW;
        }
        return switch (flavor) {
            case MAGISTRATURE -> BRAZIL_BLUE;
            case DEFENSORIA -> BRAZIL_GREEN;
            case PROCURADORIA -> BRAZIL_GREEN_DARK;
            case PROFESSIONAL -> BRAZIL_BLUE;
        };
    }

    private boolean touchesProtectedScope(ProfessionalInstitutionalAccessGrant grant) {
        if (grant == null) {
            return false;
        }
        return grant.requiresStepUp()
                || grant.getGrantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE
                || grant.getGrantType() == ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO
                || grant.getGrantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO;
    }

    private boolean isExpiringSoon(ProfessionalInstitutionalAccessGrant grant) {
        if (grant == null || grant.getFimVigencia() == null || !grant.isApproved() || !Boolean.TRUE.equals(grant.getAtivo())) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), grant.getFimVigencia());
        return days >= 0 && days <= 7;
    }

    private String territorialKey(ProfessionalInstitutionalAccessGrant grant) {
        return firstNonBlank(
                joinNonBlank("/", grant.getUf(), grant.getComarca()),
                joinNonBlank("/", grant.getTribunal(), grant.getUf()),
                grant.getUf(),
                grant.getComarca()
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String joinNonBlank(String separator,
                                String... values) {
        ArrayList<String> items = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (hasText(value)) {
                    items.add(value.trim());
                }
            }
        }
        return items.isEmpty() ? "" : String.join(separator, items);
    }

    private String normalizeKey(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "GENERIC";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record BaseSurface(
            String actorClass,
            String title,
            String organizationalLens,
            PjbFrontendVisualThemeView visualTheme,
            List<PjbFrontendAvatarCardView> profileGallery,
            List<PjbFrontendWorkspaceBoardColumnView> board,
            List<PjbFrontendProfessionalProcessSpotlightView> spotlightProcesses,
            List<String> linkedModules,
            List<String> quickRoutes,
            List<String> warnings,
            List<PjbFrontendProfessionalRoleSegmentView> inheritedSegments,
            List<PjbFrontendAnalyticMetricCardView> headlineCards
    ) {
    }

    private enum OrganFlavor {
        MAGISTRATURE(
                Set.of(ProfessionalActorClass.MAGISTRATURA, ProfessionalActorClass.APOIO_JUDICIAL),
                "MAGISTRATURE_ORGAN_EXECUTIVE",
                "Painel institucional do gabinete e colegiado",
                "MAGISTRATURE_ORGAN_EXECUTIVE_2026",
                "/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard",
                "gabinetes e órgãos",
                "escopo jurisdicional e colegiado",
                "Cobertura institucional da magistratura",
                "gabinetes, colegiados e competência formal",
                "Gabinetes e órgãos",
                "reserva jurisdicional",
                "Relatorias",
                "Colegiados",
                "Delegações",
                "Plantões e substituições"
        ),
        DEFENSORIA(
                Set.of(ProfessionalActorClass.DEFENSORIA),
                "DEFENSORIA_ORGAN_EXECUTIVE",
                "Painel institucional da unidade e cobertura defensiva",
                "DEFENSORIA_ORGAN_EXECUTIVE_2026",
                "/api/v1/frontend/app/professional/workspace/defensoria-organ-dashboard",
                "unidades defensivas",
                "assistidos, unidades e designações formais",
                "Cobertura institucional da defensoria",
                "unidades, território e proteção reforçada",
                "Unidades e núcleos",
                "assistência protegida",
                "Atuações processuais",
                "Colegialidade",
                "Designações",
                "Cobertura emergencial"
        ),
        PROCURADORIA(
                Set.of(ProfessionalActorClass.PROCURADORIA),
                "PROCURADORIA_ORGAN_EXECUTIVE",
                "Painel institucional do ente e núcleo representativo",
                "PROCURADORIA_ORGAN_EXECUTIVE_2026",
                "/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard",
                "entes e núcleos",
                "representação formal e risco institucional",
                "Cobertura institucional da procuradoria",
                "entes, núcleos e representação formal",
                "Entes e núcleos",
                "reserva institucional",
                "Atuações processuais",
                "Colegialidade",
                "Representações",
                "Cobertura contingencial"
        ),
        PROFESSIONAL(
                EnumSet.allOf(ProfessionalActorClass.class),
                "PROFESSIONAL_ORGAN_EXECUTIVE",
                "Painel organizacional profissional",
                "PROFESSIONAL_ORGAN_EXECUTIVE_2026",
                "/api/v1/frontend/app/professional/workspace/organizational-executive-dashboard",
                "âncoras organizacionais",
                "distribuição e cobertura institucional",
                "Cobertura institucional profissional",
                "unidades, território e grants",
                "Âncoras organizacionais",
                "escopo protegido",
                "Atuações processuais",
                "Colegialidade",
                "Designações",
                "Cobertura contingencial"
        );

        private final Set<ProfessionalActorClass> supportedActors;
        private final String dashboardKind;
        private final String title;
        private final String moduleKey;
        private final String endpoint;
        private final String unitLabelPlural;
        private final String territorialLabel;
        private final String segmentCoverageTitle;
        private final String segmentCoverageSubtitle;
        private final String organAnchorLabel;
        private final String protectedLabel;
        private final String relatoriaLabel;
        private final String colegiadoLabel;
        private final String designationLabel;
        private final String plantaoLabel;
        private final String profileSubtitle;

        OrganFlavor(Set<ProfessionalActorClass> supportedActors,
                    String dashboardKind,
                    String title,
                    String moduleKey,
                    String endpoint,
                    String unitLabelPlural,
                    String territorialLabel,
                    String segmentCoverageTitle,
                    String segmentCoverageSubtitle,
                    String organAnchorLabel,
                    String protectedLabel,
                    String relatoriaLabel,
                    String colegiadoLabel,
                    String designationLabel,
                    String plantaoLabel) {
            this.supportedActors = supportedActors;
            this.dashboardKind = dashboardKind;
            this.title = title;
            this.moduleKey = moduleKey;
            this.endpoint = endpoint;
            this.unitLabelPlural = unitLabelPlural;
            this.territorialLabel = territorialLabel;
            this.segmentCoverageTitle = segmentCoverageTitle;
            this.segmentCoverageSubtitle = segmentCoverageSubtitle;
            this.organAnchorLabel = organAnchorLabel;
            this.protectedLabel = protectedLabel;
            this.relatoriaLabel = relatoriaLabel;
            this.colegiadoLabel = colegiadoLabel;
            this.designationLabel = designationLabel;
            this.plantaoLabel = plantaoLabel;
            this.profileSubtitle = unitLabelPlural + " e competência formal";
        }

        private static OrganFlavor from(Usuario usuario) {
            TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
            if (tipoUsuario != null && (tipoUsuario.isMagistratura() || tipoUsuario.isAssessor() || tipoUsuario.isServidorJudiciario())) {
                return MAGISTRATURE;
            }
            if (tipoUsuario != null && tipoUsuario.isDefensoriaPublica()) {
                return DEFENSORIA;
            }
            if (tipoUsuario != null && tipoUsuario.isProcuradoria()) {
                return PROCURADORIA;
            }
            return PROFESSIONAL;
        }
    }
}
