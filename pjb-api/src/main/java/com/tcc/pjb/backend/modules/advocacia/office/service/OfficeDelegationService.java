package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoUsage;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeDelegatedAction;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoUsageRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeDelegatedActionRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;

@Service
public class OfficeDelegationService {

    public record Decision(OfficeDelegationMode mode,
                           Long equipeId,
                           Long executorUserId,
                           Long signerUserId,
                           int trustScore,
                           Long queueItemId) {
    }

    private final EquipeOfficePolicyRepository policyRepo;
    private final EquipeOfficeDelegacaoRegraRepository regraRepo;
    private final EquipeOfficeDelegacaoUsageRepository usageRepo;
    private final OfficeSignatureQueueRepository queueRepo;
    private final OfficeDelegatedActionRepository actionRepo;
    private final EquipeRepository equipeRepo;
    private final UsuarioRepository usuarioRepo;
    private final OfficeTrustScoreService trustScoreService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final AuditLedgerService auditLedgerService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;

    public OfficeDelegationService(EquipeOfficePolicyRepository policyRepo,
                                   EquipeOfficeDelegacaoRegraRepository regraRepo,
                                   EquipeOfficeDelegacaoUsageRepository usageRepo,
                                   OfficeSignatureQueueRepository queueRepo,
                                   OfficeDelegatedActionRepository actionRepo,
                                   EquipeRepository equipeRepo,
                                   UsuarioRepository usuarioRepo,
                                   OfficeTrustScoreService trustScoreService,
                                   OfficeWorkspaceModeService officeWorkspaceModeService,
                                   ObjectProvider<HttpServletRequest> requestProvider,
                                   AuditLedgerService auditLedgerService,
                                   OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService) {
        this.policyRepo = Objects.requireNonNull(policyRepo);
        this.regraRepo = Objects.requireNonNull(regraRepo);
        this.usageRepo = Objects.requireNonNull(usageRepo);
        this.queueRepo = Objects.requireNonNull(queueRepo);
        this.actionRepo = Objects.requireNonNull(actionRepo);
        this.equipeRepo = Objects.requireNonNull(equipeRepo);
        this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
        this.trustScoreService = Objects.requireNonNull(trustScoreService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.requestProvider = Objects.requireNonNull(requestProvider);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
    }

    @Transactional
    public Decision decideAndRecord(Long equipeId,
                                   Long executorUserId,
                                   OfficeActionType actionType,
                                   String resourceType,
                                   String resourceId,
                                   String payloadHash,
                                   String summary) {
        return decideAndRecord(equipeId, executorUserId, actionType, resourceType, resourceId, payloadHash, summary, null, false);
    }

    @Transactional
    public Decision decideAndRecord(Long equipeId,
                                   Long executorUserId,
                                   OfficeActionType actionType,
                                   String resourceType,
                                   String resourceId,
                                   String payloadHash,
                                   String summary,
                                   String ramoDireito) {
        return decideAndRecord(equipeId, executorUserId, actionType, resourceType, resourceId, payloadHash, summary, ramoDireito, false);
    }

    @Transactional
    public Decision decideAndRecord(Long equipeId,
                                   Long executorUserId,
                                   OfficeActionType actionType,
                                   String resourceType,
                                   String resourceId,
                                   String payloadHash,
                                   String summary,
                                   String ramoDireito,
                                   boolean forceQueue) {

        Objects.requireNonNull(equipeId, "equipeId");
        Objects.requireNonNull(executorUserId, "executorUserId");
        Objects.requireNonNull(actionType, "actionType");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");

        PjbFrontendOfficeProcessAccessView processAccess = null;
        if ("PROCESSO".equalsIgnoreCase(resourceType)) {
            Long processoId = parseLong(resourceId);
            if (processoId == null) {
                throw new IllegalArgumentException("resourceId de processo invalido.");
            }
            HttpServletRequest request = requestProvider.getIfAvailable();
            processAccess = officeProcessWorkspaceScopeService.access(processoId, actionType, request);
            if (!processAccess.allowed()) {
                throw new IllegalStateException("Processo fora do escopo operacional do workspace: " + String.join(", ", processAccess.blockers()));
            }
        }

        Equipe equipe = equipeRepo.findById(equipeId).orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
        Usuario executor = usuarioRepo.findById(executorUserId).orElseThrow(() -> new EntityNotFoundException("Executor não encontrado."));

        EquipeOfficePolicy policy = policyRepo.findByEquipeId(equipeId).orElse(null);

        PjbFrontendOfficeModeView workspaceMode = currentWorkspaceMode(equipeId);
        boolean officeModeActive = workspaceMode != null
                && workspaceMode.activeEquipeId() != null
                && Objects.equals(workspaceMode.activeEquipeId(), equipeId)
                && !"PERSONAL".equalsIgnoreCase(workspaceMode.mode());

        if (workspaceMode != null && "PERSONAL".equalsIgnoreCase(workspaceMode.mode())) {
            OfficeDelegatedAction a = buildAction(equipe, executor, executor, OfficeDelegationMode.SELF, actionType, resourceType, resourceId, payloadHash, null);
            actionRepo.save(a);
            auditLedgerService.appendSafely("ADV_OFFICE_ACTION_SELF_PERSONAL_MODE", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.SELF, equipeId, executorUserId, executorUserId, 0, null);
        }

        if (!officeModeActive || policy == null || !policy.isEnabled() || policy.getSignerUserId() == null) {
            OfficeDelegatedAction a = buildAction(equipe, executor, executor, OfficeDelegationMode.SELF, actionType, resourceType, resourceId, payloadHash, null);
            actionRepo.save(a);
            auditLedgerService.appendSafely("ADV_OFFICE_ACTION_SELF", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.SELF, equipeId, executorUserId, executorUserId, 0, null);
        }

        Long signerUserId = policy.getSignerUserId();
        Usuario signer = usuarioRepo.findById(signerUserId).orElseThrow(() -> new EntityNotFoundException("Signatário não encontrado."));

        if (forceQueue || (processAccess != null && processAccess.queueRequired())) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, 0, q.getId());
        }

