package com.tcc.pjb.backend.service.julgamento.safety;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionFocusResponse;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionPreflightResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionConfusionAudit;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.model.repository.julgamento.DecisionConfusionAuditRepository;
import com.tcc.pjb.backend.model.repository.julgamento.DecisionFocusSessionRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.julgamento.coverage.JulgamentoCoverageIntelligenceService;

@Service
public class DecisionSafetyService {

    private static final Set<String> BLOCK_RESULTS = Set.of("BLOCKED", "MISMATCH");

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final DecisionFocusSessionRepository decisionFocusSessionRepository;
    private final DecisionConfusionAuditRepository decisionConfusionAuditRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final JulgamentoCoverageIntelligenceService julgamentoCoverageIntelligenceService;
    private final DecisionClientBindingGuardService decisionClientBindingGuardService;
    private final DecisionBiometricStepUpService decisionBiometricStepUpService;
    private final AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService;

    public DecisionSafetyService(CurrentUserService currentUserService,
                                 ProcessoRepository processoRepository,
                                 DecisionFocusSessionRepository decisionFocusSessionRepository,
                                 DecisionConfusionAuditRepository decisionConfusionAuditRepository,
                                 WorkItemRepository workItemRepository,
                                 InstitutionalActorRoutingService institutionalActorRoutingService,
                                 JulgamentoCoverageIntelligenceService julgamentoCoverageIntelligenceService,
                                 DecisionClientBindingGuardService decisionClientBindingGuardService,
                                 DecisionBiometricStepUpService decisionBiometricStepUpService,
                                 AtoProcessualSecurityPolicyService atoProcessualSecurityPolicyService) {
        this.currentUserService = currentUserService;
        this.processoRepository = processoRepository;
        this.decisionFocusSessionRepository = decisionFocusSessionRepository;
        this.decisionConfusionAuditRepository = decisionConfusionAuditRepository;
        this.workItemRepository = workItemRepository;
        this.institutionalActorRoutingService = institutionalActorRoutingService;
        this.julgamentoCoverageIntelligenceService = julgamentoCoverageIntelligenceService;
        this.decisionClientBindingGuardService = decisionClientBindingGuardService;
        this.decisionBiometricStepUpService = decisionBiometricStepUpService;
        this.atoProcessualSecurityPolicyService = atoProcessualSecurityPolicyService;
    }

    @Transactional
    public DecisionFocusResponse openFocus(Long processoId, String windowBinding, String tabBinding, String routeBinding) {
        Usuario usuario = requireMagistrado();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        String safeWindowBinding = requireBinding(windowBinding, "janela");
        String safeTabBinding = requireBinding(tabBinding, "aba");
        String safeRouteBinding = trimToNull(routeBinding);
        expireSessions(usuario.getId(), Instant.now());
        List<DecisionFocusSession> active = decisionFocusSessionRepository.findActiveByUsuario(usuario.getId());
        Optional<DecisionFocusSession> armedOther = active.stream()
                .filter(this::isActive)
                .filter(session -> !Objects.equals(session.getProcesso().getId(), processoId))
                .findFirst();
        if (armedOther.isPresent()) {
            throw validation("Existe foco decisional ativo em outro processo. Encerre antes de trocar de caso.")
                    .addMetadado("processo_em_foco", armedOther.get().getProcesso().getId())
                    .addMetadado("session_id", armedOther.get().getId());
        }
        DecisionFocusSession existing = active.stream()
                .filter(this::isActive)
                .filter(session -> Objects.equals(session.getProcesso().getId(), processoId))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            refreshBinding(existing, safeWindowBinding, safeTabBinding, safeRouteBinding);
            existing.setLastHeartbeatAt(Instant.now());
            existing.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
            decisionFocusSessionRepository.save(existing);
            return toResponse(existing);
        }

