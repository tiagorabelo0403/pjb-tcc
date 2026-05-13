package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Service
public class InstitutionalSessionSecuritySignalService {

    private final IdentidadeJuridicaNacionalService identidadeService;

    public InstitutionalSessionSecuritySignalService(IdentidadeJuridicaNacionalService identidadeService) {
        this.identidadeService = Objects.requireNonNull(identidadeService);
    }

    public InstitutionalSessionSecuritySignal collect(Usuario usuario) {
        IdentidadeJuridicaNacional.GovBrNivel govbr = resolveGovBrNivel(usuario);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpServletRequest request = currentRequest();
        boolean mfa = resolveMfa(authentication);
        boolean managedLogin = resolveManagedInstitutionalLogin(request, authentication);
        boolean cert = resolveCertificate(request);
        boolean trustedNetwork = resolveTrustedNetwork(request);
        boolean remoteApproved = headerEquals(request, "X-PJB-REMOTE-CERT-AUTH", "true") || headerEquals(request, "X-PJB-REMOTE-CERT-AUTHORIZED", "true");
        boolean deviceApproved = headerEquals(request, "X-PJB-DEVICE-HOMOLOGATED", "true") || headerEquals(request, "X-PJB-TRUSTED-DEVICE", "true");
        ArrayList<String> evidence = new ArrayList<>();
        evidence.add("govbr=" + govbr.name());
        if (mfa) evidence.add("mfa_ativo");
        if (managedLogin) evidence.add("login_institucional_gerenciado");
        if (cert) evidence.add("certificado_detectado");
        if (trustedNetwork) evidence.add("rede_institucional");
        if (remoteApproved) evidence.add("autorizacao_remota_certificado");
        if (deviceApproved) evidence.add("dispositivo_homologado");
        return new InstitutionalSessionSecuritySignal(govbr, mfa, managedLogin, cert, trustedNetwork, remoteApproved, deviceApproved, List.copyOf(evidence));
    }

    private IdentidadeJuridicaNacional.GovBrNivel resolveGovBrNivel(Usuario usuario) {
        if (usuario == null || usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            return IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO;
        }
        return identidadeService.buscarPorDocumento(usuario.getCpf())
                .map(IdentidadeJuridicaNacional::getGovBrNivel)
                .orElse(IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO);
    }

    private boolean resolveMfa(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object amr = jwtAuth.getToken().getClaims().get("amr");
            if (containsMfa(amr)) return true;
            String acr = stringClaim(jwtAuth, "acr");
            String aal = stringClaim(jwtAuth, "aal");
            String loa = stringClaim(jwtAuth, "loa");
            return containsAny(acr, "mfa", "2fa", "silver", "gold", "prata", "ouro")
                    || containsAny(aal, "2", "3")
                    || containsAny(loa, "2", "3");
        }
        return false;
    }

    private boolean containsMfa(Object amr) {
        if (amr instanceof String s) {
            return containsAny(s, "mfa", "otp", "totp", "2fa");
        }
        if (amr instanceof Collection<?> c) {
            for (Object item : c) {
                if (item != null && containsAny(String.valueOf(item), "mfa", "otp", "totp", "2fa")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String stringClaim(JwtAuthenticationToken auth, String key) {
        String claim = auth.getToken().getClaimAsString(key);
        return claim == null ? "" : claim;
    }


    private boolean resolveManagedInstitutionalLogin(HttpServletRequest request, Authentication authentication) {
        if (headerEquals(request, "X-PJB-INSTITUTIONAL-LOGIN", "true")
                || headerPresent(request, "X-PJB-INSTITUTIONAL-ACCOUNT")
                || headerPresent(request, "X-PJB-INSTITUTIONAL-LOGIN-ID")) {
            return true;
        }
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch(authority -> containsAny(authority, "institutional_login", "institucional_login", "institution_managed"));
    }

    private boolean resolveCertificate(HttpServletRequest request) {
        return headerPresent(request, "X-SSL-Client-Verify")
                || headerPresent(request, "X-PJB-Client-Cert-Serial")
                || headerPresent(request, "X-Client-Certificate")
                || headerPresent(request, "X-SSL-CERT");
    }

    private boolean resolveTrustedNetwork(HttpServletRequest request) {
        if (headerEquals(request, "X-PJB-INSTITUTIONAL-NETWORK", "trusted")) {
            return true;
        }
        if (request == null) return false;
        String remote = request.getRemoteAddr();
        if (remote == null) return false;
        return remote.startsWith("10.")
                || remote.startsWith("192.168.")
                || remote.startsWith("172.16.")
                || remote.startsWith("172.17.")
                || remote.startsWith("172.18.")
                || remote.startsWith("172.19.")
                || remote.startsWith("172.2")
                || remote.startsWith("127.")
                || remote.equals("0:0:0:0:0:0:0:1")
                || remote.equals("::1");
    }

    private boolean headerPresent(HttpServletRequest request, String name) {
        return request != null && request.getHeader(name) != null && !request.getHeader(name).isBlank();
    }

    private boolean headerEquals(HttpServletRequest request, String name, String expected) {
        if (request == null) return false;
        String value = request.getHeader(name);
        return value != null && value.equalsIgnoreCase(expected);
    }

    private boolean containsAny(String raw, String... values) {
        if (raw == null) return false;
        String token = raw.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (token.contains(value.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }

    public record InstitutionalSessionSecuritySignal(
            IdentidadeJuridicaNacional.GovBrNivel govBrNivel,
            boolean mfaAtivo,
            boolean loginInstitucionalGerenciado,
            boolean certificadoICPDetectado,
            boolean redeInstitucionalConfiavel,
            boolean autorizacaoRemotaCertificado,
            boolean dispositivoHomologado,
            List<String> evidencias
    ) {
        public boolean govBrPrataOuOuro() {
            return govBrNivel == IdentidadeJuridicaNacional.GovBrNivel.PRATA || govBrNivel == IdentidadeJuridicaNacional.GovBrNivel.OURO;
        }
    }
}
