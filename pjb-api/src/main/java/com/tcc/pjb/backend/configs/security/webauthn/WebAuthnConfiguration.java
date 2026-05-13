package com.tcc.pjb.backend.configs.security.webauthn;

import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.core.security.webauthn.WebAuthnCredentialRepository;
import com.tcc.pjb.backend.core.security.webauthn.WebAuthnProperties;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

@Configuration
public class WebAuthnConfiguration {

    @Bean
    public RelyingParty relyingParty(WebAuthnProperties props,
                                     WebAuthnCredentialRepository credentialRepository) {
        Objects.requireNonNull(props);
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(props.getRpId())
                .name(props.getRpName())
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .origins(props.getOrigins())
                .build();
    }
}
