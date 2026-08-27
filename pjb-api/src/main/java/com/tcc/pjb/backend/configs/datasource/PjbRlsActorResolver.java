package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.configs.security.UsuarioPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolve o ator autenticado (id numérico + papéis) a partir do contexto de segurança, sem tocar o
 * banco, para alimentar as GUCs de RLS por conexão ({@code app.pjb_actor_id} / {@code app.pjb_actor_roles}).
 *
 * <p>Estas GUCs são <b>dedicadas</b> e independentes das GUCs de sigilo ({@code app.pjb_unit_code} etc.),
 * de propósito: as policies de ator (support_ticket, judge_travel_exception, legal_ai_audit_log,
 * intimacao_audiencia) não podem interferir no read model de sigilo do processo (V221).</p>
 *
 * <p>Falha ou ausência de autenticação resolve para ator vazio ({@link #ANONYMOUS}) — e as policies
 * são permissivas quando o contexto de ator está vazio (jobs, boot, migrations, anônimo não quebram).</p>
 */
@Component
public class PjbRlsActorResolver {

    public static final ActorSettings ANONYMOUS = new ActorSettings("", "");

    private static final String UID_CLAIM = "uid";

    public ActorSettings currentOrAnonymous() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ANONYMOUS;
            }
            return new ActorSettings(resolveActorId(auth), resolveRoles(auth));
        } catch (RuntimeException ex) {
            return ANONYMOUS;
        }
    }

    private String resolveActorId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jat) {
            Jwt jwt = jat.getToken();
            if (jwt != null) {
                String uid = asNumeric(jwt.getClaims().get(UID_CLAIM));
                if (uid != null) {
                    return uid;
                }
                String sub = asNumeric(jwt.getSubject());
                if (sub != null) {
                    return sub;
                }
            }
        }
        if (auth.getPrincipal() instanceof UsuarioPrincipal up && up.getId() != null) {
            return String.valueOf(up.getId());
        }
        return "";
    }

    /** Papéis delimitados por barra vertical: {@code |ROLE_A|ROLE_B|}, para casamento posicional exato. */
    private String resolveRoles(Authentication auth) {
        StringBuilder sb = new StringBuilder();
        auth.getAuthorities().forEach(a -> {
            String authority = a == null ? null : a.getAuthority();
            if (authority != null && !authority.isBlank()) {
                sb.append('|').append(authority.trim());
            }
        });
        if (sb.isEmpty()) {
            return "";
        }
        return sb.append('|').toString();
    }

    private static String asNumeric(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        if (s.isBlank()) {
            return null;
        }
        try {
            Long.parseLong(s);
            return s;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record ActorSettings(String actorId, String roles) {
        public ActorSettings {
            actorId = actorId == null ? "" : actorId.trim();
            roles = roles == null ? "" : roles;
        }
    }
}
