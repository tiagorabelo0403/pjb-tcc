package com.tcc.pjb.backend.service.security.access;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

@Service
public class PersonalProcessAccessGuardService {

    private static final Duration STRONG_AUTH_FRESHNESS = Duration.ofHours(12);

    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository securityProfileRepository;

    public PersonalProcessAccessGuardService(CurrentUserService currentUserService,
                                             UserSecurityProfileRepository securityProfileRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.securityProfileRepository = Objects.requireNonNull(securityProfileRepository);
    }

    public PersonalProcessAccessEnvelope requireOwnProcessAccess(String capability) {
        PersonalProcessAccessEnvelope envelope = resolveOwnProcessAccess(capability);
        if (!envelope.allowed()) {
            throw new AccessDeniedPjbException(envelope.denialMessage());
        }
        return envelope;
    }

    public PersonalProcessAccessEnvelope resolveOwnProcessAccess(String capability) {
        Usuario usuario = currentUserService.getRequired();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserSecurityProfile profile = usuario.getId() == null
                ? null
                : securityProfileRepository.findByUsuarioId(usuario.getId()).orElse(null);
        AccessSignals signals = extractSignals(authentication, profile);
        boolean criticalProfile = isCriticalProfile(usuario);
        boolean profileSupportsPersonalGovContext = supportsPersonalGovContext(usuario);
        boolean govEnough = signals.hasGovBrContext();
        boolean strongEnough = !criticalProfile || signals.hasStrongAuth();
        boolean deviceEnough = !criticalProfile || signals.hasDeviceBinding();
        boolean personalModeResolved = profileSupportsPersonalGovContext || signals.hasGovBrContext();

        LinkedHashSet<String> evidences = new LinkedHashSet<>();
        if (signals.hasGovBrContext()) {
            evidences.add("GOVBR_SESSION");
        }
        if (signals.profileGovVerified()) {
            evidences.add("GOV_PROFILE_VERIFIED");
        }
        if (signals.hasStrongAuth()) {
            evidences.add("STRONG_AUTH");
        }
        if (signals.profileStrongAuthFresh()) {
            evidences.add("PROFILE_STRONG_AUTH_FRESH");
        }
        if (signals.hasDeviceBinding()) {
            evidences.add("DEVICE_BOUND");
        }
        if (signals.profileTotpEnabled()) {
            evidences.add("PROFILE_MFA_ENROLLED");
        }
        if (signals.hasCertificateContext()) {
            evidences.add("CERTIFICATE_CONTEXT");
        }

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> nextActions = new LinkedHashSet<>();
        if (!govEnough) {
            blockers.add("LOGIN_GOVBR_REQUIRED");
            nextActions.add("Entre no modo pessoal com Gov.br em /api/v1/auth/govbr/authorize antes de consultar processos próprios.");
        }
        if (signals.hasCertificateContext() && !signals.hasGovBrContext()) {
            blockers.add("PERSONAL_CONTEXT_CANNOT_USE_CERTIFICATE_ONLY");
            nextActions.add("Troque do contexto profissional certificado para o contexto pessoal Gov.br antes de consultar processos próprios.");
        }
        if (!personalModeResolved) {
            blockers.add("PERSONAL_IDENTITY_CONTEXT_UNRESOLVED");
            nextActions.add("Vincule a identidade civil Gov.br ao usuário antes de abrir o painel de processos pessoais.");
        }
        if (criticalProfile && !strongEnough) {
            blockers.add("REINFORCED_STRONG_AUTH_REQUIRED");
            nextActions.add("Perfis críticos precisam de MFA, passkey ou autenticação forte recente para consultar processos próprios.");
        }
        if (criticalProfile && !deviceEnough) {
            blockers.add("REINFORCED_DEVICE_BINDING_REQUIRED");
            nextActions.add("Perfis críticos precisam usar dispositivo vinculado, passkey aprovada ou trilha forte equivalente no modo pessoal.");
        }

        String mode = signals.hasGovBrContext() ? "GOVBR_PESSOAL" : signals.hasCertificateContext() ? "CERT_PROFISSIONAL" : "UNRESOLVED";
        boolean allowed = blockers.isEmpty();
        return new PersonalProcessAccessEnvelope(
                allowed,
                capability == null || capability.isBlank() ? "OWN_PROCESS_ACCESS" : capability.trim().toUpperCase(Locale.ROOT),
                mode,
                criticalProfile,
                signals.hasGovBrContext(),
                signals.hasStrongAuth(),
                signals.hasDeviceBinding(),
                List.copyOf(evidences),
                List.copyOf(blockers),
                List.copyOf(nextActions),
                denialMessage(usuario, capability, mode, blockers, nextActions)
        );
    }

