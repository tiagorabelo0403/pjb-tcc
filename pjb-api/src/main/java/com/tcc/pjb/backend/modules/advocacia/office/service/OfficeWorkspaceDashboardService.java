package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeMembershipView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeTeamMemberView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePresence;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePresenceRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspaceProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceDashboardService {

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final AdvOfficeWorkspaceProfileRepository profileRepository;
    private final EquipeOfficePolicyRepository policyRepository;
    private final MembroEquipeRepository membroEquipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdvOfficeWorkspacePresenceRepository presenceRepository;

    public OfficeWorkspaceDashboardService(CurrentUserService currentUserService,
                                           OfficeWorkspaceModeService officeWorkspaceModeService,
                                           AdvOfficeWorkspaceProfileRepository profileRepository,
                                           EquipeOfficePolicyRepository policyRepository,
                                           MembroEquipeRepository membroEquipeRepository,
                                           UsuarioRepository usuarioRepository,
                                           AdvOfficeWorkspacePresenceRepository presenceRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.profileRepository = Objects.requireNonNull(profileRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.presenceRepository = Objects.requireNonNull(presenceRepository);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceSummaryView currentSummary(HttpServletRequest request, Long requestedEquipeId) {
        Usuario current = currentUserService.getRequired();
        if (!current.isAdvogado()) {
            return null;
        }
        Long equipeId = requestedEquipeId != null ? requestedEquipeId : resolveDashboardEquipeId(current.getId(), request);
        if (equipeId == null) {
            return null;
        }
        if (!membroEquipeRepository.existsByUsuario_IdAndEquipe_IdAndAtivoTrue(current.getId(), equipeId)
                && profileRepository.findByEquipe_Id(equipeId).map(AdvOfficeWorkspaceProfile::getOwnerUserId).filter(current.getId()::equals).isEmpty()) {
            throw new IllegalArgumentException("Usuario nao possui vinculo com o escritorio solicitado.");
        }
        AdvOfficeWorkspaceProfile profile = profileRepository.findByEquipe_Id(equipeId).orElse(null);
        EquipeOfficePolicy policy = policyRepository.findByEquipeId(equipeId).orElse(null);
        Long founderUserId = profile == null ? null : profile.getOwnerUserId();
        Usuario founder = founderUserId == null ? null : usuarioRepository.findById(founderUserId).orElse(null);
        Long patronoUserId = policy != null && policy.getSignerUserId() != null ? policy.getSignerUserId() : founderUserId;
        Usuario patrono = patronoUserId == null ? null : usuarioRepository.findById(patronoUserId).orElse(null);
        Instant cutoff = Instant.now().minus(OfficeWorkspacePresenceService.ONLINE_WINDOW);
        Map<Long, AdvOfficeWorkspacePresence> onlineByUser = presenceRepository.findByEquipe_IdAndLastSeenAtAfterOrderByLastSeenAtDesc(equipeId, cutoff).stream()
                .collect(Collectors.toMap(AdvOfficeWorkspacePresence::getUserId, Function.identity(), (left, right) -> left.getLastSeenAt().isAfter(right.getLastSeenAt()) ? left : right));
        List<MembroEquipe> activeMembers = membroEquipeRepository.carregarComUsuario(equipeId).stream()
                .filter(MembroEquipe::isAtivo)
                .filter(item -> item.getUsuario() != null && item.getUsuario().isAtivo())
                .toList();
        var officeMode = officeWorkspaceModeService.current(request);
        PjbFrontendOfficeMembershipView currentMembership = officeMode.memberships().stream()
                .filter(item -> Objects.equals(item.equipeId(), equipeId))
                .findFirst()
                .orElse(null);
        List<PjbFrontendOfficeTeamMemberView> members = new ArrayList<>();
        for (MembroEquipe membro : activeMembers) {
            AdvOfficeWorkspacePresence presence = onlineByUser.get(membro.getUsuario().getId());
            members.add(new PjbFrontendOfficeTeamMemberView(
                    membro.getUsuario().getId(),
                    membro.getUsuario().getNome(),
                    membro.getUsuario().getEmail(),
                    professionalRegistration(membro.getUsuario()),
                    membro.getPapel() == null ? null : membro.getPapel().name(),
                    membro.getCargo(),
                    Objects.equals(patronoUserId, membro.getUsuario().getId()),
                    Objects.equals(founderUserId, membro.getUsuario().getId()),
                    Objects.equals(current.getId(), membro.getUsuario().getId()),
                    currentMembership != null && Objects.equals(current.getId(), membro.getUsuario().getId()) && currentMembership.activeSelection(),
                    presence != null,
                    presence == null ? null : presence.getLastSeenAt(),
                    currentMembership == null || !Objects.equals(current.getId(), membro.getUsuario().getId()) ? null : currentMembership.workspacePriority(),
                    affiliationType(membro.getPapel(), founderUserId, membro.getUsuario().getId())
            ));
        }
        List<PjbFrontendOfficeTeamMemberView> sortedMembers = members.stream()
                .sorted(Comparator.comparing(PjbFrontendOfficeTeamMemberView::online).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::patrono).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::fundador).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<PjbFrontendOfficeTeamMemberView> onlineMembers = sortedMembers.stream().filter(PjbFrontendOfficeTeamMemberView::online).toList();
        Set<RamoDireito> allowedRamosSet = policy == null || policy.getAllowedRamos() == null ? Set.of() : policy.getAllowedRamos();
        List<String> allowedRamos = allowedRamosSet.isEmpty()
                ? java.util.Arrays.stream(RamoDireito.values()).map(Enum::name).sorted().toList()
                : allowedRamosSet.stream().map(Enum::name).sorted().toList();
        long ownedOfficeCount = profileRepository.findByOwnerUserIdOrderByCreatedAtDesc(current.getId()).size();
        long membershipCount = membroEquipeRepository.findByUsuario_Id(current.getId()).stream().filter(MembroEquipe::isAtivo).count();
        boolean currentWorkspaceSelected = currentMembership != null && currentMembership.activeSelection();
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        if (profile != null) {
            hints.add("Escritorio institucional visivel no cockpit do afiliado e do patrono.");
        }
        hints.add("Total de membros ativos: " + activeMembers.size() + ".");
        hints.add("Membros online agora: " + onlineMembers.size() + ".");
        if (patrono != null) {
            hints.add("Assinatura governada vinculada ao patrono " + patrono.getNome() + ".");
        }
        if (founder != null && !Objects.equals(founderUserId, patronoUserId)) {
            hints.add("Fundador institucional: " + founder.getNome() + ".");
        }
        if (ownedOfficeCount == 0L) {
            hints.add("Atuacao propria pode materializar escritorio pessoal para o mesmo cockpit institucional.");
        }
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (policy != null && policy.isEnabled() && policy.isForcePatronoCertificate()) {
            blockers.add("ASSINATURA_PATRONAL_OBRIGATORIA");
        }
        return new PjbFrontendOfficeWorkspaceSummaryView(
                equipeId,
                profile == null ? activeMembers.stream().findFirst().map(m -> m.getEquipe().getNome()).orElse(null) : profile.getDisplayName(),
                founderUserId,
                founder == null ? null : founder.getNome(),
                patronoUserId,
                patrono == null ? null : patrono.getNome(),
                Objects.equals(founderUserId, current.getId()),
                membroEquipeRepository.existsByUsuario_IdAndEquipe_IdAndAtivoTrue(current.getId(), equipeId),
                currentWorkspaceSelected,
                officeMode.mode(),
                ownedOfficeCount,
                membershipCount,
                membroEquipeRepository.countByEquipe_Id(equipeId),
                activeMembers.size(),
                onlineMembers.size(),
                profile != null && profile.isAllBrazilianLawEnabled(),
                policy != null && policy.isEnabled() && policy.isForcePatronoCertificate(),
                allowedRamos,
                List.copyOf(blockers),
                List.copyOf(hints),
                sortedMembers,
                onlineMembers
        );
    }

    private Long resolveDashboardEquipeId(Long usuarioId, HttpServletRequest request) {
        Long preferred = officeWorkspaceModeService.resolvePreferredEquipeId(usuarioId, request);
        if (preferred != null) {
            return preferred;
        }
        List<AdvOfficeWorkspaceProfile> owned = profileRepository.findByOwnerUserIdOrderByCreatedAtDesc(usuarioId);
        if (!owned.isEmpty()) {
            return owned.get(0).getEquipe().getId();
        }
        List<MembroEquipe> memberships = membroEquipeRepository.findByUsuario_Id(usuarioId).stream().filter(MembroEquipe::isAtivo).toList();
        if (memberships.size() == 1) {
            return memberships.get(0).getEquipe().getId();
        }
        return null;
    }

    private String professionalRegistration(Usuario usuario) {
        if (usuario.getRegistroProfissional() != null && !usuario.getRegistroProfissional().isBlank()) {
            return usuario.getRegistroProfissional();
        }
        if (usuario.getOab() != null && !usuario.getOab().isBlank()) {
            return usuario.getOab();
        }
        return null;
    }

    private String affiliationType(PapelEquipe papel, Long founderUserId, Long userId) {
        if (Objects.equals(founderUserId, userId)) {
            return "FOUNDER";
        }
        if (papel == null) {
            return "AFFILIATE";
        }
        return switch (papel) {
            case ADVOGADO_SENIOR, ADMINISTRADOR, COORDENADOR -> "PATRONO_TEAM";
            default -> "AFFILIATE";
        };
    }
}
