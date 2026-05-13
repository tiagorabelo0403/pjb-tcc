package com.tcc.pjb.backend.core.security.mtls;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

@Configuration
public class MtlsGuardConfiguration {

    @Bean
    public ApplicationRunner mtlsGuard(MtlsProperties props, Environment env) {
        Objects.requireNonNull(props, "props");
        Objects.requireNonNull(env, "env");
        return args -> {
            if (!props.isEnabled()) return;
            boolean sslEnabled = Boolean.parseBoolean(env.getProperty("server.ssl.enabled", "false"));
            String clientAuth = env.getProperty("server.ssl.client-auth", "");
            if (!sslEnabled) throw new IllegalStateException("mTLS enabled but server.ssl.enabled=false");
            if (!"need".equalsIgnoreCase(clientAuth)) throw new IllegalStateException("mTLS enabled but server.ssl.client-auth!=need");
        };
    }
}
