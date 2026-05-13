package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationDecisionResultView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationFinalApprovalRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationInviteRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeAffiliationInvite;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePreference;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficeDelegacaoRegra;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeAffiliationInviteStatus;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeWorkspaceMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeAffiliationInviteRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeAffiliationInviteService {

    private static final Set<RamoDireito> SENSITIVE_RAMOS = EnumSet.of(
            RamoDireito.PENAL,
            RamoDireito.PROCESSUAL_PENAL,
            RamoDireito.EXECUCAO_PENAL,
            RamoDireito.MILITAR,
            RamoDireito.INFANCIA_JUVENTUDE
    );

    private final CurrentUserService currentUserService;
    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final AdvOfficeWorkspaceProfileRepository profileRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final EquipeOfficeDelegacaoRegraRepository regraRepository;
    private final AdvOfficeWorkspacePreferenceRepository preferenceRepository;
    private final AdvOfficeAffiliationInviteRepository inviteRepository;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final AuditLedgerService auditLedgerService;
    private final RequestIdempotencyService requestIdempotencyService;
    private final GovBrAssuranceExtractor govBrAssuranceExtractor;
    private final GovBrAssurancePolicy govBrAssurancePolicy;

    public OfficeAffiliationInviteService(CurrentUserService currentUserService,
                                          EquipeRepository equipeRepository,
                                          UsuarioRepository usuarioRepository,
                                          MembroEquipeRepository membroEquipeRepository,
                                          AdvOfficeWorkspaceProfileRepository profileRepository,
                                          EquipeOfficePolicyRepository policyRepository,
                                          EquipeOfficeDelegacaoRegraRepository regraRepository,
                                          AdvOfficeWorkspacePreferenceRepository preferenceRepository,
                                          AdvOfficeAffiliationInviteRepository inviteRepository,
                                          OfficeWorkspaceModeService officeWorkspaceModeService,
                                          AuditLedgerService auditLedgerService,
                                          RequestIdempotencyService requestIdempotencyService,
                                          GovBrAssuranceExtractor govBrAssuranceExtractor,
                                          GovBrAssurancePolicy govBrAssurancePolicy) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.equipeRepository = Objects.requireNonNull(equipeRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.regraRepository = Objects.requireNonNull(regraRepository);
        this.preferenceRepository = Objects.requireNonNull(preferenceRepository);
        this.inviteRepository = Objects.requireNonNull(inviteRepository);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.requestIdempotencyService = Objects.requireNonNull(requestIdempotencyService);
        this.govBrAssuranceExtractor = Objects.requireNonNull(govBrAssuranceExtractor);
        this.govBrAssurancePolicy = Objects.requireNonNull(govBrAssurancePolicy);
    }

    @Transactional
    public PjbFrontendOfficeAffiliationInviteView createInvite(FrontendOfficeAffiliationInviteRequest request) {
        Usuario owner = currentUserService.getRequired();
        Equipe equipe = equipeRepository.findById(request.equipeId()).orElseThrow(() -> new IllegalArgumentException("Escritorio nao encontrado."));
        assertManageable(equipe.getId(), owner.getId());
        if (isBlank(request.invitedEmail()) && isBlank(request.invitedCpf()) && isBlank(request.invitedOab())) {
            throw new IllegalArgumentException("Informe email, CPF ou OAB do advogado a ser afiliado.");
        }
        Usuario target = resolveTarget(request.invitedEmail(), request.invitedCpf(), request.invitedOab());
        if (target != null && !target.isAdvogado()) {
            throw new IllegalArgumentException("Usuario alvo nao possui perfil de advocacia/OAB.");
        }
        if (target != null && membroEquipeRepository.existsByUsuario_IdAndEquipe_IdAndAtivoTrue(target.getId(), equipe.getId())) {
            throw new IllegalStateException("Advogado ja esta afiliado a este escritorio.");
        }

        boolean allowAllRamos = Boolean.TRUE.equals(request.allowAllRamos()) || request.allowedRamos() == null || request.allowedRamos().isEmpty();
        Set<RamoDireito> allowedRamos = allowAllRamos ? EnumSet.noneOf(RamoDireito.class) : EnumSet.copyOf(request.allowedRamos());
        boolean sensitiveInvite = requiresReinforcedFlow(request.papelEquipe(), allowAllRamos, allowedRamos, request.minTrustForAuto(), equipe.getId());
        if (sensitiveInvite && identityFactorCount(request.invitedEmail(), request.invitedCpf(), request.invitedOab(), target) < 2) {
            throw new IllegalArgumentException("Convites reforcados exigem ao menos duas chaves de identificacao entre email, CPF e OAB.");
        }

        String normalizedEmail = normalizeEmail(firstNonBlank(request.invitedEmail(), target == null ? null : target.getEmail()));
        String normalizedCpf = normalizeDigits(firstNonBlank(request.invitedCpf(), target == null ? null : target.getCpf()));
        String normalizedOab = normalizeOab(firstNonBlank(request.invitedOab(), target == null ? null : target.getOab()));
        List<AdvOfficeAffiliationInvite> conflicts = inviteRepository.findOpenIdentityConflicts(
                equipe.getId(),
                List.of(OfficeAffiliationInviteStatus.PENDING, OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL),
                target == null ? null : target.getId(),
                normalizedEmail,
                normalizedCpf,
                normalizedOab);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Ja existe convite aberto ou aguardando confirmacao final para esta identidade.");
        }

        OfficeWorkspaceMode modeOnAccept = OfficeWorkspaceMode.fromString(request.modeOnAccept());
        AdvOfficeAffiliationInvite invite = new AdvOfficeAffiliationInvite();
        invite.setEquipe(equipe);
        invite.setCreatedByUserId(owner.getId());
        invite.setTargetUserId(target == null ? null : target.getId());
        invite.setInvitedNome(firstNonBlank(request.invitedNome(), target == null ? null : target.getNome()));
        invite.setInvitedEmail(normalizedEmail);
        invite.setInvitedCpf(normalizedCpf);
        invite.setInvitedOab(normalizedOab);
        invite.setPapelEquipe(request.papelEquipe());
        invite.setCargo(blankToNull(request.cargo()));
        invite.setAllowAllRamos(allowAllRamos);
        invite.setAllowedRamosOverride(allowedRamos);
        invite.setMinTrustForAuto(request.minTrustForAuto());
        invite.setMaxAutoPorDia(request.maxAutoPorDia());
        invite.setBlockPersonalCases(Boolean.TRUE.equals(request.blockPersonalCases()));
        invite.setAutoActivateOnAccept(Boolean.TRUE.equals(request.autoActivateOnAccept()));
        invite.setModeOnAccept(modeOnAccept == null ? OfficeWorkspaceMode.HYBRID : modeOnAccept);
        invite.setWorkspacePriority(normalizePriority(request.workspacePriority()));
        invite.setRequiresFinalApproval(sensitiveInvite);
        invite.setRequiredAssuranceLevel(sensitiveInvite ? "ouro" : "prata");
        invite.setStatus(OfficeAffiliationInviteStatus.PENDING);
        invite.setCreatedAt(Instant.now());
        invite.setExpiresAt(invite.getCreatedAt().plus(sensitiveInvite ? 7 : 30, ChronoUnit.DAYS));
        invite.setFinalApprovalDeadlineAt(sensitiveInvite ? invite.getCreatedAt().plus(72, ChronoUnit.HOURS) : null);
        invite.setRecipientIdentityHash(Hashes.sha256Hex(identityMaterial(invite)));
        invite.setInvitePayloadHash(Hashes.sha256Hex(canonicalPayload(invite)));
        inviteRepository.save(invite);
        auditLedgerService.appendSafely("ADV_OFFICE_AFFILIATION_INVITE_CREATED", "EQUIPE", String.valueOf(equipe.getId()), "invite=" + invite.getId() + " target=" + identityLabel(invite) + " reinforced=" + invite.isRequiresFinalApproval());
        return view(invite, owner, true, false);
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeAffiliationInviteView> myIncomingInvites() {
        Usuario usuario = currentUserService.getRequired();
        return inviteRepository.findForIdentity(
                        List.of(OfficeAffiliationInviteStatus.PENDING, OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL),
                        usuario.getId(),
                        normalizeEmail(usuario.getEmail()),
                        normalizeDigits(usuario.getCpf()),
                        normalizeOab(usuario.getOab()))
                .stream()
                .map(invite -> view(invite, usuario, false, isActionable(invite, usuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOfficeAffiliationInviteView> officeInvites(Long equipeId) {
        Usuario usuario = currentUserService.getRequired();
        assertManageable(equipeId, usuario.getId());
        return inviteRepository.findByEquipeIdOrderByCreatedAtDesc(equipeId).stream()
                .map(invite -> view(invite, usuario, true, isActionable(invite, usuario)))
                .toList();
    }

    @Transactional
    public PjbFrontendOfficeAffiliationInviteView revokeInvite(Long inviteId) {
        Usuario usuario = currentUserService.getRequired();
        AdvOfficeAffiliationInvite invite = inviteRepository.fetchById(inviteId).orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado."));
        assertManageable(invite.getEquipe().getId(), usuario.getId());
        if (!invite.getStatus().isTerminal()) {
            invite.setStatus(OfficeAffiliationInviteStatus.REVOKED);
            invite.setRespondedAt(Instant.now());
            invite.setResponseByUserId(usuario.getId());
            inviteRepository.save(invite);
            auditLedgerService.appendSafely("ADV_OFFICE_AFFILIATION_INVITE_REVOKED", "EQUIPE", String.valueOf(invite.getEquipe().getId()), "invite=" + invite.getId());
        }
        return view(invite, usuario, true, false);
    }

    @Transactional
    public PjbFrontendOfficeAffiliationDecisionResultView acceptInvite(Long inviteId, FrontendOfficeAffiliationDecisionRequest request) {
        Usuario usuario = currentUserService.getRequired();
        if (request == null || !Boolean.TRUE.equals(request.acceptTerms())) {
            throw new IllegalArgumentException("Aceite expresso dos termos do vinculo e obrigatorio.");
        }
        AdvOfficeAffiliationInvite invite = actionableInvite(inviteId, usuario);
        assertAssurance(invite.getRequiredAssuranceLevel(), invite.isRequiresFinalApproval());

        String requestHash = Hashes.sha256Hex("AFFILIATION_ACCEPT|" + inviteId + "|" + usuario.getId() + "|" + defaultIdempotencyKey(request == null ? null : request.idempotencyKey(), inviteId, usuario.getId()));
        RequestIdempotencyBeginResult begin = beginIdempotent("ADV_OFFICE_AFFILIATION_ACCEPT", requestHash);
        if (!begin.created()) {
            AdvOfficeAffiliationInvite reloaded = inviteRepository.fetchById(inviteId).orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado."));
            return decisionResultForExistingState(reloaded, usuario);
        }

        try {
            OfficeWorkspaceMode mode = OfficeWorkspaceMode.fromString(request.mode());
            if (mode == null) {
                mode = invite.getModeOnAccept() == null ? OfficeWorkspaceMode.HYBRID : invite.getModeOnAccept();
            }
            boolean autoActivate = request.autoActivateOnLogin() != null ? request.autoActivateOnLogin() : invite.isAutoActivateOnAccept();
            boolean allowPersonalOwnCases = request.allowPersonalOwnCases() != null ? request.allowPersonalOwnCases() : !invite.isBlockPersonalCases();

            invite.setAcceptedMode(mode);
            invite.setAcceptedAutoActivateOnLogin(autoActivate);
            invite.setAcceptedAllowPersonalOwnCases(allowPersonalOwnCases);
            invite.setAcceptedAt(Instant.now());
            invite.setAcceptedByUserId(usuario.getId());
            invite.setTargetUserId(usuario.getId());

            PjbFrontendOfficeAffiliationDecisionResultView result;
            if (invite.isRequiresFinalApproval()) {
                invite.setStatus(OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL);
                inviteRepository.save(invite);
                auditLedgerService.appendSafely("ADV_OFFICE_AFFILIATION_INVITE_ACCEPTED_PENDING_FINAL_APPROVAL", "EQUIPE", String.valueOf(invite.getEquipe().getId()), "invite=" + invite.getId() + " user=" + usuario.getId());
                result = new PjbFrontendOfficeAffiliationDecisionResultView(
                        view(invite, usuario, false, false),
                        officeWorkspaceModeService.current(null),
                        false,
                        true);
            } else {
                PjbFrontendOfficeModeView officeMode = activateInvite(invite, usuario, invite.getAcceptedMode(), invite.getAcceptedAutoActivateOnLogin(), invite.getAcceptedAllowPersonalOwnCases(), usuario.getId(), null, true);
                result = new PjbFrontendOfficeAffiliationDecisionResultView(
                        view(invite, usuario, false, false),
                        officeMode,
                        true,
                        false);
            }
            requestIdempotencyService.complete(requestHash, "ADV_OFFICE_AFFILIATION_INVITE", String.valueOf(invite.getId()), Hashes.sha256Hex(result.toString()), result.toString());
            return result;
        } catch (RuntimeException ex) {
            requestIdempotencyService.fail(requestHash);
            throw ex;
        }
    }

    @Transactional
    public PjbFrontendOfficeAffiliationDecisionResultView confirmInviteActivation(Long inviteId, FrontendOfficeAffiliationFinalApprovalRequest request) {
        Usuario usuario = currentUserService.getRequired();
        if (request == null || !Boolean.TRUE.equals(request.confirmActivation())) {
            throw new IllegalArgumentException("Confirmacao final obrigatoria para ativacao reforcada.");
        }
        AdvOfficeAffiliationInvite invite = inviteRepository.fetchById(inviteId).orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado."));
        assertManageable(invite.getEquipe().getId(), usuario.getId());
        if (invite.getStatus() != OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL) {
            throw new IllegalStateException("Convite nao esta aguardando confirmacao final.");
        }
        if (invite.getTargetUserId() == null) {
            throw new IllegalStateException("Convite ainda nao possui destinatario autenticado para ativacao final.");
        }
        assertAssurance(invite.getRequiredAssuranceLevel(), true);

        String requestHash = Hashes.sha256Hex("AFFILIATION_FINAL_APPROVAL|" + inviteId + "|" + usuario.getId() + "|" + defaultIdempotencyKey(request.idempotencyKey(), inviteId, usuario.getId()));
        RequestIdempotencyBeginResult begin = beginIdempotent("ADV_OFFICE_AFFILIATION_FINAL_APPROVAL", requestHash);
        if (!begin.created()) {
            AdvOfficeAffiliationInvite reloaded = inviteRepository.fetchById(inviteId).orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado."));
            return new PjbFrontendOfficeAffiliationDecisionResultView(view(reloaded, usuario, true, false), null, reloaded.getStatus() == OfficeAffiliationInviteStatus.ACCEPTED, false);
        }

        try {
            Usuario target = usuarioRepository.findById(invite.getTargetUserId()).orElseThrow(() -> new IllegalStateException("Usuario alvo nao encontrado para ativacao final."));
            PjbFrontendOfficeModeView officeMode = activateInvite(
                    invite,
                    target,
                    invite.getAcceptedMode() == null ? invite.getModeOnAccept() : invite.getAcceptedMode(),
                    Boolean.TRUE.equals(invite.getAcceptedAutoActivateOnLogin()),
                    Boolean.TRUE.equals(invite.getAcceptedAllowPersonalOwnCases()),
                    usuario.getId(),
                    blankToNull(request.justification()),
                    false);
            PjbFrontendOfficeAffiliationDecisionResultView result = new PjbFrontendOfficeAffiliationDecisionResultView(
                    view(invite, usuario, true, false),
                    officeMode,
                    true,
                    false);
            requestIdempotencyService.complete(requestHash, "ADV_OFFICE_AFFILIATION_INVITE", String.valueOf(invite.getId()), Hashes.sha256Hex(result.toString()), result.toString());
            return result;
        } catch (RuntimeException ex) {
            requestIdempotencyService.fail(requestHash);
            throw ex;
        }
    }

    @Transactional
    public PjbFrontendOfficeAffiliationInviteView rejectInvite(Long inviteId) {
        Usuario usuario = currentUserService.getRequired();
        AdvOfficeAffiliationInvite invite = actionableInvite(inviteId, usuario);
        invite.setStatus(OfficeAffiliationInviteStatus.REJECTED);
        invite.setRespondedAt(Instant.now());
        invite.setResponseByUserId(usuario.getId());
        inviteRepository.save(invite);
        auditLedgerService.appendSafely("ADV_OFFICE_AFFILIATION_INVITE_REJECTED", "EQUIPE", String.valueOf(invite.getEquipe().getId()), "invite=" + invite.getId());
        return view(invite, usuario, false, false);
    }

    private PjbFrontendOfficeModeView activateInvite(AdvOfficeAffiliationInvite invite,
                                                     Usuario usuario,
                                                     OfficeWorkspaceMode mode,
                                                     boolean autoActivate,
                                                     boolean allowPersonalOwnCases,
                                                     Long finalApproverUserId,
                                                     String finalApprovalJustification,
                                                     boolean updateCurrentSessionContext) {
        invite.setStatus(OfficeAffiliationInviteStatus.ACCEPTED);
        invite.setRespondedAt(Instant.now());
        invite.setResponseByUserId(usuario.getId());
        invite.setAcceptedAt(invite.getAcceptedAt() == null ? Instant.now() : invite.getAcceptedAt());
        invite.setAcceptedByUserId(invite.getAcceptedByUserId() == null ? usuario.getId() : invite.getAcceptedByUserId());
        invite.setTargetUserId(usuario.getId());
        if (invite.isRequiresFinalApproval()) {
            invite.setFinalApprovedAt(Instant.now());
            invite.setFinalApprovedByUserId(finalApproverUserId);
            invite.setFinalApprovalJustification(finalApprovalJustification);
        }

        MembroEquipe membro = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(usuario.getId(), invite.getEquipe().getId()).orElseGet(() -> {
            MembroEquipe entity = new MembroEquipe();
            entity.setUsuario(usuario);
            entity.setEquipe(invite.getEquipe());
            return entity;
        });
        membro.setAtivo(true);
        membro.setPapel(invite.getPapelEquipe());
        membro.setCargo(invite.getCargo());
        if (membro.getDataAdmissao() == null) {
            membro.setDataAdmissao(LocalDate.now());
        }
        membroEquipeRepository.save(membro);

        EquipeOfficeDelegacaoRegra regra = regraRepository.findByEquipeAndUser(invite.getEquipe().getId(), usuario.getId()).orElseGet(() -> {
            EquipeOfficeDelegacaoRegra entity = new EquipeOfficeDelegacaoRegra();
            entity.setEquipe(invite.getEquipe());
            entity.setUsuario(usuario);
            return entity;
        });
        regra.setAtivo(true);
        regra.setBloqueiaPessoal(invite.isBlockPersonalCases());
        regra.setMinTrustAutoOverride(invite.getMinTrustForAuto());
        regra.setMaxAutoPorDiaOverride(invite.getMaxAutoPorDia());
        regra.setAllowedRamosOverride(invite.isAllowAllRamos() ? EnumSet.noneOf(RamoDireito.class) : enumSet(invite.getAllowedRamosOverride()));
        regra.setWorkspacePriority(normalizePriority(invite.getWorkspacePriority()));
        regra.setAutoActivateWorkspace(autoActivate);
        regraRepository.save(regra);

        savePreference(usuario.getId(), invite.getEquipe().getId(), mode, autoActivate, allowPersonalOwnCases);
        inviteRepository.save(invite);
        auditLedgerService.appendSafely("ADV_OFFICE_AFFILIATION_INVITE_ACTIVATED", "EQUIPE", String.valueOf(invite.getEquipe().getId()), "invite=" + invite.getId() + " user=" + usuario.getId() + " approver=" + finalApproverUserId);
        if (!updateCurrentSessionContext) {
            return null;
        }
        return officeWorkspaceModeService.update(new FrontendOfficeModeUpdateRequest(invite.getEquipe().getId(), mode.name(), autoActivate, allowPersonalOwnCases));
    }

    private PjbFrontendOfficeAffiliationDecisionResultView decisionResultForExistingState(AdvOfficeAffiliationInvite invite, Usuario usuario) {
        boolean activated = invite.getStatus() == OfficeAffiliationInviteStatus.ACCEPTED;
        boolean awaiting = invite.getStatus() == OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL;
        PjbFrontendOfficeModeView officeMode = activated ? officeWorkspaceModeService.current(null) : null;
        return new PjbFrontendOfficeAffiliationDecisionResultView(view(invite, usuario, false, false), officeMode, activated, awaiting);
    }

    private RequestIdempotencyBeginResult beginIdempotent(String action, String requestHash) {
        try {
            return requestIdempotencyService.begin(action, requestHash, Duration.ofMinutes(5));
        } catch (IdempotencyInProgressException ex) {
            throw new IllegalStateException("Operacao ja esta em processamento para este convite.");
        }
    }

    private void savePreference(Long usuarioId, Long equipeId, OfficeWorkspaceMode mode, boolean autoActivate, boolean allowPersonalOwnCases) {
        AdvOfficeWorkspacePreference preference = preferenceRepository.findByUsuarioId(usuarioId).orElseGet(() -> {
            AdvOfficeWorkspacePreference entity = new AdvOfficeWorkspacePreference();
            entity.setUsuarioId(usuarioId);
            entity.setCreatedAt(Instant.now());
            return entity;
        });
        preference.setPreferredEquipeId(mode == OfficeWorkspaceMode.PERSONAL ? null : equipeId);
        preference.setMode(mode.name());
        preference.setAutoActivateOnLogin(autoActivate);
        preference.setAllowPersonalOwnCases(allowPersonalOwnCases);
        preference.setUpdatedAt(Instant.now());
        preferenceRepository.save(preference);
    }

    private AdvOfficeAffiliationInvite actionableInvite(Long inviteId, Usuario usuario) {
        AdvOfficeAffiliationInvite invite = inviteRepository.fetchById(inviteId).orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado."));
        if (invite.getStatus() != OfficeAffiliationInviteStatus.PENDING) {
            throw new IllegalStateException("Convite nao esta pendente para aceite.");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            invite.setStatus(OfficeAffiliationInviteStatus.EXPIRED);
            invite.setRespondedAt(Instant.now());
            inviteRepository.save(invite);
            throw new IllegalStateException("Convite expirado.");
        }
        if (!matchesIdentity(invite, usuario)) {
            throw new IllegalStateException("Convite nao pertence ao usuario autenticado.");
        }
        return invite;
    }

    private void assertManageable(Long equipeId, Long userId) {
        AdvOfficeWorkspaceProfile profile = profileRepository.findByEquipe_Id(equipeId).orElseThrow(() -> new IllegalArgumentException("Escritorio nao encontrado."));
        if (Objects.equals(profile.getOwnerUserId(), userId)) {
            return;
        }
        EquipeOfficePolicy policy = policyRepository.findByEquipeId(equipeId).orElse(null);
        if (policy != null && Objects.equals(policy.getSignerUserId(), userId)) {
            return;
        }
        MembroEquipe membro = membroEquipeRepository.findByUsuario_IdAndEquipe_Id(userId, equipeId).orElse(null);
        if (membro != null && membro.isAtivo() && (membro.getPapel() == PapelEquipe.ADMINISTRADOR || membro.getPapel() == PapelEquipe.COORDENADOR || membro.getPapel() == PapelEquipe.ADVOGADO_SENIOR)) {
            return;
        }
        throw new IllegalStateException("Usuario nao pode gerenciar afiliacoes deste escritorio.");
    }

    private Usuario resolveTarget(String email, String cpf, String oab) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null) {
            Usuario byEmail = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
            if (byEmail != null) {
                return byEmail;
            }
        }
        String normalizedCpf = normalizeDigits(cpf);
        if (normalizedCpf != null) {
            Usuario byCpf = usuarioRepository.findByCpf(normalizedCpf).orElse(null);
            if (byCpf != null) {
                return byCpf;
            }
        }
        String normalizedOab = normalizeOab(oab);
        if (normalizedOab != null) {
            return usuarioRepository.findAll().stream().filter(user -> normalizedOab.equalsIgnoreCase(normalizeOab(user.getOab()))).findFirst().orElse(null);
        }
        return null;
    }

    private boolean matchesIdentity(AdvOfficeAffiliationInvite invite, Usuario usuario) {
        if (invite.getTargetUserId() != null && Objects.equals(invite.getTargetUserId(), usuario.getId())) {
            return true;
        }
        if (invite.getInvitedEmail() != null && invite.getInvitedEmail().equalsIgnoreCase(normalizeEmail(usuario.getEmail()))) {
            return true;
        }
        if (invite.getInvitedCpf() != null && invite.getInvitedCpf().equals(normalizeDigits(usuario.getCpf()))) {
            return true;
        }
        return invite.getInvitedOab() != null && invite.getInvitedOab().equalsIgnoreCase(normalizeOab(usuario.getOab()));
    }

    private boolean isActionable(AdvOfficeAffiliationInvite invite, Usuario usuario) {
        return invite.getStatus() == OfficeAffiliationInviteStatus.PENDING && matchesIdentity(invite, usuario) && (invite.getExpiresAt() == null || !invite.getExpiresAt().isBefore(Instant.now()));
    }

    private PjbFrontendOfficeAffiliationInviteView view(AdvOfficeAffiliationInvite invite, Usuario usuario, boolean manageable, boolean actionable) {
        EquipeOfficePolicy policy = policyRepository.findByEquipeId(invite.getEquipe().getId()).orElse(null);
        Long patronoUserId = policy == null ? null : policy.getSignerUserId();
        String patronoNome = patronoUserId == null ? null : usuarioRepository.findById(patronoUserId).map(Usuario::getNome).orElse(null);
        return new PjbFrontendOfficeAffiliationInviteView(
                invite.getId(),
                invite.getEquipe().getId(),
                invite.getEquipe().getNome(),
                patronoUserId,
                patronoNome,
                invite.getStatus().name(),
                invite.getInvitedNome(),
                maskEmail(invite.getInvitedEmail()),
                maskCpf(invite.getInvitedCpf()),
                invite.getInvitedOab(),
                invite.getPapelEquipe().name(),
                invite.getCargo(),
                invite.isAllowAllRamos() ? List.of() : enumNames(invite.getAllowedRamosOverride()),
                invite.isAllowAllRamos(),
                invite.getMinTrustForAuto(),
                invite.getMaxAutoPorDia(),
                invite.isBlockPersonalCases(),
                invite.isAutoActivateOnAccept(),
                invite.getModeOnAccept().name(),
                invite.getWorkspacePriority(),
                invite.isRequiresFinalApproval(),
                invite.getRequiredAssuranceLevel(),
                invite.getStatus() == OfficeAffiliationInviteStatus.AWAITING_FINAL_APPROVAL,
                manageable,
                actionable,
                invite.getCreatedAt(),
                invite.getAcceptedAt(),
                invite.getExpiresAt(),
                invite.getFinalApprovalDeadlineAt());
    }

    private boolean requiresReinforcedFlow(PapelEquipe papelEquipe,
                                           boolean allowAllRamos,
                                           Set<RamoDireito> allowedRamos,
                                           Integer minTrustForAuto,
                                           Long equipeId) {
        EquipeOfficePolicy policy = policyRepository.findByEquipeId(equipeId).orElse(null);
        boolean roleSensitive = papelEquipe == PapelEquipe.ADMINISTRADOR || papelEquipe == PapelEquipe.COORDENADOR || papelEquipe == PapelEquipe.ADVOGADO_SENIOR;
        boolean ramoSensitive = allowAllRamos || allowedRamos.stream().anyMatch(SENSITIVE_RAMOS::contains);
        boolean trustSensitive = minTrustForAuto != null && minTrustForAuto >= 8;
        boolean signerSensitive = policy != null && policy.isForcePatronoCertificate();
        return roleSensitive || ramoSensitive || trustSensitive || signerSensitive;
    }

    private int identityFactorCount(String invitedEmail, String invitedCpf, String invitedOab, Usuario target) {
        int count = 0;
        if (!isBlank(invitedEmail) || (target != null && !isBlank(target.getEmail()))) {
            count++;
        }
        if (!isBlank(invitedCpf) || (target != null && !isBlank(target.getCpf()))) {
            count++;
        }
        if (!isBlank(invitedOab) || (target != null && !isBlank(target.getOab()))) {
            count++;
        }
        return count;
    }

    private void assertAssurance(String requiredAssuranceLevel, boolean sensitive) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String atual = govBrAssuranceExtractor.extract(authentication);
        String required = isBlank(requiredAssuranceLevel) ? (sensitive ? "ouro" : "prata") : requiredAssuranceLevel.trim().toLowerCase(Locale.ROOT);
        if (!assuranceSatisfied(atual, required)) {
            throw new IllegalStateException("Nivel Gov.br insuficiente para este fluxo. Necessario nivel " + required + ".");
        }
        govBrAssurancePolicy.exigeStepUp(atual, sensitive);
    }

    private boolean assuranceSatisfied(String current, String required) {
        return assuranceRank(current) >= assuranceRank(required);
    }

    private int assuranceRank(String level) {
        if (level == null) {
            return 0;
        }
        String normalized = level.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ouro", "gold", "loa3", "3" -> 3;
            case "prata", "silver", "loa2", "2" -> 2;
            case "bronze", "loa1", "1" -> 1;
            default -> 0;
        };
    }

    private List<String> enumNames(Set<RamoDireito> ramos) {
        if (ramos == null || ramos.isEmpty()) {
            return List.of();
        }
        return ramos.stream().map(Enum::name).sorted().toList();
    }

    private Set<RamoDireito> enumSet(Set<RamoDireito> ramos) {
        if (ramos == null || ramos.isEmpty()) {
            return EnumSet.noneOf(RamoDireito.class);
        }
        return EnumSet.copyOf(ramos);
    }

    private String identityLabel(AdvOfficeAffiliationInvite invite) {
        return firstNonBlank(invite.getInvitedEmail(), invite.getInvitedCpf(), invite.getInvitedOab(), invite.getInvitedNome());
    }

    private String canonicalPayload(AdvOfficeAffiliationInvite invite) {
        return String.join("|",
                String.valueOf(invite.getEquipe().getId()),
                String.valueOf(invite.getCreatedByUserId()),
                safe(invite.getInvitedEmail()),
                safe(invite.getInvitedCpf()),
                safe(invite.getInvitedOab()),
                safe(invite.getPapelEquipe() == null ? null : invite.getPapelEquipe().name()),
                safe(invite.getCargo()),
                String.valueOf(invite.isAllowAllRamos()),
                String.join(",", enumNames(invite.getAllowedRamosOverride())),
                String.valueOf(invite.getMinTrustForAuto()),
                String.valueOf(invite.getMaxAutoPorDia()),
                String.valueOf(invite.isBlockPersonalCases()),
                String.valueOf(invite.isAutoActivateOnAccept()),
                safe(invite.getModeOnAccept() == null ? null : invite.getModeOnAccept().name()),
                String.valueOf(invite.getWorkspacePriority()),
                String.valueOf(invite.isRequiresFinalApproval()),
                safe(invite.getRequiredAssuranceLevel()));
    }

    private String identityMaterial(AdvOfficeAffiliationInvite invite) {
        return String.join("|",
                safe(invite.getInvitedEmail()),
                safe(invite.getInvitedCpf()),
                safe(invite.getInvitedOab()));
    }

    private String defaultIdempotencyKey(String raw, Long inviteId, Long userId) {
        if (!isBlank(raw)) {
            return raw.trim();
        }
        return inviteId + ":" + userId;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private int normalizePriority(Integer priority) {
        if (priority == null) {
            return 100;
        }
        return Math.max(1, Math.min(priority, 999));
    }

    private String normalizeEmail(String email) {
        return isBlank(email) ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDigits(String value) {
        if (isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizeOab(String oab) {
        return isBlank(oab) ? null : oab.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return null;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return "**@" + parts[1];
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
