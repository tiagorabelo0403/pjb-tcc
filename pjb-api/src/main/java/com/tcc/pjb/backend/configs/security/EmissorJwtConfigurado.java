package com.tcc.pjb.backend.configs.security;

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class EmissorJwtConfigurado {

    private static final List<String> PROPRIEDADES_DO_EMISSOR = List.of(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            "spring.security.oauth2.resourceserver.jwt.public-key-location");

    private EmissorJwtConfigurado() {
    }

    public static boolean presente(Environment environment) {
        return PROPRIEDADES_DO_EMISSOR.stream().anyMatch(chave -> StringUtils.hasText(environment.getProperty(chave)));
    }
}
