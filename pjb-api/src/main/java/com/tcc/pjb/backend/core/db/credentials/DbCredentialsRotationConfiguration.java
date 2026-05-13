package com.tcc.pjb.backend.core.db.credentials;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Objects;

@Configuration
@ConditionalOnProperty(prefix = "pjb.db.credentials.rotation", name = "enabled", havingValue = "true")
public class DbCredentialsRotationConfiguration {

    @Bean
    public DbCredentialsProvider dbCredentialsProvider(DbCredentialsRotationProperties props,
                                                       ObjectMapper mapper,
                                                       @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        Objects.requireNonNull(props, "props");
        Objects.requireNonNull(mapper, "mapper");
        String p = props.getProvider() == null ? "" : props.getProvider().trim().toLowerCase(Locale.ROOT);
        if (!props.isEnabled()) return () -> { throw new IllegalStateException("rotation disabled"); };
        return switch (p) {
            case "vault" -> new VaultDbCredentialsProvider(props.getVaultUrl(), props.getVaultPath(), props.getVaultTokenEnv(), props.getRequestTimeout(), mapper, httpClient);
            default -> throw new IllegalStateException("unknown provider: " + p);
        };
    }
}
