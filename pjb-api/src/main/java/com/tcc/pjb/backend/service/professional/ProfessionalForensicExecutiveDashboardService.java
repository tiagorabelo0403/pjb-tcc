package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAnalyticMetricCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAvatarCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartPointView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartSeriesView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalProcessSpotlightView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendWorkspaceBoardColumnView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendWorkspaceBoardItemView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalCapability;
import com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentVisibilityScope;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVector;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalRecentAuditDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import com.tcc.pjb.backend.service.identity.UserAvatarService;
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
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalForensicExecutiveDashboardService {

    private static final String BRAZIL_GREEN = "#009C3B";
    private static final String BRAZIL_GREEN_DARK = "#006B2D";
    private static final String BRAZIL_YELLOW = "#FFDF00";
    private static final String BRAZIL_YELLOW_SOFT = "#F6E27A";
    private static final String BRAZIL_BLUE = "#002776";
    private static final String BRAZIL_BLUE_ALT = "#1B4D9B";
    private static final String BRAZIL_WHITE = "#FFFFFF";
    private static final String BRAZIL_BACKGROUND = "#06162E";
    private static final String BRAZIL_SURFACE = "#0C2247";
    private static final String BRAZIL_SURFACE_ALT = "#12315E";
    private static final String BRAZIL_TEXT = "#F5F8FF";

    private final CurrentUserService currentUserService;
    private final ProfessionalProcessAccessVectorService accessVectorService;
    private final ProfessionalProcessViewAuditService auditService;
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final UserAvatarService userAvatarService;
    private final NamedParameterJdbcTemplate jdbc;

    public ProfessionalForensicExecutiveDashboardService(CurrentUserService currentUserService,
                                                         ProfessionalProcessAccessVectorService accessVectorService,
                                                         ProfessionalProcessViewAuditService auditService,
                                                         ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                                         ProcessoRepository processoRepository,
                                                         MovimentacaoProcessualRepository movimentacaoRepository,
                                                         UserAvatarService userAvatarService,
                                                         NamedParameterJdbcTemplate jdbc) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.accessVectorService = Objects.requireNonNull(accessVectorService);
        this.auditService = Objects.requireNonNull(auditService);
        this.grantRepository = Objects.requireNonNull(grantRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.userAvatarService = Objects.requireNonNull(userAvatarService);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional(readOnly = true)
    public PjbFrontendProfessionalWorkspaceExecutiveDashboardView dashboard(Authentication authentication,
                                                                            LocalDate from,
                                                                            LocalDate to) {
        Usuario usuario = currentUserService.getRequired();
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null || from.isAfter(safeTo) ? safeTo.minusDays(45) : from;
        ProfessionalProcessAccessVector contextVector = searchContextVector(usuario);
        List<AccessibleProcessProjection> projections = loadAccessibleProcesses(usuario, safeFrom, safeTo, 72);
        PjbFrontendVisualThemeView visualTheme = buildBrazilExecutiveTheme();
        GrantSnapshot grantSnapshot = grantSnapshot(usuario);
        List<ProfessionalRecentAuditDto> recentAudit = auditService.recentForCurrentUser();
        List<PjbFrontendAnalyticMetricCardView> headlineCards = buildHeadlineCards(contextVector, projections, grantSnapshot, recentAudit);
        List<PjbFrontendChartSeriesView> charts = buildCharts(projections, grantSnapshot, visualTheme);
        List<PjbFrontendAvatarCardView> profileGallery = buildProfileGallery(usuario, contextVector, grantSnapshot);
        List<PjbFrontendWorkspaceBoardColumnView> board = buildBoard(projections, grantSnapshot, recentAudit);
        List<PjbFrontendProfessionalProcessSpotlightView> spotlightProcesses = projections.stream()
                .limit(8)
                .map(this::toSpotlight)
                .toList();
        LinkedHashSet<String> linkedModules = new LinkedHashSet<>();
        linkedModules.add("PROFESSIONAL_EXECUTIVE_DASHBOARD");
        linkedModules.add(contextVector.panelMode());
        linkedModules.add("BRAZIL_VISUAL_THEME");
        linkedModules.add("PROFESSIONAL_FORENSIC_PANEL");
        linkedModules.add("PROFESSIONAL_ACCESS_MATRIX");
        if (contextVector.capabilities().contains(ProfessionalCapability.USE_AI_ASSIST)) {
            linkedModules.add("PROCESSUAL_AI");
        }
        if (contextVector.capabilities().contains(ProfessionalCapability.VIEW_CALENDAR)) {
            linkedModules.add("OPERATIONAL_CALENDAR");
        }
        if (contextVector.capabilities().contains(ProfessionalCapability.USE_JUDICIAL_CALCULATOR)) {
            linkedModules.add("JUDICIAL_CALCULATOR");
        }
        LinkedHashSet<String> quickRoutes = new LinkedHashSet<>();
        quickRoutes.add("/api/v1/frontend/app/professional/workspace/executive-dashboard");
        quickRoutes.add("/api/v1/professional/forensic-panel/workspace");
        quickRoutes.add("/api/v1/professional/forensic-panel/institutional-overview");
        quickRoutes.add("/api/v1/professional/forensic-panel/process-search");
        quickRoutes.add("/api/v1/professional/forensic-panel/client-360");
        quickRoutes.add("/api/v1/professional/access-grants/workspace");
        quickRoutes.add("/api/v1/professional/access-grants/governance-dashboard");
        quickRoutes.add("/api/v1/professional/access-grants/operational-dashboard");
        quickRoutes.add("/api/v1/public/consultas-publicas/workspace");
        if (contextVector.actorClass() == ProfessionalActorClass.ADVOCACIA) {
            quickRoutes.add("/api/v1/frontend/app/offices/workspace/executive-dashboard");
        }
        if (contextVector.actorClass() == ProfessionalActorClass.MAGISTRATURA || contextVector.actorClass() == ProfessionalActorClass.APOIO_JUDICIAL) {
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard");
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard");
            linkedModules.add("MAGISTRATURE_EXECUTIVE_DASHBOARD_2026");
            linkedModules.add("MAGISTRATURE_ORGAN_EXECUTIVE_2026");
        }
        if (contextVector.actorClass() == ProfessionalActorClass.DEFENSORIA) {
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/defensoria-executive-dashboard");
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/defensoria-organ-dashboard");
            linkedModules.add("DEFENSORIA_EXECUTIVE_DASHBOARD_2026");
            linkedModules.add("DEFENSORIA_ORGAN_EXECUTIVE_2026");
        }
        if (contextVector.actorClass() == ProfessionalActorClass.PROCURADORIA) {
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/procuradoria-executive-dashboard");
            quickRoutes.add("/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard");
            linkedModules.add("PROCURADORIA_EXECUTIVE_DASHBOARD_2026");
            linkedModules.add("PROCURADORIA_ORGAN_EXECUTIVE_2026");
        }
        List<String> warnings = new ArrayList<>();
        if (projections.isEmpty()) {
            warnings.add("SEM_PROCESSOS_ACESSIVEIS_NA_JANELA_EXECUTIVA_ATUAL");
        }
        if (grantSnapshot.pending() > 0) {
            warnings.add("HA_GRANTS_PENDENTES_DE_HOMOLOGACAO");
        }
        if (projections.stream().anyMatch(item -> item.vector().requiresStepUp())) {
            warnings.add("HA_PROCESSOS_COM_STEP_UP_OBRIGATORIO");
        }
        return new PjbFrontendProfessionalWorkspaceExecutiveDashboardView(
                Instant.now(),
                contextVector.actorClass().name(),
                contextVector.panelMode(),
                displayRole(usuario, contextVector.actorClass()),
                territorialAnchor(usuario),
                visualTheme,
                headlineCards,
                charts,
                profileGallery,
                board,
                spotlightProcesses,
                List.copyOf(linkedModules),
                List.copyOf(quickRoutes),
                List.copyOf(warnings)
        );
    }

    private List<AccessibleProcessProjection> loadAccessibleProcesses(Usuario usuario,
                                                                      LocalDate from,
                                                                      LocalDate to,
                                                                      int limit) {
        String sql = """
                select p.id
                from tb_processo p
                where (p.status_processo is null or p.status_processo <> 'ARQUIVADO')
                  and (:uf = '' or upper(coalesce(p.uf,'')) = :uf)
                  and (:comarca = '' or upper(coalesce(p.comarca,'')) = :comarca)
                  and (
                        :fromDate is null
                        or coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) >= :fromDate
                  )
                  and (
                        :toDate is null
                        or coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) < :toDateExclusive
                  )
                order by coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) desc nulls last, p.id desc
                limit :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uf", normalizeToken(usuario.getUf()))
                .addValue("comarca", normalizeToken(usuario.getComarca()))
                .addValue("fromDate", from == null ? null : from.atStartOfDay())
                .addValue("toDate", to == null ? null : to.atStartOfDay())
                .addValue("toDateExclusive", to == null ? null : to.plusDays(1).atStartOfDay())
                .addValue("limit", Math.max(24, Math.min(limit, 160)));
        List<Long> ids = jdbc.query(sql, params, (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<Long, AccessibleProcessProjection> projections = new LinkedHashMap<>();
        for (Long id : ids) {
            Processo processo = processoRepository.findProcessoCompletoById(id).orElse(null);
            if (processo == null) {
                continue;
            }
            ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
            if (!vector.allowed()) {
                continue;
            }
            MovimentacaoProcessual movement = movimentacaoRepository.findTop1ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).orElse(null);
            projections.put(processo.getId(), new AccessibleProcessProjection(processo, vector, movement));
            if (projections.size() >= 48) {
                break;
            }
        }
        return List.copyOf(projections.values());
    }

    private List<PjbFrontendAnalyticMetricCardView> buildHeadlineCards(ProfessionalProcessAccessVector contextVector,
                                                                       List<AccessibleProcessProjection> projections,
                                                                       GrantSnapshot grantSnapshot,
                                                                       List<ProfessionalRecentAuditDto> recentAudit) {
        long total = projections.size();
        long represented = projections.stream().filter(item -> item.vector().represented()).count();
        long publicQualified = projections.stream().filter(item -> item.vector().publicOnly()).count();
        long confidentialEligible = projections.stream().filter(item -> confidentialEligible(item.vector())).count();
        long stepUp = projections.stream().filter(item -> item.vector().requiresStepUp()).count();
        OutcomeSummary outcomes = summarizeOutcomes(projections.stream().map(AccessibleProcessProjection::processo).toList());
        ArrayList<PjbFrontendAnalyticMetricCardView> cards = new ArrayList<>();
        cards.add(metricCard("VISIBLE", labelForVisible(contextVector.actorClass()), Long.toString(total), territorialAnchor(currentUserService.getRequired()), "PRIMARY", "/api/v1/professional/forensic-panel/institutional-overview"));
        cards.add(metricCard("REPRESENTED", labelForRepresented(contextVector.actorClass()), Long.toString(represented), percent(represented, total), "SUCCESS", "/api/v1/professional/forensic-panel/client-360"));
        cards.add(metricCard("PUBLIC", "Leitura pública qualificada", Long.toString(publicQualified), percent(publicQualified, total), "INFO", "/api/v1/public/consultas-publicas/workspace"));
        cards.add(metricCard("CONFIDENTIAL", "Material sensível elegível", Long.toString(confidentialEligible), percent(confidentialEligible, total), "WARNING", "/api/v1/professional/forensic-panel/process-search"));
        cards.add(metricCard("STEP_UP", "Step-up obrigatório", Long.toString(stepUp), stepUp > 0 ? "reautenticação exigida" : "janela limpa", "SECONDARY", "/api/v1/professional/access-grants/operational-dashboard"));
        cards.add(metricCard("GRANTS", "Grants ativos", Long.toString(grantSnapshot.active()), grantSnapshot.pending() + " pendentes", "PRIMARY", "/api/v1/professional/access-grants/workspace"));
        cards.add(metricCard("RESULTS", "Procedentes consolidados", Long.toString(outcomes.procedentes()), percent(outcomes.procedentes(), total), "SUCCESS", "/api/v1/frontend/app/professional/workspace/executive-dashboard"));
        cards.add(metricCard("AUDIT", "Consultas auditadas", Long.toString(recentAudit.size()), recentAudit.isEmpty() ? "sem eventos recentes" : safe(recentAudit.get(0).operationType()), "NEUTRAL", "/api/v1/professional/forensic-panel/workspace"));
        return List.copyOf(cards);
    }

    private List<PjbFrontendChartSeriesView> buildCharts(List<AccessibleProcessProjection> projections,
                                                         GrantSnapshot grantSnapshot,
                                                         PjbFrontendVisualThemeView theme) {
        OutcomeSummary outcomes = summarizeOutcomes(projections.stream().map(AccessibleProcessProjection::processo).toList());
        List<PjbFrontendChartSeriesView> charts = new ArrayList<>();
        charts.add(chartSeries(
                "OUTCOME_DISTRIBUTION",
                "Resultado consolidado da janela",
                "DONUT",
                "BRAZIL_RESULTS",
                List.of(
                        point("PROCEDENTE", "Procedentes", outcomes.procedentes(), "SUCCESS"),
                        point("IMPROCEDENTE", "Improcedentes", outcomes.improcedentes(), "INFO"),
                        point("PARCIAL", "Parcialmente procedentes", outcomes.parciais(), "WARNING"),
                        point("ACORDO", "Acordos", outcomes.acordos(), "PRIMARY"),
                        point("PENDENTE", "Sem resultado consolidado", outcomes.pendentes(), "NEUTRAL")
                ),
                theme.chartPaletteHex()
        ));
        charts.add(chartSeries(
                "ACCESS_SCOPE_DISTRIBUTION",
                "Escopo de acesso na carteira visível",
                "BAR",
                "BRAZIL_ACCESS",
                List.of(
                        point("PUBLIC_QUALIFIED", "Público qualificado", projections.stream().filter(item -> item.vector().publicOnly()).count(), "INFO"),
                        point("REPRESENTED", "Representados ou competentes", projections.stream().filter(item -> item.vector().represented()).count(), "SUCCESS"),
                        point("CONFIDENTIAL", "Sensíveis elegíveis", projections.stream().filter(item -> confidentialEligible(item.vector())).count(), "WARNING"),
                        point("STEP_UP", "Com step-up", projections.stream().filter(item -> item.vector().requiresStepUp()).count(), "SECONDARY")
                ),
                List.of(BRAZIL_GREEN, BRAZIL_BLUE, BRAZIL_YELLOW, BRAZIL_BLUE_ALT)
        ));
        charts.add(chartSeries(
                "RAMO_DISTRIBUTION",
                "Distribuição por ramo do direito",
                "STACKED_BAR",
                "BRAZIL_BRANCHES",
                toPoints(countByLabel(projections, item -> safeName(item.processo().getRamoDireito())), "PRIMARY"),
                List.of(BRAZIL_BLUE, BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_GREEN_DARK)
        ));
        charts.add(chartSeries(
                "SIGILO_DISTRIBUTION",
                "Sigilo e reserva documental",
                "COLUMN",
                "BRAZIL_SIGILO",
                toPoints(countByLabel(projections, item -> sigiloLabel(item.processo(), item.vector())), "WARNING"),
                List.of(BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_GREEN, BRAZIL_BLUE_ALT)
        ));
        charts.add(chartSeries(
                "MOVEMENT_FRESHNESS",
                "Ritmo de movimentação",
                "LINE",
                "BRAZIL_RHYTHM",
                List.of(
                        point("D07", "0-7 dias", countByFreshness(projections, 0, 7), "SUCCESS"),
                        point("D30", "8-30 dias", countByFreshness(projections, 8, 30), "PRIMARY"),
                        point("D90", "31-90 dias", countByFreshness(projections, 31, 90), "INFO"),
                        point("DPLUS", "90+ dias", countByFreshness(projections, 91, Integer.MAX_VALUE), "NEUTRAL")
                ),
                List.of(BRAZIL_GREEN, BRAZIL_BLUE, BRAZIL_BLUE_ALT, BRAZIL_YELLOW)
        ));
        charts.add(chartSeries(
                "GRANT_STATUS",
                "Governança de grants",
                "BAR",
                "BRAZIL_GRANTS",
                List.of(
                        point("ACTIVE", "Ativos", grantSnapshot.active(), "SUCCESS"),
                        point("PENDING", "Pendentes", grantSnapshot.pending(), "WARNING"),
                        point("REQUESTED", "Solicitados", grantSnapshot.requestedByCurrentUser(), "PRIMARY"),
                        point("GLOBAL_PENDING", "Fila superior", grantSnapshot.globalPending(), "INFO")
                ),
                List.of(BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_BLUE_ALT)
        ));
        return List.copyOf(charts);
    }

    private List<PjbFrontendAvatarCardView> buildProfileGallery(Usuario usuario,
                                                                ProfessionalProcessAccessVector contextVector,
                                                                GrantSnapshot grantSnapshot) {
        ArrayList<PjbFrontendAvatarCardView> cards = new ArrayList<>();
        cards.add(toAvatarCard(usuario.getId(), safe(usuario.getNome()), displayRole(usuario, contextVector.actorClass()), true, contextVector.actorClass().name(), "/api/v1/frontend/app/me"));
        cards.add(virtualAvatarCard("ACCESS", labelForRepresented(contextVector.actorClass()), contextVector.primaryBasis().displayName(), contextVector.actorClass().name(), "/api/v1/professional/forensic-panel/workspace"));
        cards.add(virtualAvatarCard("GRANTS", "Governança ativa", grantSnapshot.active() + " grants ativos", "PRIMARY", "/api/v1/professional/access-grants/governance-dashboard"));
        cards.add(virtualAvatarCard("PUBLIC", "Consulta pública conectada", "atos públicos e triagem", "INFO", "/api/v1/public/consultas-publicas/workspace"));
        return List.copyOf(cards);
    }

    private PjbFrontendAvatarCardView toAvatarCard(Long userId,
                                                   String nome,
                                                   String subtitle,
                                                   boolean online,
                                                   String accentTag,
                                                   String route) {
        Optional<UsuarioAvatar> avatar = userId == null ? Optional.empty() : userAvatarService.find(userId);
        String avatarUrl = avatar.isPresent() && userId != null ? "/api/v1/frontend/app/offices/team-members/" + userId + "/avatar" : null;
        String avatarEtag = avatar.map(UsuarioAvatar::getSha256).orElse(null);
        return new PjbFrontendAvatarCardView(
                userId,
                nome,
                subtitle,
                avatarUrl,
                avatarEtag,
                initials(nome),
                online,
                accentTag,
                accentHex(accentTag),
                badgeHex(accentTag),
                online ? BRAZIL_GREEN : BRAZIL_BLUE_ALT,
                route
        );
    }

    private PjbFrontendAvatarCardView virtualAvatarCard(String key,
                                                        String nome,
                                                        String subtitle,
                                                        String accentTag,
                                                        String route) {
        return new PjbFrontendAvatarCardView(
                null,
                nome,
                subtitle,
                null,
                null,
                initials(key),
                false,
                accentTag,
                accentHex(accentTag),
                badgeHex(accentTag),
                BRAZIL_BLUE_ALT,
                route
        );
    }

    private List<PjbFrontendWorkspaceBoardColumnView> buildBoard(List<AccessibleProcessProjection> projections,
                                                                 GrantSnapshot grantSnapshot,
                                                                 List<ProfessionalRecentAuditDto> recentAudit) {
        List<PjbFrontendWorkspaceBoardItemView> stepUpItems = projections.stream()
                .filter(item -> item.vector().requiresStepUp())
                .limit(6)
                .map(item -> boardItem(
                        "STEP_UP_" + item.processo().getId(),
                        resolveNumero(item.processo()),
                        safe(item.processo().getClasseProcessual()),
                        item.vector().primaryBasis().displayName(),
                        "WARNING",
                        "/api/v1/professional/forensic-panel/processos/" + resolveNumero(item.processo()) + "/access-matrix"
                ))
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> sensitiveItems = projections.stream()
                .filter(item -> confidentialEligible(item.vector()))
                .limit(6)
                .map(item -> boardItem(
                        "SENSITIVE_" + item.processo().getId(),
                        resolveNumero(item.processo()),
                        safe(item.processo().getAssunto()),
                        scopeSummary(item.vector()),
                        "SECONDARY",
                        "/api/v1/professional/forensic-panel/processos/" + resolveNumero(item.processo())
                ))
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> publicItems = projections.stream()
                .filter(item -> item.vector().publicOnly())
                .limit(6)
                .map(item -> boardItem(
                        "PUBLIC_" + item.processo().getId(),
                        resolveNumero(item.processo()),
                        safe(item.processo().getClasseProcessual()),
                        movementLabel(item.movement(), item.processo()),
                        "INFO",
                        "/api/v1/public/processos-pessoas/processos/" + resolveNumero(item.processo()) + "/resumo"
                ))
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> auditItems = recentAudit.stream()
                .limit(6)
                .map(item -> boardItem(
                        "AUDIT_" + item.auditId(),
                        safe(item.numeroProcesso()),
                        safe(item.operationType()),
                        item.accessBasis(),
                        item.success() ? "PRIMARY" : "WARNING",
                        "/api/v1/professional/forensic-panel/workspace"
                ))
                .toList();
        return List.of(
                boardColumn("STEP_UP", "Fila de step-up", stepUpItems.isEmpty() ? grantSnapshot.pending() : stepUpItems.size(), "WARNING", stepUpItems),
                boardColumn("SENSITIVE", "Escopo sensível", sensitiveItems.size(), "SECONDARY", sensitiveItems),
                boardColumn("PUBLIC", "Autos públicos qualificados", publicItems.size(), "INFO", publicItems),
                boardColumn("AUDIT", "Auditoria recente", auditItems.size(), "PRIMARY", auditItems)
        );
    }

    private PjbFrontendProfessionalProcessSpotlightView toSpotlight(AccessibleProcessProjection projection) {
        Processo processo = projection.processo();
        return new PjbFrontendProfessionalProcessSpotlightView(
                processo.getId(),
                resolveNumero(processo),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                classifyOutcome(processo),
                projection.vector().primaryBasis().displayName(),
                movementLabel(projection.movement(), processo),
                "/api/v1/professional/forensic-panel/processos/" + resolveNumero(processo),
                projection.vector().represented() ? "/api/v1/processos/pessoais/cockpit?processoId=" + processo.getId() : null
        );
    }

    private PjbFrontendVisualThemeView buildBrazilExecutiveTheme() {
        return new PjbFrontendVisualThemeView(
                "BRAZIL_EXECUTIVE_2026",
                "Brasil Executivo",
                BRAZIL_GREEN,
                BRAZIL_YELLOW,
                BRAZIL_BLUE,
                BRAZIL_GREEN_DARK,
                BRAZIL_WHITE,
                BRAZIL_BACKGROUND,
                BRAZIL_SURFACE,
                BRAZIL_SURFACE_ALT,
                BRAZIL_GREEN,
                BRAZIL_BLUE,
                List.of(BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_GREEN_DARK, BRAZIL_BLUE_ALT, BRAZIL_YELLOW_SOFT)
        );
    }

    private GrantSnapshot grantSnapshot(Usuario usuario) {
        List<ProfessionalInstitutionalAccessGrant> active = usuario == null || usuario.getId() == null
                ? List.of()
                : grantRepository.findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(usuario.getId());
        List<ProfessionalInstitutionalAccessGrant> requested = usuario == null || usuario.getId() == null
                ? List.of()
                : grantRepository.findTop50ByRequestedByUserIdOrderByIdDesc(usuario.getId());
        List<ProfessionalInstitutionalAccessGrant> pending = grantRepository.findTop50ByApprovalStatusOrderByIdDesc(ProfessionalGrantApprovalStatus.PENDING);
        return new GrantSnapshot(
                active.size(),
                pending.size(),
                requested.size(),
                pending.stream().limit(8).map(this::grantLabel).toList()
        );
    }

    private String grantLabel(ProfessionalInstitutionalAccessGrant grant) {
        if (grant == null) {
            return null;
        }
        String numero = grant.getProcesso() == null ? null : resolveNumero(grant.getProcesso());
        return List.of(safeName(grant.getGrantType()), safe(numero)).stream().filter(item -> item != null && !item.isBlank()).reduce((a, b) -> a + " · " + b).orElse("GRANT");
    }

    private PjbFrontendAnalyticMetricCardView metricCard(String key,
                                                         String label,
                                                         String value,
                                                         String secondaryValue,
                                                         String accentTone,
                                                         String route) {
        return new PjbFrontendAnalyticMetricCardView(
                key,
                label,
                value,
                secondaryValue,
                accentTone,
                accentHex(accentTone),
                surfaceHex(accentTone),
                BRAZIL_TEXT,
                route
        );
    }

    private PjbFrontendChartSeriesView chartSeries(String key,
                                                   String label,
                                                   String chartType,
                                                   String paletteKey,
                                                   List<PjbFrontendChartPointView> points,
                                                   List<String> paletteHex) {
        return new PjbFrontendChartSeriesView(key, label, chartType, paletteKey, paletteHex, points);
    }

    private PjbFrontendChartPointView point(String key,
                                            String label,
                                            long value,
                                            String accentTone) {
        return new PjbFrontendChartPointView(key, label, value, accentTone, accentHex(accentTone));
    }

    private List<PjbFrontendChartPointView> toPoints(Map<String, Long> counts, String accentTone) {
        if (counts.isEmpty()) {
            return List.of(point("SEM_DADOS", "Sem dados", 0L, accentTone));
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> point(normalizeKey(entry.getKey()), entry.getKey(), entry.getValue(), accentTone))
                .toList();
    }

    private PjbFrontendWorkspaceBoardColumnView boardColumn(String key,
                                                            String label,
                                                            long total,
                                                            String accentTone,
                                                            List<PjbFrontendWorkspaceBoardItemView> items) {
        return new PjbFrontendWorkspaceBoardColumnView(key, label, total, accentHex(accentTone), surfaceHex(accentTone), items);
    }

    private PjbFrontendWorkspaceBoardItemView boardItem(String key,
                                                        String title,
                                                        String subtitle,
                                                        String meta,
                                                        String accentTone,
                                                        String route) {
        return new PjbFrontendWorkspaceBoardItemView(key, title, subtitle, meta, accentTone, accentHex(accentTone), surfaceHex(accentTone), route);
    }

    private OutcomeSummary summarizeOutcomes(Collection<Processo> processes) {
        long procedentes = 0L;
        long improcedentes = 0L;
        long parciais = 0L;
        long acordos = 0L;
        long pendentes = 0L;
        for (Processo processo : processes) {
            String outcome = classifyOutcome(processo);
            switch (outcome) {
                case "PROCEDENTE" -> procedentes++;
                case "IMPROCEDENTE" -> improcedentes++;
                case "PARCIAL" -> parciais++;
                case "ACORDO" -> acordos++;
                default -> pendentes++;
            }
        }
        return new OutcomeSummary(procedentes, improcedentes, parciais, acordos, pendentes);
    }

    private String classifyOutcome(Processo processo) {
        if (processo == null) {
            return "PENDENTE";
        }
        String status = safeName(processo.getStatusProcesso()).toUpperCase(Locale.ROOT);
        String result = safe(processo.getResultadoFinal()).toUpperCase(Locale.ROOT);
        String text = (status + " " + result).trim();
        if (text.contains("PARCIAL")) {
            return "PARCIAL";
        }
        if (text.contains("ACORD") || text.contains("HOMOLOG")) {
            return "ACORDO";
        }
        if (text.contains("PROCEDENT")) {
            return "PROCEDENTE";
        }
        if (text.contains("IMPROCEDENT")) {
            return "IMPROCEDENTE";
        }
        return "PENDENTE";
    }

    private long countByFreshness(List<AccessibleProcessProjection> projections, int fromDays, int toDays) {
        LocalDate today = LocalDate.now();
        return projections.stream()
                .map(item -> resolveMovementDate(item.movement(), item.processo()))
                .filter(Objects::nonNull)
                .map(date -> ChronoUnit.DAYS.between(date, today))
                .filter(days -> days >= fromDays && days <= toDays)
                .count();
    }

    private LocalDate resolveMovementDate(MovimentacaoProcessual movement, Processo processo) {
        if (movement != null && movement.getDataMovimentacao() != null) {
            return movement.getDataMovimentacao().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        if (processo == null) {
            return null;
        }
        LocalDateTime timestamp = firstNonNull(processo.getDataUltimaMovimentacao(), processo.getDataAtualizacao(), processo.getDataCriacao(), processo.getDataDistribuicao());
        return timestamp == null ? null : timestamp.toLocalDate();
    }

    private String movementLabel(MovimentacaoProcessual movement, Processo processo) {
        if (movement != null && movement.getDescricao() != null && !movement.getDescricao().isBlank()) {
            return movement.getDescricao();
        }
        if (processo != null && processo.getDataUltimaMovimentacao() != null) {
            return "Atualizado em " + processo.getDataUltimaMovimentacao().toLocalDate();
        }
        return "Sem movimentação consolidada";
    }

    private boolean confidentialEligible(ProfessionalProcessAccessVector vector) {
        return vector.allowedScopes().stream().anyMatch(scope -> switch (scope) {
            case COUNSEL_REPRESENTED_PARTY, INSTITUTIONAL_REPRESENTATION, COURT_INTERNAL, CHAMBER_INTERNAL, EVIDENCE_RESTRICTED -> true;
            default -> false;
        });
    }

    private Map<String, Long> countByLabel(List<AccessibleProcessProjection> projections,
                                           Function<AccessibleProcessProjection, String> classifier) {
        return projections.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(Function.identity(), LinkedHashMap::new, java.util.stream.Collectors.counting()));
    }

    private ProfessionalProcessAccessVector searchContextVector(Usuario usuario) {
        Processo synthetic = new Processo();
        synthetic.setId(-1L);
        synthetic.setUf(usuario == null ? null : usuario.getUf());
        synthetic.setComarca(usuario == null ? null : usuario.getComarca());
        synthetic.setNivelSigilo(NivelSigilo.PUBLICO);
        return accessVectorService.resolve(usuario, synthetic);
    }

    private String labelForVisible(ProfessionalActorClass actorClass) {
        return switch (actorClass) {
            case MAGISTRATURA -> "Acervo jurisdicional visível";
            case DEFENSORIA -> "Assistidos e feitos visíveis";
            case PROCURADORIA -> "Carteira institucional visível";
            case ADVOCACIA -> "Carteira profissional visível";
            case APOIO_JUDICIAL -> "Janela delegada visível";
            case OUTRO -> "Processos visíveis";
        };
    }

    private String labelForRepresented(ProfessionalActorClass actorClass) {
        return switch (actorClass) {
            case MAGISTRATURA -> "Competência ativa";
            case DEFENSORIA -> "Designação ativa";
            case PROCURADORIA -> "Representação formal";
            case ADVOCACIA -> "Mandato e atuação";
            case APOIO_JUDICIAL -> "Delegação formal";
            case OUTRO -> "Vínculo ativo";
        };
    }

    private String displayRole(Usuario usuario, ProfessionalActorClass actorClass) {
        String tipo = usuario == null || usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name();
        return switch (actorClass) {
            case MAGISTRATURA -> tipo == null ? "Magistratura" : "Magistratura · " + tipo;
            case DEFENSORIA -> tipo == null ? "Defensoria Pública" : "Defensoria Pública · " + tipo;
            case PROCURADORIA -> tipo == null ? "Procuradoria" : "Procuradoria · " + tipo;
            case ADVOCACIA -> tipo == null ? "Advocacia" : "Advocacia · " + tipo;
            case APOIO_JUDICIAL -> tipo == null ? "Apoio judicial" : "Apoio judicial · " + tipo;
            case OUTRO -> tipo == null ? "Painel profissional" : tipo;
        };
    }

    private String territorialAnchor(Usuario usuario) {
        return List.of(safe(usuario == null ? null : usuario.getUf()).toUpperCase(Locale.ROOT), safe(usuario == null ? null : usuario.getComarca()))
                .stream()
                .filter(item -> item != null && !item.isBlank())
                .reduce((a, b) -> a + " / " + b)
                .orElse("BRASIL");
    }

    private String sigiloLabel(Processo processo, ProfessionalProcessAccessVector vector) {
        if (processo != null && processo.getNivelSigilo() != null) {
            return processo.getNivelSigilo().name();
        }
        if (confidentialEligible(vector)) {
            return "RESTRITO";
        }
        return vector.publicOnly() ? "PUBLICO" : "QUALIFICADO";
    }

    private String scopeSummary(ProfessionalProcessAccessVector vector) {
        if (vector.allowedScopes().contains(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL)) {
            return "Câmara interna";
        }
        if (vector.allowedScopes().contains(ProfessionalDocumentVisibilityScope.COURT_INTERNAL)) {
            return "Gabinete e interno";
        }
        if (vector.allowedScopes().contains(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION)) {
            return "Representação institucional";
        }
        if (vector.allowedScopes().contains(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY)) {
            return "Parte representada";
        }
        if (vector.allowedScopes().contains(ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW)) {
            return "Profissional sem mandato";
        }
        return "Ato público";
    }

    private String accentHex(String accentTone) {
        return switch (safe(accentTone).toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "ADVOCACIA", "MAGISTRATURA" -> BRAZIL_GREEN;
            case "WARNING", "SECONDARY", "DEFENSORIA" -> BRAZIL_YELLOW;
            case "INFO", "PROCURADORIA", "APOIO_JUDICIAL" -> BRAZIL_BLUE_ALT;
            case "PRIMARY" -> BRAZIL_BLUE;
            default -> BRAZIL_WHITE;
        };
    }

    private String badgeHex(String accentTone) {
        return switch (safe(accentTone).toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "ADVOCACIA", "MAGISTRATURA" -> BRAZIL_GREEN_DARK;
            case "WARNING", "SECONDARY", "DEFENSORIA" -> BRAZIL_YELLOW_SOFT;
            case "INFO", "PROCURADORIA", "APOIO_JUDICIAL" -> BRAZIL_BLUE_ALT;
            case "PRIMARY" -> BRAZIL_BLUE;
            default -> BRAZIL_SURFACE_ALT;
        };
    }

    private String surfaceHex(String accentTone) {
        return switch (safe(accentTone).toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "ADVOCACIA", "MAGISTRATURA" -> "#0D2E23";
            case "WARNING", "SECONDARY", "DEFENSORIA" -> "#322A0B";
            case "INFO", "PROCURADORIA", "APOIO_JUDICIAL" -> "#102948";
            case "PRIMARY" -> BRAZIL_SURFACE;
            default -> BRAZIL_SURFACE_ALT;
        };
    }

    private String percent(long part, long total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round((double) part * 100.0d / (double) total) + "%";
    }

    private String resolveNumero(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
            return processo.getNumeroProcesso();
        }
        return processo.getNumero();
    }

    private String initials(String value) {
        if (value == null || value.isBlank()) {
            return "PJ";
        }
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(parts[0].length(), 2)).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeName(Enum<?> value) {
        return value == null ? "SEM_DADO" : value.name();
    }

    private String normalizeToken(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        return safe(value).trim().replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(Locale.ROOT);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record AccessibleProcessProjection(
            Processo processo,
            ProfessionalProcessAccessVector vector,
            MovimentacaoProcessual movement
    ) {
    }

    private record OutcomeSummary(
            long procedentes,
            long improcedentes,
            long parciais,
            long acordos,
            long pendentes
    ) {
    }

    private record GrantSnapshot(
            long active,
            long pending,
            long requestedByCurrentUser,
            List<String> pendingLabels
    ) {
        long globalPending() {
            return pending;
        }
    }
}
