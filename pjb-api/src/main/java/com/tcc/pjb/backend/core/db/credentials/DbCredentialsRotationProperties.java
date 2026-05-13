package com.tcc.pjb.backend.core.db.credentials;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "pjb.db.credentials.rotation")
public class DbCredentialsRotationProperties {

    @NotNull
    private boolean enabled = false;

    @NotNull
    private Duration refreshInterval = Duration.ofMinutes(10);

    @NotBlank
    private String provider = "vault";

    private String vaultUrl = "";

    @NotBlank
    private String vaultTokenEnv = "PJB_VAULT_TOKEN";

    private String vaultPath = "";

    @NotNull
    private Duration requestTimeout = Duration.ofSeconds(4);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }


    @AssertTrue(message = "pjb.db.credentials.rotation.provider deve ser informado quando a rotação estiver habilitada")
    public boolean isProviderValidWhenEnabled() {
        if (!enabled) {
            return true;
        }
        return provider != null && !provider.isBlank();
    }

    @AssertTrue(message = "pjb.db.credentials.rotation.vault-url deve ser informado quando a rotação Vault estiver habilitada")
    public boolean isVaultUrlValidWhenVaultEnabled() {
        if (!enabled || provider == null || !provider.trim().equalsIgnoreCase("vault")) {
            return true;
        }
        return vaultUrl != null && !vaultUrl.isBlank();
    }

    @AssertTrue(message = "pjb.db.credentials.rotation.vault-path deve ser informado quando a rotação Vault estiver habilitada")
    public boolean isVaultPathValidWhenVaultEnabled() {
        if (!enabled || provider == null || !provider.trim().equalsIgnoreCase("vault")) {
            return true;
        }
        return vaultPath != null && !vaultPath.isBlank();
    }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    public String getVaultTokenEnv() {
        return vaultTokenEnv;
    }

    public void setVaultTokenEnv(String vaultTokenEnv) {
        this.vaultTokenEnv = vaultTokenEnv;
    }

    public String getVaultPath() {
        return vaultPath;
    }

    public void setVaultPath(String vaultPath) {
        this.vaultPath = vaultPath;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
