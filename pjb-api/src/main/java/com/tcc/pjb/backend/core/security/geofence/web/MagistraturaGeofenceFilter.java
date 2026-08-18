package com.tcc.pjb.backend.core.security.geofence.web;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService.Avaliacao;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService.Decisao;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class MagistraturaGeofenceFilter extends OncePerRequestFilter {

    private final MagistraturaGeofencePolicyService policyService;
    private final CurrentUserService currentUserService;
    private final ClientIpResolver clientIpResolver;
    private final AuditLedgerService auditLedgerService;

    public MagistraturaGeofenceFilter(MagistraturaGeofencePolicyService policyService,
                                       CurrentUserService currentUserService,
                                       ClientIpResolver clientIpResolver,
                                       AuditLedgerService auditLedgerService) {
        this.policyService = Objects.requireNonNull(policyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Usuario u = currentUserService.getOrNull();
        if (u == null || u.getTipoUsuario() == null || !u.getTipoUsuario().requiresHardwareAuthAssurance()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIpResolver.resolve(request);
        Avaliacao avaliacao = policyService.avaliar(u, ip);

        auditLedgerService.appendSafely("CARREIRA_JURIDICA_GEOFENCE", "SECURITY", String.valueOf(u.getId()),
                avaliacao.decisao().name() + (avaliacao.motivo() != null ? " " + avaliacao.motivo() : ""));

        if (avaliacao.decisao() == Decisao.BLOQUEADO_VPN) {
            deny(response, "PJB_VPN_DETECTADA_DESATIVE");
            return;
        }
        if (avaliacao.decisao() == Decisao.BLOQUEADO_PAIS || avaliacao.decisao() == Decisao.BLOQUEADO_UF) {
            deny(response, "PJB_GEO_FORA_DE_ESCOPO");
            return;
        }
        if (avaliacao.decisao() == Decisao.INDISPONIVEL) {
            deny(response, "PJB_GEO_INDISPONIVEL");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void deny(HttpServletResponse response, String code) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
