package com.tcc.pjb.backend.core.identity.govbr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrAccountEntryGovernanceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GovBrIdentityAssuranceApplicationServiceTest {

    @Test
    void elevaNivelQuandoHaContaVinculadaContextoFechadoEDispositivoConfiavel() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserSecurityProfileRepository profileRepository = mock(UserSecurityProfileRepository.class);
        TrustedDeviceRepository trustedDeviceRepository = mock(TrustedDeviceRepository.class);
        GovBrAccountEntryGovernanceApplicationService governanceService = mock(GovBrAccountEntryGovernanceApplicationService.class);
        GovBrOidcProperties props = new GovBrOidcProperties(true, false, "a", "t", "u", null, "cid", null, "https://app.jus.br/callback", "https://app.jus.br/stepup", null, "j", "iss", null, null, Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofMinutes(5));
        GovBrIdentityAssuranceApplicationService service = new GovBrIdentityAssuranceApplicationService(props, currentUserService, profileRepository, trustedDeviceRepository, governanceService);

        Usuario usuario = new Usuario();
        usuario.setId(77L);
        when(currentUserService.getOrNull()).thenReturn(usuario);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setGovVerifiedAt(LocalDateTime.now());
        profile.setGovEmailVerified(true);
        profile.setGovPhoneVerified(true);
        when(profileRepository.findByUserId(77L)).thenReturn(Optional.of(profile));
        TrustedDevice device = new TrustedDevice();
        device.setVerifiedAt(LocalDateTime.now());
        when(trustedDeviceRepository.findActiveByUser(77L)).thenReturn(List.of(device));
        when(governanceService.atual()).thenReturn(new GovBrAccountEntryGovernanceAggregate(
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                77L,
                true,
                true,
                true,
                true,
                "app.jus.br",
                "app.jus.br",
                List.of("MPCE/FAMILIA"),
                List.of(),
                List.of(),
                List.of("PKCE", "NONCE"),
                Instant.now()
        ));

        var aggregate = service.atual();

        assertThat(aggregate.nivelGarantia()).isEqualTo("ALTO");
        assertThat(aggregate.strongBindingReady()).isTrue();
        assertThat(aggregate.garantias()).contains("TRUSTED_DEVICE_ATIVO", "STRONG_BINDING_USUARIO_DISPOSITIVO_CONTEXTO");
    }
}