    private String denialMessage(Usuario usuario,
                                 String capability,
                                 String mode,
                                 Collection<String> blockers,
                                 Collection<String> nextActions) {
        StringBuilder builder = new StringBuilder();
        builder.append("Acesso pessoal ao processo bloqueado");
        if (capability != null && !capability.isBlank()) {
            builder.append(" para ").append(capability.trim().toUpperCase(Locale.ROOT));
        }
        builder.append(": perfil=")
                .append(usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "NAO_CLASSIFICADO")
                .append(", modo=")
                .append(mode)
                .append(", bloqueios=")
                .append(String.join(",", blockers));
        if (!nextActions.isEmpty()) {
            builder.append(". Próxima ação: ").append(nextActions.iterator().next());
        }
        return builder.toString();
    }


    public void requireCurrentUserAsParty(com.tcc.pjb.backend.model.entity.Processo processo) {
        Usuario usuario = currentUserService.getRequired();
        String cpf = usuario.getCpf();
        if (cpf == null || cpf.isBlank() || processo == null) {
            throw new AccessDeniedPjbException("Acesso pessoal ao processo bloqueado: usuário sem CPF civil válido ou processo ausente.");
        }
        boolean linked = cpf.equals(processo.getParteAutoraCpf())
                || cpf.equals(processo.getParteReuCpf())
                || (processo.getUsuario() != null && cpf.equals(processo.getUsuario().getCpf()));
        if (!linked) {
            throw new AccessDeniedPjbException("Acesso pessoal ao processo bloqueado: a identidade civil autenticada não está vinculada ao processo informado.");
        }
    }

