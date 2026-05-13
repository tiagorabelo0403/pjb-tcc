package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.MagistraturaAccessScopeResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalJudiciaryPopulationSizing;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustApprovalDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalTrustApprovalDecisionStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalTrustGovernanceOrchestrationApplicationService {

    private static final int CNJ_TRIBUNAIS_NACIONAIS = 91;
    private static final Duration POPULATION_SIZING_CACHE_TTL = Duration.ofSeconds(20);
    private static final int CNJ_MAGISTRADOS_ATIVOS_BASELINE = 18_748;
    private static final int CNJ_SERVIDORES_ATIVOS_BASELINE = 278_826;
    private static final int CNJ_USUARIOS_CORE_BASELINE = CNJ_MAGISTRADOS_ATIVOS_BASELINE + CNJ_SERVIDORES_ATIVOS_BASELINE;

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalTrustApprovalDecisionStateRepository decisionRepository;
    private final AtomicReference<CachedPopulationSizing> populationSizingCache = new AtomicReference<>();

    public InstitutionalTrustGovernanceOrchestrationApplicationService(CurrentUserService currentUserService,
                                                                      InstitutionalAffiliationStateRepository affiliationRepository,
                                                                      InstitutionalNominationStateRepository nominationRepository,
                                                                      InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                                      InstitutionalTrustApprovalDecisionStateRepository decisionRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.decisionRepository = Objects.requireNonNull(decisionRepository);
    }

    public InstitutionalJudiciaryPopulationSizing dimensionarUsuariosInternos() {
        CachedPopulationSizing cache = populationSizingCache.get();
        if (isFresh(cache)) {
            return cache.sizing();
        }
        List<InstitutionalAffiliation> affiliations = affiliationRepository.findActive();
        Instant now = Instant.now();
        List<InstitutionalNomination> activeNominations = nominationRepository.findByAffiliationIds(affiliations.stream().map(InstitutionalAffiliation::affiliationId).toList()).stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        int contextCount = activeNominations.size();
        int picoSessoesPlanejado = Math.max(12_000, (int) Math.ceil(CNJ_USUARIOS_CORE_BASELINE * 0.065d));
        int replicasLeituraMinimas = Math.max(5, distinctUfCount(affiliations) / 4 + 1);
        int bucketsEscritaMinimos = picoSessoesPlanejado > 20_000 ? 64 : 32;
        LinkedHashSet<String> segmentos = new LinkedHashSet<>();
        affiliations.stream().map(InstitutionalAffiliation::organizationScope).filter(Objects::nonNull).map(Enum::name).forEach(segmentos::add);
        if (segmentos.isEmpty()) {
            segmentos.addAll(List.of("FORUM", "PROMOTORIA", "NUCLEO_DEFENSORIA", "PROCURADORIA_PUBLICA", "DELEGACIA", "ORGAO_TECNICO_CONVENIADO"));
        }
        InstitutionalJudiciaryPopulationSizing sizing = new InstitutionalJudiciaryPopulationSizing(
                CNJ_TRIBUNAIS_NACIONAIS,
                CNJ_MAGISTRADOS_ATIVOS_BASELINE,
                CNJ_SERVIDORES_ATIVOS_BASELINE,
                CNJ_USUARIOS_CORE_BASELINE,
                affiliations.size(),
                activeNominations.size(),
                contextCount,
                picoSessoesPlanejado,
                replicasLeituraMinimas,
                bucketsEscritaMinimos,
                List.of("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA"),
                List.copyOf(segmentos),
                List.of(
                        InstitutionalTrustGovernanceMessages.BASELINE_CNJ,
                        InstitutionalTrustGovernanceMessages.PARTITION_AXES,
                        InstitutionalTrustGovernanceMessages.partitionBuckets(bucketsEscritaMinimos),
                        InstitutionalTrustGovernanceMessages.replicaFloor(replicasLeituraMinimas),
                        InstitutionalTrustGovernanceMessages.modeledContexts(contextCount)),
                Instant.now());
        populationSizingCache.set(new CachedPopulationSizing(sizing, Instant.now().plus(POPULATION_SIZING_CACHE_TTL)));
        return sizing;
    }


    private boolean isFresh(CachedPopulationSizing cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    public InstitutionalTrustGovernanceProfile avaliarAtual(String affiliationId, String nominationId) {
        Usuario user = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalNomination nomination = resolveNomination(user.getId(), affiliationId, nominationId, now);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        InstitutionalEntrySummary entrySummary = safeResolveEntrySummary();
        InstitutionalEntryContext matchedContext = resolveContext(entrySummary, nomination);
        return buildProfile(user, affiliation, nomination, matchedContext, now);
    }

    public InstitutionalTrustGovernanceProfile decidir(String affiliationId,
                                                       String nominationId,
                                                       InstitutionalTrustApprovalKind approvalKind,
                                                       boolean approved,
                                                       List<String> fundamentos) {
        Usuario approver = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalNomination nomination = resolveNomination(null, affiliationId, nominationId, now);
        if (nomination == null) {
            throw new IllegalArgumentException("Nomeação institucional não encontrada para a governança de confiança.");
        }
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        enforceAuthority(approver, approvalKind, affiliation, nomination, now);
        String profileKey = profileKey(affiliation, nomination);
        InstitutionalTrustApprovalDecision decision = new InstitutionalTrustApprovalDecision(
                UUID.randomUUID().toString(),
                profileKey,
                affiliation == null ? null : affiliation.affiliationId(),
                nomination.nominationId(),
                nomination.nominatedUserId(),
                approvalKind,
                approver.getId(),
                approver.getNome(),
                approved,
                mergeDecisionFundamentos(fundamentos, approvalKind, approved, approver),
                now,
                null);
        decisionRepository.save(decision);
        InstitutionalEntryContext context = resolveContext(safeResolveEntrySummary(), nomination);
        return buildProfile(approver, affiliation, nomination, context, now);
    }

    private InstitutionalTrustGovernanceProfile buildProfile(Usuario currentUser,
                                                             InstitutionalAffiliation affiliation,
                                                             InstitutionalNomination nomination,
                                                             InstitutionalEntryContext context,
                                                             Instant now) {
        if (nomination == null) {
            return new InstitutionalTrustGovernanceProfile(
                    null,
                    affiliation == null ? null : affiliation.affiliationId(),
                    null,
                    null,
                    null,
                    null,
                    affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    affiliation == null || affiliation.trustFloor() == null ? null : affiliation.trustFloor().name(),
                    false,
                    false,
                    false,
                    currentUser.getTipoUsuario() != null && currentUser.getTipoUsuario().isInstitucional(),
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    false,
                    false,
                    true,
                    resolveDataPlaneKey(affiliation, null),
                    List.of("nomeacao_institucional_ausente"),
                    List.of(InstitutionalTrustGovernanceMessages.INSTITUTION_CREATES_PROFILE_PJB_HARDENS),
                    now);
        }
        String profileKey = profileKey(affiliation, nomination);
        LinkedHashSet<InstitutionalTrustApprovalKind> required = requiredApprovals(affiliation, nomination);
        Map<InstitutionalTrustApprovalKind, InstitutionalTrustApprovalDecision> latestByKind = latestDecisions(profileKey);
        LinkedHashSet<String> approvedKinds = new LinkedHashSet<>();
        LinkedHashSet<String> pendingKinds = new LinkedHashSet<>();
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalTrustGovernanceMessages.TRI_KEY_GOVERNANCE);
        fundamentos.add(InstitutionalTrustGovernanceMessages.INSTITUTION_CREATES_PROFILE_PJB_HARDENS);
        fundamentos.add(InstitutionalTrustGovernanceMessages.ENTRY_ROUTED_BY_CONTEXT);
        fundamentos.add(InstitutionalTrustGovernanceMessages.SHARED_ACCOUNT_FORBIDDEN);
        fundamentos.add(InstitutionalTrustGovernanceMessages.profileKey(profileKey));
        if (affiliation != null) {
            fundamentos.addAll(affiliation.fundamentos());
        }
        for (InstitutionalTrustApprovalKind kind : required) {
            fundamentos.add(InstitutionalTrustGovernanceMessages.approvalRequired(kind.name()));
            InstitutionalTrustApprovalDecision decision = latestByKind.get(kind);
            if (decision == null) {
                pendingKinds.add(kind.name());
                fundamentos.add(InstitutionalTrustGovernanceMessages.approvalPending(kind.name()));
                continue;
            }
            fundamentos.addAll(decision.fundamentos());
            if (decision.approved()) {
                approvedKinds.add(kind.name());
                fundamentos.add(InstitutionalTrustGovernanceMessages.approvalSatisfied(kind.name()));
            } else {
                pendingKinds.add(kind.name());
                findings.add("aprovacao_rejeitada=" + kind.name());
                fundamentos.add(InstitutionalTrustGovernanceMessages.approvalRejected(kind.name()));
            }
        }
        boolean judicialFlowSensitive = judicialFlowSensitive(nomination, context);
        boolean fullyApproved = pendingKinds.isEmpty() && approvedKinds.size() == required.size();
        boolean entryReady = fullyApproved && nomination.ativaEm(now) && (affiliation == null || affiliation.ativa());
        boolean directPersonalAccessAvailable = nomination.tipoUsuario() != null && (nomination.tipoUsuario().isMagistratura() || nomination.tipoUsuario().isAdvocacia() || nomination.tipoUsuario() == TipoUsuario.CIDADAO);
        boolean routeToPersonalPanel = !entryReady && directPersonalAccessAvailable;
        String panelCode = resolvePanelCode(nomination, context);
        String landingPath = resolveLandingPath(nomination, context, routeToPersonalPanel);
        String accentColor = context == null ? defaultAccent(nomination, affiliation) : context.accentColor();
        String processArea = resolveProcessAreaCode(nomination, affiliation);
        String dataPlane = resolveDataPlaneKey(affiliation, context);
        fundamentos.add(InstitutionalTrustGovernanceMessages.panel(panelCode));
        fundamentos.add(InstitutionalTrustGovernanceMessages.landing(landingPath));
        fundamentos.add(InstitutionalTrustGovernanceMessages.dataPlane(dataPlane));
        fundamentos.add(InstitutionalTrustGovernanceMessages.audience(processArea));
        if (!nomination.ativaEm(now)) {
            findings.add("nomeacao_institucional_inativa");
        }
        if (affiliation != null && !affiliation.ativa()) {
            findings.add("afiliacao_institucional_inativa");
        }
        if (!fullyApproved) {
            findings.add("governanca_confianca_pendente");
        }
        if (judicialFlowSensitive) {
            findings.add("painel_sensivel_exige_governanca_completa");
        }
        return new InstitutionalTrustGovernanceProfile(
                profileKey,
                affiliation == null ? null : affiliation.affiliationId(),
                nomination.nominationId(),
                nomination.nominatedUserId(),
                nomination.nominatedUserName(),
                nomination.tipoUsuario() == null ? null : nomination.tipoUsuario().name(),
                affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                nomination.unidadeCodigo(),
                nomination.caixaCodigo(),
                panelCode,
                landingPath,
                accentColor,
                processArea,
                nomination.trustFloor() == null ? affiliation == null || affiliation.trustFloor() == null ? null : affiliation.trustFloor().name() : nomination.trustFloor().name(),
                nomination.requerStepUpMfa(),
                nomination.requerCertificadoICP(),
                nomination.requerRedeInstitucional(),
                directPersonalAccessAvailable,
                judicialFlowSensitive,
                required.stream().map(Enum::name).toList(),
                List.copyOf(approvedKinds),
                List.copyOf(pendingKinds),
                fullyApproved,
                entryReady,
                routeToPersonalPanel,
                dataPlane,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                now);
    }

    private InstitutionalNomination resolveNomination(Long currentUserId,
                                                      String affiliationId,
                                                      String nominationId,
                                                      Instant now) {
        if (nominationId != null && !nominationId.isBlank()) {
            return nominationRepository.findByNominationId(nominationId).orElse(null);
        }
        if (currentUserId == null) {
            return null;
        }
        return nominationRepository.findByNominatedUserId(currentUserId).stream()
                .filter(item -> item.ativaEm(now))
                .filter(item -> affiliationId == null || affiliationId.isBlank() || item.affiliationId().equalsIgnoreCase(affiliationId))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private InstitutionalAffiliation resolveAffiliation(String affiliationId, InstitutionalNomination nomination) {
        String target = affiliationId == null || affiliationId.isBlank()
                ? nomination == null ? null : nomination.affiliationId()
                : affiliationId.trim();
        return target == null ? null : affiliationRepository.findByAffiliationId(target).orElse(null);
    }

    private InstitutionalEntrySummary safeResolveEntrySummary() {
        try {
            return entryContextApplicationService.resolverEntradaAtual();
        } catch (Exception ex) {
            return null;
        }
    }

    private InstitutionalEntryContext resolveContext(InstitutionalEntrySummary summary, InstitutionalNomination nomination) {
        if (summary == null || nomination == null) {
            return null;
        }
        return summary.contextos().stream()
                .filter(item -> nomination.unidadeCodigo().equalsIgnoreCase(item.unidadeCodigo()))
                .filter(item -> nomination.caixaCodigo().equalsIgnoreCase(item.caixaCodigo()))
                .findFirst()
                .orElse(summary.contextoPreferencial());
    }

    private LinkedHashSet<InstitutionalTrustApprovalKind> requiredApprovals(InstitutionalAffiliation affiliation,
                                                                            InstitutionalNomination nomination) {
        LinkedHashSet<InstitutionalTrustApprovalKind> approvals = new LinkedHashSet<>();
        approvals.add(InstitutionalTrustApprovalKind.PJB);
        approvals.add(InstitutionalTrustApprovalKind.DIRETOR_GERAL);
        if (nomination != null && nomination.tipoUsuario() != null && nomination.tipoUsuario().isMagistratura()) {
            return approvals;
        }
        if (nomination != null && (containsQueueMutation(nomination) || containsSignature(nomination) || isForumScoped(affiliation))) {
            approvals.add(InstitutionalTrustApprovalKind.MAGISTRADO_REFERENCIAL);
        }
        return approvals;
    }

    private Map<InstitutionalTrustApprovalKind, InstitutionalTrustApprovalDecision> latestDecisions(String profileKey) {
        LinkedHashMap<InstitutionalTrustApprovalKind, InstitutionalTrustApprovalDecision> out = new LinkedHashMap<>();
        for (InstitutionalTrustApprovalDecision decision : decisionRepository.findByProfileKey(profileKey)) {
            out.putIfAbsent(decision.approvalKind(), decision);
        }
        return out;
    }

    private boolean containsQueueMutation(InstitutionalNomination nomination) {
        return nomination.capacidades() != null && nomination.capacidades().stream().anyMatch(CapacidadeCaixaInstitucional::isMutacaoFila);
    }

    private boolean containsSignature(InstitutionalNomination nomination) {
        return nomination.capacidades() != null && nomination.capacidades().stream().anyMatch(cap -> cap.isAtoDeAssinaturaOuManifestacao() || cap.isAtoDeCiencia());
    }

    private boolean judicialFlowSensitive(InstitutionalNomination nomination, InstitutionalEntryContext context) {
        return containsQueueMutation(nomination)
                || containsSignature(nomination)
                || (context != null && context.totalUrgentes() > 0)
                || nomination.requerStepUpMfa()
                || nomination.requerCertificadoICP();
    }

    private boolean isForumScoped(InstitutionalAffiliation affiliation) {
        if (affiliation == null || affiliation.organizationScope() == null) {
            return false;
        }
        return switch (affiliation.organizationScope()) {
            case FORUM, SECRETARIA_UNIDADE_JUDICIARIA, CENTRAL_AUDIENCIAS, CENTRAL_MANDADOS, CEJUSC, CONTADORIA, EQUIPE_PSICOSSOCIAL -> true;
            default -> false;
        };
    }

    private String resolvePanelCode(InstitutionalNomination nomination, InstitutionalEntryContext context) {
        if (nomination != null && nomination.tipoUsuario() != null && nomination.tipoUsuario().isMagistratura()) {
            return MagistraturaAccessScopeResolver.resolve(
                    nomination.tipoUsuario(),
                    null,
                    nomination.unidadeCodigo(),
                    nomination.caixaCodigo()
            ).panelCode();
        }
        if (context != null && context.landingPanel() != null) {
            return context.landingPanel().name();
        }
        return nomination.panelPreferencial() == null ? "PAINEL_ORGAO" : nomination.panelPreferencial().name();
    }

    private String resolveLandingPath(InstitutionalNomination nomination, InstitutionalEntryContext context, boolean routeToPersonalPanel) {
        if (routeToPersonalPanel) {
            return InstitutionalApiRoutes.painelPessoal();
        }
        if (nomination != null && nomination.tipoUsuario() != null && nomination.tipoUsuario().isMagistratura()) {
            return MagistraturaAccessScopeResolver.resolve(
                    nomination.tipoUsuario(),
                    null,
                    nomination.unidadeCodigo(),
                    nomination.caixaCodigo()
            ).landingPath();
        }
        if (context != null && context.landingPath() != null && !context.landingPath().isBlank()) {
            return context.landingPath();
        }
        return InstitutionalApiRoutes.painelExecutivoComUnidade(nomination.unidadeCodigo());
    }

    private String resolveProcessAreaCode(InstitutionalNomination nomination, InstitutionalAffiliation affiliation) {
        if (nomination.tipoUsuario() != null) {
            return nomination.tipoUsuario().papelArquitetural();
        }
        return affiliation == null || affiliation.organizationScope() == null ? "INSTITUCIONAL" : affiliation.organizationScope().name();
    }

    private String defaultAccent(InstitutionalNomination nomination, InstitutionalAffiliation affiliation) {
        if (nomination.nominationRole() != null && nomination.nominationRole().isGestaoMestre()) {
            return "ADMIN_INSTITUCIONAL";
        }
        return affiliation == null || affiliation.organizationScope() == null ? "INSTITUCIONAL" : affiliation.organizationScope().name();
    }

    private String resolveDataPlaneKey(InstitutionalAffiliation affiliation, InstitutionalEntryContext context) {
        String uf = firstNonBlank(context == null ? null : context.uf(), affiliation == null ? null : affiliation.uf());
        String scope = affiliation == null || affiliation.organizationScope() == null ? "INSTITUCIONAL" : affiliation.organizationScope().name();
        String normalizedUf = uf == null || uf.isBlank() ? "NACIONAL" : uf.trim().toUpperCase(Locale.ROOT);
        return scope + '_' + normalizedUf;
    }

    private void enforceAuthority(Usuario approver,
                                  InstitutionalTrustApprovalKind approvalKind,
                                  InstitutionalAffiliation affiliation,
                                  InstitutionalNomination targetNomination,
                                  Instant now) {
        TipoUsuario tipo = approver.getTipoUsuario();
        if (approvalKind == InstitutionalTrustApprovalKind.PJB) {
            if (tipo == null || !tipo.isAdmin()) {
                throw new IllegalStateException("A aprovação PJB exige administrador da plataforma.");
            }
            return;
        }
        List<InstitutionalNomination> approverNominations = nominationRepository.findByNominatedUserId(approver.getId()).stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        boolean sameAffiliation = affiliation != null && approverNominations.stream().anyMatch(item -> affiliation.affiliationId().equalsIgnoreCase(item.affiliationId()));
        if (approvalKind == InstitutionalTrustApprovalKind.DIRETOR_GERAL) {
            boolean allowed = approverNominations.stream()
                    .filter(item -> sameAffiliation || item.unidadeCodigo().equalsIgnoreCase(targetNomination.unidadeCodigo()))
                    .anyMatch(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre());
            if (!allowed) {
                throw new IllegalStateException("A aprovação de diretoria geral exige gestor mestre da mesma malha institucional.");
            }
            return;
        }
        if (approvalKind == InstitutionalTrustApprovalKind.MAGISTRADO_REFERENCIAL) {
            boolean allowed = tipo != null && tipo.isMagistratura();
            if (!allowed) {
                throw new IllegalStateException("A aprovação magistrado referencial exige usuário com perfil de magistratura.");
            }
        }
    }

    private List<String> mergeDecisionFundamentos(List<String> fundamentos,
                                                  InstitutionalTrustApprovalKind approvalKind,
                                                  boolean approved,
                                                  Usuario approver) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (fundamentos != null) {
            fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).forEach(out::add);
        }
        out.add(InstitutionalTrustGovernanceMessages.authority(approvalKind.name()));
        out.add(approved ? InstitutionalTrustGovernanceMessages.approvalSatisfied(approvalKind.name()) : InstitutionalTrustGovernanceMessages.approvalRejected(approvalKind.name()));
        out.add("approver_user_id=" + approver.getId());
        return List.copyOf(out);
    }

    private static String profileKey(InstitutionalAffiliation affiliation, InstitutionalNomination nomination) {
        String affiliationId = affiliation == null ? nomination.affiliationId() : affiliation.affiliationId();
        return affiliationId + "|" + nomination.nominationId();
    }

    private static int distinctUfCount(List<InstitutionalAffiliation> affiliations) {
        return (int) affiliations.stream()
                .map(InstitutionalAffiliation::uf)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> item.toUpperCase(Locale.ROOT))
                .distinct()
                .count();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record CachedPopulationSizing(InstitutionalJudiciaryPopulationSizing sizing, Instant expiresAt) { }
}
