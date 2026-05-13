package com.tcc.pjb.backend.core.kernel.governance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.InstitutionalPolicyProfileRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionalPolicyResolver {

    private final InstitutionalPolicyProfileRepository repository;

    public InstitutionalPolicySnapshotReport resolve(Processo processo,
                                                     PropostaAcordo proposta,
                                                     List<ChatMensagem> recentChat,
                                                     InstitutionalGovernanceContextReport governance,
                                                     NegotiationChatDigestReport chatDigest,
                                                     NegotiationApprovalMatrixReport approvalMatrix,
                                                     NegotiationChannelGovernanceReport channelGovernance) {
        return resolve(processo, proposta, recentChat, governance, chatDigest, approvalMatrix, channelGovernance, null);
    }

    public InstitutionalPolicySnapshotReport resolve(Processo processo,
                                                     PropostaAcordo proposta,
                                                     List<ChatMensagem> recentChat,
                                                     InstitutionalGovernanceContextReport governance,
                                                     NegotiationChatDigestReport chatDigest,
                                                     NegotiationApprovalMatrixReport approvalMatrix,
                                                     NegotiationChannelGovernanceReport channelGovernance,
                                                     String resolvedRitoCode) {
        Objects.requireNonNull(processo, "processo");

        PolicyAxisContext axis = buildAxisContext(processo, resolvedRitoCode);
        InstitutionalPolicyProfile profile = resolveBestProfile(processo, axis);

        Set<String> mandatoryDirectives = new LinkedHashSet<>();
        Set<String> blockingDirectives = new LinkedHashSet<>();
        Set<String> releaseGuardrails = new LinkedHashSet<>();
        Set<String> escalationTriggers = new LinkedHashSet<>();

        String policyKey = profile != null ? profile.getPolicyKey() : defaultPolicyKey(axis);
        String policyTier = profile != null ? safeUpper(profile.getPolicyTier()) : defaultPolicyTier(processo, axis);
        String policyVersion = profile != null ? safeText(profile.getPolicyVersion(), "POLICY/2026.1") : "POLICY/2026.1";
        boolean approvalRequired = profile != null && profile.isApprovalRequired();
        boolean strictRelease = profile != null && profile.isStrictRelease();
        double confidence = profile != null ? 0.84d : 0.62d;

        if (profile != null) {
            mandatoryDirectives.addAll(split(profile.getMandatoryDirectives()));
            blockingDirectives.addAll(split(profile.getBlockingDirectives()));
            releaseGuardrails.addAll(split(profile.getReleaseGuardrails()));
            escalationTriggers.addAll(split(profile.getEscalationTriggers()));
        }

        mandatoryDirectives.addAll(buildAxisMandatoryDirectives(axis));
        releaseGuardrails.addAll(buildAxisGuardrails(axis));
        escalationTriggers.addAll(buildAxisEscalationTriggers(axis));

        if (governance != null) {
            mandatoryDirectives.addAll(governance.policyGuards());
            blockingDirectives.addAll(governance.governanceAlerts());
            releaseGuardrails.addAll(governance.escalationPlaybooks());
            confidence += governance.governanceAlerts().isEmpty() ? 0.04d : -0.05d;
        }
        if (chatDigest != null) {
            mandatoryDirectives.addAll(chatDigest.protectedTopics());
            escalationTriggers.addAll(chatDigest.escalationSignals());
            blockingDirectives.addAll(chatDigest.forbiddenMoves());
            if ("BLOCKED_RELEASE".equals(chatDigest.sendMode())) {
                strictRelease = true;
            }
        }
        if (approvalMatrix != null) {
            approvalRequired = approvalRequired || !approvalMatrix.approvalGates().isEmpty() || !"READY_FOR_RELEASE".equals(approvalMatrix.approvalBand());
            releaseGuardrails.addAll(approvalMatrix.releaseChecklist());
            escalationTriggers.addAll(approvalMatrix.escalationLanes());
            blockingDirectives.addAll(approvalMatrix.internalControls());
            if ("BLOCKED_RELEASE".equals(approvalMatrix.releaseMode())) {
                strictRelease = true;
                confidence -= 0.04d;
            }
        }
        if (channelGovernance != null) {
            releaseGuardrails.addAll(channelGovernance.releaseBoundaries());
            mandatoryDirectives.addAll(channelGovernance.participantDirectives());
            mandatoryDirectives.addAll(channelGovernance.auditDirectives());
            mandatoryDirectives.addAll(channelGovernance.memoryDirectives());
            blockingDirectives.addAll(channelGovernance.deliveryGuardrails());
            escalationTriggers.addAll(channelGovernance.fallbackLanes());
            if ("APPROVAL_HANDSHAKE_REQUIRED".equals(channelGovernance.approvalHandshake())) {
                approvalRequired = true;
            }
        }
        if (proposta == null || proposta.getValorAcordo() == null || proposta.getValorAcordo().signum() <= 0) {
            mandatoryDirectives.add("Formalizar valor de acordo versionado antes de qualquer fechamento externo.");
            blockingDirectives.add("Não liberar mensagem de fechamento sem valor-base validado pela proposta vigente.");
            strictRelease = true;
        }
        if (proposta != null && proposta.getValorAcordo() != null && proposta.getValorAcordo().compareTo(BigDecimal.valueOf(50000)) >= 0) {
            approvalRequired = true;
            escalationTriggers.add("Faixa econômica sensível exige dupla checagem institucional.");
        }
        if (recentChat != null && recentChat.size() >= 12) {
            releaseGuardrails.add("Consolidar síntese interna da negociação antes de ampliar o ruído do canal após histórico extenso.");
        }
        if (recentChat != null && recentChat.stream().filter(Objects::nonNull).map(ChatMensagem::getConteudo).filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains("confidencial") || s.contains("sigilo"))) {
            strictRelease = true;
            mandatoryDirectives.add("Aplicar compartilhamento mínimo e linguagem controlada para itens sigilosos.");
        }
        if (isSensitiveBranch(axis.ramoDireito()) || isSensitiveMatter(axis.materia())) {
            approvalRequired = true;
            strictRelease = strictRelease || isStrictByDomain(axis);
            mandatoryDirectives.add("Submeter mensagem estratégica a régua reforçada do domínio sensível identificado.");
        }
        if (profile != null) {
            confidence += profileScore(axis, processo, profile) >= 380 ? 0.07d : 0.03d;
        }

        confidence += axis.ramoDireito() != null ? 0.02d : 0.0d;
        confidence += axis.materia() != null ? 0.01d : 0.0d;
        confidence += axis.ritoProcessual() != null ? 0.02d : 0.0d;

        InstitutionalPolicyAxisReport policyAxes = buildAxisReport(axis, profile, processo);
        String status = strictRelease || approvalRequired ? "POLICY_ATTENTION" : "POLICY_STABLE";
        return new InstitutionalPolicySnapshotReport(
                "NEGOTIATION_POLICY",
                status,
                round(clamp(confidence)),
                policyKey,
                policyTier,
                policyVersion,
                approvalRequired,
                strictRelease,
                List.copyOf(mandatoryDirectives),
                List.copyOf(blockingDirectives),
                List.copyOf(releaseGuardrails),
                List.copyOf(escalationTriggers),
                PayloadMaps.ofEntries(
                        "processoId", processo.getId(),
                        "propostaId", proposta != null ? proposta.getId() : null,
                        "policyKey", policyKey,
                        "policyTier", policyTier,
                        "policyVersion", policyVersion,
                        "profileFound", profile != null,
                        "chatCount", recentChat != null ? recentChat.size() : 0,
                        "approvalRequired", approvalRequired,
                        "strictRelease", strictRelease,
                        "resolvedRamoDireito", axis.ramoDireito(),
                        "resolvedMateria", axis.materia(),
                        "resolvedRito", axis.ritoProcessual(),
                        "resolvedClasseProcessual", axis.classeProcessual(),
                        "resolvedFaseProcessual", axis.faseProcessual(),
                        "resolvedTribunalCodigo", axis.tribunalCodigo(),
                        "selectionMode", policyAxes.selectionMode(),
                        "matchedAxes", policyAxes.matchedAxes(),
                        "declaredAxes", policyAxes.declaredAxes()
                ),
                policyAxes
        );
    }

    private InstitutionalPolicyProfile resolveBestProfile(Processo processo, PolicyAxisContext axis) {
        List<InstitutionalPolicyProfile> candidates = new ArrayList<>();
        repository.findTopByProcessoIdOrderByDataAtualizacaoDesc(processo.getId()).ifPresent(candidates::add);
        if (processo.getEquipe() != null && processo.getEquipe().getId() != null) {
            repository.findTopByEquipeIdOrderByDataAtualizacaoDesc(processo.getEquipe().getId()).ifPresent(candidates::add);
        }
        candidates.addAll(repository.findTop200ByOrderByDataAtualizacaoDesc());
        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(this::isDeclaredProfile)
                .distinct()
                .map(profile -> new ScoredProfile(profile, profileScore(axis, processo, profile)))
                .filter(scored -> scored.score() > Integer.MIN_VALUE)
                .max(Comparator.comparingInt(ScoredProfile::score)
                        .thenComparing(scored -> scored.profile().getDataAtualizacao(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ScoredProfile::profile)
                .orElse(null);
    }

    private boolean isDeclaredProfile(InstitutionalPolicyProfile profile) {
        return profile.getPolicyKey() != null && !profile.getPolicyKey().isBlank();
    }

    private int profileScore(PolicyAxisContext axis, Processo processo, InstitutionalPolicyProfile profile) {
        int score = baseTierScore(profile);
        if (profile.getProcessoId() != null) {
            if (!Objects.equals(profile.getProcessoId(), processo.getId())) {
                return Integer.MIN_VALUE;
            }
            score += 500;
        }
        if (profile.getEquipeId() != null) {
            Long equipeId = processo.getEquipe() != null ? processo.getEquipe().getId() : null;
            if (!Objects.equals(profile.getEquipeId(), equipeId)) {
                return Integer.MIN_VALUE;
            }
            score += 260;
        }
        score = scoreAxis(score, axis.ramoDireito(), profile.getRamoDireito(), 110);
        if (score == Integer.MIN_VALUE) {
            return score;
        }
        score = scoreAxis(score, axis.materia(), profile.getMateria(), 105);
        if (score == Integer.MIN_VALUE) {
            return score;
        }
        score = scoreAxis(score, axis.ritoProcessual(), profile.getRitoProcessual(), 120);
        if (score == Integer.MIN_VALUE) {
            return score;
        }
        score = scoreAxis(score, axis.faseProcessual(), profile.getFaseProcessual(), 60);
        if (score == Integer.MIN_VALUE) {
            return score;
        }
        score = scoreAxis(score, axis.tribunalCodigo(), profile.getTribunalCodigo(), 85);
        if (score == Integer.MIN_VALUE) {
            return score;
        }
        String declaredClasse = normalizeFreeText(profile.getClasseProcessual());
        if (declaredClasse != null) {
            String actualClasse = normalizeFreeText(axis.classeProcessual());
            if (actualClasse == null) {
                return Integer.MIN_VALUE;
            }
            if (actualClasse.equals(declaredClasse) || actualClasse.contains(declaredClasse) || declaredClasse.contains(actualClasse)) {
                score += 70;
            } else {
                return Integer.MIN_VALUE;
            }
        }
        return score;
    }

    private int baseTierScore(InstitutionalPolicyProfile profile) {
        String tier = safeUpper(profile.getPolicyTier());
        return switch (tier) {
            case "PROCESSO" -> 120;
            case "EQUIPE_PROCESSUAL" -> 110;
            case "TRIBUNAL" -> 90;
            case "VARA" -> 95;
            case "RAMO" -> 75;
            case "MATERIA" -> 75;
            case "RITO" -> 80;
            case "RITO_RAMO_MATERIA" -> 100;
            case "GLOBAL" -> 20;
            default -> 40;
        };
    }

    private int scoreAxis(int score, String actual, String declared, int exactWeight) {
        String normalizedDeclared = safeUpper(declared);
        if (normalizedDeclared == null) {
            return score;
        }
        String normalizedActual = safeUpper(actual);
        if (normalizedActual == null) {
            return Integer.MIN_VALUE;
        }
        return normalizedActual.equals(normalizedDeclared) ? score + exactWeight : Integer.MIN_VALUE;
    }

    private InstitutionalPolicyAxisReport buildAxisReport(PolicyAxisContext axis,
                                                          InstitutionalPolicyProfile profile,
                                                          Processo processo) {
        List<String> declaredAxes = profile == null
                ? List.of()
                : Stream.of(
                                axisEntry("RAMO", profile.getRamoDireito()),
                                axisEntry("MATERIA", profile.getMateria()),
                                axisEntry("RITO", profile.getRitoProcessual()),
                                axisEntry("CLASSE", profile.getClasseProcessual()),
                                axisEntry("FASE", profile.getFaseProcessual()),
                                axisEntry("TRIBUNAL", profile.getTribunalCodigo())
                        )
                        .filter(Objects::nonNull)
                        .toList();
        List<String> matchedAxes = profile == null
                ? Stream.of(
                                axisEntry("RAMO", axis.ramoDireito()),
                                axisEntry("MATERIA", axis.materia()),
                                axisEntry("RITO", axis.ritoProcessual()),
                                axisEntry("CLASSE", axis.classeProcessual()),
                                axisEntry("FASE", axis.faseProcessual()),
                                axisEntry("TRIBUNAL", axis.tribunalCodigo())
                        )
                        .filter(Objects::nonNull)
                        .toList()
                : Stream.of(
                                matchAxis("RAMO", axis.ramoDireito(), profile.getRamoDireito()),
                                matchAxis("MATERIA", axis.materia(), profile.getMateria()),
                                matchAxis("RITO", axis.ritoProcessual(), profile.getRitoProcessual()),
                                matchAxis("CLASSE", axis.classeProcessual(), profile.getClasseProcessual()),
                                matchAxis("FASE", axis.faseProcessual(), profile.getFaseProcessual()),
                                matchAxis("TRIBUNAL", axis.tribunalCodigo(), profile.getTribunalCodigo())
                        )
                        .filter(Objects::nonNull)
                        .toList();
        String selectionMode = profile == null
                ? "SYNTHETIC_DEFAULT"
                : deriveSelectionMode(profile, processo, matchedAxes.size());
        return new InstitutionalPolicyAxisReport(
                axis.ramoDireito(),
                axis.materia(),
                axis.ritoProcessual(),
                axis.classeProcessual(),
                axis.faseProcessual(),
                axis.tribunalCodigo(),
                selectionMode,
                matchedAxes,
                declaredAxes
        );
    }

    private String deriveSelectionMode(InstitutionalPolicyProfile profile, Processo processo, int matchedAxisCount) {
        if (profile.getProcessoId() != null && Objects.equals(profile.getProcessoId(), processo.getId())) {
            return "PROCESS_OVERRIDE";
        }
        if (profile.getEquipeId() != null && processo.getEquipe() != null && Objects.equals(profile.getEquipeId(), processo.getEquipe().getId())) {
            return "TEAM_OVERRIDE";
        }
        if (matchedAxisCount >= 3) {
            return "AXIS_EXPLICIT_MATCH";
        }
        if (matchedAxisCount >= 1) {
            return "AXIS_PARTIAL_MATCH";
        }
        return "GENERIC_PROFILE";
    }

    private List<String> buildAxisMandatoryDirectives(PolicyAxisContext axis) {
        LinkedHashSet<String> directives = new LinkedHashSet<>();
        if (axis.ramoDireito() != null) {
            directives.add("Aplicar régua institucional compatível com o ramo " + axis.ramoDireito() + ".");
        }
        if (axis.materia() != null) {
            directives.add("Ancorar a narrativa, a linguagem e os pedidos acessórios na matéria " + axis.materia() + ".");
        }
        if (axis.ritoProcessual() != null) {
            directives.add("Observar cadência, simplicidade e timing negocial aderentes ao rito " + axis.ritoProcessual() + ".");
        }
        if (axis.faseProcessual() != null) {
            directives.add("Checar compatibilidade da mensagem com a fase processual " + axis.faseProcessual() + ".");
        }
        if (axis.tribunalCodigo() != null) {
            directives.add("Aplicar régua institucional do tribunal " + axis.tribunalCodigo() + " para linguagem, aprovação e circulação.");
        }
        return List.copyOf(directives);
    }

    private List<String> buildAxisGuardrails(PolicyAxisContext axis) {
        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        if (axis.ritoProcessual() != null && axis.ritoProcessual().startsWith("JUIZADO_ESPECIAL")) {
            guardrails.add("Evitar densidade excessiva e manter objetividade compatível com a lógica dos juizados especiais.");
        }
        if (axis.ritoProcessual() != null && axis.ritoProcessual().startsWith("TRABALHISTA")) {
            guardrails.add("Preservar pragmatismo de audiência, economicidade argumentativa e foco em liquidez da proposta trabalhista.");
        }
        if (axis.ramoDireito() != null && "TRIBUTARIO".equals(axis.ramoDireito())) {
            guardrails.add("Submeter concessões tributárias a checagem reforçada de competência, liquidez e autorização institucional.");
        }
        if (axis.ramoDireito() != null && "PENAL".equals(axis.ramoDireito())) {
            guardrails.add("Restringir comunicações externas a linguagem estritamente institucional em contexto penal.");
        }
        if (axis.materia() != null && (axis.materia().contains("FAMILIA") || axis.materia().contains("INFANCIA"))) {
            guardrails.add("Aplicar linguagem mínima necessária e compartimentalização reforçada por matéria sensível.");
        }
        return List.copyOf(guardrails);
    }

    private List<String> buildAxisEscalationTriggers(PolicyAxisContext axis) {
        LinkedHashSet<String> triggers = new LinkedHashSet<>();
        if (axis.ramoDireito() != null && isSensitiveBranch(axis.ramoDireito())) {
            triggers.add("Ramo sensível requer escalada institucional preventiva antes de fechamento externo.");
        }
        if (axis.materia() != null && isSensitiveMatter(axis.materia())) {
            triggers.add("Matéria sensível requer revisão ampliada de sigilo, linguagem e alçada.");
        }
        if (axis.ritoProcessual() != null && axis.ritoProcessual().contains("EXECUCAO")) {
            triggers.add("Rito executivo exige checagem reforçada de exequibilidade e liquidez da rodada negocial.");
        }
        return List.copyOf(triggers);
    }

    private boolean isSensitiveBranch(String ramoDireito) {
        return ramoDireito != null && Set.of("PENAL", "MILITAR", "ELEITORAL", "TRIBUTARIO", "INFANCIA_JUVENTUDE").contains(ramoDireito);
    }

    private boolean isSensitiveMatter(String materia) {
        return materia != null && (materia.contains("PENAL") || materia.contains("MILITAR") || materia.contains("FAMILIA") || materia.contains("INFANCIA") || materia.contains("SAUDE"));
    }

    private boolean isStrictByDomain(PolicyAxisContext axis) {
        return (axis.materia() != null && (axis.materia().contains("FAMILIA") || axis.materia().contains("INFANCIA")))
                || (axis.ramoDireito() != null && Set.of("PENAL", "MILITAR").contains(axis.ramoDireito()));
    }

    private PolicyAxisContext buildAxisContext(Processo processo, String resolvedRitoCode) {
        String ramo = resolveRamoDireito(processo, resolvedRitoCode);
        String materia = resolveMateria(processo, ramo);
        String rito = resolveRitoProcessual(processo, resolvedRitoCode);
        String classe = normalizeFreeText(processo.getClasseProcessual());
        String fase = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null;
        Jurisdicao jurisdicao = processo.getJurisdicao();
        String tribunal = jurisdicao != null ? safeUpper(firstNonBlank(jurisdicao.getCodigo(), jurisdicao.getSigla())) : null;
        return new PolicyAxisContext(ramo, materia, rito, classe, fase, tribunal);
    }

    private String resolveRamoDireito(Processo processo, String resolvedRitoCode) {
        if (processo.getRamoDireito() != null) {
            return processo.getRamoDireito().name();
        }
        RitoProcessual rito = parseRito(resolvedRitoCode);
        if (rito == null && processo.getRito() != null) {
            rito = processo.getRito();
        }
        if (rito == null) {
            return null;
        }
        if (rito.isPenal()) {
            return RamoDireito.PENAL.name();
        }
        if (rito.isTrabalhista()) {
            return RamoDireito.TRABALHISTA.name();
        }
        if (rito.isPrevidenciario()) {
            return RamoDireito.PREVIDENCIARIO.name();
        }
        if (rito.isEleitoral()) {
            return RamoDireito.ELEITORAL.name();
        }
        if (rito.isMilitar()) {
            return RamoDireito.MILITAR.name();
        }
        if (rito.isTribFazenda()) {
            return RamoDireito.TRIBUTARIO.name();
        }
        if (rito.isAdministrativo()) {
            return RamoDireito.ADMINISTRATIVO.name();
        }
        if (rito.isAmbiental()) {
            return RamoDireito.AMBIENTAL.name();
        }
        if (rito.isInfancia()) {
            return RamoDireito.INFANCIA_JUVENTUDE.name();
        }
        if (rito.isAgrario()) {
            return RamoDireito.AGRARIO.name();
        }
        return RamoDireito.CIVIL.name();
    }

    private String resolveMateria(Processo processo, String ramoDireito) {
        if (processo.getMateria() != null) {
            return safeUpper(processo.getMateria().name());
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getMateria() != null) {
            return safeUpper(processo.getJurisdicao().getMateria().name());
        }
        if (ramoDireito == null) {
            return inferMateriaFromText(processo.getClasseProcessual(), processo.getAssunto());
        }
        try {
            return safeUpper(MateriaJurisdicao.fromRamo(RamoDireito.valueOf(ramoDireito)).name());
        } catch (Exception ignored) {
            return inferMateriaFromText(processo.getClasseProcessual(), processo.getAssunto());
        }
    }

    private String inferMateriaFromText(String classeProcessual, String assunto) {
        String base = normalizeFreeText(firstNonBlank(classeProcessual, assunto));
        if (base == null) {
            return null;
        }
        String token = normalizeToken(base);
        if (token.contains("FAMILIA") || token.contains("ALIMENT")) {
            return "FAMILIA";
        }
        if (token.contains("TRABALH")) {
            return "TRABALHISTA";
        }
        if (token.contains("PREVIDEN")) {
            return "PREVIDENCIARIA";
        }
        if (token.contains("CONSUM")) {
            return "CONSUMIDOR";
        }
        if (token.contains("TRIBUT")) {
            return "TRIBUTARIA";
        }
        if (token.contains("PENAL") || token.contains("CRIM")) {
            return "PENAL";
        }
        if (token.contains("AMBIENT")) {
            return "AMBIENTAL";
        }
        if (token.contains("AGRAR")) {
            return "AGRARIO";
        }
        return null;
    }

    private String resolveRitoProcessual(Processo processo, String resolvedRitoCode) {
        String explicit = safeUpper(resolvedRitoCode);
        if (explicit != null) {
            return explicit;
        }
        if (processo.getRito() != null) {
            return processo.getRito().name();
        }
        RitoProcessual parsed = parseRito(processo.getClasseProcessual());
        return parsed != null ? parsed.name() : null;
    }

    private RitoProcessual parseRito(String raw) {
        try {
            return RitoProcessual.tryParse(raw).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String defaultPolicyKey(PolicyAxisContext axis) {
        return Stream.of("PJB_NEGOTIATION", axis.ramoDireito(), axis.materia(), axis.ritoProcessual())
                .filter(Objects::nonNull)
                .map(this::normalizeToken)
                .filter(token -> !token.isBlank())
                .reduce((left, right) -> left + '_' + right)
                .orElse("PJB_NEGOTIATION_DEFAULT");
    }

    private String defaultPolicyTier(Processo processo, PolicyAxisContext axis) {
        if (processo.getEquipe() != null) {
            return "EQUIPE_PROCESSUAL";
        }
        if (axis.tribunalCodigo() != null && axis.ritoProcessual() != null && axis.materia() != null) {
            return "RITO_RAMO_MATERIA";
        }
        if (axis.ritoProcessual() != null) {
            return "RITO";
        }
        if (axis.materia() != null) {
            return "MATERIA";
        }
        if (axis.ramoDireito() != null) {
            return "RAMO";
        }
        return "PROCESSO";
    }

    private String axisEntry(String label, String value) {
        String normalized = safeUpper(value);
        return normalized == null ? null : label + ':' + normalized;
    }

    private String matchAxis(String label, String actual, String declared) {
        String declaredNormalized = safeUpper(declared);
        if (declaredNormalized == null) {
            return null;
        }
        String actualNormalized = safeUpper(actual);
        if (actualNormalized == null) {
            return null;
        }
        if (Objects.equals(declaredNormalized, actualNormalized)) {
            return label + ':' + actualNormalized;
        }
        if ("CLASSE".equals(label)) {
            String actualFree = normalizeFreeText(actualNormalized);
            String declaredFree = normalizeFreeText(declaredNormalized);
            if (actualFree != null && declaredFree != null && (actualFree.contains(declaredFree) || declaredFree.contains(actualFree))) {
                return label + ':' + declaredFree;
            }
        }
        return null;
    }

    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safeUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeFreeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeToken(String value) {
        String normalized = safeUpper(value);
        if (normalized == null) {
            return "";
        }
        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('-', '_')
                .replace(' ', '_')
                .replace('/', '_')
                .replaceAll("[^A-Z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(0.99d, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record PolicyAxisContext(
            String ramoDireito,
            String materia,
            String ritoProcessual,
            String classeProcessual,
            String faseProcessual,
            String tribunalCodigo
    ) {
    }

    private record ScoredProfile(
            InstitutionalPolicyProfile profile,
            int score
    ) {
    }
}