    private boolean supportsPersonalGovContext(Usuario usuario) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return false;
        }
        return tipo == TipoUsuario.CIDADAO
                || tipo.isAdvocacia()
                || tipo.isPerito()
                || tipo.isAuxiliarJustica()
                || tipo.isServidorJudiciario()
                || tipo.isMagistratura()
                || tipo.isMinisterioPublico()
                || tipo.isDefensoriaPublica()
                || tipo.isProcuradoria()
                || tipo.isCartorioExtrajudicial()
                || tipo.isSegurancaPublica();
    }

    private boolean isCriticalProfile(Usuario usuario) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return false;
        }
        if (tipo.isMagistratura() || tipo.isMinisterioPublico()) {
            return true;
        }
        return tipo == TipoUsuario.DESEMBARGADOR
                || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL
                || tipo == TipoUsuario.MINISTRO
                || tipo == TipoUsuario.PROMOTOR_ELEITORAL
                || tipo == TipoUsuario.PROMOTOR_TRABALHISTA
                || tipo == TipoUsuario.PROCURADOR_GERAL_REPUBLICA;
    }

    private AccessSignals extractSignals(Authentication authentication, UserSecurityProfile profile) {
        Jwt jwt = authentication instanceof JwtAuthenticationToken token ? token.getToken() : null;
        Map<String, Object> claims = jwt != null ? jwt.getClaims() : Map.of();
        Set<String> authorities = authentication == null
                ? Set.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .map(v -> v.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> amr = toUpperList(claims.get("amr"));
        List<String> acr = new ArrayList<>(toUpperList(claims.get("acr")));
        String acrSingle = upper(textClaim(claims, "acr"));
        if (acrSingle != null && !acrSingle.isBlank()) {
            acr.add(acrSingle);
        }
        List<String> channels = new ArrayList<>();
        channels.add(upper(textClaim(claims, "auth_source")));
        channels.add(upper(textClaim(claims, "auth_channel")));
        channels.add(upper(textClaim(claims, "identity_provider")));
        channels.add(upper(textClaim(claims, "idp")));
        channels.add(upper(textClaim(claims, "login_strategy")));
        channels.add(upper(textClaim(claims, "profile_context")));
        channels.add(upper(textClaim(claims, "session_context")));
        boolean govHint = containsGovHint(channels)
                || claims.containsKey("govbr_sub")
                || claims.containsKey("govbr_account_id")
                || truthy(claims.get("govbr"))
                || truthy(claims.get("govbr_authenticated"));
        boolean certificateHint = containsAny(channels, List.of("CERT", "CERTIFICATE", "OAB", "ICP", "PROFISSIONAL"))
                || authorities.stream().anyMatch(v -> v.contains("CERT") || v.contains("OAB"))
                || hasText(textClaim(claims, "certificate_fingerprint"))
                || hasText(textClaim(claims, "cert_fp"))
                || hasText(textClaim(claims, "certificate_alias"));
        boolean strongClaim = containsAny(amr, List.of("MFA", "PWD+MFA", "PASSKEY", "WEBAUTHN", "OTP", "TOTP", "FIDO2"))
                || containsAny(acr, List.of("AAL2", "AAL3", "MFA", "PASSKEY", "WEBAUTHN"))
                || truthy(claims.get("mfa"))
                || truthy(claims.get("strong_auth"));
        boolean deviceClaim = truthy(claims.get("device_bound"))
                || truthy(claims.get("bound_device"))
                || truthy(claims.get("trusted_device"))
                || truthy(claims.get("passkey_device"));
        boolean profileGovVerified = profile != null && profile.getGovVerifiedAt() != null;
        boolean profileStrongFresh = profile != null && profile.getLastStrongAuthAt() != null
                && !profile.getLastStrongAuthAt().isBefore(LocalDateTime.now().minus(STRONG_AUTH_FRESHNESS));
        boolean profileTotpEnabled = profile != null && profile.isTotpEnabled();
        boolean govContext = govHint || (profileGovVerified && !certificateHint);
        boolean strongAuth = strongClaim || profileStrongFresh;
        boolean deviceBinding = deviceClaim || (profileGovVerified && profileTotpEnabled);
        return new AccessSignals(
                govContext,
                strongAuth,
                deviceBinding,
                certificateHint,
                profileGovVerified,
                profileStrongFresh,
                profileTotpEnabled,
                List.copyOf(channels.stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).toList()),
                List.copyOf(amr),
                List.copyOf(authorities)
        );
    }

    private static boolean containsGovHint(Collection<String> values) {
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (value.contains("GOV") || value.contains("BR_OIDC") || value.contains("OIDC_GOV")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(Collection<String> values, Collection<String> tokens) {
        for (String value : values) {
            if (value == null) {
                continue;
            }
            for (String token : tokens) {
                if (value.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> toUpperList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).map(PersonalProcessAccessGuardService::upper)
                    .filter(Objects::nonNull).filter(v -> !v.isBlank()).toList();
        }
        String text = String.valueOf(raw);
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(upper(text));
    }

    private static String textClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean truthy(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return "true".equals(text) || "1".equals(text) || "yes".equals(text) || "sim".equals(text);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public record PersonalProcessAccessEnvelope(boolean allowed,
                                                String capability,
                                                String accessMode,
                                                boolean criticalProfile,
                                                boolean govBrAuthenticated,
                                                boolean strongAuth,
                                                boolean deviceBound,
                                                List<String> evidences,
                                                List<String> blockers,
                                                List<String> nextActions,
                                                String denialMessage) {
    }

    private record AccessSignals(boolean hasGovBrContext,
                                 boolean hasStrongAuth,
                                 boolean hasDeviceBinding,
                                 boolean hasCertificateContext,
                                 boolean profileGovVerified,
                                 boolean profileStrongAuthFresh,
                                 boolean profileTotpEnabled,
                                 List<String> channels,
                                 List<String> amr,
                                 List<String> authorities) {
    }
}
