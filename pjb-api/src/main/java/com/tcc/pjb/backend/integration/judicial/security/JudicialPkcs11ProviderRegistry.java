package com.tcc.pjb.backend.integration.judicial.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.time.Duration;
import java.time.Instant;
import java.security.Security;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class JudicialPkcs11ProviderRegistry {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_CACHE = 32;

    private final JudicialConnectorSecurityProperties properties;
    private final JudicialSecretResolver secretResolver;
    private final ConcurrentHashMap<String, Provider> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> touch = new ConcurrentHashMap<>();

    public JudicialPkcs11ProviderRegistry(JudicialConnectorSecurityProperties properties,
                                          JudicialSecretResolver secretResolver) {
        this.properties = Objects.requireNonNull(properties);
        this.secretResolver = Objects.requireNonNull(secretResolver);
    }

    public Provider resolve(String moduleRef) {
        String key = normalized(moduleRef);
        if (key == null) {
            throw new JudicialConnectorCryptographicException("PKCS11 module reference is required.");
        }
        cleanup();
        Provider provider = cache.computeIfAbsent(key, this::buildProvider);
        touch.put(key, Instant.now());
        trimOverflow();
        return provider;
    }


    private void cleanup() {
        Instant now = Instant.now();
        touch.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue() == null || entry.getValue().isBefore(now.minus(CACHE_TTL));
            if (expired) {
                cache.remove(entry.getKey());
            }
            return expired;
        });
    }

    private void trimOverflow() {
        int overflow = cache.size() - MAX_CACHE;
        if (overflow <= 0) {
            return;
        }
        touch.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByValue())
                .limit(overflow)
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(key -> {
                    cache.remove(key);
                    touch.remove(key);
                });
    }

    private Provider buildProvider(String moduleRef) {
        JudicialConnectorSecurityProperties.Pkcs11ModuleSource module = properties.getPkcs11Modules().get(moduleRef);
        if (module == null) {
            throw new JudicialConnectorCryptographicException("PKCS11 module not found for reference " + moduleRef + '.');
        }
        String library = resolveSecret(module.getLibrary());
        if (blank(library)) {
            throw new JudicialConnectorCryptographicException("PKCS11 library is required for module " + moduleRef + '.');
        }
        Provider prototype = Security.getProvider("SunPKCS11");
        if (prototype == null) {
            throw new JudicialConnectorCryptographicException("SunPKCS11 provider is unavailable in the current runtime.");
        }
        try {
            String normalizedModuleName = normalized(module.getName());
            String providerName = normalizedModuleName != null ? normalizedModuleName : "PJB_" + moduleRef.replace('-', '_').replace('.', '_');
            StringBuilder builder = new StringBuilder();
            builder.append("name=").append(providerName).append('\n');
            builder.append("library=").append(library.trim()).append('\n');
            if (module.getSlot() != null) {
                builder.append("slot=").append(module.getSlot()).append('\n');
            }
            if (module.getSlotListIndex() != null) {
                builder.append("slotListIndex=").append(module.getSlotListIndex()).append('\n');
            }
            String tokenLabel = resolveSecret(module.getTokenLabel());
            if (normalized(tokenLabel) != null) {
                builder.append("tokenLabel=").append(tokenLabel.trim()).append('\n');
            }
            String attributesMode = resolveSecret(module.getAttributesMode());
            if (normalized(attributesMode) != null) {
                builder.append("attributes=").append(attributesMode.trim()).append('\n');
            }
            String normalizedProviderName = providerName.toLowerCase(Locale.ROOT);
            Path configPath = Files.createTempFile("pjb-pkcs11-" + normalizedProviderName, ".cfg");
            Files.writeString(configPath, builder.toString(), StandardCharsets.UTF_8);
            configPath.toFile().deleteOnExit();
            Provider configured = prototype.configure(configPath.toAbsolutePath().toString());
            Provider alreadyRegistered = Security.getProvider(configured.getName());
            if (alreadyRegistered != null) {
                return alreadyRegistered;
            }
            Security.addProvider(configured);
            Provider registered = Security.getProvider(configured.getName());
            return registered != null ? registered : configured;
        } catch (IOException ex) {
            throw new JudicialConnectorCryptographicException("Unable to initialize PKCS11 provider for module " + moduleRef + '.', ex);
        }
    }

    private String resolveSecret(String value) {
        return normalized(secretResolver.resolve(value));
    }

    private String normalized(String value) {
        if (blank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
