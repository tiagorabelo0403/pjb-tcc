package com.tcc.pjb.backend.configs.security;

import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

public class DbUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DbUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Usuário inválido");
        }
        String key = username.trim();

        Optional<Usuario> u = usuarioRepository.findByCpf(key);
        if (u.isEmpty()) {
            u = usuarioRepository.findByEmail(key);
        }
        Usuario usuario = u.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String password = normalizePassword(usuario.getSenha());

        var authorities = PjbGrantedAuthorityFactory.authoritiesFor(usuario.getTipoUsuario(), usuario.getEnteFederativo());
        return new User(key, password, usuario.isAtivo(), true, true, true, authorities);
    }

    private String normalizePassword(String stored) {
        if (stored == null || stored.isBlank()) {
            throw new IllegalStateException("Password hash missing");
        }
        if (stored.startsWith("{")) return stored;
        String s = stored.trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$")) {
            return "{bcrypt}" + s;
        }
        if (lower.startsWith("$argon2")) {
            return "{argon2}" + s;
        }
        throw new IllegalStateException("Unsupported password hash format");
    }
}
