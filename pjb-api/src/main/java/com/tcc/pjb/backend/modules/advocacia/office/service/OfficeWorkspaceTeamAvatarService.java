package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.repository.UsuarioAvatarRepository;
import com.tcc.pjb.backend.service.identity.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceTeamAvatarService {

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;
    private final UserAvatarService userAvatarService;
    private final UsuarioAvatarRepository usuarioAvatarRepository;

    public OfficeWorkspaceTeamAvatarService(CurrentUserService currentUserService,
                                            OfficeWorkspaceDashboardService officeWorkspaceDashboardService,
                                            UserAvatarService userAvatarService,
                                            UsuarioAvatarRepository usuarioAvatarRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
        this.userAvatarService = Objects.requireNonNull(userAvatarService);
        this.usuarioAvatarRepository = Objects.requireNonNull(usuarioAvatarRepository);
    }

    public record AvatarReadResult(ObjectReadResult content, String sha256) {}

    @Transactional(readOnly = true)
    public AvatarReadResult read(Long userId, HttpServletRequest request) throws IOException {
        var current = currentUserService.getRequired();
        if (!Objects.equals(current.getId(), userId)) {
            var summary = officeWorkspaceDashboardService.currentSummary(request, null);
            boolean allowed = summary != null
                    && summary.members() != null
                    && summary.members().stream().anyMatch(item -> Objects.equals(item.userId(), userId));
            if (!allowed) {
                throw new IllegalStateException("office_team_avatar_not_allowed");
            }
        }
        UsuarioAvatar meta = usuarioAvatarRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new IllegalStateException("office_team_avatar_not_allowed"));
        return new AvatarReadResult(userAvatarService.read(userId), meta.getSha256());
    }
}
