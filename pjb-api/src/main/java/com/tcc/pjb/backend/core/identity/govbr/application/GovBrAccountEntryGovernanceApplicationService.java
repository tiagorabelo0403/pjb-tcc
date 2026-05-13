package com.tcc.pjb.backend.core.identity.govbr.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedCurrentEntryClosure;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrAccountEntryGovernanceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GovBrAccountEntryGovernanceApplicationService {

    private final GovBrOidcProperties props;
    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService;

    public GovBrAccountEntryGovernanceApplicationService(GovBrOidcProperties props,
                                                         CurrentUserService currentUserService,
                                                         UserSecurityProfileRepository userSecurityProfileRepository,
                                                         InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService) {
        this.props = Objects.requireNonNull(props);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
        this.delegatedGovernanceClosureApplicationService = Objects.requireNonNull(delegatedGovernanceClosureApplicationService);
    }

    public GovBrAccountEntryGovernanceAggregate atual() {
        Usuario usuario = currentUserService.getOrNull();
        UserSecurityProfile profile = usuario == null || usuario.getId() == null
                ? null
                : userSecurityProfileRepository.findByUserId(usuario.getId()).orElse(null);
        InstitutionalDelegatedCurrentEntryClosure closure = delegatedGovernanceClosureApplicationService.entradaAtual();

        String redirectPrincipalHost = host(props.redirectUri());
        String redirectStepUpHost = host(props.effectiveStepUpRedirectUri());
        boolean redirectPrincipalSeguro = isSecureCallback(props.redirectUri());
        boolean redirectStepUpSeguro = isSecureCallback(props.effectiveStepUpRedirectUri());
        boolean dominiosOficiaisCompativeis = isOfficialOrLocal(redirectPrincipalHost) && isOfficialOrLocal(redirectStepUpHost);

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> garantias = new LinkedHashSet<>();

        boolean authorizeConfigured = hasText(props.authorizeUrl());
        boolean tokenConfigured = hasText(props.tokenUrl());
        boolean userInfoConfigured = hasText(props.userinfoUrl());
        boolean jwksConfigured = hasText(props.jwksUrl());
        boolean issuerConfigured = hasText(props.issuer());

        if (props.enabled()) {
            if (!authorizeConfigured) blockers.add("GOVBR_AUTHORIZE_URL_MISSING");
            if (!tokenConfigured) blockers.add("GOVBR_TOKEN_URL_MISSING");
            if (!userInfoConfigured) blockers.add("GOVBR_USERINFO_URL_MISSING");
            if (!props.mockEnabled() && !jwksConfigured) blockers.add("GOVBR_JWKS_URL_MISSING");
            if (!props.mockEnabled() && !issuerConfigured) blockers.add("GOVBR_ISSUER_MISSING");
            if (!redirectPrincipalSeguro) blockers.add("GOVBR_REDIRECT_URI_INSECURE");
            if (!redirectStepUpSeguro) blockers.add("GOVBR_STEPUP_REDIRECT_URI_INSECURE");
            if (!props.mockEnabled() && !dominiosOficiaisCompativeis) blockers.add("GOVBR_OFFICIAL_DOMAIN_REQUIRED");
        }

        if (props.enabled() && props.mockEnabled()) {
            warnings.add("GOVBR_MOCK_ENABLED");
        }
        if (props.enabled() && usuario != null && (profile == null || profile.getGovVerifiedAt() == null)) {
            warnings.add("USUARIO_SEM_VINCULO_GOVBR_VALIDADO");
        }
        if (closure != null && !closure.possuiContextoDelegadoAtivo()) {
            warnings.add("ENTRADA_INSTITUCIONAL_DELEGADA_AINDA_NAO_FECHADA");
        }

        if (props.enabled()) {
            garantias.add("LOGIN_UNICO_COM_PKCE");
            garantias.add("VALIDACAO_DE_NONCE_NO_CALLBACK");
            garantias.add("VALIDACAO_DE_ID_TOKEN_COM_JWKS");
        }
        if (profile != null && profile.getGovVerifiedAt() != null) {
            garantias.add("CONTA_GOVBR_VINCULADA_AO_USUARIO_ATUAL");
        }
        if (closure != null && closure.possuiContextoDelegadoAtivo()) {
            garantias.add("ENTRADA_INSTITUCIONAL_RESOLVIDA_POR_ORGAO_UNIDADE_CAIXA_E_CAPACIDADE");
        }

        return new GovBrAccountEntryGovernanceAggregate(
                props.enabled(),
                props.mockEnabled(),
                authorizeConfigured,
                tokenConfigured,
                userInfoConfigured,
                jwksConfigured,
                issuerConfigured,
                redirectPrincipalSeguro,
                redirectStepUpSeguro,
                dominiosOficiaisCompativeis,
                usuario == null ? null : usuario.getId(),
                profile != null && profile.getGovVerifiedAt() != null,
                profile != null && profile.isGovEmailVerified(),
                profile != null && profile.isGovPhoneVerified(),
                closure != null && closure.possuiContextoDelegadoAtivo(),
                redirectPrincipalHost,
                redirectStepUpHost,
                closure == null ? List.of() : closure.contextosDelegados(),
                List.copyOf(blockers),
                List.copyOf(warnings),
                List.copyOf(garantias),
                Instant.now()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isSecureCallback(String value) {
        String host = host(value);
        if (host == null) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.")) {
                return true;
            }
            return "https".equalsIgnoreCase(scheme);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isOfficialOrLocal(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || normalized.startsWith("127.")) {
            return true;
        }
        return props.officialProductionDomainSuffixes().stream().anyMatch(normalized::endsWith);
    }

    private String host(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String host = URI.create(value.trim()).getHost();
            return host == null || host.isBlank() ? null : host.toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return null;
        }
    }
}
