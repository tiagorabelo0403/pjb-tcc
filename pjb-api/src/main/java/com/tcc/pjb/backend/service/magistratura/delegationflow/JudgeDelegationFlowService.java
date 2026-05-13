package com.tcc.pjb.backend.service.magistratura.delegationflow;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationScope;
import com.tcc.pjb.backend.core.security.magistratura.delegation.JudgeDelegationService;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowApproveResponse;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowRequest;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationFlowView;
import com.tcc.pjb.backend.model.dto.judge.delegation.JudgeDelegationIssueResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlow;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlowStatus;
import com.tcc.pjb.backend.model.repository.JudgeDelegationFlowRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;

@Service
public class JudgeDelegationFlowService {

    private final JudgeDelegationFlowRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserService currentUserService;
    private final JudgeDelegationService judgeDelegationService;
    private final NotificationService notificationService;
    private final AuditLedgerService auditLedgerService;

    public JudgeDelegationFlowService(JudgeDelegationFlowRepository repository,
                                      UsuarioRepository usuarioRepository,
                                      CurrentUserService currentUserService,
                                      JudgeDelegationService judgeDelegationService,
                                      NotificationService notificationService,
                                      AuditLedgerService auditLedgerService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.currentUserService = currentUserService;
        this.judgeDelegationService = judgeDelegationService;
        this.notificationService = notificationService;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional
    public JudgeDelegationFlowView solicitar(JudgeDelegationFlowRequest request) {
        Objects.requireNonNull(request, "request é obrigatório");
        Objects.requireNonNull(request.magistrateId(), "magistrateId é obrigatório");
        Usuario delegate = currentUserService.getRequired();
        Usuario magistrate = usuarioRepository.findById(request.magistrateId())
                .orElseThrow(() -> new IllegalArgumentException("Magistrado não encontrado"));
        if (!magistrate.isMagistrado()) {
            throw new SecurityException("Destinatário informado não pertence à magistratura.");
        }
        if (!delegate.isAtivo()) {
            throw new SecurityException("Usuário solicitante inativo.");
        }
        JudgeDelegationFlow flow = JudgeDelegationFlow.builder()
                .magistrate(magistrate)
                .delegate(delegate)
                .scope(DelegationScope.parse(request.scope()))
                .status(JudgeDelegationFlowStatus.SOLICITADA)
                .requestedReason(limit(request.motivo(), 600))
                .deviceBindingHash(limit(request.deviceBindingHash(), 128))
                .durationMinutes(resolveDuration(request.duracaoMinutos()))
                .build();
        JudgeDelegationFlow saved = repository.save(flow);
        auditLedgerService.appendSafely("JUDGE_DELEGATION_REQUESTED", "JUDGE_DELEGATION_FLOW", String.valueOf(saved.getId()), saved.getRequestUuid().toString());
        notificationService.notifyUser(magistrate, null,
                "Solicitação de delegação pendente",
                "Há uma nova solicitação de delegação de gabinete aguardando decisão.",
                "/api/v1/judge/delegation/requests/pending");
        return toView(saved);
    }

    @Transactional
    public JudgeDelegationFlowApproveResponse aprovar(Long requestId) {
        JudgeDelegationFlow flow = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        Usuario magistrate = currentUserService.getRequired();
        requireMagistrateOwner(flow, magistrate);
        if (flow.getStatus() != JudgeDelegationFlowStatus.SOLICITADA) {
            throw new IllegalStateException("Solicitação não está pendente para aprovação.");
        }
        JudgeDelegationService.IssueResult issue = judgeDelegationService.emitirDelegacaoParaAssessorDetalhado(
                flow.getDelegate().getId(),
                flow.getDurationMinutes(),
                flow.getScope(),
                flow.getDeviceBindingHash());
        flow.setStatus(JudgeDelegationFlowStatus.APROVADA);
        flow.setApprovedAt(LocalDateTime.now());
        flow.setApprovedBy(magistrate);
        flow.setTokenJti(issue.payload().jti());
        flow.setExpiresAt(LocalDateTime.ofInstant(issue.expiresAt(), ZoneOffset.UTC));
        JudgeDelegationFlow saved = repository.save(flow);
        auditLedgerService.appendSafely("JUDGE_DELEGATION_APPROVED", "JUDGE_DELEGATION_FLOW", String.valueOf(saved.getId()), issue.payload().jti());
        notificationService.notifyUser(saved.getDelegate(), null,
                "Delegação aprovada",
                "Sua solicitação de delegação foi aprovada pela magistratura responsável.",
                "/api/v1/judge/delegation/active");
        return new JudgeDelegationFlowApproveResponse(
                toView(saved),
                new JudgeDelegationIssueResponse(
                        issue.token(),
                        issue.payload().jti(),
                        issue.payload().magistrateId(),
                        issue.payload().delegateId(),
                        issue.payload().scope().toUpperCase(),
                        issue.expiresAt())
        );
    }

    @Transactional
    public JudgeDelegationFlowView rejeitar(Long requestId, String motivo) {
        JudgeDelegationFlow flow = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        Usuario magistrate = currentUserService.getRequired();
        requireMagistrateOwner(flow, magistrate);
        if (flow.getStatus() != JudgeDelegationFlowStatus.SOLICITADA) {
            throw new IllegalStateException("Solicitação não está pendente para rejeição.");
        }
        flow.setStatus(JudgeDelegationFlowStatus.REJEITADA);
        flow.setRejectedAt(LocalDateTime.now());
        flow.setRejectedBy(magistrate);
        if (motivo != null && !motivo.isBlank()) {
            flow.setRequestedReason(limit(flow.getRequestedReason()) + " | REJEICAO: " + limit(motivo, 240));
        }
        JudgeDelegationFlow saved = repository.save(flow);
        auditLedgerService.appendSafely("JUDGE_DELEGATION_REJECTED", "JUDGE_DELEGATION_FLOW", String.valueOf(saved.getId()), saved.getRequestUuid().toString());
        notificationService.notifyUser(saved.getDelegate(), null,
                "Delegação rejeitada",
                "Sua solicitação de delegação foi rejeitada.",
                "/api/v1/judge/delegation/mine");
        return toView(saved);
    }

    @Transactional
    public JudgeDelegationFlowView revogar(Long requestId, String motivo) {
        JudgeDelegationFlow flow = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        Usuario actor = currentUserService.getRequired();
        if (!Objects.equals(actor.getId(), flow.getMagistrate().getId()) && !Objects.equals(actor.getId(), flow.getDelegate().getId())) {
            throw new SecurityException("Apenas magistrado titular ou delegado podem revogar a delegação.");
        }
        if (flow.getStatus() != JudgeDelegationFlowStatus.APROVADA) {
            throw new IllegalStateException("Apenas delegações aprovadas podem ser revogadas.");
        }
        flow.setStatus(JudgeDelegationFlowStatus.REVOGADA);
        flow.setRevokedAt(LocalDateTime.now());
        flow.setRevokedBy(actor);
        if (motivo != null && !motivo.isBlank()) {
            flow.setRequestedReason(limit(flow.getRequestedReason()) + " | REVOGACAO: " + limit(motivo, 240));
        }
        JudgeDelegationFlow saved = repository.save(flow);
        auditLedgerService.appendSafely("JUDGE_DELEGATION_REVOKED", "JUDGE_DELEGATION_FLOW", String.valueOf(saved.getId()), saved.getTokenJti() != null ? saved.getTokenJti() : saved.getRequestUuid().toString());
        Usuario otherParty = Objects.equals(actor.getId(), saved.getMagistrate().getId()) ? saved.getDelegate() : saved.getMagistrate();
        notificationService.notifyUser(otherParty, null,
                "Delegação revogada",
                "Uma delegação ativa de gabinete foi revogada.",
                "/api/v1/judge/delegation/active");
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<JudgeDelegationFlowView> pendentesParaMagistrado() {
        Usuario magistrate = currentUserService.getRequired();
        return repository.findTop50ByMagistrate_IdAndStatusOrderByRequestedAtDesc(magistrate.getId(), JudgeDelegationFlowStatus.SOLICITADA)
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<JudgeDelegationFlowView> minhasSolicitacoes() {
        Usuario user = currentUserService.getRequired();
        return repository.findTop50ByDelegate_IdOrderByRequestedAtDesc(user.getId())
                .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<JudgeDelegationFlowView> ativasDaMagistratura() {
        Usuario user = currentUserService.getRequired();
        List<JudgeDelegationFlow> flows = user.isMagistrado()
                ? repository.findTop50ByMagistrate_IdAndStatusOrderByRequestedAtDesc(user.getId(), JudgeDelegationFlowStatus.APROVADA)
                : repository.findTop50ByDelegate_IdAndStatusOrderByRequestedAtDesc(user.getId(), JudgeDelegationFlowStatus.APROVADA);
        LocalDateTime now = LocalDateTime.now();
        return flows.stream().peek(flow -> expireIfNeeded(flow, now)).map(this::toView).toList();
    }

    @Transactional
    public long expirarFluxosVencidos(Long magistrateId) {
        LocalDateTime now = LocalDateTime.now();
        List<JudgeDelegationFlow> flows = repository.findTop50ByMagistrate_IdAndStatusOrderByRequestedAtDesc(magistrateId, JudgeDelegationFlowStatus.APROVADA);
        long total = 0L;
        for (JudgeDelegationFlow flow : flows) {
            if (expireIfNeeded(flow, now)) {
                total++;
            }
        }
        return total;
    }

    private boolean expireIfNeeded(JudgeDelegationFlow flow, LocalDateTime now) {
        if (flow.getStatus() == JudgeDelegationFlowStatus.APROVADA && flow.getExpiresAt() != null && flow.getExpiresAt().isBefore(now)) {
            flow.setStatus(JudgeDelegationFlowStatus.EXPIRADA);
            repository.save(flow);
            return true;
        }
        return false;
    }

    private void requireMagistrateOwner(JudgeDelegationFlow flow, Usuario magistrate) {
        if (!magistrate.isMagistrado() || !Objects.equals(magistrate.getId(), flow.getMagistrate().getId())) {
            throw new SecurityException("Ação restrita ao magistrado titular da delegação.");
        }
    }

    private int resolveDuration(Integer durationMinutes) {
        if (durationMinutes == null) {
            return 60;
        }
        return Math.max(5, Math.min(durationMinutes, 24 * 60));
    }

    private JudgeDelegationFlowView toView(JudgeDelegationFlow flow) {
        return new JudgeDelegationFlowView(
                flow.getId(),
                flow.getRequestUuid() != null ? flow.getRequestUuid().toString() : null,
                flow.getMagistrate() != null ? flow.getMagistrate().getId() : null,
                flow.getMagistrate() != null ? flow.getMagistrate().getNome() : null,
                flow.getDelegate() != null ? flow.getDelegate().getId() : null,
                flow.getDelegate() != null ? flow.getDelegate().getNome() : null,
                flow.getScope() != null ? flow.getScope().name() : null,
                flow.getStatus() != null ? flow.getStatus().name() : null,
                flow.getDurationMinutes(),
                flow.getRequestedReason(),
                flow.getDeviceBindingHash(),
                toInstant(flow.getRequestedAt()),
                toInstant(flow.getApprovedAt()),
                toInstant(flow.getRejectedAt()),
                toInstant(flow.getRevokedAt()),
                toInstant(flow.getExpiresAt()),
                flow.getTokenJti());
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private String limit(String value) {
        return limit(value, 600);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
