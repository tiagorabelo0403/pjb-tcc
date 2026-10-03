package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePreference;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeWorkspaceMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceModeService {

    public static final String HEADER_MODE = "X-Office-Mode";
    public static final String COOKIE_MODE = "PJB_OFFICE_MODE";
    public static final String COOKIE_EQUIPE = "PJB_EQUIPE_ID";

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceMembershipService membershipService;
    private final AdvOfficeWorkspacePreferenceRepository preferenceRepository;
    private final OfficePersonalScopeService personalScopeService;
    private final AuditLedgerService auditLedgerService;

    public OfficeWorkspaceModeService(CurrentUserService currentUserService,
                                      OfficeWorkspaceMembershipService membershipService,
                                      AdvOfficeWorkspacePreferenceRepository preferenceRepository,
                                      OfficePersonalScopeService personalScopeService,
                                      AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.membershipService = Objects.requireNonNull(membershipService);
        this.preferenceRepository = Objects.requireNonNull(preferenceRepository);
        this.personalScopeService = Objects.requireNonNull(personalScopeService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeModeView current(HttpServletRequest request) {
        return buildView(currentUserService.getRequired(), request, null, null, null);
    }

    @Transactional
    public PjbFrontendOfficeModeView update(FrontendOfficeModeUpdateRequest request) {
        Usuario usuario = currentUserService.getRequired();
        OfficeWorkspaceMode mode = OfficeWorkspaceMode.fromString(request.mode());
        if (mode == null) {
            throw new IllegalArgumentException("Modo de escritorio invalido.");
        }
        List<PjbFrontendOfficeMembershipView> memberships = membershipService.memberships(usuario.getId());
        if (mode != OfficeWorkspaceMode.PERSONAL) {
            if (request.equipeId() == null) {
                throw new IllegalArgumentException("Equipe obrigatoria para modo escritorio/hibrido.");
            }
            if (!containsMembership(memberships, request.equipeId())) {
                throw new IllegalArgumentException("Usuario nao possui vinculo ativo com a equipe informada.");
            }
        }
        AdvOfficeWorkspacePreference preference = preferenceRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (preference == null) {
            preference = new AdvOfficeWorkspacePreference();
            preference.setUsuarioId(usuario.getId());
            preference.setCreatedAt(Instant.now());
        }
        preference.setPreferredEquipeId(mode == OfficeWorkspaceMode.PERSONAL ? null : request.equipeId());
        preference.setMode(mode.name());
        preference.setAutoActivateOnLogin(Boolean.TRUE.equals(request.autoActivateOnLogin()));
        preference.setAllowPersonalOwnCases(mode != OfficeWorkspaceMode.OFFICE || Boolean.TRUE.equals(request.allowPersonalOwnCases()));
        preference.setUpdatedAt(Instant.now());
        if (preference.getCreatedAt() == null) {
            preference.setCreatedAt(preference.getUpdatedAt());
        }
        preferenceRepository.save(preference);
        auditLedgerService.appendSafely("ADV_OFFICE_MODE_UPDATE", "FRONTEND", String.valueOf(usuario.getId()), "mode=" + preference.getMode() + " equipe=" + preference.getPreferredEquipeId());
        return buildView(usuario, null, preference, mode, request.equipeId());
    }

    @Transactional
    public PjbFrontendOfficeModeView clear() {
        Usuario usuario = currentUserService.getRequired();
        preferenceRepository.findByUsuarioId(usuario.getId()).ifPresent(preferenceRepository::delete);
        auditLedgerService.appendSafely("ADV_OFFICE_MODE_CLEAR", "FRONTEND", String.valueOf(usuario.getId()), "clear=true");
        return buildView(usuario, null, null, OfficeWorkspaceMode.PERSONAL, null);
    }

    private PjbFrontendOfficeModeView buildView(Usuario usuario,
                                                HttpServletRequest request,
                                                AdvOfficeWorkspacePreference preferenceOverride,
                                                OfficeWorkspaceMode forcedMode,
                                                Long forcedEquipeId) {
        if (!usuario.isAdvogado()) {
            return new PjbFrontendOfficeModeView(
                    OfficeWorkspaceMode.PERSONAL.name(),
                    null,
                    null,
                    null,
                    null,
                    false,
                    true,
                    false,
                    false,
                    false,
                    false,
                    List.of(),
                    List.of("Usuario fora do escopo de advocacia/escritorio."),
                    enumNames(RamoDireito.values()),
                    true,
                    null,
                    null,
                    null,
                    false,
                    usuario.getId(),
                    usuario.getNome());
        }
        List<PjbFrontendOfficeMembershipView> memberships = membershipService.memberships(usuario.getId());
        AdvOfficeWorkspacePreference preference = preferenceOverride != null ? preferenceOverride : preferenceRepository.findByUsuarioId(usuario.getId()).orElse(null);
        MembroEquipe membroAtivo = EquipeContexto.getMembroDaEquipeAtiva();
        Long activeEquipeId = forcedEquipeId;
        if (activeEquipeId == null && membroAtivo != null && membroAtivo.getEquipe() != null) {
            activeEquipeId = membroAtivo.getEquipe().getId();
        }
        if (activeEquipeId == null) {
            activeEquipeId = preferredEquipeFromRequestOrPreference(request, preference, memberships);
        }
        PjbFrontendOfficeMembershipView activeMembership = findActiveMembership(memberships, activeEquipeId);
        OfficePersonalScopeService.ScopeDecision scope = personalScopeService.decide(usuario.getId());
        OfficeWorkspaceMode mode = forcedMode != null ? forcedMode : effectiveMode(request, preference, activeMembership, scope, memberships);
        boolean canOpenPersonalOwnCases = canOpenPersonalOwnCases(mode, preference, scope);
        boolean officeProcessesVisibleByDefault = mode != OfficeWorkspaceMode.PERSONAL && activeMembership != null;
        boolean detached = mode != OfficeWorkspaceMode.PERSONAL && activeMembership == null && !memberships.isEmpty();
        boolean requiresSelection = mode != OfficeWorkspaceMode.PERSONAL && activeMembership == null && memberships.size() > 1;
        List<String> effectiveAllowedRamos = activeMembership == null || mode == OfficeWorkspaceMode.PERSONAL
                ? enumNames(RamoDireito.values())
                : activeMembership.allowedRamos();
        boolean canViewAllRamos = activeMembership == null || mode == OfficeWorkspaceMode.PERSONAL || activeMembership.canViewAllRamos();
        Integer trustScore = activeMembership == null ? null : activeMembership.trustScore();
        String trustLevel = activeMembership == null ? null : activeMembership.trustLevel();
        Integer requiredMinTrustForAuto = activeMembership == null ? null : activeMembership.minTrustRequired();
        boolean patronCertificateRequired = activeMembership != null && activeMembership.patronCertificateRequired();
        Long effectiveSignerUserId = activeMembership == null ? usuario.getId() : activeMembership.seniorUserId() == null ? usuario.getId() : activeMembership.seniorUserId();
        String effectiveSignerNome = activeMembership == null ? usuario.getNome() : activeMembership.seniorNome() == null ? usuario.getNome() : activeMembership.seniorNome();
        List<String> hints = buildHints(mode, activeMembership, scope, memberships, canOpenPersonalOwnCases, effectiveAllowedRamos, canViewAllRamos, trustScore, requiredMinTrustForAuto, patronCertificateRequired, effectiveSignerNome);
        PjbFrontendOfficeModeView view = new PjbFrontendOfficeModeView(
                mode.name(),
                activeMembership == null ? null : activeMembership.equipeId(),
                activeMembership == null ? null : activeMembership.equipeNome(),
                activeMembership == null ? null : activeMembership.seniorUserId(),
                activeMembership == null ? null : activeMembership.seniorNome(),
                officeProcessesVisibleByDefault,
                canOpenPersonalOwnCases,
                preference != null && preference.isAutoActivateOnLogin(),
                scope.personalBlocked(),
                requiresSelection,
                detached,
                markActiveMemberships(memberships, activeMembership == null ? null : activeMembership.equipeId()),
                hints,
                effectiveAllowedRamos,
                canViewAllRamos,
                trustScore,
                trustLevel,
                requiredMinTrustForAuto,
                patronCertificateRequired,
                effectiveSignerUserId,
                effectiveSignerNome);
        auditLedgerService.appendSafely("ADV_OFFICE_MODE_VIEW", "FRONTEND", String.valueOf(usuario.getId()), "mode=" + view.mode() + " equipe=" + view.activeEquipeId() + " signer=" + view.effectiveSignerUserId());
        return view;
    }

    @Transactional(readOnly = true)
    public Long resolvePreferredEquipeId(Long usuarioId, HttpServletRequest request) {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null || !usuario.isAdvogado()) {
            return null;
        }
        List<PjbFrontendOfficeMembershipView> memberships = membershipService.memberships(usuarioId);
        AdvOfficeWorkspacePreference preference = preferenceRepository.findByUsuarioId(usuarioId).orElse(null);
        Long preferredEquipeId = preferredEquipeFromRequestOrPreference(request, preference, memberships);
        PjbFrontendOfficeMembershipView activeMembership = findActiveMembership(memberships, preferredEquipeId);
        OfficeWorkspaceMode mode = effectiveMode(request, preference, activeMembership, personalScopeService.decide(usuarioId), memberships);
        if (mode == OfficeWorkspaceMode.PERSONAL) {
            return null;
        }
        return activeMembership == null ? null : activeMembership.equipeId();
    }

    private OfficeWorkspaceMode effectiveMode(HttpServletRequest request,
                                              AdvOfficeWorkspacePreference preference,
                                              PjbFrontendOfficeMembershipView activeMembership,
                                              OfficePersonalScopeService.ScopeDecision scope,
                                              List<PjbFrontendOfficeMembershipView> memberships) {
        OfficeWorkspaceMode requestMode = request == null ? null : OfficeWorkspaceMode.fromString(firstNonBlank(headerValue(request, HEADER_MODE), cookieValue(request, COOKIE_MODE)));
        if (requestMode != null) {
            if (requestMode == OfficeWorkspaceMode.PERSONAL && scope.personalBlocked()) {
                return memberships.size() <= 1 ? OfficeWorkspaceMode.OFFICE : OfficeWorkspaceMode.HYBRID;
            }
            return requestMode;
        }
        OfficeWorkspaceMode storedMode = preference == null ? null : OfficeWorkspaceMode.fromString(preference.getMode());
        if (storedMode != null && preference.isAutoActivateOnLogin()) {
            if (storedMode == OfficeWorkspaceMode.PERSONAL && scope.personalBlocked()) {
                return memberships.size() <= 1 ? OfficeWorkspaceMode.OFFICE : OfficeWorkspaceMode.HYBRID;
            }
            return storedMode;
        }
        if (scope.personalBlocked()) {
            return memberships.size() <= 1 ? OfficeWorkspaceMode.OFFICE : OfficeWorkspaceMode.HYBRID;
        }
        if (activeMembership != null) {
            return OfficeWorkspaceMode.HYBRID;
        }
        return OfficeWorkspaceMode.PERSONAL;
    }

    private boolean canOpenPersonalOwnCases(OfficeWorkspaceMode mode,
                                            AdvOfficeWorkspacePreference preference,
                                            OfficePersonalScopeService.ScopeDecision scope) {
        if (mode == OfficeWorkspaceMode.PERSONAL || mode == OfficeWorkspaceMode.HYBRID) {
            return true;
        }
        return preference != null && preference.isAllowPersonalOwnCases() && !scope.personalBlocked();
    }

    private Long preferredEquipeFromRequestOrPreference(HttpServletRequest request,
                                                        AdvOfficeWorkspacePreference preference,
                                                        List<PjbFrontendOfficeMembershipView> memberships) {
        Long requestEquipeId = parseLong(firstNonBlank(request == null ? null : headerValue(request, "X-Equipe-ID"), request == null ? null : cookieValue(request, COOKIE_EQUIPE)));
        if (requestEquipeId != null && containsMembership(memberships, requestEquipeId)) {
            return requestEquipeId;
        }
        Long storedEquipeId = preference == null ? null : preference.getPreferredEquipeId();
        if (storedEquipeId != null && containsMembership(memberships, storedEquipeId)) {
            return storedEquipeId;
        }
        List<PjbFrontendOfficeMembershipView> autoCandidates = new ArrayList<>();
        for (PjbFrontendOfficeMembershipView membership : memberships) {
            if (membership.elegivelAutoAtivacao()) {
                autoCandidates.add(membership);
            }
        }
        autoCandidates.sort(Comparator.comparing(PjbFrontendOfficeMembershipView::workspacePriority)
                .thenComparing(PjbFrontendOfficeMembershipView::equipeNome, String.CASE_INSENSITIVE_ORDER));
        if (!autoCandidates.isEmpty()) {
            PjbFrontendOfficeMembershipView first = autoCandidates.get(0);
            long samePriority = 0;
            for (PjbFrontendOfficeMembershipView candidate : autoCandidates) {
                if (Objects.equals(candidate.workspacePriority(), first.workspacePriority())) {
                    samePriority++;
                }
            }
            if (samePriority == 1) {
                return first.equipeId();
            }
            return null;
        }
        if (memberships.size() == 1) {
            return memberships.get(0).equipeId();
        }
        return null;
    }

    private List<PjbFrontendOfficeMembershipView> markActiveMemberships(List<PjbFrontendOfficeMembershipView> memberships, Long activeEquipeId) {
        List<PjbFrontendOfficeMembershipView> out = new ArrayList<>(memberships.size());
        for (PjbFrontendOfficeMembershipView item : memberships) {
            out.add(new PjbFrontendOfficeMembershipView(
                    item.equipeId(),
                    item.equipeNome(),
                    item.papelEquipe(),
                    item.cargo(),
                    item.seniorUserId(),
                    item.seniorNome(),
                    item.officePolicyEnabled(),
                    item.bloqueiaCausasProprias(),
                    item.membroAtivo(),
                    item.elegivelAutoAtivacao(),
                    Objects.equals(item.equipeId(), activeEquipeId),
                    item.allowedRamos(),
                    item.canViewAllRamos(),
                    item.trustScore(),
                    item.trustLevel(),
                    item.minTrustRequired(),
                    item.patronCertificateRequired(),
                    item.workspacePriority()));
        }
        return List.copyOf(out);
    }

    private boolean containsMembership(List<PjbFrontendOfficeMembershipView> memberships, Long equipeId) {
        for (PjbFrontendOfficeMembershipView membership : memberships) {
            if (Objects.equals(membership.equipeId(), equipeId)) {
                return true;
            }
        }
        return false;
    }

    private PjbFrontendOfficeMembershipView findActiveMembership(List<PjbFrontendOfficeMembershipView> memberships,
                                                                 Long equipeId) {
        for (PjbFrontendOfficeMembershipView membership : memberships) {
            if (Objects.equals(membership.equipeId(), equipeId)) {
                return membership;
            }
        }
        return null;
    }

    private List<String> buildHints(OfficeWorkspaceMode mode,
                                    PjbFrontendOfficeMembershipView activeMembership,
                                    OfficePersonalScopeService.ScopeDecision scope,
                                    List<PjbFrontendOfficeMembershipView> memberships,
                                    boolean canOpenPersonalOwnCases,
                                    List<String> effectiveAllowedRamos,
                                    boolean canViewAllRamos,
                                    Integer trustScore,
                                    Integer requiredMinTrustForAuto,
                                    boolean patronCertificateRequired,
                                    String effectiveSignerNome) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        if (mode == OfficeWorkspaceMode.PERSONAL) {
            hints.add("Processos proprios em primeiro plano.");
        }
        if (activeMembership != null) {
            hints.add("Sessao vinculada ao escritorio " + activeMembership.equipeNome() + ".");
            hints.add("Prioridade operacional do vinculo: " + activeMembership.workspacePriority() + ".");
            if (activeMembership.seniorNome() != null) {
                hints.add("Fluxo subordinado ao advogado senior " + activeMembership.seniorNome() + ".");
            }
        }
        if (!canViewAllRamos && !effectiveAllowedRamos.isEmpty()) {
            hints.add("Visibilidade limitada aos ramos: " + String.join(", ", effectiveAllowedRamos) + ".");
        }
        if (canOpenPersonalOwnCases) {
            hints.add("Pode abrir causas proprias sem perder o contexto do escritorio.");
        }
        if (scope.personalBlocked()) {
            hints.add("A politica do escritorio bloqueia atuacao pessoal como contexto padrao.");
        }
        if (trustScore != null && requiredMinTrustForAuto != null && trustScore < requiredMinTrustForAuto) {
            hints.add("Nivel de confianca atual abaixo do minimo para assinatura automatica do patrono.");
        }
        if (patronCertificateRequired && effectiveSignerNome != null) {
            hints.add("Documentos assinados em modo escritorio saem com o certificado do patrono " + effectiveSignerNome + ".");
        }
        if (memberships.size() > 1 && activeMembership == null && mode != OfficeWorkspaceMode.PERSONAL) {
            hints.add("Ha mais de um escritorio elegivel. Se houver empate de prioridade, selecao explicita e obrigatoria.");
        }
        if (memberships.isEmpty()) {
            hints.add("Sem vinculos ativos de escritorio no momento.");
        }
        return List.copyOf(hints);
    }

    private String headerValue(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> enumNames(Enum<?>[] values) {
        ArrayList<String> out = new ArrayList<>();
        for (Enum<?> value : values) {
            out.add(value.name());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(out);
    }
}
