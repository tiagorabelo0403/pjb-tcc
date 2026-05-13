package com.tcc.pjb.backend.integration.judicial.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class JudicialKeyStoreLoader {

    private final JudicialConnectorSecurityProperties properties;
    private final JudicialPkcs11ProviderRegistry pkcs11ProviderRegistry;
    private final JudicialSecretResolver secretResolver;
    private final ResourceLoader resourceLoader;

    public JudicialKeyStoreLoader(JudicialConnectorSecurityProperties properties,
                                  JudicialPkcs11ProviderRegistry pkcs11ProviderRegistry,
                                  JudicialSecretResolver secretResolver,
                                  ResourceLoader resourceLoader) {
        this.properties = Objects.requireNonNull(properties);
        this.pkcs11ProviderRegistry = Objects.requireNonNull(pkcs11ProviderRegistry);
        this.secretResolver = Objects.requireNonNull(secretResolver);
        this.resourceLoader = Objects.requireNonNull(resourceLoader);
    }

    public JudicialKeyStoreMaterial loadKeyStore(String reference) {
        String ref = normalized(reference);
        if (ref == null) {
            return null;
        }
        JudicialConnectorSecurityProperties.KeyStoreSource source = properties.getKeyStores().get(ref);
        if (source == null) {
            throw new JudicialConnectorCryptographicException("KeyStore source not found for reference " + ref + '.');
        }
        return loadKeyStore(ref, source);
    }

    public JudicialKeyStoreMaterial loadTrustStore(String reference) {
        String ref = normalized(reference);
        if (ref == null) {
            return null;
        }
        JudicialConnectorSecurityProperties.TrustStoreSource source = properties.getTrustStores().get(ref);
        if (source == null) {
            throw new JudicialConnectorCryptographicException("TrustStore source not found for reference " + ref + '.');
        }
        return loadTrustStore(ref, source);
    }

    private JudicialKeyStoreMaterial loadKeyStore(String reference,
                                                  JudicialConnectorSecurityProperties.KeyStoreSource source) {
        try {
            String pkcs11Module = normalized(resolve(source.getPkcs11Module()));
            if (pkcs11Module != null) {
                Provider provider = pkcs11ProviderRegistry.resolve(pkcs11Module);
                KeyStore keyStore = KeyStore.getInstance(firstNonBlank(resolve(source.getType()), "PKCS11"), provider);
                char[] pin = chars(firstNonBlank(resolve(source.getPin()), resolve(source.getPassword())));
                keyStore.load(null, pin);
                return new JudicialKeyStoreMaterial(
                        reference,
                        firstNonBlank(resolve(source.getType()), "PKCS11"),
                        provider.getName(),
                        keyStore,
                        pin,
                        chars(resolve(source.getKeyPassword())),
                        normalized(resolve(source.getAlias())),
                        true,
                        Map.of("pkcs11Module", pkcs11Module, "provider", provider.getName())
                );
            }
            String location = resolve(source.getLocation());
            String base64 = resolve(source.getBase64());
            byte[] bytes = readBytes(location, base64);
            char[] storePassword = chars(resolve(source.getPassword()));
            KeyStore keyStore = newKeyStore(resolveType(resolve(source.getType()), location), resolve(source.getProvider()));
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                keyStore.load(inputStream, storePassword);
            }
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("location", normalized(location));
            metadata.put("provider", normalized(resolve(source.getProvider())));
            metadata.entrySet().removeIf(entry -> entry.getValue() == null);
            return new JudicialKeyStoreMaterial(
                    reference,
                    resolveType(resolve(source.getType()), location),
                    normalized(resolve(source.getProvider())),
                    keyStore,
                    storePassword,
                    chars(resolve(source.getKeyPassword())),
                    normalized(resolve(source.getAlias())),
                    false,
                    Map.copyOf(metadata)
            );
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to load KeyStore for reference " + reference + '.', ex);
        }
    }

    private JudicialKeyStoreMaterial loadTrustStore(String reference,
                                                    JudicialConnectorSecurityProperties.TrustStoreSource source) {
        try {
            String location = resolve(source.getLocation());
            String base64 = resolve(source.getBase64());
            byte[] bytes = readBytes(location, base64);
            char[] storePassword = chars(resolve(source.getPassword()));
            KeyStore keyStore = newKeyStore(resolveType(resolve(source.getType()), location), resolve(source.getProvider()));
            try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                keyStore.load(inputStream, storePassword);
            }
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("location", normalized(location));
            metadata.put("provider", normalized(resolve(source.getProvider())));
            metadata.entrySet().removeIf(entry -> entry.getValue() == null);
            return new JudicialKeyStoreMaterial(
                    reference,
                    resolveType(resolve(source.getType()), location),
                    normalized(resolve(source.getProvider())),
                    keyStore,
                    storePassword,
                    null,
                    null,
                    false,
                    Map.copyOf(metadata)
            );
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to load TrustStore for reference " + reference + '.', ex);
        }
    }

    private KeyStore newKeyStore(String type, String providerName) throws Exception {
        String resolvedType = firstNonBlank(type, "PKCS12");
        String resolvedProvider = normalized(providerName);
        return resolvedProvider == null ? KeyStore.getInstance(resolvedType) : KeyStore.getInstance(resolvedType, resolvedProvider);
    }

    private byte[] readBytes(String location, String base64) throws IOException {
        String base64Value = normalized(base64);
        if (base64Value != null) {
            return Base64.getMimeDecoder().decode(base64Value);
        }
        String resolvedLocation = normalized(location);
        if (resolvedLocation == null) {
            throw new JudicialConnectorCryptographicException("Key material source location or base64 value is required.");
        }
        Resource resource = resourceLoader.getResource(isResourceLocation(resolvedLocation) ? resolvedLocation : "file:" + resolvedLocation);
        if (!resource.exists()) {
            throw new JudicialConnectorCryptographicException("Key material resource not found at " + resolvedLocation + '.');
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private boolean isResourceLocation(String value) {
        return value.startsWith("classpath:") || value.startsWith("file:") || value.startsWith("http:") || value.startsWith("https:");
    }

    private String resolveType(String explicitType, String location) {
        String type = normalized(explicitType);
        if (type != null) {
            return type;
        }
        String source = normalized(location);
        if (source == null) {
            return "PKCS12";
        }
        String lowered = source.toLowerCase(Locale.ROOT);
        if (lowered.endsWith(".jks")) {
            return "JKS";
        }
        return "PKCS12";
    }

    private char[] chars(String value) {
        String normalized = normalized(value);
        return normalized == null ? null : normalized.toCharArray();
    }

    private String resolve(String value) {
        return secretResolver.resolve(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (normalized(value) != null) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }
}
