package com.tcc.pjb.backend.core.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario getRequired() {
        return getOptional().orElseThrow(() -> new IllegalStateException("Usuário não autenticado ou não encontrado no banco."));
    }

    public Usuario get() {
        return getRequired();
    }

    public Usuario getOrNull() {
        return getOptional().orElse(null);
    }

    public long currentUserIdOrZero() {
        Usuario u = getOrNull();
        return u != null && u.getId() != null ? u.getId() : 0L;
    }

    public Optional<Usuario> currentUser() {
        return getOptional();
    }

    public Optional<Usuario> getOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();

        if (auth instanceof JwtAuthenticationToken jat) {
            Jwt jwt = jat.getToken();
            if (jwt != null) {
                Object uid = jwt.getClaims().get("uid");
                if (uid != null) {
                    String s = String.valueOf(uid).trim();
                    if (!s.isBlank()) {
                        try {
                            long id = Long.parseLong(s);
                            return usuarioRepository.findById(id);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                String cpf = jwt.getClaimAsString("cpf");
                if (cpf != null && !cpf.isBlank()) {
                    Optional<Usuario> byCpf = usuarioRepository.findByCpf(cpf.trim());
                    if (byCpf.isPresent()) return byCpf;
                }

                String email = jwt.getClaimAsString("email");
                if (email != null && !email.isBlank()) {
                    Optional<Usuario> byEmail = usuarioRepository.findByEmail(email.trim());
                    if (byEmail.isPresent()) return byEmail;
                }

                String sub = jwt.getSubject();
                if (sub != null && !sub.isBlank()) {
                    String s = sub.trim();
                    try {
                        long id = Long.parseLong(s);
                        return usuarioRepository.findById(id);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String key = auth.getName();
        if (key == null || key.isBlank()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String s) {
                key = s;
            } else {
                return Optional.empty();
            }
        }

        return findByKey(key.trim());
    }

    private Optional<Usuario> findByKey(String key) {
        Optional<Usuario> byCpf = usuarioRepository.findByCpf(key);
        if (byCpf.isPresent()) return byCpf;
        return usuarioRepository.findByEmail(key);
    }
}
