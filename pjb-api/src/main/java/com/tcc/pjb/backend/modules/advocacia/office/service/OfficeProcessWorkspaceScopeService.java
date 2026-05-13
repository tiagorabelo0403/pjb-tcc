package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeProcessWorkspaceScopeService {

    private static final int MIN_SENSITIVE_TRUST = 8;
    private static final Set<RamoDireito> SENSITIVE_RAMOS = Set.of(
            RamoDireito.FAMILIA,
            RamoDireito.INFANCIA_JUVENTUDE,
            RamoDireito.PREVIDENCIARIO,
            RamoDireito.TRIBUTARIO,
            RamoDireito.ELEITORAL);

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedgerService;

    public OfficeProcessWorkspaceScopeService(CurrentUserService currentUserService,
                                              OfficeWorkspaceModeService officeWorkspaceModeService,
                                              ProcessoRepository processoRepository,
                                              AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public WorkspaceProcessFilterProfile currentFilterProfile(HttpServletRequest request) {
        Usuario usuario = currentUserService.getRequired();
        PjbFrontendOfficeModeView officeMode = officeWorkspaceModeService.current(request);
        boolean officeScope = officeMode.activeEquipeId() != null && !"PERSONAL".equalsIgnoreCase(officeMode.mode());
        boolean includePersonal = "PERSONAL".equalsIgnoreCase(officeMode.mode())
                || "HYBRID".equalsIgnoreCase(officeMode.mode())
                || Boolean.TRUE.equals(officeMode.canOpenPersonalOwnCases());
        Set<String> allowedRamos = officeMode.canViewAllRamos()
                ? enumNames(RamoDireito.values())
                : new LinkedHashSet<>(officeMode.effectiveAllowedRamos() == null ? List.of() : officeMode.effectiveAllowedRamos());
        boolean allowSensitive = "PERSONAL".equalsIgnoreCase(officeMode.mode())
                || (officeMode.currentTrustScore() != null
                && officeMode.currentTrustScore() >= Math.max(zeroIfNull(officeMode.requiredMinTrustForAuto()), MIN_SENSITIVE_TRUST));
        boolean blockPersonalCases = officeScope && officeMode.personalBlockedByOfficePolicy();
        return new WorkspaceProcessFilterProfile(
                usuario.getId(),
                normalizeDigits(usuario.getCpf()),
                officeScope ? officeMode.activeEquipeId() : null,
                includePersonal,
                officeMode.canViewAllRamos(),
                allowedRamos.isEmpty() ? Set.of("__NONE__") : allowedRamos,
                allowSensitive,
                blockPersonalCases,
                officeMode.currentTrustScore(),
                officeMode.requiredMinTrustForAuto(),
                officeMode.mode(),
                officeMode.activeEquipeNome(),
                officeMode.effectiveSignerUserId(),
                officeMode.effectiveSignerNome(),
                officeMode.patronCertificateRequired());
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceProcessPageView currentWorkspaceProcesses(FrontendOfficeWorkspaceProcessQueryRequest request,
                                                                               HttpServletRequest httpRequest) {
        FrontendOfficeWorkspaceProcessQueryRequest safeRequest = request == null
                ? new FrontendOfficeWorkspaceProcessQueryRequest(0, 20, null, null, null, null)
                : request;
        WorkspaceProcessFilterProfile baseProfile = currentFilterProfile(httpRequest);
        WorkspaceProcessFilterProfile profile = overrideIncludePersonal(baseProfile, safeRequest.includePersonalOwnCases());
        int page = safePage(safeRequest.page());
        int size = safeSize(safeRequest.size());
        Page<Processo> result = processoRepository.searchWorkspaceVisible(trimToNull(safeRequest.search()), safeRequest.status(), safeRequest.ramoDireito(), PageRequest.of(page, size));
        List<PjbFrontendOfficeWorkspaceProcessView> items = result.getContent().stream()
                .map(processo -> toWorkspaceProcess(processo, profile))
                .filter(PjbFrontendOfficeWorkspaceProcessView::visibleInWorkspace)
                .toList();
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (profile.activeEquipeId() == null && !"PERSONAL".equalsIgnoreCase(profile.mode())) {
            blockers.add("WORKSPACE_SEM_EQUIPE_ATIVA");
        }
        if (!profile.canViewAllRamos() && profile.allowedRamos().isEmpty()) {
            blockers.add("WORKSPACE_SEM_RAMOS_AUTORIZADOS");
        }
        if (!profile.allowSensitive()) {
            warnings.add("PROCESSOS_SENSIVEIS_OCULTADOS_POR_TRUST");
        }
        auditLedgerService.appendSafely("ADV_OFFICE_WORKSPACE_PROCESS_QUERY", "FRONTEND", String.valueOf(profile.usuarioId()), "mode=" + profile.mode() + " equipe=" + profile.activeEquipeId() + " page=" + page + " size=" + size);
        return new PjbFrontendOfficeWorkspaceProcessPageView(
                profile.mode(),
                profile.activeEquipeId(),
                profile.activeEquipeNome(),
                profile.includePersonalOwnCases(),
                profile.canViewAllRamos(),
                profile.canViewAllRamos() ? enumNames(RamoDireito.values()).stream().sorted().toList() : profile.allowedRamos().stream().sorted().toList(),
                profile.currentTrustScore(),
                profile.requiredMinTrustForAuto(),
                page,
                size,
                result.getTotalElements(),
                items.size(),
                List.copyOf(new LinkedHashSet<>(blockers)),
                List.copyOf(new LinkedHashSet<>(warnings)),
                items);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessAccessView access(Long processoId,
                                                     OfficeActionType actionType,
                                                     HttpServletRequest request) {
        WorkspaceProcessFilterProfile profile = currentFilterProfile(request);
        Processo processo = processoRepository.findWorkspaceScopedById(processoId)
                .orElseThrow(() -> new EntityNotFoundException("Processo nao encontrado no workspace ativo."));
        PjbFrontendOfficeWorkspaceProcessView processView = toWorkspaceProcess(processo, profile);
        LinkedHashSet<String> blockers = new LinkedHashSet<>(processView.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>(processView.warnings());
        boolean queueRequired = false;
        if (actionType != null && processView.visibleInWorkspace()) {
            if (processView.sensitive() && profile.currentTrustScore() != null && profile.currentTrustScore() < Math.max(zeroIfNull(profile.requiredMinTrustForAuto()), MIN_SENSITIVE_TRUST)) {
                blockers.add("TRUST_INSUFICIENTE_PARA_OPERACAO_SENSIVEL");
            }
            if (profile.patronCertificateRequired() && profile.effectiveSignerUserId() != null && !Objects.equals(profile.effectiveSignerUserId(), profile.usuarioId())) {
                warnings.add("ASSINATURA_PATRONAL_OBRIGATORIA");
                queueRequired = actionType.isIrreversivel() || actionType.patronalQueueCandidate();
            }
        }
        boolean allowed = processView.visibleInWorkspace() && blockers.isEmpty();
        auditLedgerService.appendSafely("ADV_OFFICE_WORKSPACE_PROCESS_ACCESS", "PROCESSO", String.valueOf(processoId), "action=" + (actionType == null ? "READ" : actionType.name()) + " allowed=" + allowed);
        return new PjbFrontendOfficeProcessAccessView(
                processo.getId(),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                profile.activeEquipeId(),
                profile.mode(),
                actionType == null ? "READ" : actionType.name(),
                allowed,
                processView.visibleInWorkspace(),
                queueRequired,
                profile.effectiveSignerUserId(),
                profile.effectiveSignerNome(),
                List.copyOf(blockers),
                List.copyOf(warnings));
    }

    @Transactional(readOnly = true)
    public void requireAccess(Long processoId, OfficeActionType actionType, HttpServletRequest request) {
        PjbFrontendOfficeProcessAccessView access = access(processoId, actionType, request);
        if (!access.allowed()) {
            throw new IllegalStateException("Processo fora do escopo operacional do workspace: " + String.join(", ", access.blockers()));
        }
    }


    @Transactional(readOnly = true)
    public boolean supportsCurrentUser() {
        Usuario usuario = currentUserService.getOrNull();
        return usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAdvocacia();
    }

    private PjbFrontendOfficeWorkspaceProcessView toWorkspaceProcess(Processo processo, WorkspaceProcessFilterProfile profile) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        boolean officeOwned = processo.getEquipe() != null && Objects.equals(processo.getEquipe().getId(), profile.activeEquipeId());
        boolean personalOwned = processo.getEquipe() == null && processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), profile.usuarioId());
        boolean visible = "PERSONAL".equalsIgnoreCase(profile.mode()) ? personalOwned : officeOwned || (profile.includePersonalOwnCases() && personalOwned);
        boolean ownPersonalCase = isPersonalCase(processo, profile.usuarioId(), profile.userCpf());
        boolean sensitive = isSensitiveProcess(processo);
        if (!visible) {
            blockers.add("FORA_DO_WORKSPACE_ATIVO");
        }
        if (!profile.canViewAllRamos() && processo.getRamoDireito() != null && !profile.allowedRamos().contains(processo.getRamoDireito().name())) {
            blockers.add("RAMO_NAO_AUTORIZADO");
            visible = false;
        }
        if (sensitive && !profile.allowSensitive()) {
            blockers.add("TRUST_INSUFICIENTE_PARA_SIGILO_OU_RAMO_SENSIVEL");
            visible = false;
        }
        if (ownPersonalCase && profile.blockPersonalCases()) {
            blockers.add("CAUSA_PROPRIA_BLOQUEADA_NO_WORKSPACE");
            visible = false;
        }
        if (profile.patronCertificateRequired() && profile.effectiveSignerUserId() != null && !Objects.equals(profile.effectiveSignerUserId(), profile.usuarioId()) && officeOwned) {
            warnings.add("ASSINATURA_PATRONAL_OBRIGATORIA");
        }
        if (sensitive) {
            warnings.add("PROCESSO_SENSIVEL");
        }
        return new PjbFrontendOfficeWorkspaceProcessView(
                processo.getId(),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                processo.getEquipe() == null ? null : processo.getEquipe().getId(),
                processo.getEquipe() == null ? null : processo.getEquipe().getNome(),
                processo.getUsuario() == null ? null : processo.getUsuario().getId(),
                processo.getUsuario() == null ? null : processo.getUsuario().getNome(),
                processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name(),
                processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name(),
                officeOwned,
                visible,
                sensitive,
                ownPersonalCase,
                profile.patronCertificateRequired() && officeOwned,
                List.copyOf(blockers),
                List.copyOf(warnings));
    }

    private boolean isSensitiveProcess(Processo processo) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return true;
        }
        return processo.getRamoDireito() != null && (processo.getRamoDireito().isPenalLike() || SENSITIVE_RAMOS.contains(processo.getRamoDireito()));
    }

    private boolean isPersonalCase(Processo processo, Long usuarioId, String cpf) {
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuarioId)) {
            return true;
        }
        if (cpf == null) {
            return false;
        }
        return cpf.equals(normalizeDigits(processo.getParteAutoraCpf())) || cpf.equals(normalizeDigits(processo.getParteReuCpf()));
    }

    private WorkspaceProcessFilterProfile overrideIncludePersonal(WorkspaceProcessFilterProfile profile, Boolean includePersonalOwnCases) {
        if (includePersonalOwnCases == null) {
            return profile;
        }
        return new WorkspaceProcessFilterProfile(
                profile.usuarioId(),
                profile.userCpf(),
                profile.activeEquipeId(),
                includePersonalOwnCases && profile.includePersonalOwnCases(),
                profile.canViewAllRamos(),
                profile.allowedRamos(),
                profile.allowSensitive(),
                profile.blockPersonalCases(),
                profile.currentTrustScore(),
                profile.requiredMinTrustForAuto(),
                profile.mode(),
                profile.activeEquipeNome(),
                profile.effectiveSignerUserId(),
                profile.effectiveSignerNome(),
                profile.patronCertificateRequired());
    }

    private int safePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int safeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Set<String> enumNames(RamoDireito[] values) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (RamoDireito value : values) {
            names.add(value.name());
        }
        return names;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    public record WorkspaceProcessFilterProfile(Long usuarioId,
                                                String userCpf,
                                                Long activeEquipeId,
                                                boolean includePersonalOwnCases,
                                                boolean canViewAllRamos,
                                                Set<String> allowedRamos,
                                                boolean allowSensitive,
                                                boolean blockPersonalCases,
                                                Integer currentTrustScore,
                                                Integer requiredMinTrustForAuto,
                                                String mode,
                                                String activeEquipeNome,
                                                Long effectiveSignerUserId,
                                                String effectiveSignerNome,
                                                boolean patronCertificateRequired) {
    }
}
