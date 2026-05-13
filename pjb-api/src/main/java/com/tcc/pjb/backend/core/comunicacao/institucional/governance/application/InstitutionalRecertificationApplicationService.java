package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalRecertificationApplicationService {

    private static final Duration LIST_CACHE_TTL = Duration.ofSeconds(20);

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final CurrentUserService currentUserService;
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService;
    private final ConcurrentHashMap<String, CachedCycles> listCache = new ConcurrentHashMap<>();

    public InstitutionalRecertificationApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                          InstitutionalAffiliationRequestStateRepository requestRepository,
                                                          InstitutionalNominationStateRepository nominationRepository,
                                                          CurrentUserService currentUserService,
                                                          InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officialSourceDossierApplicationService = Objects.requireNonNull(officialSourceDossierApplicationService);
    }

    public List<InstitutionalRecertificationCycle> listar(String scopeCode) {
        String cacheKey = normalizeScope(scopeCode);
        CachedCycles cache = listCache.get(cacheKey);
        if (isFresh(cache)) {
            return cache.cycles();
        }
        InstitutionalOrganizationScope scope = InstitutionalOrganizationScope.fromTexto(scopeCode);
        Instant now = Instant.now();
        List<InstitutionalAffiliation> affiliations = scope == null
                ? affiliationRepository.findAll()
                : affiliationRepository.findByOrganizationScope(scope);
        Map<String, InstitutionalAffiliationRequest> requestByAffiliationId = requestRepository.findLatestByMaterializedAffiliationIds(
                        affiliations.stream().map(InstitutionalAffiliation::affiliationId).toList()).stream()
                .collect(Collectors.toMap(
                        InstitutionalAffiliationRequest::materializedAffiliationId,
                        item -> item,
                        (left, right) -> maxUpdated(left, right)));
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationIds(affiliations.stream().map(InstitutionalAffiliation::affiliationId).toList());
        List<InstitutionalRecertificationCycle> cycles = affiliations.stream()
                .map(item -> buildCycle(item, requestByAffiliationId.get(item.affiliationId()), nominations, now))
                .sorted(Comparator.comparing(InstitutionalRecertificationCycle::dueNow, Comparator.reverseOrder())
                        .thenComparing(InstitutionalRecertificationCycle::nextDueAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(InstitutionalRecertificationCycle::orgaoSigla, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        listCache.put(cacheKey, new CachedCycles(cycles, Instant.now().plus(LIST_CACHE_TTL)));
        return cycles;
    }

    public InstitutionalRecertificationCycle recertificar(String affiliationId, List<String> fundamentos) {
        InstitutionalAffiliation current = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalAffiliationRequest request = requestRepository.findLatestByMaterializedAffiliationId(affiliationId)
                .orElse(null);
        List<InstitutionalNomination> nominations = nominationRepository.findByAffiliationId(current.affiliationId());
        InstitutionalRecertificationCycle cycle = buildCycle(current, request, nominations, now);
        List<String> pendingIssues = cycle.pendingIssues();
        InstitutionalAffiliationStatus targetStatus = current.status();
        if (!pendingIssues.isEmpty() && current.status() != InstitutionalAffiliationStatus.REVOGADA) {
            targetStatus = InstitutionalAffiliationStatus.SUSPENSA;
        } else if (current.status() == InstitutionalAffiliationStatus.SOLICITADA || current.status() == InstitutionalAffiliationStatus.EM_VALIDACAO_PJB) {
            targetStatus = current.status();
        } else if (current.status() != InstitutionalAffiliationStatus.REVOGADA) {
            targetStatus = InstitutionalAffiliationStatus.HOMOLOGADA;
        }
        List<String> mergedFundamentos = appendFundamentos(
                fundamentos,
                "recertificacao_periodica_executada",
                "recertificacao_usuario=" + usuario.getId(),
                "recertificacao_em=" + now,
                "pendencias=" + pendingIssues.size());
        InstitutionalAffiliation saved = affiliationRepository.save(current.withStatus(targetStatus, now, mergedFundamentos));
        listCache.clear();
        return buildCycle(saved, request, nominations, now);
    }


    private boolean isFresh(CachedCycles cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private String normalizeScope(String scopeCode) {
        InstitutionalOrganizationScope scope = InstitutionalOrganizationScope.fromTexto(scopeCode);
        return scope == null ? "GLOBAL" : scope.name();
    }

    private InstitutionalRecertificationCycle buildCycle(InstitutionalAffiliation affiliation,
                                                         InstitutionalAffiliationRequest request,
                                                         List<InstitutionalNomination> allNominations,
                                                         Instant now) {
        List<InstitutionalNomination> nominations = allNominations.stream()
                .filter(item -> item.affiliationId().equals(affiliation.affiliationId()))
                .toList();
        List<InstitutionalNomination> activeNominations = nominations.stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        long totalAdministrators = nominations.stream()
                .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                .count();
        long activeAdministrators = activeNominations.stream()
                .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                .count();
        Instant lastVerifiedAt = max(affiliation.updatedAt(), request == null ? null : max(request.decidedAt(), request.updatedAt()));
        long cycleDays = resolveCycleDays(affiliation);
        Instant nextDueAt = lastVerifiedAt == null ? now : lastVerifiedAt.plus(cycleDays, ChronoUnit.DAYS);
        InstitutionalOfficialSourceDossier sovereignDossier = officialSourceDossierApplicationService.gerarAfiliacao(affiliation);
        List<String> pendingIssues = pendingIssues(affiliation, activeNominations.size(), activeAdministrators, nextDueAt, now, sovereignDossier);
        boolean dualSatisfied = !affiliation.requerDuplaAprovacaoAdministrador() || activeAdministrators >= 2;
        boolean dueNow = !pendingIssues.isEmpty();
        boolean compliant = affiliation.ativa() && pendingIssues.isEmpty();
        return new InstitutionalRecertificationCycle(
                affiliation.affiliationId(),
                affiliation.organizationScope() == null ? null : affiliation.organizationScope().name(),
                affiliation.orgaoSigla(),
                affiliation.orgaoNome(),
                affiliation.unidadeCodigo(),
                affiliation.unidadeNome(),
                affiliation.status().name(),
                totalAdministrators,
                activeAdministrators,
                activeNominations.size(),
                affiliation.requerDuplaAprovacaoAdministrador(),
                dualSatisfied,
                dueNow,
                compliant,
                lastVerifiedAt,
                nextDueAt,
                pendingIssues,
                buildFundamentos(affiliation, request, cycleDays, pendingIssues, dualSatisfied, sovereignDossier),
                now
        );
    }

    private List<String> pendingIssues(InstitutionalAffiliation affiliation,
                                       long totalActiveNominations,
                                       long activeAdministrators,
                                       Instant nextDueAt,
                                       Instant now,
                                       InstitutionalOfficialSourceDossier sovereignDossier) {
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        if (!affiliation.ativa()) {
            issues.add("afiliacao_nao_homologada_ou_suspensa");
        }
        if (totalActiveNominations == 0) {
            issues.add("sem_nomeacoes_ativas");
        }
        if (activeAdministrators == 0) {
            issues.add("sem_administrador_ativo");
        }
        if (affiliation.requerDuplaAprovacaoAdministrador() && activeAdministrators < 2) {
            issues.add("dupla_administracao_institucional_insuficiente");
        }
        if (nextDueAt != null && !nextDueAt.isAfter(now)) {
            issues.add("janela_recertificacao_expirada");
        }
        if (affiliation.canaisHabilitados() == null || affiliation.canaisHabilitados().isEmpty()) {
            issues.add("sem_canais_habilitados_confirmados");
        }
        if (sovereignDossier != null && !sovereignDossier.sovereignRecognitionReady()) {
            issues.add("reconhecimento_soberano_insuficiente");
        }
        if (sovereignDossier != null && sovereignDossier.dueNow()) {
            issues.add("recertificacao_soberana_pendente");
        }
        if (sovereignDossier != null) {
            issues.addAll(sovereignDossier.blockingIssues());
        }
        return List.copyOf(issues.stream().filter(Objects::nonNull).distinct().toList());
    }

    private List<String> buildFundamentos(InstitutionalAffiliation affiliation,
                                          InstitutionalAffiliationRequest request,
                                          long cycleDays,
                                          List<String> pendingIssues,
                                          boolean dualSatisfied,
                                          InstitutionalOfficialSourceDossier sovereignDossier) {
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("ciclo_dias=" + cycleDays);
        fundamentos.add("dual_admin_ok=" + dualSatisfied);
        fundamentos.add("scope=" + (affiliation.organizationScope() == null ? "NAO_INFORMADO" : affiliation.organizationScope().name()));
        if (request != null) {
            fundamentos.add("request_id=" + request.requestId());
        }
        if (sovereignDossier != null) {
            fundamentos.add("reconhecimento_soberano_pronto=" + sovereignDossier.sovereignRecognitionReady());
            fundamentos.add("dossie_soberano_due_now=" + sovereignDossier.dueNow());
            if (sovereignDossier.nextMandatoryReviewAt() != null) {
                fundamentos.add("proxima_revisao_soberana=" + sovereignDossier.nextMandatoryReviewAt());
            }
            fundamentos.addAll(sovereignDossier.blockingIssues());
        }
        fundamentos.addAll(pendingIssues);
        fundamentos.addAll(affiliation.fundamentos());
        return List.copyOf(fundamentos.stream().filter(Objects::nonNull).distinct().toList());
    }

    private long resolveCycleDays(InstitutionalAffiliation affiliation) {
        if (affiliation.restringeCertificadoRedeInstitucional()) {
            return 30;
        }
        if (affiliation.requerDuplaAprovacaoAdministrador()) {
            return 45;
        }
        return 90;
    }

    private InstitutionalAffiliationRequest maxUpdated(InstitutionalAffiliationRequest left, InstitutionalAffiliationRequest right) {
        return max(left.updatedAt(), left.decidedAt()).isAfter(max(right.updatedAt(), right.decidedAt())) ? left : right;
    }

    private Instant max(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private List<String> appendFundamentos(List<String> fundamentos, String... extras) {
        ArrayList<String> out = new ArrayList<>();
        if (fundamentos != null) {
            out.addAll(fundamentos);
        }
        if (extras != null) {
            for (String extra : extras) {
                if (extra != null && !extra.isBlank()) {
                    out.add(extra.trim());
                }
            }
        }
        return List.copyOf(out.stream().distinct().toList());
    }


    private record CachedCycles(List<InstitutionalRecertificationCycle> cycles, Instant expiresAt) { }
}
