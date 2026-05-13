package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePresence;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeWorkspacePresenceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspacePresenceService {

    static final Duration ONLINE_WINDOW = Duration.ofMinutes(2);
    private static final Duration WRITE_THROTTLE = Duration.ofSeconds(15);

    private final CurrentUserService currentUserService;
    private final AdvOfficeWorkspacePresenceRepository presenceRepository;

    public OfficeWorkspacePresenceService(CurrentUserService currentUserService,
                                          AdvOfficeWorkspacePresenceRepository presenceRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.presenceRepository = Objects.requireNonNull(presenceRepository);
    }

    @Transactional
    public void touchCurrentWorkspace(String sourcePath) {
        Usuario usuario = currentUserService.getOrNull();
        MembroEquipe membro = EquipeContexto.getMembroDaEquipeAtiva();
        if (usuario == null || usuario.getId() == null || membro == null || membro.getEquipe() == null || membro.getEquipe().getId() == null || !membro.isAtivo()) {
            return;
        }
        Instant now = Instant.now();
        AdvOfficeWorkspacePresence presence = presenceRepository.findByEquipe_IdAndUserId(membro.getEquipe().getId(), usuario.getId()).orElseGet(() -> {
            AdvOfficeWorkspacePresence entity = new AdvOfficeWorkspacePresence();
            entity.setEquipe(membro.getEquipe());
            entity.setUserId(usuario.getId());
            entity.setCreatedAt(now);
            return entity;
        });
        if (presence.getLastSeenAt() != null
                && presence.getLastSeenAt().isAfter(now.minus(WRITE_THROTTLE))
                && Objects.equals(normalizePath(sourcePath), presence.getSourcePath())) {
            return;
        }
        presence.setMembroEquipeId(membro.getId());
        presence.setOfficeMode("OFFICE");
        presence.setSourcePath(normalizePath(sourcePath));
        presence.setLastSeenAt(now);
        presence.setUpdatedAt(now);
        presenceRepository.save(presence);
    }

    @Transactional(readOnly = true)
    public Instant onlineCutoff() {
        return Instant.now().minus(ONLINE_WINDOW);
    }

    private String normalizePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }
        String normalized = sourcePath.trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }
}
