package com.tcc.pjb.backend.core.security.magistratura.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.*;
import com.tcc.pjb.backend.model.entity.Usuario;

public class DelegationTokenAugmentationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DelegationTokenAugmentationFilter.class);

    public static final String HEADER_DCP = "X-PJB-Delegation";
    public static final String HEADER_DEVICE_BINDING = "X-PJB-Device-Binding";

    public static final String ROLE_JUIZ_DELEGADO = "ROLE_JUIZ_DELEGADO";

    private final DelegationTokenService tokenService;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;
    private final DelegationValidationService validationService;

    public DelegationTokenAugmentationFilter(DelegationTokenService tokenService,
                                             CurrentUserService currentUserService,
                                             AuditLedgerService auditLedgerService,
                                             DelegationValidationService validationService) {
        this.tokenService = tokenService;
        this.currentUserService = currentUserService;
        this.auditLedgerService = auditLedgerService;
        this.validationService = validationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(HEADER_DCP);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                deny(response);
                return;
            }

            Usuario delegate = currentUserService.getRequired();
            DelegationTokenPayload payload = tokenService.verifyAndDecode(token);
            DelegationCredential credential = validationService.validateForDelegate(
                    payload,
                    delegate,
                    request.getHeader(HEADER_DEVICE_BINDING)
            );
            RequestContext.setDelegationCredential(credential);

            
            SecurityContextHolder.getContext().setAuthentication(withExtraAuthority(auth, ROLE_JUIZ_DELEGADO));

            auditLedgerService.appendSafely(
                    "DELEGATION_USED",
                    "DCP",
                    payload.jti(),
                    "delegate=" + delegate.getId() + " magistrate=" + payload.magistrateId() + " scope=" + credential.scope().name()
            );

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            
            log.warn("DCP inválido/negado: {}", e.getMessage());
            deny(response);
        } finally {
            
            
            RequestContext.setDelegationCredential(null);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private static Authentication withExtraAuthority(Authentication original, String authority) {
        List<GrantedAuthority> authorities = new ArrayList<>(original.getAuthorities());
        SimpleGrantedAuthority extra = new SimpleGrantedAuthority(authority);
        boolean present = authorities.stream().anyMatch(existing -> authority.equals(existing.getAuthority()));
        if (!present) {
            authorities.add(extra);
        }
        return new UsernamePasswordAuthenticationToken(original.getPrincipal(), original.getCredentials(), authorities);
    }

    private static void deny(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"delegation_denied\"}");
    }
}