        DecisionFocusSession session = new DecisionFocusSession();
        session.setUsuario(usuario);
        session.setProcesso(processo);
        session.setSessionToken(Hashes.sha256Hex(usuario.getId() + ":" + processo.getId() + ":" + Instant.now().toEpochMilli()));
        session.setProcessFingerprint(buildFingerprint(processo));
        refreshBinding(session, safeWindowBinding, safeTabBinding, safeRouteBinding);
        session.setNumeroSnapshot(resolveNumero(processo));
        session.setClasseSnapshot(trimToNull(processo.getClasseProcessual()));
        session.setAutorSnapshot(trimToNull(processo.getParteAutoraNome()));
        session.setReuSnapshot(trimToNull(processo.getParteReuNome()));
        session.setAssuntoSnapshot(trimToNull(processo.getAssunto()));
        session.setSummarySnapshot(buildSummary(processo));
        session.setOpenedAt(Instant.now());
        session.setLastHeartbeatAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        session.setStatus("OPEN");
        decisionFocusSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public DecisionFocusResponse armFocus(Long sessionId) {
        Usuario usuario = requireMagistrado();
        DecisionFocusSession session = requireOwnedSession(sessionId, usuario.getId());
        if (!isActive(session)) {
            throw validation("O foco decisional já expirou ou foi encerrado.");
        }
        decisionClientBindingGuardService.assertBoundActiveClient(session, session.getProcesso().getId());
        session.setStatus("ARMED");
        session.setArmedAt(Instant.now());
        session.setLastCheckedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(12, ChronoUnit.MINUTES));
        decisionFocusSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public DecisionFocusResponse currentFocus() {
        Usuario usuario = requireMagistrado();
        expireSessions(usuario.getId(), Instant.now());
        return decisionFocusSessionRepository.findActiveByUsuario(usuario.getId()).stream()
                .filter(this::isActive)
                .max(Comparator.comparing(DecisionFocusSession::getOpenedAt))
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public void releaseFocus(Long sessionId) {
        Usuario usuario = requireMagistrado();
        DecisionFocusSession session = requireOwnedSession(sessionId, usuario.getId());
        session.setStatus("RELEASED");
        session.setReleasedAt(Instant.now());
        session.setExpiresAt(Instant.now());
        decisionFocusSessionRepository.save(session);
    }

    @Transactional
    public DecisionFocusResponse heartbeat(Long sessionId, String windowBinding, String tabBinding, String routeBinding) {
        Usuario usuario = requireMagistrado();
        DecisionFocusSession session = requireOwnedSession(sessionId, usuario.getId());
        if (!isActive(session)) {
            throw validation("O foco decisional já expirou ou foi encerrado.");
        }
        String safeWindowBinding = requireBinding(windowBinding, "janela");
        String safeTabBinding = requireBinding(tabBinding, "aba");
        refreshBinding(session, safeWindowBinding, safeTabBinding, trimToNull(routeBinding));
        session.setLastHeartbeatAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(12, ChronoUnit.MINUTES));
        decisionFocusSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public DecisionPreflightResponse preflight(Long processoId, String actType, String primaryText, String reasoningText) {
        Usuario usuario = requireMagistrado();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Analysis analysis = analyze(processo, usuario, normalizeText(primaryText, reasoningText));
        return new DecisionPreflightResponse(
                analysis.result,
                analysis.semanticScore,
                analysis.competingScore,
                analysis.competingProcessoId,
                buildFingerprint(processo),
                List.copyOf(analysis.flags),
                buildSummary(processo)
        );
    }

    @Transactional
    public SafetyOutcome requireSafeDecisionContext(Processo processo,
                                                    Usuario usuario,
                                                    String actType,
                                                    String primaryText,
                                                    String reasoningText) {
        requireMagistrado(usuario);
        julgamentoCoverageIntelligenceService.assertDecisionCoverage(processo, usuario, actType);
        AtoProcessualDescriptor descriptor = atoProcessualSecurityPolicyService.descriptorForActType(actType);
        String canonicalActType = atoProcessualSecurityPolicyService.canonicalActType(actType);
        String normalizedDecisionText = normalizeText(primaryText, reasoningText);
        if (descriptor.securityProfile().requiresHumanReason() && trimToNull(reasoningText) == null && trimToNull(primaryText) == null) {
            throw validation("O ato sensível exige conteúdo e fundamentação mínimos para blindagem decisória.");
        }
        expireSessions(usuario.getId(), Instant.now());
        DecisionFocusSession session = decisionFocusSessionRepository.findArmedByUsuarioAndProcesso(usuario.getId(), processo.getId(), Instant.now()).stream()
                .findFirst()
                .orElseThrow(() -> validation("Para praticar ato decisório é obrigatório abrir e armar o foco decisional do processo correto."));
        if (descriptor.securityProfile().requiresBindingCheck()) {
            decisionClientBindingGuardService.assertBoundActiveClient(session, processo.getId());
        }
        decisionBiometricStepUpService.requireAndConsume(usuario, processo, session, canonicalActType, normalizedDecisionText);
        Analysis analysis = analyze(processo, usuario, normalizedDecisionText);
        if (descriptor.securityProfile().requiresQuantumSignature()) {
            analysis.flags.add("Ato com exigência reforçada de assinatura quântica ou evidência criptográfica equivalente.");
        }
        if (descriptor.securityProfile().requiresCrossCheck()) {
            analysis.flags.add("Ato sujeito a conferência cruzada obrigatória antes da liberação final.");
        }
        session.setLastCheckedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(8, ChronoUnit.MINUTES));
        decisionFocusSessionRepository.save(session);
        persistAudit(processo, usuario, session, canonicalActType, analysis, normalizedDecisionText);
        if (BLOCK_RESULTS.contains(analysis.result)) {
            throw validation("O sistema bloqueou o ato porque encontrou risco concreto de troca de processo.")
                    .addMetadado("processo_id", processo.getId())
                    .addMetadado("fingerprint", buildFingerprint(processo))
                    .addMetadado("flags", List.copyOf(analysis.flags))
                    .addMetadado("competing_processo_id", analysis.competingProcessoId)
                    .addMetadado("semantic_score", analysis.semanticScore)
                    .addMetadado("competing_score", analysis.competingScore)
                    .addMetadado("act_type", canonicalActType)
                    .addMetadado("security_action", descriptor.securityProfile().securityAction());
        }
        return new SafetyOutcome(buildFingerprint(processo), List.copyOf(analysis.flags), analysis.semanticScore);
    }

