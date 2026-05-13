package com.tcc.pjb.backend.service.profile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.profile.GovBrIdentityShieldResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

@Service
public class GovBrIdentityShieldService {

    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final GovBrOidcProperties properties;

    public GovBrIdentityShieldService(CurrentUserService currentUserService,
                                      UserSecurityProfileRepository userSecurityProfileRepository,
                                      GovBrOidcProperties properties) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
        this.properties = Objects.requireNonNull(properties);
    }

    public GovBrIdentityShieldResponse currentPosture() {
        Usuario actor = currentUserService.getRequired();
        UserSecurityProfile profile = actor.getId() == null ? null : userSecurityProfileRepository.findByUserId(actor.getId()).orElse(null);
        boolean vinculado = profile != null && profile.getGovVerifiedAt() != null;
        String nivelConfianca = resolveConfidence(profile);
        boolean stepUpRequerido = requireStepUp(actor, profile);
        List<String> controles = new ArrayList<>();
        if (properties.enabled()) {
            controles.add("SSO_GOVBR_FEDERADO");
            controles.add("OIDC_PKCE");
        }
        if (profile != null && profile.isTotpEnabled()) {
            controles.add("TOTP_ATIVO");
        }
        if (vinculado) {
            controles.add("VINCULO_GOVBR_VALIDADO");
        }
        if (profile != null && profile.getLastStrongAuthAt() != null) {
            controles.add("HISTORICO_AUTENTICACAO_FORTE");
        }
        List<String> pendencias = new ArrayList<>();
        if (!properties.enabled()) {
            pendencias.add("Integração Gov.br desabilitada no ambiente atual.");
        }
        if (!vinculado && properties.enabled()) {
            pendencias.add("Usuário ainda não vinculou identidade federada Gov.br.");
        }
        if (stepUpRequerido) {
            pendencias.add("Sessão crítica exige reforço por step-up antes de operações sensíveis.");
        }
        return new GovBrIdentityShieldResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                properties.enabled(),
                vinculado,
                nivelConfianca,
                stepUpRequerido,
                "/api/v1/auth/govbr/stepup/start",
                profile != null ? profile.getGovVerifiedAt() : null,
                profile != null ? profile.getLastStrongAuthAt() : null,
                List.copyOf(controles),
                List.copyOf(pendencias)
        );
    }

    private String resolveConfidence(UserSecurityProfile profile) {
        if (profile == null || profile.getGovVerifiedAt() == null) {
            return "NAO_VINCULADO";
        }
        if (profile.isGovEmailVerified() && profile.isGovPhoneVerified()) {
            return "OURO_OPERACIONAL";
        }
        if (profile.isGovEmailVerified() || profile.isGovPhoneVerified()) {
            return "PRATA_OPERACIONAL";
        }
        return "BRONZE_ASSISTIDO";
    }

    private boolean requireStepUp(Usuario actor, UserSecurityProfile profile) {
        if (!properties.enabled()) {
            return false;
        }
        if (actor.getTipoUsuario() != null && actor.getTipoUsuario().isPerfilCritico()) {
            if (profile == null || profile.getLastStrongAuthAt() == null) {
                return true;
            }
            return Duration.between(profile.getLastStrongAuthAt(), LocalDateTime.now()).toHours() >= 12;
        }
        return profile == null || profile.getGovVerifiedAt() == null;
    }
}
