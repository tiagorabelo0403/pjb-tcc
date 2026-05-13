package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.MagistraturaAccessScopeResolver;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustApprovalDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalTrustApprovalDecisionStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalHorizontalDataPlaneApplicationService {

    private static final int CNJ_MAGISTRADOS_ATIVOS_BASELINE = 18_748;
    private static final int CNJ_SERVIDORES_ATIVOS_BASELINE = 278_826;
    private static final int CNJ_USUARIOS_CORE_BASELINE = CNJ_MAGISTRADOS_ATIVOS_BASELINE + CNJ_SERVIDORES_ATIVOS_BASELINE;
    private static final List<String> PARTITION_AXES = List.of("UF", "TRIBUNAL_OU_ORGAO", "UNIDADE", "CAIXA");

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalTrustApprovalDecisionStateRepository decisionRepository;
    private final InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService;
    private final PjbDataSourceRoutingProperties routingProperties;

    public InstitutionalHorizontalDataPlaneApplicationService(CurrentUserService currentUserService,
                                                              InstitutionalAffiliationStateRepository affiliationRepository,
                                                              InstitutionalNominationStateRepository nominationRepository,
                                                              InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                              InstitutionalTrustApprovalDecisionStateRepository decisionRepository,
                                                              InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService,
                                                              PjbDataSourceRoutingProperties routingProperties) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.decisionRepository = Objects.requireNonNull(decisionRepository);
        this.operatingModelClosureApplicationService = Objects.requireNonNull(operatingModelClosureApplicationService);
        this.routingProperties = Objects.requireNonNull(routingProperties);
    }

    public InstitutionalHorizontalDataPlanePlan avaliarAtual(String affiliationId, String nominationId) {
        Usuario currentUser = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalNomination nomination = resolveNomination(currentUser == null ? null : currentUser.getId(), affiliationId, nominationId, now);
        InstitutionalAffiliation affiliation = resolveAffiliation(affiliationId, nomination);
        InstitutionalEntrySummary entrySummary = safeResolveEntrySummary();
        InstitutionalEntryContext context = resolveContext(entrySummary, nomination);
        InstitutionalOperatingModelClosure closure = operatingModelClosureApplicationService.consolidar(
                affiliation,
                nomination == null ? List.of() : List.of(nomination),
                affiliation == null ? null : affiliation.destinatarioKind(),
                firstNonBlank(affiliation == null ? null : affiliation.comarca(), context == null ? null : context.comarca()),
                firstNonBlank(affiliation == null ? null : affiliation.uf(), context == null ? null : context.uf()));
        return buildPlan(currentUser, affiliation, nomination, context, closure, now);
    }

    private InstitutionalHorizontalDataPlanePlan buildPlan(Usuario currentUser,
                                                           InstitutionalAffiliation affiliation,
                                                           InstitutionalNomination nomination,
                                                           InstitutionalEntryContext context,
                                                           InstitutionalOperatingModelClosure closure,
                                                           Instant now) {
        if (nomination == null) {
            LinkedHashSet<String> findings = new LinkedHashSet<>();
            findings.add("nomeacao_institucional_ausente");
            return new InstitutionalHorizontalDataPlanePlan(
                    null,
                    affiliation == null ? null : affiliation.affiliationId(),
                    null,
                    affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                    affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().requestedMunicipality(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().requestedUf(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().responsibleTribunalCode(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().responsibleUnitCode(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().responsibleUnitName(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().responsibleComarca(),
                    null,
                    null,
                    null,
                    false,
                    currentUser != null && currentUser.getTipoUsuario() != null && (currentUser.getTipoUsuario().isMagistratura() || currentUser.getTipoUsuario().isAdvocacia() || currentUser.getTipoUsuario() == TipoUsuario.CIDADAO),
                    closure != null && closure.coverageRoute() != null && closure.coverageRoute().localUnitPresent(),
                    closure == null || closure.coverageRoute() == null ? null : closure.coverageRoute().coverageMode(),
                    resolveCoarseDataPlaneKey(affiliation, context),
                    null,
                    null,
                    0,
                    resolveBucketCount(affiliation),
                    null,
                    PARTITION_AXES,
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.copyOf(findings),
                    List.of(
                            InstitutionalHorizontalDataPlaneMessages.CNJ_SSO_REQUIRED,
                            InstitutionalHorizontalDataPlaneMessages.PLATFORM_APPROVAL_CHAIN,
                            InstitutionalHorizontalDataPlaneMessages.DATA_PLANE_GRANULAR),
                    now);
        }

        String profileKey = profileKey(affiliation, nomination);
        InstitutionalOperatingCoverageRoute coverageRoute = closure == null ? null : closure.coverageRoute();
        String requestedMunicipality = coverageRoute == null ? firstNonBlank(affiliation == null ? null : affiliation.comarca(), context == null ? null : context.comarca()) : coverageRoute.requestedMunicipality();
        String requestedUf = normalizeUpper(firstNonBlank(
                coverageRoute == null ? null : coverageRoute.requestedUf(),
                context == null ? null : context.uf(),
                affiliation == null ? null : affiliation.uf(),
                inferUfFromOrgao(affiliation == null ? null : affiliation.orgaoSigla())));
        String responsibleTribunalCode = normalizeUpper(firstNonBlank(
                coverageRoute == null ? null : coverageRoute.responsibleTribunalCode(),
                deriveTribunalCode(affiliation),
                affiliation == null ? null : affiliation.orgaoSigla()));
        String responsibleUnitCode = firstNonBlank(
                coverageRoute == null ? null : coverageRoute.responsibleUnitCode(),
                nomination.unidadeCodigo(),
                context == null ? null : context.unidadeCodigo(),
                affiliation == null ? null : affiliation.unidadeCodigo());
        String responsibleUnitName = firstNonBlank(
                coverageRoute == null ? null : coverageRoute.responsibleUnitName(),
                context == null ? null : context.unidadeNome(),
                affiliation == null ? null : affiliation.unidadeNome());
        String responsibleComarca = firstNonBlank(
                coverageRoute == null ? null : coverageRoute.responsibleComarca(),
                context == null ? null : context.comarca(),
                affiliation == null ? null : affiliation.comarca());
        String panelCode = resolvePanelCode(nomination, context);
        Map<InstitutionalTrustApprovalKind, InstitutionalTrustApprovalDecision> latestDecisions = latestDecisions(profileKey);
        LinkedHashSet<InstitutionalTrustApprovalKind> requiredApprovals = requiredApprovals(affiliation, nomination);
        LinkedHashSet<String> approvedApprovals = new LinkedHashSet<>();
        LinkedHashSet<String> pendingApprovals = new LinkedHashSet<>();
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        for (InstitutionalTrustApprovalKind requiredApproval : requiredApprovals) {
            InstitutionalTrustApprovalDecision decision = latestDecisions.get(requiredApproval);
            if (decision != null && decision.approved()) {
                approvedApprovals.add(requiredApproval.name());
            } else {
                pendingApprovals.add(requiredApproval.name());
                findings.add("aprovacao_pendente=" + requiredApproval.name());
            }
        }
        boolean fullyApproved = pendingApprovals.isEmpty();
        boolean readyForInstitutionalPanel = fullyApproved && nomination.ativaEm(now) && (affiliation == null || affiliation.ativa());
        boolean directPersonalAccessAvailable = nomination.tipoUsuario() != null && (nomination.tipoUsuario().isMagistratura() || nomination.tipoUsuario().isAdvocacia() || nomination.tipoUsuario() == TipoUsuario.CIDADAO);
        boolean routeToPersonalPanel = !readyForInstitutionalPanel && directPersonalAccessAvailable;
        String landingPath = resolveLandingPath(nomination, context, responsibleUnitCode, routeToPersonalPanel);
        int bucketCount = resolveBucketCount(affiliation);
        String primaryWritePartitionKey = partitionKey(requestedUf, responsibleTribunalCode, responsibleUnitCode, nomination.caixaCodigo());
        int writeShardBucket = stableBucket(primaryWritePartitionKey, bucketCount);
        String horizontalDataPlaneKey = primaryWritePartitionKey + "|B" + writeShardBucket;
        String warmArchivePartitionKey = partitionKey(requestedUf, responsibleTribunalCode, responsibleUnitCode, "ARQUIVO");
        String readReplicaCode = resolveReadReplicaCode(requestedUf, responsibleTribunalCode);
        LinkedHashMap<String, String> routingHeaders = buildRoutingHeaders(requestedUf, responsibleTribunalCode, affiliation, responsibleUnitCode, nomination.caixaCodigo(), readReplicaCode);
        if (coverageRoute != null && coverageRoute.localUnitPresent()) {
            findings.add("cobertura_local_ativa");
        }
        if (coverageRoute != null && !coverageRoute.localUnitPresent()) {
            findings.add("fallback_para_sede_competente");
        }
        if (!nomination.ativaEm(now)) {
            findings.add("nomeacao_institucional_inativa");
        }
        if (affiliation != null && !affiliation.ativa()) {
            findings.add("afiliacao_institucional_inativa");
        }
        if (nomination.requerRedeInstitucional()) {
            findings.add("rede_institucional_obrigatoria");
        }
        if (nomination.requerCertificadoICP()) {
            findings.add("certificado_icp_obrigatorio");
        }
        if (nomination.requerStepUpMfa()) {
            findings.add("step_up_mfa_obrigatorio");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.CNJ_SSO_REQUIRED);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.PLATFORM_APPROVAL_CHAIN);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.DATA_PLANE_GRANULAR);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.MONOLITH_READY_FOR_SPLIT);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.SHARED_ACCOUNTS_FORBIDDEN);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.PERSONAL_IDENTITY_AND_FUNCTIONAL_CONTEXT);
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.profile(profileKey));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.panel(panelCode));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.landing(landingPath));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.coverageMode(coverageRoute == null ? "INDEFINIDO" : coverageRoute.coverageMode()));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.replica(readReplicaCode));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.tribunal(responsibleTribunalCode));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.routingKey(horizontalDataPlaneKey));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.primaryPartition(primaryWritePartitionKey));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.archivePartition(warmArchivePartitionKey));
        fundamentos.add(InstitutionalHorizontalDataPlaneMessages.writeBucket(writeShardBucket, bucketCount));
        if (coverageRoute != null && coverageRoute.requestedMunicipality() != null) {
            if (coverageRoute.localUnitPresent()) {
                fundamentos.add(InstitutionalHorizontalDataPlaneMessages.localCoverage(coverageRoute.requestedMunicipality(), responsibleUnitCode));
            } else {
                fundamentos.add(InstitutionalHorizontalDataPlaneMessages.municipalityFallback(coverageRoute.requestedMunicipality(), responsibleUnitCode));
            }
            fundamentos.addAll(coverageRoute.fundamentos());
        }
        if (affiliation != null) {
            fundamentos.addAll(affiliation.fundamentos());
        }
        for (String requiredApproval : requiredApprovals.stream().map(Enum::name).toList()) {
            fundamentos.add(InstitutionalHorizontalDataPlaneMessages.approval(requiredApproval));
        }
        routingHeaders.forEach((name, value) -> fundamentos.add(InstitutionalHorizontalDataPlaneMessages.header(name, value)));
        return new InstitutionalHorizontalDataPlanePlan(
                profileKey,
                affiliation == null ? null : affiliation.affiliationId(),
                nomination.nominationId(),
                affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation == null || affiliation.destinatarioKind() == null ? null : affiliation.destinatarioKind().name(),
                requestedMunicipality,
                requestedUf,
                responsibleTribunalCode,
                responsibleUnitCode,
                responsibleUnitName,
                responsibleComarca,
                nomination.caixaCodigo(),
                panelCode,
                landingPath,
                readyForInstitutionalPanel,
                routeToPersonalPanel,
                coverageRoute != null && coverageRoute.localUnitPresent(),
                coverageRoute == null ? null : coverageRoute.coverageMode(),
                horizontalDataPlaneKey,
                primaryWritePartitionKey,
                readReplicaCode,
                writeShardBucket,
                bucketCount,
                warmArchivePartitionKey,
                PARTITION_AXES,
                routingHeaders,
                requiredApprovals.stream().map(Enum::name).toList(),
                List.copyOf(approvedApprovals),
                List.copyOf(pendingApprovals),
                List.copyOf(findings),
                List.copyOf(fundamentos),
                now);
    }

    private InstitutionalNomination resolveNomination(Long currentUserId, String affiliationId, String nominationId, Instant now) {
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
        String target = firstNonBlank(affiliationId, nomination == null ? null : nomination.affiliationId());
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

    private String resolveLandingPath(InstitutionalNomination nomination,
                                      InstitutionalEntryContext context,
                                      String responsibleUnitCode,
                                      boolean routeToPersonalPanel) {
        if (routeToPersonalPanel) {
            return InstitutionalApiRoutes.painelPessoal();
        }
        if (nomination != null && nomination.tipoUsuario() != null && nomination.tipoUsuario().isMagistratura()) {
            return MagistraturaAccessScopeResolver.resolve(
                    nomination.tipoUsuario(),
                    null,
                    firstNonBlank(responsibleUnitCode, nomination.unidadeCodigo()),
                    nomination.caixaCodigo()
            ).landingPath();
        }
        if (context != null && context.landingPath() != null && !context.landingPath().isBlank()) {
            return context.landingPath();
        }
        return InstitutionalApiRoutes.painelExecutivoComUnidade(firstNonBlank(responsibleUnitCode, nomination.unidadeCodigo()));
    }

    private int resolveBucketCount(InstitutionalAffiliation affiliation) {
        int base = CNJ_USUARIOS_CORE_BASELINE > 280_000 ? 64 : 32;
        String scope = affiliation == null || affiliation.organizationScope() == null ? null : affiliation.organizationScope().name();
        if (scope == null) {
            return base;
        }
        if (scope.contains("TRIBUNAL") || scope.contains("FORUM") || scope.contains("SECRETARIA")) {
            return Math.max(base, 64);
        }
        if (scope.contains("PROCURADORIA") || scope.contains("PROMOTORIA") || scope.contains("DEFENSORIA")) {
            return Math.max(32, base / 2);
        }
        return base;
    }

    private LinkedHashMap<String, String> buildRoutingHeaders(String requestedUf,
                                                              String responsibleTribunalCode,
                                                              InstitutionalAffiliation affiliation,
                                                              String responsibleUnitCode,
                                                              String caixaCodigo,
                                                              String readReplicaCode) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = routingProperties.getRegionalSelection();
        putHeader(headers, regionalSelection.getRequestHeaderUf(), requestedUf);
        putHeader(headers, regionalSelection.getRequestHeaderTribunal(), responsibleTribunalCode);
        putHeader(headers, regionalSelection.getRequestHeaderOrgao(), affiliation == null ? responsibleTribunalCode : firstNonBlank(affiliation.orgaoSigla(), responsibleTribunalCode));
        putHeader(headers, regionalSelection.getRequestHeaderUnidade(), responsibleUnitCode);
        putHeader(headers, regionalSelection.getRequestHeaderCaixa(), caixaCodigo);
        putHeader(headers, regionalSelection.getRequestHeaderReplica(), readReplicaCode);
        return headers;
    }

    private void putHeader(Map<String, String> headers, String name, String value) {
        if (name == null || name.isBlank() || value == null || value.isBlank()) {
            return;
        }
        headers.put(name.trim(), value.trim());
    }

    private String resolveReadReplicaCode(String requestedUf, String responsibleTribunalCode) {
        PjbDataSourceRoutingProperties.RegionalSelection regionalSelection = routingProperties.getRegionalSelection();
        String tribunalKey = normalizeUpper(responsibleTribunalCode);
        String ufKey = normalizeUpper(requestedUf);
        if (regionalSelection.getTribunalToReplica().containsKey(tribunalKey)) {
            return regionalSelection.getTribunalToReplica().get(tribunalKey);
        }
        if (regionalSelection.getUfToReplica().containsKey(ufKey)) {
            return regionalSelection.getUfToReplica().get(ufKey);
        }
        if (ufKey == null) {
            return null;
        }
        return "read-" + ufKey.toLowerCase(Locale.ROOT);
    }

    private String partitionKey(String uf, String tribunalOuOrgao, String unidadeCodigo, String caixaCodigo) {
        return String.join("|",
                normalizeUpper(defaultString(uf, "NACIONAL")),
                normalizeUpper(defaultString(tribunalOuOrgao, "ORGAO")),
                normalizeUpper(defaultString(unidadeCodigo, "UNIDADE")),
                normalizeUpper(defaultString(caixaCodigo, "CAIXA")));
    }

    private int stableBucket(String key, int bucketCount) {
        byte[] bytes = defaultString(key, "PJB").getBytes(StandardCharsets.UTF_8);
        long hash = 1125899906842597L;
        for (byte value : bytes) {
            hash = 31L * hash + value;
        }
        long positive = hash == Long.MIN_VALUE ? 0 : Math.abs(hash);
        return (int) (positive % Math.max(1, bucketCount));
    }

    private String resolveCoarseDataPlaneKey(InstitutionalAffiliation affiliation, InstitutionalEntryContext context) {
        String uf = firstNonBlank(context == null ? null : context.uf(), affiliation == null ? null : affiliation.uf());
        String scope = affiliation == null || affiliation.organizationScope() == null ? "INSTITUCIONAL" : affiliation.organizationScope().name();
        return scope + '_' + normalizeUpper(defaultString(uf, "NACIONAL"));
    }

    private String deriveTribunalCode(InstitutionalAffiliation affiliation) {
        if (affiliation == null) {
            return null;
        }
        return firstNonBlank(
                affiliation.orgaoSigla() != null && affiliation.orgaoSigla().matches("(?i)^(TJ|TRF|TRE|TRT|STM|TST|STJ|STF).*") ? affiliation.orgaoSigla() : null,
                affiliation.blueprintCode(),
                affiliation.orgaoSigla());
    }

    private String inferUfFromOrgao(String orgaoSigla) {
        if (orgaoSigla == null || orgaoSigla.isBlank()) {
            return null;
        }
        String normalized = orgaoSigla.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 4 && (normalized.startsWith("TJ") || normalized.startsWith("TR") || normalized.startsWith("MP") || normalized.startsWith("DP") || normalized.startsWith("PG"))) {
            String suffix = normalized.substring(normalized.length() - 2);
            if (suffix.chars().allMatch(Character::isLetter)) {
                return suffix;
            }
        }
        return null;
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
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

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String profileKey(InstitutionalAffiliation affiliation, InstitutionalNomination nomination) {
        String affiliationId = affiliation == null ? nomination.affiliationId() : affiliation.affiliationId();
        return affiliationId + "|" + nomination.nominationId();
    }
}