    @Transactional
    public WorkItem registrarConferenciaCruzada(Processo processo,
                                                Usuario usuario,
                                                String actType,
                                                String textoPrincipal) {
        AtoProcessualDescriptor descriptor = atoProcessualSecurityPolicyService.descriptorForActType(actType);
        String canonicalActType = atoProcessualSecurityPolicyService.canonicalActType(actType);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.secretarySaneamento(processo.getId(), "CONFERENCIA_DECISORIA");
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("CONFERE_DECISAO:" + canonicalActType + ':' + processo.getId() + ':' + usuario.getId())
                .type(WorkItemType.CERTIDAO)
                .titulo("Conferência cruzada de " + descriptor.titulo() + " — " + resolveNumero(processo))
                .descricao("Confirmar número, partes, binding e fingerprint antes da liberação do ato " + canonicalActType + ". Hash: " + Hashes.sha256Hex(textoPrincipal == null ? "" : textoPrincipal))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("Fingerprint=" + buildFingerprint(processo) + " | Autor=" + safe(processo.getParteAutoraNome()) + " | Réu=" + safe(processo.getParteReuNome()) + " | Fundamento=" + safe(descriptor.fundamentoPadrao()))
                .dueAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        return workItemRepository.save(workItem);
    }


    @Transactional
    public Optional<WorkItem> registrarConferenciaCruzadaSeNecessario(Processo processo,
                                                                      Usuario usuario,
                                                                      String actType,
                                                                      String textoPrincipal) {
        if (!atoProcessualSecurityPolicyService.requiresCrossCheck(actType)) {
            return Optional.empty();
        }
        return Optional.of(registrarConferenciaCruzada(processo, usuario, actType, textoPrincipal));
    }