        if (signer.getId().equals(executor.getId())) {
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.SELF, actionType, resourceType, resourceId, payloadHash, null);
            actionRepo.save(a);
            auditLedgerService.appendSafely("ADV_OFFICE_ACTION_SELF", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.SELF, equipeId, executorUserId, signerUserId, 10, null);
        }

        if (officeModeActive && ramoDireito != null && workspaceMode.effectiveAllowedRamos() != null && !workspaceMode.effectiveAllowedRamos().isEmpty()
                && !workspaceMode.canViewAllRamos()
                && workspaceMode.effectiveAllowedRamos().stream().noneMatch(value -> value.equalsIgnoreCase(ramoDireito))) {
            throw new IllegalStateException("Atuacao em modo escritorio sem permissao para o ramo " + ramoDireito + ".");
        }

        OfficeTrustScoreService.TrustScore trust = trustScoreService.avaliar(executorUserId, equipeId);

        EquipeOfficeDelegacaoRegra regra = regraRepo.findByEquipeAndUser(equipeId, executorUserId).orElse(null);
        boolean regraAtiva = regra == null || regra.isAtivo();
        if (!regraAtiva) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        int minTrust = regra != null && regra.getMinTrustAutoOverride() != null ? regra.getMinTrustAutoOverride() : policy.getMinTrustAuto();
        int maxAuto = regra != null && regra.getMaxAutoPorDiaOverride() != null ? regra.getMaxAutoPorDiaOverride() : policy.getMaxAutoPorDia();

        EnumSet<OfficeActionType> allowed = EnumSet.noneOf(OfficeActionType.class);
        if (policy.getAutoActions() != null) allowed.addAll(policy.getAutoActions());
        if (regra != null && regra.getAutoActionsOverride() != null && !regra.getAutoActionsOverride().isEmpty()) {
            allowed.clear();
            allowed.addAll(regra.getAutoActionsOverride());
        }

        boolean isAllowed = allowed.contains(actionType);

        if (actionType.isIrreversivel()) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        if (!isAllowed) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        if (trust.frozen()) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        if (trust.score() < minTrust) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        EquipeOfficeDelegacaoUsage usage = usageRepo.findByEquipeUserDia(equipeId, executorUserId, LocalDate.now()).orElse(null);
        int used = usage != null ? usage.getAutoUsado() : 0;
        if (maxAuto > 0 && used >= maxAuto) {
            OfficeSignatureQueueItem q = createQueue(equipe, executor, signer, actionType, resourceType, resourceId, payloadHash, summary);
            OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.QUEUE, actionType, resourceType, resourceId, payloadHash, q);
            actionRepo.save(a);
            bumpUsage(equipe, executor, false);
            auditLedgerService.appendSafely("ADV_OFFICE_QUEUE_CREATED", resourceType, resourceId, payloadHash);
            return new Decision(OfficeDelegationMode.QUEUE, equipeId, executorUserId, signerUserId, trust.score(), q.getId());
        }

        OfficeDelegatedAction a = buildAction(equipe, executor, signer, OfficeDelegationMode.AUTO, actionType, resourceType, resourceId, payloadHash, null);
        actionRepo.save(a);
        bumpUsage(equipe, executor, true);
        auditLedgerService.appendSafely("ADV_OFFICE_ACTION_AUTO", resourceType, resourceId, payloadHash);
        return new Decision(OfficeDelegationMode.AUTO, equipeId, executorUserId, signerUserId, trust.score(), null);
    }


    private PjbFrontendOfficeModeView currentWorkspaceMode(Long equipeId) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return null;
        }
        try {
            PjbFrontendOfficeModeView view = officeWorkspaceModeService.current(request);
            if (view == null || view.activeEquipeId() == null) {
                return view;
            }
            return Objects.equals(view.activeEquipeId(), equipeId) ? view : view;
        } catch (Exception ex) {
            return null;
        }
    }

    private OfficeSignatureQueueItem createQueue(Equipe equipe,
                                                Usuario executor,
                                                Usuario signer,
                                                OfficeActionType actionType,
                                                String resourceType,
                                                String resourceId,
                                                String payloadHash,
                                                String summary) {
        OfficeSignatureQueueItem q = new OfficeSignatureQueueItem();
        q.setEquipe(equipe);
        q.setExecutor(executor);
        q.setSigner(signer);
        q.setActionType(actionType);
        q.setResourceType(resourceType);
        q.setResourceId(resourceId);
        q.setStatus(OfficeQueueStatus.PENDING);
        q.setPayloadHash(payloadHash);
        q.setSummary(summary);
        q.setRequestId(RequestContext.getRequestId().orElse(null));
        return queueRepo.save(q);
    }

    private OfficeDelegatedAction buildAction(Equipe equipe,
                                             Usuario executor,
                                             Usuario signer,
                                             OfficeDelegationMode mode,
                                             OfficeActionType actionType,
                                             String resourceType,
                                             String resourceId,
                                             String payloadHash,
                                             OfficeSignatureQueueItem queueItem) {
        OfficeDelegatedAction a = new OfficeDelegatedAction();
        a.setEquipe(equipe);
        a.setExecutor(executor);
        a.setSigner(signer);
        a.setMode(mode);
        a.setActionType(actionType);
        a.setResourceType(resourceType);
        a.setResourceId(resourceId);
        a.setQueueItem(queueItem);
        a.setPayloadHash(payloadHash);
        a.setRequestId(RequestContext.getRequestId().orElse(null));
        return a;
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void bumpUsage(Equipe equipe, Usuario executor, boolean isAuto) {
        LocalDate dia = LocalDate.now();
        EquipeOfficeDelegacaoUsage u = usageRepo.findByEquipeUserDia(equipe.getId(), executor.getId(), dia).orElseGet(() -> {
            EquipeOfficeDelegacaoUsage nu = new EquipeOfficeDelegacaoUsage();
            nu.setEquipe(equipe);
            nu.setUsuario(executor);
            nu.setDia(dia);
            return nu;
        });
        if (isAuto) {
            u.setAutoUsado(u.getAutoUsado() + 1);
        } else {
            u.setQueueCriado(u.getQueueCriado() + 1);
        }
        u.setUltimoEventoEm(LocalDateTime.now());
        usageRepo.save(u);
    }
}
