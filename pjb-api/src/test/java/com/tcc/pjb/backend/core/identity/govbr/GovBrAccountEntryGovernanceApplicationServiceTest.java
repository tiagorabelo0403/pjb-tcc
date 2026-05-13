package com.tcc.pjb.backend.core.identity.govbr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedCurrentEntryClosure;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrAccountEntryGovernanceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrAccountEntryGovernanceAggregate;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GovBrAccountEntryGovernanceApplicationServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock UserSecurityProfileRepository profileRepository;
    @Mock InstitutionalDelegatedGovernanceClosureApplicationService closureService;

    @Test
    void deveApontarGovernancaProntaQuandoUsuarioJaPossuiContaGovBrEContextoDelegado() {
        GovBrOidcProperties props = new GovBrOidcProperties(
                true,
                false,
                "https://sso.gov.br/authorize",
                "https://sso.gov.br/token",
                "https://sso.gov.br/userinfo",
                "https://sso.gov.br/picture",
                "client",
                "",
                "https://pjb.jus.br/api/v1/cidadao/govbr/link/callback",
                "https://pjb.jus.br/api/v1/auth/govbr/stepup/callback",
                "openid email profile govbr_confiabilidades",
                "https://sso.gov.br/jwks",
                "https://sso.gov.br",
                "https://frontend.jus.br/success",
                "https://frontend.jus.br/error",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                Duration.ofMinutes(5)
        );
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setGovVerifiedAt(LocalDateTime.now());
        profile.setGovEmailVerified(true);
        profile.setGovPhoneVerified(true);

        when(currentUserService.getOrNull()).thenReturn(usuario);
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile));
        when(closureService.entradaAtual()).thenReturn(new InstitutionalDelegatedCurrentEntryClosure(
                9L,
                "IDENTITY",
                true,
                true,
                true,
                true,
                List.of("CIDADAO"),
                List.of("MPCE/FAMILIA/TRIAGEM"),
                List.of("ok"),
                Instant.now()
        ));

        GovBrAccountEntryGovernanceApplicationService service = new GovBrAccountEntryGovernanceApplicationService(props, currentUserService, profileRepository, closureService);
        GovBrAccountEntryGovernanceAggregate aggregate = service.atual();

        assertThat(aggregate.blockers()).isEmpty();
        assertThat(aggregate.contaGovBrVinculada()).isTrue();
        assertThat(aggregate.contextoInstitucionalPronto()).isTrue();
        assertThat(aggregate.dominiosOficiaisCompativeis()).isTrue();
    }
}