    private void persistAudit(Processo processo,
                              Usuario usuario,
                              DecisionFocusSession session,
                              String actType,
                              Analysis analysis,
                              String normalizedText) {
        DecisionConfusionAudit audit = new DecisionConfusionAudit();
        audit.setProcesso(processo);
        audit.setUsuario(usuario);
        audit.setFocusSession(session);
        audit.setActType(trimToNull(actType) == null ? "DECISAO" : actType.trim().toUpperCase(Locale.ROOT));
        audit.setTargetProcessFingerprint(buildFingerprint(processo));
        audit.setRequestTextHash(Hashes.sha256Hex(normalizedText));
        audit.setResultStatus(analysis.result);
        audit.setSemanticScore(analysis.semanticScore);
        audit.setCompetingScore(analysis.competingScore);
        audit.setCompetingProcessoId(analysis.competingProcessoId);
        audit.setReasonsJson(analysis.flags.stream().collect(Collectors.joining(" | ")));
        decisionConfusionAuditRepository.save(audit);
    }

    private Analysis analyze(Processo processo, Usuario usuario, String normalizedText) {
        Analysis analysis = new Analysis();
        analysis.semanticScore = scoreAgainstProcess(processo, normalizedText);
        if (analysis.semanticScore < 2) {
            analysis.flags.add("Texto decisório com baixa aderência às partes ou ao contexto do processo.");
        }
        List<DecisionFocusSession> recent = decisionFocusSessionRepository.findTop20RecentByUsuario(usuario.getId());
        for (DecisionFocusSession candidate : recent) {
            if (!Objects.equals(candidate.getProcesso().getId(), processo.getId())) {
                int score = scoreAgainstSession(candidate, normalizedText);
                if (score > analysis.competingScore) {
                    analysis.competingScore = score;
                    analysis.competingProcessoId = candidate.getProcesso().getId();
                }
            }
        }
        if (analysis.competingScore >= analysis.semanticScore + 2 && analysis.competingScore >= 3) {
            analysis.flags.add("O texto se parece mais com outro processo recentemente focado pelo magistrado.");
            analysis.result = "BLOCKED";
            return analysis;
        }
        if (analysis.semanticScore < 2 && analysis.competingScore >= 2) {
            analysis.flags.add("Há risco de confusão contextual entre processos em janelas recentes.");
            analysis.result = "MISMATCH";
            return analysis;
        }
        if (analysis.semanticScore < 2) {
            analysis.result = "WARN";
            return analysis;
        }
        analysis.result = "AUTHORIZED";
        return analysis;
    }

    private int scoreAgainstProcess(Processo processo, String normalizedText) {
        return scoreTokens(normalizedText, tokensForProcess(processo));
    }

