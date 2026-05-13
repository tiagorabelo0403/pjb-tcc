package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAnalyticMetricCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartPointView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartSeriesView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleSegmentView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalRoleExecutiveDashboardService {

    private static final String BRAZIL_GREEN = "#009C3B";
    private static final String BRAZIL_GREEN_DARK = "#006B2D";
    private static final String BRAZIL_YELLOW = "#FFDF00";
    private static final String BRAZIL_BLUE = "#002776";
    private static final String BRAZIL_BLUE_ALT = "#1B4D9B";
    private static final String BRAZIL_SURFACE_ALT = "#12315E";
    private static final String BRAZIL_TEXT = "#F5F8FF";

    private final CurrentUserService currentUserService;
    private final ProfessionalForensicExecutiveDashboardService executiveDashboardService;
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public ProfessionalRoleExecutiveDashboardService(CurrentUserService currentUserService,
                                                     ProfessionalForensicExecutiveDashboardService executiveDashboardService,
                                                     ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                                     NamedParameterJdbcTemplate jdbc) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.executiveDashboardService = Objects.requireNonNull(executiveDashboardService);
        this.grantRepository = Objects.requireNonNull(grantRepository);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalRoleExecutiveDashboardView magistratureDashboard(Authentication authentication,
                                                                                   LocalDate from,
                                                                                   LocalDate to) {
        return build(authentication, from, to, RoleFlavor.MAGISTRATURE);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalRoleExecutiveDashboardView defensoriaDashboard(Authentication authentication,
                                                                                 LocalDate from,
                                                                                 LocalDate to) {
        return build(authentication, from, to, RoleFlavor.DEFENSORIA);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalRoleExecutiveDashboardView procuradoriaDashboard(Authentication authentication,
                                                                                   LocalDate from,
                                                                                   LocalDate to) {
        return build(authentication, from, to, RoleFlavor.PROCURADORIA);
    }

    private PjbFrontendProfessionalRoleExecutiveDashboardView build(Authentication authentication,
                                                                    LocalDate from,
                                                                    LocalDate to,
                                                                    RoleFlavor flavor) {
        Usuario usuario = currentUserService.getRequired();
        PjbFrontendProfessionalWorkspaceExecutiveDashboardView base = executiveDashboardService.dashboard(authentication, from, to);
        RoleSnapshot snapshot = loadSnapshot(usuario, flavor, from, to);
        List<PjbFrontendAnalyticMetricCardView> headlineCards = buildHeadlineCards(flavor, snapshot);
        List<PjbFrontendProfessionalRoleSegmentView> segments = buildSegments(flavor, snapshot, base.visualTheme());
        LinkedHashSet<String> linkedModules = new LinkedHashSet<>(base.linkedModules());
        linkedModules.add(flavor.moduleKey);
        linkedModules.add("ROLE_EXECUTIVE_SURFACE");
        LinkedHashSet<String> quickRoutes = new LinkedHashSet<>(base.quickRoutes());
        quickRoutes.add(flavor.endpoint);
        quickRoutes.addAll(flavor.quickRoutes);
        LinkedHashSet<String> warnings = new LinkedHashSet<>(base.warnings());
        if (!flavor.supportedActors.contains(ProfessionalActorClass.valueOf(base.actorClass()))) {
            warnings.add("ROLE_SURFACE_OPENED_OUTSIDE_PRIMARY_ACTOR_CLASS");
        }
        if (snapshot.expiringSoon > 0) {
            warnings.add("HA_GRANTS_COM_EXPIRACAO_IMINENTE");
        }
        if (snapshot.pendingStepUp > 0) {
            warnings.add("HA_ITENS_COM_STEP_UP_PENDENTE");
        }
        return new PjbFrontendProfessionalRoleExecutiveDashboardView(
                Instant.now(),
                base.actorClass(),
                flavor.dashboardKind,
                flavor.title,
                organizationalLens(usuario, flavor),
                base.visualTheme(),
                headlineCards,
                segments,
                base.profileGallery(),
                base.board(),
                base.spotlightProcesses(),
                List.copyOf(linkedModules),
                List.copyOf(quickRoutes),
                List.copyOf(warnings)
        );
    }

    private RoleSnapshot loadSnapshot(Usuario usuario, RoleFlavor flavor, LocalDate from, LocalDate to) {
        List<ProfessionalInstitutionalAccessGrant> grants = grantRepository.findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(usuario.getId()).stream()
                .filter(grant -> flavor.supportedActors.contains(grant.getActorClass()))
                .toList();
        LocalDateTime now = LocalDateTime.now();
        long approvedActive = grants.stream().filter(grant -> grant.isAtivoNaJanela(now)).count();
        long pending = grants.stream().filter(ProfessionalInstitutionalAccessGrant::isPending).count();
        long pendingStepUp = grants.stream().filter(grant -> grant.requiresStepUp() && grant.isAtivoNaJanela(now)).count();
        long expiringSoon = grants.stream().filter(grant -> grant.isAtivoNaJanela(now))
                .filter(grant -> grant.getFimVigencia() != null && ChronoUnit.DAYS.between(now, grant.getFimVigencia()) >= 0 && ChronoUnit.DAYS.between(now, grant.getFimVigencia()) <= 7)
                .count();
        long processScoped = grants.stream().filter(grant -> grant.getProcesso() != null).count();
        long territorial = grants.stream().filter(grant -> hasText(grant.getUf()) || hasText(grant.getComarca()) || hasText(grant.getUnidadeJudiciariaCodigo())).count();
        long relatoria = grants.stream().filter(grant -> grant.getGrantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO).count();
        long colegiado = grants.stream().filter(grant -> grant.getGrantType() == ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO).count();
        long gab = grants.stream().filter(grant -> grant.getGrantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE).count();
        long substituicoes = grants.stream().filter(grant -> grant.getGrantType() == ProfessionalAccessGrantType.SUBSTITUICAO || grant.getGrantType() == ProfessionalAccessGrantType.PLANTAO || grant.getGrantType() == ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL).count();
        long unitAnchors = grants.stream().map(this::organizationalAnchor).filter(this::hasText).distinct().count();
        long representedEnte = grants.stream().map(ProfessionalInstitutionalAccessGrant::getEnteCode).filter(this::hasText).distinct().count();
        long representedPeople = grants.stream().map(grant -> grant.getProcesso() == null ? null : normalizeHumanName(grant.getProcesso().getParteAutoraNome())).filter(this::hasText).distinct().count();
        RoleWindowMetrics metrics = loadWindowMetrics(usuario, from, to);
        return new RoleSnapshot(approvedActive, pending, pendingStepUp, expiringSoon, processScoped, territorial, relatoria, colegiado, gab, substituicoes, unitAnchors, representedEnte, representedPeople, metrics);
    }

    private RoleWindowMetrics loadWindowMetrics(Usuario usuario, LocalDate from, LocalDate to) {
        String sql = """
                select
                    count(*) as total,
                    count(*) filter (where coalesce(p.resultado_final,'') ilike '%procedente%' and coalesce(p.resultado_final,'') not ilike '%improcedente%') as procedentes,
                    count(*) filter (where coalesce(p.resultado_final,'') ilike '%improcedente%') as improcedentes,
                    count(*) filter (where coalesce(p.resultado_final,'') ilike '%parcial%') as parciais,
                    count(*) filter (where coalesce(p.resultado_final,'') ilike '%acordo%' or coalesce(p.resultado_final,'') ilike '%homolog%') as acordos,
                    count(*) filter (where p.nivel_sigilo is not null and p.nivel_sigilo <> 'PUBLICO') as sigilosos,
                    count(*) filter (where coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) >= :freshDate) as recentes,
                    count(*) filter (where p.status_processo = 'ARQUIVADO') as arquivados
                from tb_processo p
                where (:uf = '' or upper(coalesce(p.uf,'')) = :uf)
                  and (:comarca = '' or upper(coalesce(p.comarca,'')) = :comarca)
                  and (:fromDate is null or coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) >= :fromDate)
                  and (:toDateExclusive is null or coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) < :toDateExclusive)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uf", normalizedToken(usuario.getUf()))
                .addValue("comarca", normalizedToken(usuario.getComarca()))
                .addValue("fromDate", from == null ? null : from.atStartOfDay())
                .addValue("toDateExclusive", to == null ? null : to.plusDays(1).atStartOfDay())
                .addValue("freshDate", LocalDateTime.now().minusDays(15));
        return jdbc.query(sql, params, rs -> {
            if (!rs.next()) {
                return new RoleWindowMetrics(0, 0, 0, 0, 0, 0, 0, 0);
            }
            return new RoleWindowMetrics(
                    rs.getLong("total"),
                    rs.getLong("procedentes"),
                    rs.getLong("improcedentes"),
                    rs.getLong("parciais"),
                    rs.getLong("acordos"),
                    rs.getLong("sigilosos"),
                    rs.getLong("recentes"),
                    rs.getLong("arquivados")
            );
        });
    }

    private List<PjbFrontendAnalyticMetricCardView> buildHeadlineCards(RoleFlavor flavor, RoleSnapshot snapshot) {
        ArrayList<PjbFrontendAnalyticMetricCardView> cards = new ArrayList<>();
        cards.add(metricCard("ACTIVE_GRANTS", flavor.activeLabel, Long.toString(snapshot.activeGrants), flavor.activeDetail(snapshot), flavor.primaryAccent, flavor.endpoint));
        cards.add(metricCard("PENDING", "Pendências formais", Long.toString(snapshot.pendingGrants), snapshot.pendingStepUp + " com step-up", flavor.warningAccent, "/api/v1/professional/access-grants/operational-dashboard"));
        cards.add(metricCard("WINDOW", flavor.windowLabel, Long.toString(snapshot.window.total), snapshot.window.recentes + " recentes", flavor.secondaryAccent, "/api/v1/professional/forensic-panel/institutional-overview"));
        cards.add(metricCard("OUTCOME", "Procedentes consolidados", Long.toString(snapshot.window.procedentes), percent(snapshot.window.procedentes, snapshot.window.total), "SUCCESS", flavor.endpoint));
        cards.add(metricCard("SENSITIVE", flavor.sensitiveLabel, Long.toString(snapshot.window.sigilosos), flavor.sensitiveDetail(snapshot), "SECONDARY", "/api/v1/professional/forensic-panel/process-search"));
        cards.add(metricCard("EXPIRING", "Expiração iminente", Long.toString(snapshot.expiringSoon), snapshot.unitAnchors + " âncoras", "INFO", "/api/v1/professional/access-grants/governance-dashboard"));
        return List.copyOf(cards);
    }

    private List<PjbFrontendProfessionalRoleSegmentView> buildSegments(RoleFlavor flavor,
                                                                       RoleSnapshot snapshot,
                                                                       PjbFrontendVisualThemeView theme) {
        return switch (flavor) {
            case MAGISTRATURE -> List.of(
                    new PjbFrontendProfessionalRoleSegmentView(
                            "MAG_JURISDICTION",
                            "Competência jurisdicional",
                            "relatoria, colegiado, substituição e gabinete",
                            BRAZIL_BLUE,
                            List.of(
                                    metricCard("RELATORIA", "Relatorias ativas", Long.toString(snapshot.relatoria), snapshot.processScoped + " por processo", "PRIMARY", "/api/v1/professional/forensic-panel/workspace"),
                                    metricCard("COLEGIADO", "Composição colegiada", Long.toString(snapshot.colegiado), snapshot.gabinete + " delegações", "INFO", "/api/v1/professional/access-grants/governance-dashboard"),
                                    metricCard("SUBSTITUICAO", "Substituições e plantão", Long.toString(snapshot.substituicoes), snapshot.expiringSoon + " expiram em breve", "WARNING", "/api/v1/professional/access-grants/operational-dashboard")
                            ),
                            List.of(chartSeries("MAG_FLOW", "Jurisdição ativa", "BAR", "BRAZIL_MAG_JURISDICTION", List.of(
                                    point("RELATORIA", "Relatoria", snapshot.relatoria, BRAZIL_BLUE),
                                    point("COLEGIADO", "Colegiado", snapshot.colegiado, BRAZIL_GREEN),
                                    point("GABINETE", "Gabinete", snapshot.gabinete, BRAZIL_YELLOW),
                                    point("SUBSTITUICAO", "Substituição", snapshot.substituicoes, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard", "/api/v1/professional/access-grants/workspace")
                    ),
                    new PjbFrontendProfessionalRoleSegmentView(
                            "MAG_DECISIONS",
                            "Resultado e acervo",
                            "visão por janela e criticidade",
                            BRAZIL_GREEN,
                            List.of(
                                    metricCard("TOTAL", "Acervo da janela", Long.toString(snapshot.window.total), snapshot.window.arquivados + " arquivados", "SUCCESS", flavor.endpoint),
                                    metricCard("PROC", "Procedentes", Long.toString(snapshot.window.procedentes), percent(snapshot.window.procedentes, snapshot.window.total), "SUCCESS", flavor.endpoint),
                                    metricCard("IMPROC", "Improcedentes", Long.toString(snapshot.window.improcedentes), percent(snapshot.window.improcedentes, snapshot.window.total), "NEUTRAL", flavor.endpoint)
                            ),
                            List.of(chartSeries("MAG_RESULTS", "Resultados jurisdicionais", "DONUT", "BRAZIL_MAG_RESULTS", List.of(
                                    point("PROCEDENTE", "Procedente", snapshot.window.procedentes, BRAZIL_GREEN),
                                    point("IMPROCEDENTE", "Improcedente", snapshot.window.improcedentes, BRAZIL_BLUE),
                                    point("PARCIAL", "Parcial", snapshot.window.parciais, BRAZIL_YELLOW),
                                    point("ACORDO", "Acordo", snapshot.window.acordos, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/professional/forensic-panel/institutional-overview")
                    )
            );
            case DEFENSORIA -> List.of(
                    new PjbFrontendProfessionalRoleSegmentView(
                            "DEF_ASSISTIDOS",
                            "Assistidos e cobertura",
                            "designação processual, territorial e unidade",
                            BRAZIL_GREEN,
                            List.of(
                                    metricCard("ASSISTIDOS", "Assistidos ancorados", Long.toString(snapshot.representedPeople), snapshot.processScoped + " designações", "SUCCESS", "/api/v1/professional/forensic-panel/client-360"),
                                    metricCard("TERRITORIO", "Cobertura territorial", Long.toString(snapshot.territorialCoverage), snapshot.unitAnchors + " unidades", "PRIMARY", "/api/v1/professional/access-grants/governance-dashboard"),
                                    metricCard("STEPUP", "Proteção reforçada", Long.toString(snapshot.pendingStepUp), snapshot.window.sigilosos + " sensíveis", "WARNING", "/api/v1/professional/access-grants/operational-dashboard")
                            ),
                            List.of(chartSeries("DEF_COVERAGE", "Cobertura defensiva", "BAR", "BRAZIL_DEF_COVERAGE", List.of(
                                    point("PROCESSO", "Processo", snapshot.processScoped, BRAZIL_GREEN),
                                    point("TERRITORIO", "Território", snapshot.territorialCoverage, BRAZIL_BLUE),
                                    point("UNIDADES", "Unidades", snapshot.unitAnchors, BRAZIL_YELLOW),
                                    point("ASSISTIDOS", "Assistidos", snapshot.representedPeople, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/frontend/app/professional/workspace/defensoria-executive-dashboard", "/api/v1/professional/forensic-panel/client-360")
                    ),
                    new PjbFrontendProfessionalRoleSegmentView(
                            "DEF_RESULTS",
                            "Janela de resultado",
                            "ramos, acordos e movimentação recente",
                            BRAZIL_BLUE,
                            List.of(
                                    metricCard("RECENTES", "Movimentação recente", Long.toString(snapshot.window.recentes), percent(snapshot.window.recentes, snapshot.window.total), "INFO", flavor.endpoint),
                                    metricCard("ACORDOS", "Acordos e homologações", Long.toString(snapshot.window.acordos), percent(snapshot.window.acordos, snapshot.window.total), "PRIMARY", flavor.endpoint),
                                    metricCard("PARCIAIS", "Parciais", Long.toString(snapshot.window.parciais), percent(snapshot.window.parciais, snapshot.window.total), "SECONDARY", flavor.endpoint)
                            ),
                            List.of(chartSeries("DEF_RESULTS_CHART", "Resultado da assistência", "DONUT", "BRAZIL_DEF_RESULTS", List.of(
                                    point("PROCEDENTE", "Procedente", snapshot.window.procedentes, BRAZIL_GREEN),
                                    point("PARCIAL", "Parcial", snapshot.window.parciais, BRAZIL_YELLOW),
                                    point("ACORDO", "Acordo", snapshot.window.acordos, BRAZIL_BLUE),
                                    point("SIGILO", "Sigilo", snapshot.window.sigilosos, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/professional/forensic-panel/workspace")
                    )
            );
            case PROCURADORIA -> List.of(
                    new PjbFrontendProfessionalRoleSegmentView(
                            "PROC_ENTES",
                            "Entes e representação",
                            "representação formal, territorial e institucional",
                            BRAZIL_BLUE,
                            List.of(
                                    metricCard("ENTES", "Entes representados", Long.toString(snapshot.representedEnte), snapshot.processScoped + " feitos", "PRIMARY", "/api/v1/professional/forensic-panel/client-360"),
                                    metricCard("LOTACAO", "Âncoras institucionais", Long.toString(snapshot.unitAnchors), snapshot.territorialCoverage + " coberturas", "INFO", "/api/v1/professional/access-grants/governance-dashboard"),
                                    metricCard("PENDENCIAS", "Pendências críticas", Long.toString(snapshot.pendingGrants), snapshot.expiringSoon + " expiram em breve", "WARNING", "/api/v1/professional/access-grants/operational-dashboard")
                            ),
                            List.of(chartSeries("PROC_REPRESENTATION", "Representação ativa", "BAR", "BRAZIL_PROC_REPRESENTATION", List.of(
                                    point("ENTES", "Entes", snapshot.representedEnte, BRAZIL_BLUE),
                                    point("PROCESSO", "Processos", snapshot.processScoped, BRAZIL_GREEN),
                                    point("TERRITORIO", "Território", snapshot.territorialCoverage, BRAZIL_YELLOW),
                                    point("UNIDADES", "Unidades", snapshot.unitAnchors, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard", "/api/v1/professional/forensic-panel/client-360")
                    ),
                    new PjbFrontendProfessionalRoleSegmentView(
                            "PROC_RISK",
                            "Resultado e risco institucional",
                            "procedência, acordos e sigilo sensível",
                            BRAZIL_GREEN,
                            List.of(
                                    metricCard("PROCEDENTE", "Procedentes", Long.toString(snapshot.window.procedentes), percent(snapshot.window.procedentes, snapshot.window.total), "SUCCESS", flavor.endpoint),
                                    metricCard("IMPROCEDENTE", "Improcedentes", Long.toString(snapshot.window.improcedentes), percent(snapshot.window.improcedentes, snapshot.window.total), "NEUTRAL", flavor.endpoint),
                                    metricCard("SIGILOSO", "Sigilosos", Long.toString(snapshot.window.sigilosos), percent(snapshot.window.sigilosos, snapshot.window.total), "SECONDARY", flavor.endpoint)
                            ),
                            List.of(chartSeries("PROC_RISK_CHART", "Mapa institucional", "DONUT", "BRAZIL_PROC_RISK", List.of(
                                    point("PROCEDENTE", "Procedente", snapshot.window.procedentes, BRAZIL_GREEN),
                                    point("IMPROCEDENTE", "Improcedente", snapshot.window.improcedentes, BRAZIL_BLUE),
                                    point("ACORDO", "Acordo", snapshot.window.acordos, BRAZIL_YELLOW),
                                    point("SIGILO", "Sigilo", snapshot.window.sigilosos, BRAZIL_BLUE_ALT)
                            ), theme.chartPaletteHex())),
                            List.of("/api/v1/professional/forensic-panel/institutional-overview")
                    )
            );
        };
    }

    private PjbFrontendAnalyticMetricCardView metricCard(String key,
                                                         String title,
                                                         String value,
                                                         String subtitle,
                                                         String accentTag,
                                                         String route) {
        return new PjbFrontendAnalyticMetricCardView(
                key,
                title,
                value,
                subtitle,
                accentTag,
                accentHex(accentTag),
                surfaceHex(accentTag),
                BRAZIL_TEXT,
                route
        );
    }

    private PjbFrontendChartSeriesView chartSeries(String key,
                                                   String title,
                                                   String chartType,
                                                   String paletteKey,
                                                   List<PjbFrontendChartPointView> points,
                                                   List<String> palette) {
        return new PjbFrontendChartSeriesView(key, title, chartType, paletteKey, palette, points);
    }

    private PjbFrontendChartPointView point(String key,
                                            String label,
                                            long value,
                                            String accentHex) {
        return new PjbFrontendChartPointView(key, label, value, null, accentHex);
    }

    private String accentHex(String accentTag) {
        return switch (accentTag) {
            case "SUCCESS" -> BRAZIL_GREEN;
            case "WARNING" -> BRAZIL_YELLOW;
            case "INFO" -> BRAZIL_BLUE_ALT;
            case "PRIMARY" -> BRAZIL_BLUE;
            case "SECONDARY" -> BRAZIL_GREEN_DARK;
            case "NEUTRAL" -> BRAZIL_SURFACE_ALT;
            default -> BRAZIL_BLUE;
        };
    }

    private String surfaceHex(String accentTag) {
        return switch (accentTag) {
            case "SUCCESS" -> "#0F2A1B";
            case "WARNING" -> "#2D2505";
            case "INFO" -> "#11294F";
            case "PRIMARY" -> "#0C2247";
            case "SECONDARY" -> "#12315E";
            case "NEUTRAL" -> BRAZIL_SURFACE_ALT;
            default -> "#0C2247";
        };
    }

    private String percent(long value, long total) {
        if (total <= 0) {
            return "0% da janela";
        }
        long pct = Math.round((double) value * 100.0d / (double) total);
        return pct + "% da janela";
    }

    private String normalizedToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "" : normalized;
    }

    private String organizationalLens(Usuario usuario, RoleFlavor flavor) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add(flavor.title);
        if (hasText(usuario.getPerfil())) {
            parts.add(usuario.getPerfil().trim());
        }
        if (hasText(usuario.getUf())) {
            parts.add(usuario.getUf().trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(usuario.getComarca())) {
            parts.add(usuario.getComarca().trim());
        }
        return String.join(" · ", parts);
    }

    private String organizationalAnchor(ProfessionalInstitutionalAccessGrant grant) {
        ArrayList<String> parts = new ArrayList<>();
        if (hasText(grant.getTribunal())) {
            parts.add(grant.getTribunal().trim());
        }
        if (hasText(grant.getUf())) {
            parts.add(grant.getUf().trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(grant.getComarca())) {
            parts.add(grant.getComarca().trim());
        }
        if (hasText(grant.getUnidadeJudiciariaCodigo())) {
            parts.add(grant.getUnidadeJudiciariaCodigo().trim());
        }
        if (hasText(grant.getOrgaoColegiadoCodigo())) {
            parts.add(grant.getOrgaoColegiadoCodigo().trim());
        }
        return String.join("/", parts);
    }

    private String normalizeHumanName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private enum RoleFlavor {
        MAGISTRATURE(
                "MAGISTRATURE_EXECUTIVE_DASHBOARD_2026",
                "Painel Executivo da Magistratura",
                "MAGISTRATURE_ROLE_EXECUTIVE",
                "/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard",
                Set.of(ProfessionalActorClass.MAGISTRATURA, ProfessionalActorClass.APOIO_JUDICIAL),
                List.of("/api/v1/magistratura/context", "/api/v1/professional/access-grants/processos/{numero}/timeline"),
                "Competências ativas",
                "Janela jurisdicional",
                "Autos sensíveis do acervo",
                BRAZIL_BLUE,
                BRAZIL_YELLOW
        ),
        DEFENSORIA(
                "DEFENSORIA_EXECUTIVE_DASHBOARD_2026",
                "Painel Executivo da Defensoria",
                "DEFENSORIA_ROLE_EXECUTIVE",
                "/api/v1/frontend/app/professional/workspace/defensoria-executive-dashboard",
                Set.of(ProfessionalActorClass.DEFENSORIA),
                List.of("/api/v1/professional/forensic-panel/client-360", "/api/v1/professional/access-grants/templates"),
                "Designações ativas",
                "Janela da assistência",
                "Materiais sensíveis assistidos",
                BRAZIL_GREEN,
                BRAZIL_YELLOW
        ),
        PROCURADORIA(
                "PROCURADORIA_EXECUTIVE_DASHBOARD_2026",
                "Painel Executivo da Procuradoria",
                "PROCURADORIA_ROLE_EXECUTIVE",
                "/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard",
                Set.of(ProfessionalActorClass.PROCURADORIA),
                List.of("/api/v1/professional/forensic-panel/client-360", "/api/v1/professional/access-grants/governance-dashboard"),
                "Representações ativas",
                "Janela institucional",
                "Carteira sensível institucional",
                BRAZIL_BLUE,
                BRAZIL_YELLOW
        );

        private final String moduleKey;
        private final String title;
        private final String dashboardKind;
        private final String endpoint;
        private final Set<ProfessionalActorClass> supportedActors;
        private final List<String> quickRoutes;
        private final String activeLabel;
        private final String windowLabel;
        private final String sensitiveLabel;
        private final String primaryAccent;
        private final String warningAccent;

        RoleFlavor(String moduleKey,
                   String title,
                   String dashboardKind,
                   String endpoint,
                   Set<ProfessionalActorClass> supportedActors,
                   List<String> quickRoutes,
                   String activeLabel,
                   String windowLabel,
                   String sensitiveLabel,
                   String primaryAccent,
                   String warningAccent) {
            this.moduleKey = moduleKey;
            this.title = title;
            this.dashboardKind = dashboardKind;
            this.endpoint = endpoint;
            this.supportedActors = supportedActors;
            this.quickRoutes = quickRoutes;
            this.activeLabel = activeLabel;
            this.windowLabel = windowLabel;
            this.sensitiveLabel = sensitiveLabel;
            this.primaryAccent = primaryAccent;
            this.warningAccent = warningAccent;
            this.secondaryAccent = BRAZIL_GREEN_DARK;
        }

        private final String secondaryAccent;

        private String activeDetail(RoleSnapshot snapshot) {
            return snapshot.processScoped + " ancorados por processo";
        }

        private String sensitiveDetail(RoleSnapshot snapshot) {
            return snapshot.pendingStepUp + " com reforço";
        }
    }

    private record RoleSnapshot(long activeGrants,
                                long pendingGrants,
                                long pendingStepUp,
                                long expiringSoon,
                                long processScoped,
                                long territorialCoverage,
                                long relatoria,
                                long colegiado,
                                long gabinete,
                                long substituicoes,
                                long unitAnchors,
                                long representedEnte,
                                long representedPeople,
                                RoleWindowMetrics window) {
    }

    private record RoleWindowMetrics(long total,
                                     long procedentes,
                                     long improcedentes,
                                     long parciais,
                                     long acordos,
                                     long sigilosos,
                                     long recentes,
                                     long arquivados) {
    }
}
