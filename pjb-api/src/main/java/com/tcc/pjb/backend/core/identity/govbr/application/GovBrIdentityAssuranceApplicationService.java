package com.tcc.pjb.backend.core.identity.govbr.application;

import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrAccountEntryGovernanceAggregate;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GovBrIdentityAssuranceApplicationService {

    private final GovBrOidcProperties props;
    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final GovBrAccountEntryGovernanceApplicationService govBrAccountEntryGovernanceApplicationService;

    public GovBrIdentityAssuranceApplicationService(GovBrOidcProperties props,
                                                    CurrentUserService currentUserService,
                                                    UserSecurityProfileRepository userSecurityProfileRepository,
                                                    TrustedDeviceRepository trustedDeviceRepository,
                                                    GovBrAccountEntryGovernanceApplicationService govBrAccountEntryGovernanceApplicationService) {
        this.props = Objects.requireNonNull(props);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
        this.trustedDeviceRepository = Objects.requireNonNull(trustedDeviceRepository);
        this.govBrAccountEntryGovernanceApplicationService = Objects.requireNonNull(govBrAccountEntryGovernanceApplicationService);
    }

    public GovBrIdentityAssuranceAggregate atual() {
        GovBrAccountEntryGovernanceAggregate governance = govBrAccountEntryGovernanceApplicationService.atual();
        Usuario usuario = currentUserService.getOrNull();
        UserSecurityProfile profile = usuario == null || usuario.getId() == null
                ? null
                : userSecurityProfileRepository.findByUserId(usuario.getId()).orElse(null);
        List<TrustedDevice> dispositivos = usuario == null || usuario.getId() == null
                ? List.of()
                : trustedDeviceRepository.findActiveByUser(usuario.getId());

        boolean trustedDeviceAtivo = dispositivos.stream().anyMatch(device -> !device.isRevogado() && device.getVerifiedAt() != null);
        boolean tokenVerificationReady = governance.jwksConfigured() && governance.issuerConfigured() && governance.tokenConfigured();
        boolean strongBindingReady = governance.contaGovBrVinculada() && governance.contextoInstitucionalPronto() && trustedDeviceAtivo;

        LinkedHashSet<String> blockers = new LinkedHashSet<>(governance.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>(governance.warnings());
        LinkedHashSet<String> garantias = new LinkedHashSet<>(governance.garantias());

        if (props.enabled() && !trustedDeviceAtivo) {
            warnings.add("DISPOSITIVO_CONFIAVEL_AINDA_NAO_VALIDADO");
        }
        if (props.enabled() && profile != null && !profile.isGovPhoneVerified()) {
            warnings.add("TELEFONE_GOVBR_NAO_CONFIRMADO");
        }
        if (props.enabled() && profile != null && !profile.isGovEmailVerified()) {
            warnings.add("EMAIL_GOVBR_NAO_CONFIRMADO");
        }
        if (props.enabled() && !tokenVerificationReady) {
            blockers.add("VERIFICACAO_CRIPTOGRAFICA_GOVBR_INCOMPLETA");
        }
        if (strongBindingReady) {
            garantias.add("STRONG_BINDING_USUARIO_DISPOSITIVO_CONTEXTO");
        }
        if (trustedDeviceAtivo) {
            garantias.add("TRUSTED_DEVICE_ATIVO");
        }

        return new GovBrIdentityAssuranceAggregate(
                props.enabled(),
                governance.currentUserId(),
                governance.contaGovBrVinculada(),
                governance.contextoInstitucionalPronto(),
                governance.redirectPrincipalSeguro() && governance.redirectStepUpSeguro(),
                governance.dominiosOficiaisCompativeis(),
                tokenVerificationReady,
                trustedDeviceAtivo,
                strongBindingReady,
                nivelGarantia(blockers.isEmpty(), strongBindingReady, governance.contaGovBrVinculada()),
                List.copyOf(blockers),
                List.copyOf(warnings),
                List.copyOf(garantias),
                Instant.now()
        );
    }

    private String nivelGarantia(boolean semBloqueios, boolean strongBindingReady, boolean contaGovBrVinculada) {
        if (!props.enabled()) {
            return "DESABILITADO";
        }
        if (semBloqueios && strongBindingReady) {
            return "ALTO";
        }
        if (semBloqueios && contaGovBrVinculada) {
            return "SUBSTANCIAL";
        }
        return "BASICO";
    }
}