    private int scoreAgainstSession(DecisionFocusSession session, String normalizedText) {
        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, session.getNumeroSnapshot());
        addTokens(tokens, session.getClasseSnapshot());
        addTokens(tokens, session.getAutorSnapshot());
        addTokens(tokens, session.getReuSnapshot());
        addTokens(tokens, session.getAssuntoSnapshot());
        return scoreTokens(normalizedText, tokens);
    }

    private int scoreTokens(String normalizedText, Set<String> tokens) {
        int score = 0;
        for (String token : tokens) {
            if (token.length() >= 4 && normalizedText.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokensForProcess(Processo processo) {
        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, resolveNumero(processo));
        addTokens(tokens, processo.getClasseProcessual());
        addTokens(tokens, processo.getAssunto());
        addTokens(tokens, processo.getParteAutoraNome());
        addTokens(tokens, processo.getParteReuNome());
        return tokens;
    }

    private void addTokens(Set<String> tokens, String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return;
        }
        if (normalized.length() >= 4) {
            tokens.add(normalized);
        }
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 4) {
                tokens.add(token);
            }
        }
    }

    private DecisionFocusResponse toResponse(DecisionFocusSession session) {
        return new DecisionFocusResponse(
                session.getId(),
                session.getProcesso().getId(),
                session.getNumeroSnapshot(),
                session.getProcessFingerprint(),
                session.getStatus(),
                session.getSummarySnapshot(),
                session.getAutorSnapshot(),
                session.getReuSnapshot(),
                session.getClasseSnapshot(),
                session.getBindingFingerprint(),
                session.getOpenedAt(),
                session.getArmedAt(),
                session.getLastHeartbeatAt(),
                session.getExpiresAt()
        );
    }

    private void refreshBinding(DecisionFocusSession session, String windowBinding, String tabBinding, String routeBinding) {
        session.setWindowBinding(windowBinding);
        session.setTabBinding(tabBinding);
        session.setRouteBinding(routeBinding);
        session.setBindingFingerprint(decisionClientBindingGuardService.computeBindingFingerprint(windowBinding, tabBinding, routeBinding));
    }

    private DecisionFocusSession requireOwnedSession(Long sessionId, Long usuarioId) {
        DecisionFocusSession session = decisionFocusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("DecisionFocusSession", sessionId));
        if (!Objects.equals(session.getUsuario().getId(), usuarioId)) {
            throw validation("O foco decisional não pertence ao usuário autenticado.");
        }
        return session;
    }

    private void expireSessions(Long usuarioId, Instant now) {
        List<DecisionFocusSession> sessions = decisionFocusSessionRepository.findActiveByUsuario(usuarioId);
        boolean changed = false;
        for (DecisionFocusSession session : sessions) {
            if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now)) {
                session.setStatus("EXPIRED");
                session.setReleasedAt(now);
                changed = true;
            }
        }
        if (changed) {
            decisionFocusSessionRepository.saveAll(sessions);
        }
    }

    private String buildFingerprint(Processo processo) {
        return Hashes.sha256Hex(String.join("|",
                String.valueOf(processo.getId()),
                safe(resolveNumero(processo)),
                safe(processo.getClasseProcessual()),
                safe(processo.getAssunto()),
                safe(processo.getParteAutoraNome()),
                safe(processo.getParteReuNome())));
    }

    private String buildSummary(Processo processo) {
        return "Confirme o número " + safe(resolveNumero(processo))
                + ", autor " + safe(processo.getParteAutoraNome())
                + ", réu " + safe(processo.getParteReuNome())
                + " e classe " + safe(processo.getClasseProcessual()) + '.';
    }

    private String resolveNumero(Processo processo) {
        return trimToNull(processo.getNumeroUnificado()) != null ? processo.getNumeroUnificado() : safe(processo.getNumeroProcesso());
    }

    private TipoUsuario resolveAssignedRole(Usuario usuario) {
        return usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()
                ? TipoUsuario.SERVIDOR_FORUM
                : usuario.getTipoUsuario();
    }

    private boolean isActive(DecisionFocusSession session) {
        return session != null
                && ("OPEN".equalsIgnoreCase(session.getStatus()) || "ARMED".equalsIgnoreCase(session.getStatus()))
                && session.getExpiresAt() != null
                && session.getExpiresAt().isAfter(Instant.now());
    }

    private Usuario requireMagistrado() {
        return requireMagistrado(currentUserService.getRequired());
    }

    private Usuario requireMagistrado(Usuario usuario) {
        if (usuario == null || !usuario.isMagistrado()) {
            throw validation("O foco decisional é exclusivo para perfis da magistratura.");
        }
        return usuario;
    }

    private String normalizeText(String primaryText, String reasoningText) {
        return normalize((primaryText == null ? "" : primaryText) + " " + (reasoningText == null ? "" : reasoningText));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String safe(String value) {
        return trimToNull(value) == null ? "-" : value.trim();
    }

    private String requireBinding(String value, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw validation("É obrigatório informar o binding ativo de " + label + " para foco decisional seguro.");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ErroDeValidacaoException validation(String detail) {
        return new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, detail);
    }

    private static final class Analysis {
        private String result = "AUTHORIZED";
        private int semanticScore;
        private int competingScore;
        private Long competingProcessoId;
        private final List<String> flags = new ArrayList<>();
    }

    public record SafetyOutcome(String fingerprint, List<String> flags, int semanticScore) {
    }
}
