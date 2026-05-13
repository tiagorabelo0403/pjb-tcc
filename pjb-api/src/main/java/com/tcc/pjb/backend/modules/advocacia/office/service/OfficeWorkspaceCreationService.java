package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOwnedOfficeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePreference;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeWorkspaceMode;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePreferenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePresenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceCreationService {

    private final CurrentUserService currentUserService;
    private final EquipeRepository equipeRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final AdvOfficeWorkspacePreferenceRepository preferenceRepository;
    private final AdvOfficeWorkspaceProfileRepository profileRepository;
    private final AdvOfficeWorkspacePresenceRepository presenceRepository;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final AuditLedgerService auditLedgerService;

    public OfficeWorkspaceCreationService(CurrentUserService currentUserService,
                                          EquipeRepository equipeRepository,
                                          MembroEquipeRepository membroEquipeRepository,
                                          EquipeOfficePolicyRepository policyRepository,
                                          AdvOfficeWorkspacePreferenceRepository preferenceRepository,
                                          AdvOfficeWorkspaceProfileRepository profileRepository,
                                          AdvOfficeWorkspacePresenceRepository presenceRepository,
                                          OfficeWorkspaceModeService officeWorkspaceModeService,
                                          AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.equipeRepository = Objects.requireNonNull(equipeRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.preferenceRepository = Objects.requireNonNull(preferenceRepository);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.presenceRepository = Objects.requireNonNull(presenceRepository);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public PjbFrontendOfficeModeView createOwnOffice(FrontendOfficeWorkspaceCreateRequest request) {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.isAdvogado()) {
            throw new IllegalStateException("Somente advogado pode criar escritorio proprio.");
        }
        String officeName = normalizeOfficeName(request.officeName(), usuario.getNome());
        if (equipeRepository.existsByNomeIgnoreCase(officeName)) {
            throw new IllegalArgumentException("Ja existe escritorio/equipe com este nome.");
        }

        Equipe equipe = new Equipe();
        equipe.setNome(officeName);
        equipe.setAtivo(true);
        equipe = equipeRepository.save(equipe);

        MembroEquipe membro = new MembroEquipe();
        membro.setEquipe(equipe);
        membro.setUsuario(usuario);
        membro.setPapel(PapelEquipe.ADVOGADO_SENIOR);
        membro.setCargo("Patrono fundador");
        membro.setAtivo(true);
        membro.setDataAdmissao(LocalDate.now());
        membroEquipeRepository.save(membro);

        boolean allBrazilianLawEnabled = !Boolean.FALSE.equals(request.allBrazilianLawEnabled()) && (request.allowedRamos() == null || request.allowedRamos().isEmpty());
        Set<RamoDireito> effectiveRamos = allBrazilianLawEnabled || request.allowedRamos() == null || request.allowedRamos().isEmpty()
                ? EnumSet.noneOf(RamoDireito.class)
                : EnumSet.copyOf(request.allowedRamos());

        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEquipe(equipe);
        policy.setEnabled(true);
        policy.setSignerUserId(usuario.getId());
        policy.setBloqueiaCausasProprias(false);
        policy.setForcePatronoCertificate(true);
        policy.setMinTrustAuto(10);
        policy.setMaxAutoPorDia(200);
        policy.setAllowedRamos(effectiveRamos);
        policy.setAutoActions(defaultAutoActions());
        policyRepository.save(policy);

        AdvOfficeWorkspaceProfile profile = new AdvOfficeWorkspaceProfile();
        profile.setEquipe(equipe);
        profile.setOwnerUserId(usuario.getId());
        profile.setDisplayName(officeName);
        profile.setAllBrazilianLawEnabled(allBrazilianLawEnabled);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(profile.getCreatedAt());
        profileRepository.save(profile);

        OfficeWorkspaceMode mode = OfficeWorkspaceMode.fromString(request.mode());
        if (mode == null) {
            mode = OfficeWorkspaceMode.HYBRID;
        }
        AdvOfficeWorkspacePreference preference = preferenceRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            AdvOfficeWorkspacePreference entity = new AdvOfficeWorkspacePreference();
            entity.setUsuarioId(usuario.getId());
            entity.setCreatedAt(Instant.now());
            return entity;
        });
        preference.setPreferredEquipeId(equipe.getId());
        preference.setMode(mode.name());
        preference.setAutoActivateOnLogin(!Boolean.FALSE.equals(request.autoActivateOnLogin()));
        preference.setAllowPersonalOwnCases(!Boolean.FALSE.equals(request.allowPersonalOwnCases()));
        preference.setUpdatedAt(Instant.now());
        preferenceRepository.save(preference);

        auditLedgerService.appendSafely("ADV_OFFICE_SELF_CREATE", "OFFICE", String.valueOf(usuario.getId()), "equipe=" + equipe.getId() + " nome=" + officeName + " ramos=" + (allBrazilianLawEnabled ? "ALL" : effectiveRamos.size()));
        return officeWorkspaceModeService.update(new FrontendOfficeModeUpdateRequest(equipe.getId(), mode.name(), preference.isAutoActivateOnLogin(), preference.isAllowPersonalOwnCases()));
    }

    @Transactional
    public PjbFrontendOfficeModeView ensurePersonalOffice() {
        Usuario usuario = currentUserService.getRequired();
        List<AdvOfficeWorkspaceProfile> owned = profileRepository.findByOwnerUserIdOrderByCreatedAtDesc(usuario.getId());
        if (!owned.isEmpty()) {
            Long equipeId = owned.get(0).getEquipe().getId();
            return officeWorkspaceModeService.update(new FrontendOfficeModeUpdateRequest(equipeId, OfficeWorkspaceMode.HYBRID.name(), true, true));
        }
        return createOwnOffice(new FrontendOfficeWorkspaceCreateRequest(resolveDefaultPersonalOfficeName(usuario), OfficeWorkspaceMode.HYBRID.name(), true, true, true, null));
    }

    @Transactional(readOnly = true)
    public List<PjbFrontendOwnedOfficeView> myOwnedOffices() {
        Usuario usuario = currentUserService.getRequired();
        return profileRepository.findByOwnerUserIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(profile -> {
                    EquipeOfficePolicy policy = policyRepository.findByEquipeId(profile.getEquipe().getId()).orElse(null);
                    String defaultMode = preferenceRepository.findByUsuarioId(usuario.getId())
                            .filter(pref -> Objects.equals(pref.getPreferredEquipeId(), profile.getEquipe().getId()))
                            .map(AdvOfficeWorkspacePreference::getMode)
                            .orElse(OfficeWorkspaceMode.HYBRID.name());
                    boolean autoActivate = preferenceRepository.findByUsuarioId(usuario.getId())
                            .filter(pref -> Objects.equals(pref.getPreferredEquipeId(), profile.getEquipe().getId()))
                            .map(AdvOfficeWorkspacePreference::isAutoActivateOnLogin)
                            .orElse(false);
                    List<String> allowedRamos = policy == null || policy.getAllowedRamos() == null || policy.getAllowedRamos().isEmpty()
                            ? List.of()
                            : policy.getAllowedRamos().stream().map(Enum::name).sorted().toList();
                    long totalMembers = membroEquipeRepository.countByEquipe_Id(profile.getEquipe().getId());
                    long activeMembers = membroEquipeRepository.countByEquipe_IdAndAtivoTrue(profile.getEquipe().getId());
                    long onlineMembers = presenceRepository.countByEquipe_IdAndLastSeenAtAfter(profile.getEquipe().getId(), Instant.now().minus(OfficeWorkspacePresenceService.ONLINE_WINDOW));
                    return new PjbFrontendOwnedOfficeView(
                            profile.getEquipe().getId(),
                            profile.getDisplayName(),
                            profile.getOwnerUserId(),
                            usuario.getNome(),
                            policy != null && policy.isEnabled(),
                            profile.isAllBrazilianLawEnabled(),
                            allowedRamos,
                            defaultMode,
                            autoActivate,
                            policy != null && policy.isForcePatronoCertificate(),
                            totalMembers,
                            activeMembers,
                            onlineMembers,
                            Objects.equals(profile.getEquipe().getId(), preferenceRepository.findByUsuarioId(usuario.getId()).map(AdvOfficeWorkspacePreference::getPreferredEquipeId).orElse(null)));
                })
                .toList();
    }

    private String resolveDefaultPersonalOfficeName(Usuario usuario) {
        List<String> candidates = List.of(
                "Escritorio pessoal de " + usuario.getNome(),
                usuario.getOab() == null || usuario.getOab().isBlank() ? "Escritorio pessoal #" + usuario.getId() : "Escritorio pessoal " + usuario.getOab(),
                "Escritorio pessoal usuario " + usuario.getId()
        );
        for (String candidate : candidates) {
            if (!equipeRepository.existsByNomeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return "Escritorio pessoal usuario " + usuario.getId() + "-" + Instant.now().toEpochMilli();
    }

    private String normalizeOfficeName(String officeName, String advogadoNome) {
        String normalized = officeName == null ? "" : officeName.trim();
        if (normalized.isBlank()) {
            return "Escritorio de " + advogadoNome;
        }
        return normalized;
    }

    private static Set<OfficeActionType> defaultAutoActions() {
        return EnumSet.of(OfficeActionType.PROTOCOL_SUBMIT_PJE,
                OfficeActionType.PETICIONAR,
                OfficeActionType.RECORRER,
                OfficeActionType.JUNTAR_DOCUMENTO);
    }
}
