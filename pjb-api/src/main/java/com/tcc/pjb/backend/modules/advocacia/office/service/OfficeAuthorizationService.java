package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;

@Service
public class OfficeAuthorizationService {

    private final MembroEquipeRepository membroEquipeRepository;

    public OfficeAuthorizationService(MembroEquipeRepository membroEquipeRepository) {
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
    }

    public MembroEquipe requireActiveMembership(Long userId, Long equipeId) {
        return membroEquipeRepository.findByUsuario_IdAndEquipe_Id(userId, equipeId)
                .filter(MembroEquipe::isAtivo)
                .orElseThrow(() -> new AccessDeniedException("Usuário não pertence à equipe."));
    }

    public void requireOfficeAdmin(Long userId, Long equipeId) {
        MembroEquipe m = requireActiveMembership(userId, equipeId);
        PapelEquipe p = m.getPapel();
        boolean ok = p == PapelEquipe.ADMINISTRADOR || p == PapelEquipe.COORDENADOR;
        if (!ok) {
            throw new AccessDeniedException("Sem permissão para administrar política do escritório.");
        }
    }

    public void requireSigner(Long currentUserId, Long signerUserId) {
        if (signerUserId == null || !signerUserId.equals(currentUserId)) {
            throw new AccessDeniedException("Ação permitida apenas ao signatário.");
        }
    }
}
