package com.tcc.pjb.backend.service.identity;

import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.repository.UsuarioAvatarRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserAvatarQueryService {

    private final UsuarioAvatarRepository usuarioAvatarRepository;

    public UserAvatarQueryService(UsuarioAvatarRepository usuarioAvatarRepository) {
        this.usuarioAvatarRepository = usuarioAvatarRepository;
    }

    public Optional<UsuarioAvatar> findByUsuarioId(Long usuarioId) {
        return usuarioAvatarRepository.findByUsuarioId(usuarioId);
    }
}
