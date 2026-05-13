package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceCatalogProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceAttestationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalOfficialSourceConnectorRegistry;
import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalOfficialSourceAttestationApplicationService {

    private final InstitutionalOfficialSourceDossierApplicationService dossierApplicationService;
    private final InstitutionalOfficialSourceAttestationStateRepository stateRepository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalOfficialSourceCatalogService catalogService;
    private final InstitutionalOfficialSourceConnectorRegistry connectorRegistry;
    private final Clock clock;

    public InstitutionalOfficialSourceAttestationApplicationService(InstitutionalOfficialSourceDossierApplicationService dossierApplicationService,
                                                                   InstitutionalOfficialSourceAttestationStateRepository stateRepository,
                                                                   InstitutionalAffiliationStateRepository affiliationRepository,
                                                                   InstitutionalAffiliationRequestStateRepository requestRepository,
                                                                   InstitutionalOfficialSourceCatalogService catalogService,
                                                                   InstitutionalOfficialSourceConnectorRegistry connectorRegistry,
                                                                   Clock clock) {
        this.dossierApplicationService = Objects.requireNonNull(dossierApplicationService);
        this.stateRepository = Objects.requireNonNull(stateRepository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    public InstitutionalOfficialSourceAttestation consultarAfiliacao(String affiliationId) {
        return stateRepository.findByAffiliationId(affiliationId)
                .filter(existing -> !precisaRevalidar(existing, Instant.now(clock)))
                .orElseGet(() -> revalidarAfiliacao(affiliationId, List.of("bootstrap_atestacao_soberana")));
    }

    public InstitutionalOfficialSourceAttestation consultarSolicitacao(String requestId) {
        return stateRepository.findByRequestId(requestId)
                .filter(existing -> !precisaRevalidar(existing, Instant.now(clock)))
                .orElseGet(() -> revalidarSolicitacao(requestId, List.of("bootstrap_atestacao_soberana")));
    }

    public InstitutionalOfficialSourceAttestation revalidarAfiliacao(String affiliationId, List<String> fundamentos) {
        return stateRepository.save(build(dossierApplicationService.gerarAfiliacao(affiliationId), sanitize(fundamentos), Instant.now(clock)));
    }

    public InstitutionalOfficialSourceAttestation revalidarSolicitacao(String requestId, List<String> fundamentos) {
        return stateRepository.save(build(dossierApplicationService.gerarSolicitacao(requestId), sanitize(fundamentos), Instant.now(clock)));
    }

    public int revalidarPendencias(int batchSize) {
        int capped = Math.max(1, batchSize);
        Instant now = Instant.now(clock);
        int processed = revalidarAfiliacoesVencidas(now, capped);
        if (processed >= capped) {
            return processed;
        }
        processed += revalidarSolicitacoesVencidas(now, capped - processed);
        if (processed >= capped) {
            return processed;
        }
        processed += bootstrapAfiliacoesAtivasSemAtestado(now, capped - processed);
        if (processed >= capped) {
            return processed;
        }
        processed += bootstrapSolicitacoesAtivasSemAtestado(now, capped - processed);
        return processed;
    }

    private int revalidarAfiliacoesVencidas(Instant now, int remaining) {
        int processed = 0;
        for (String affiliationId : stateRepository.findDueAffiliationIds(now, remaining)) {
            if (processed >= remaining) {
                break;
            }
            revalidarAfiliacao(affiliationId, List.of("recorrencia_automatica_fontes_oficiais", "subject=AFILIACAO", "reason=ATESTADO_VENCIDO"));
            processed++;
        }
        return processed;
    }

    private int revalidarSolicitacoesVencidas(Instant now, int remaining) {
        int processed = 0;
        for (String requestId : stateRepository.findDueRequestIds(now, remaining)) {
            if (processed >= remaining) {
                break;
            }
            revalidarSolicitacao(requestId, List.of("recorrencia_automatica_fontes_oficiais", "subject=SOLICITACAO", "reason=ATESTADO_VENCIDO"));
            processed++;
        }
        return processed;
    }

    private int bootstrapAfiliacoesAtivasSemAtestado(Instant now, int remaining) {
        int processed = 0;
        for (var affiliation : affiliationRepository.findActive()) {
            if (processed >= remaining) {
                break;
            }
            if (stateRepository.findByAffiliationId(affiliation.affiliationId()).filter(existing -> !precisaRevalidar(existing, now)).isEmpty()) {
                revalidarAfiliacao(affiliation.affiliationId(), List.of("recorrencia_automatica_fontes_oficiais", "subject=AFILIACAO", "reason=ATESTADO_AUSENTE"));
                processed++;
            }
        }
        return processed;
    }

    private int bootstrapSolicitacoesAtivasSemAtestado(Instant now, int remaining) {
        int processed = 0;
        for (var request : requestRepository.findGovernanceActive()) {
            if (processed >= remaining) {
                break;
            }
            if (stateRepository.findByRequestId(request.requestId()).filter(existing -> !precisaRevalidar(existing, now)).isEmpty()) {
                revalidarSolicitacao(request.requestId(), List.of("recorrencia_automatica_fontes_oficiais", "subject=SOLICITACAO", "reason=ATESTADO_AUSENTE"));
                processed++;
            }
        }
        return processed;
    }

    private InstitutionalOfficialSourceAttestation build(InstitutionalOfficialSourceDossier dossier,
                                                         List<String> requestFundamentos,
                                                         Instant now) {
        List<InstitutionalOfficialSourceAttestationItem> items = dossier.sources().stream()
                .map(item -> toAttestationItem(item, now))
                .toList();
        boolean automaticRefreshEligible = items.stream()
                .filter(item -> item.applicable() && item.refreshRecommended())
                .allMatch(item -> item.autoRefreshSupported() && item.connectorLiveVerificationSupported());
        Instant nextRefreshAt = items.stream()
                .filter(item -> item.applicable() && item.nextRefreshAt() != null)
                .map(InstitutionalOfficialSourceAttestationItem::nextRefreshAt)
                .min(Instant::compareTo)
                .orElse(dossier.nextMandatoryReviewAt());
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        blockers.addAll(dossier.blockingIssues());
        items.stream().flatMap(item -> item.pendingIssues().stream()).forEach(blockers::add);
        items.stream().flatMap(item -> item.connectorBlockers().stream()).forEach(blockers::add);
        String attestationStatus = resolveAttestationStatus(dossier, automaticRefreshEligible);
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("attestation_status=" + attestationStatus);
        fundamentos.add("automatic_refresh_eligible=" + automaticRefreshEligible);
        if (nextRefreshAt != null) {
            fundamentos.add("next_refresh_at=" + nextRefreshAt);
        }
        fundamentos.addAll(dossier.fundamentos());
        fundamentos.addAll(requestFundamentos);
        String integrityHash = Hashes.sha256Hex(String.join("|",
                coalesce(dossier.subjectType()),
                coalesce(dossier.subjectId()),
                coalesce(attestationStatus),
                String.valueOf(dossier.sovereignRecognitionReady()),
                String.valueOf(dossier.dueNow()),
                String.valueOf(automaticRefreshEligible),
                String.valueOf(nextRefreshAt),
                items.stream().map(InstitutionalOfficialSourceAttestationItem::integrityHash).reduce("", String::concat)));
        return new InstitutionalOfficialSourceAttestation(
                dossier.subjectType(),
                dossier.subjectId(),
                dossier.affiliationId(),
                dossier.requestId(),
                dossier.organizationScope(),
                dossier.orgaoSigla(),
                dossier.unidadeCodigo(),
                dossier.publicRecognitionStatus(),
                attestationStatus,
                dossier.sovereignRecognitionReady(),
                dossier.dueNow(),
                automaticRefreshEligible,
                now,
                nextRefreshAt,
                List.copyOf(blockers.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList()),
                items,
                List.copyOf(fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().toList()),
                integrityHash
        );
    }

    private InstitutionalOfficialSourceAttestationItem toAttestationItem(InstitutionalOfficialSourceEvidence evidence, Instant now) {
        InstitutionalOfficialSourceCatalogProfile profile = catalogService.profileFor(evidence.sourceCode());
        InstitutionalOfficialSourceConnectorProfile connector = connectorRegistry.describe(evidence.sourceCode());
        boolean refreshRecommended = evidence.applicable() && (evidence.stale() || !evidence.satisfied());
        int confidenceScore = confidenceScore(evidence, profile, connector);
        String confidenceBand = confidenceBand(confidenceScore);
        List<String> safeNextSteps = safeNextSteps(evidence, profile, connector, refreshRecommended);
        String integrityHash = Hashes.sha256Hex(String.join("|",
                coalesce(evidence.sourceCode()),
                String.valueOf(evidence.applicable()),
                String.valueOf(evidence.satisfied()),
                String.valueOf(evidence.mandatoryForAutomaticActivation()),
                String.valueOf(evidence.stale()),
                String.valueOf(refreshRecommended),
                String.valueOf(confidenceScore),
                String.valueOf(evidence.lastEvidenceAt()),
                String.valueOf(evidence.nextReviewAt()),
                connector.connectorStatus(),
                String.valueOf(connector.liveVerificationSupported()),
                String.join(",", evidence.evidenceSignals()),
                String.join(",", evidence.pendingIssues()),
                String.join(",", connector.signals()),
                String.join(",", connector.blockers())));
        return new InstitutionalOfficialSourceAttestationItem(
                evidence.sourceCode(),
                evidence.sourceLabel(),
                profile.authority(),
                profile.authorityScope(),
                profile.accessMode(),
                profile.refreshMode(),
                profile.directGovernmentSource(),
                profile.autoRefreshSupported(),
                evidence.applicable(),
                evidence.satisfied(),
                evidence.mandatoryForAutomaticActivation(),
                evidence.stale(),
                refreshRecommended,
                confidenceScore,
                confidenceBand,
                evidence.lastEvidenceAt(),
                evidence.nextReviewAt(),
                integrityHash,
                connector.connectorStatus(),
                connector.enabled(),
                connector.liveVerificationSupported(),
                connector.referenceUrl(),
                connector.checkedAt(),
                connector.nextCheckAt(),
                connector.signals(),
                connector.blockers(),
                evidence.evidenceSignals(),
                evidence.pendingIssues(),
                safeNextSteps,
                append(profile.defaultFundamentos(), connector.fundamentos(), evidence.fundamentos(), List.of(
                        "confidence_band=" + confidenceBand,
                        "auto_refresh_supported=" + profile.autoRefreshSupported(),
                        "refresh_recommended=" + refreshRecommended,
                        "attested_at=" + now))
        );
    }

    private boolean precisaRevalidar(InstitutionalOfficialSourceAttestation attestation, Instant now) {
        if (attestation.nextRefreshAt() == null) {
            return true;
        }
        if (!attestation.nextRefreshAt().isAfter(now)) {
            return true;
        }
        if (attestation.dueNow() && attestation.lastAttestedAt() != null && attestation.lastAttestedAt().plus(Duration.ofHours(12)).isBefore(now)) {
            return true;
        }
        return attestation.lastAttestedAt() == null;
    }

    private static int confidenceScore(InstitutionalOfficialSourceEvidence evidence,
                                       InstitutionalOfficialSourceCatalogProfile profile,
                                       InstitutionalOfficialSourceConnectorProfile connector) {
        if (!evidence.applicable()) {
            return 0;
        }
        int score = profile.baseConfidence();
        score += evidence.satisfied() ? 25 : -25;
        if (evidence.mandatoryForAutomaticActivation() && evidence.satisfied()) {
            score += 5;
        }
        if (evidence.stale()) {
            score -= 20;
        }
        if (!connector.liveVerificationSupported() && profile.autoRefreshSupported()) {
            score -= 15;
        }
        score -= Math.min(20, evidence.pendingIssues().size() * 5);
        score -= Math.min(20, connector.blockers().size() * 5);
        return Math.max(0, Math.min(100, score));
    }

    private static String confidenceBand(int score) {
        if (score >= 85) {
            return "ALTA";
        }
        if (score >= 60) {
            return "MODERADA";
        }
        if (score >= 35) {
            return "RESTRITA";
        }
        return "BAIXA";
    }

    private static List<String> safeNextSteps(InstitutionalOfficialSourceEvidence evidence,
                                              InstitutionalOfficialSourceCatalogProfile profile,
                                              InstitutionalOfficialSourceConnectorProfile connector,
                                              boolean refreshRecommended) {
        LinkedHashSet<String> out = new LinkedHashSet<>(profile.defaultSafeSteps());
        if (!evidence.applicable()) {
            out.add("fonte_nao_aplicavel_para_este_escopo");
            return List.copyOf(out);
        }
        if (!refreshRecommended) {
            out.add("manter_recorrencia_de_revalidacao_para_" + normalizeToken(evidence.sourceCode()));
            return List.copyOf(out);
        }
        if (profile.autoRefreshSupported() && connector.liveVerificationSupported()) {
            out.add("executar_revalidacao_automatica_" + normalizeToken(evidence.sourceCode()));
        } else if (profile.autoRefreshSupported()) {
            out.add("concluir_preparo_do_conector_soberano_" + normalizeToken(evidence.sourceCode()));
        } else {
            out.add("exigir_homologacao_humana_para_" + normalizeToken(evidence.sourceCode()));
        }
        if (!evidence.pendingIssues().isEmpty()) {
            out.add("resolver_pendencias_da_fonte_" + normalizeToken(evidence.sourceCode()));
        }
        if (!connector.blockers().isEmpty()) {
            out.add("resolver_bloqueios_do_conector_" + normalizeToken(evidence.sourceCode()));
        }
        return List.copyOf(out);
    }

    private static String resolveAttestationStatus(InstitutionalOfficialSourceDossier dossier, boolean automaticRefreshEligible) {
        if ("NEGADA".equalsIgnoreCase(dossier.publicRecognitionStatus())) {
            return "NEGADA_SOBERANA";
        }
        if (dossier.sovereignRecognitionReady() && !dossier.dueNow()) {
            return "ATESTADO_SOBERANO";
        }
        if (automaticRefreshEligible) {
            return "REVALIDACAO_AUTOMATICA_PENDENTE";
        }
        if (dossier.sovereignRecognitionReady()) {
            return "ATESTADO_COM_REVIEW_HUMANO";
        }
        return "PENDENTE_HOMOLOGACAO_SOBERANA";
    }

    private static List<String> sanitize(List<String> fundamentos) {
        if (fundamentos == null || fundamentos.isEmpty()) {
            return List.of();
        }
        return fundamentos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    @SafeVarargs
    private static List<String> append(List<String>... groups) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String item : group) {
                if (item != null && !item.isBlank()) {
                    out.add(item.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    private static String normalizeToken(String value) {
        return value == null ? "desconhecida" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String coalesce(String value) {
        return value == null ? "" : value;
    }
}
