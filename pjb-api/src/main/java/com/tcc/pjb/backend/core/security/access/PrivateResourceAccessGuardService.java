package com.tcc.pjb.backend.core.security.access;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class PrivateResourceAccessGuardService {

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;

    public PrivateResourceAccessGuardService(CurrentUserService currentUserService,
                                             PjbAuthorizationService authorizationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public Usuario requireCurrentUser() {
        return currentUserService.getRequired();
    }

    public void requireReadProcesso(Processo processo) {
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
    }

    public void requireOwnerOrPrivileged(Long ownerUserId, Processo processo, String resourceLabel) {
        Usuario actor = requireCurrentUser();
        if (matches(actor, ownerUserId)) {
            requireReadProcesso(processo);
            return;
        }
        if (!isPrivileged(actor)) {
            throw new AccessDeniedPjbException("Acesso negado ao recurso privado: " + defaultLabel(resourceLabel));
        }
        requireReadProcesso(processo);
    }

    public void requireParticipantOrPrivilegedOrReadProcesso(Long participantUserId, Processo processo, String resourceLabel) {
        Usuario actor = requireCurrentUser();
        if (matches(actor, participantUserId)) {
            requireReadProcesso(processo);
            return;
        }
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
            return;
        }
        if (!isPrivileged(actor)) {
            throw new AccessDeniedPjbException("Acesso negado ao recurso privado: " + defaultLabel(resourceLabel));
        }
    }

    private boolean matches(Usuario actor, Long ownerUserId) {
        return actor != null && actor.getId() != null && ownerUserId != null && actor.getId().equals(ownerUserId);
    }

    private boolean isPrivileged(Usuario actor) {
        TipoUsuario tipo = actor != null ? actor.getTipoUsuario() : null;
        return tipo != null && (tipo.isAdmin() || tipo.isServidorJudiciario() || tipo.isMagistratura() || tipo.isAssessor());
    }

    private String defaultLabel(String resourceLabel) {
        return resourceLabel == null || resourceLabel.isBlank() ? "recurso" : resourceLabel.trim();
    }
}
