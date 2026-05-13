package com.tcc.pjb.backend.core.security.abac.policy;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PolicyRegistry {

    private static final Logger log = LoggerFactory.getLogger(PolicyRegistry.class);

    public record ActivePolicy(AccessPolicy policy, PolicyDescriptor descriptor, String descriptorSha256) {
        public String version() {
            return policy != null ? policy.version() : null;
        }
    }

    private final ActivePolicy active;

    public PolicyRegistry(List<AccessPolicy> policies,
                          PolicyProperties properties,
                          ResourceLoader resourceLoader,
                          ObjectMapper objectMapper) {
        Map<String, AccessPolicy> byVersion = new HashMap<>();
        for (AccessPolicy p : policies) {
            if (p == null || p.version() == null || p.version().isBlank()) continue;
            byVersion.put(p.version().trim(), p);
        }

        PolicyDescriptor descriptor = loadDescriptor(properties, resourceLoader, objectMapper);
        String sha256 = descriptor == null ? null : sha256Of(properties.getDescriptorResource(), resourceLoader);

        String desired = properties.getVersion();
        if (desired == null || desired.isBlank()) {
            desired = descriptor != null ? descriptor.getVersion() : null;
        }
        if (desired == null || desired.isBlank()) {
            desired = AbacV1Policy.VERSION;
        }

        AccessPolicy selected = byVersion.get(canonicalVersion(desired));
        if (selected == null) {
            throw new IllegalStateException("Política ABAC não encontrada para versão: " + desired + ". Disponíveis=" + byVersion.keySet());
        }

        
        if (descriptor != null && descriptor.getVersion() != null) {
            String dv = canonicalVersion(descriptor.getVersion());
            if (!dv.equals(selected.version())) {
                throw new IllegalStateException("Descritor de política version=" + dv + " não confere com política ativa=" + selected.version());
            }
        }

        log.info("PolicyRegistry ativo: version={} descriptorSha256={}", selected.version(), sha256);
        this.active = new ActivePolicy(selected, descriptor, sha256);
    }

    public ActivePolicy active() {
        return active;
    }

    private static String canonicalVersion(String raw) {
        String version = raw == null ? null : raw.trim();
        if (version == null || version.isBlank()) {
            return version;
        }
        if ("v1".equalsIgnoreCase(version) || "abac-v1".equalsIgnoreCase(version)) {
            return AbacV1Policy.VERSION;
        }
        return version;
    }

    private static PolicyDescriptor loadDescriptor(PolicyProperties props, ResourceLoader loader, ObjectMapper om) {
        try {
            String loc = Objects.requireNonNullElse(props.getDescriptorResource(), "");
            if (loc.isBlank()) return null;
            Resource r = loader.getResource(loc);
            if (!r.exists()) {
                log.warn("Descritor de política não encontrado: {}", loc);
                return null;
            }
            try (InputStream in = r.getInputStream()) {
                return om.readValue(in, PolicyDescriptor.class);
            }
        } catch (Exception e) {
            log.warn("Falha ao carregar descritor de política: {}", e.getMessage());
            return null;
        }
    }

    private static String sha256Of(String location, ResourceLoader loader) {
        try {
            if (location == null || location.isBlank()) return null;
            Resource r = loader.getResource(location);
            if (!r.exists()) return null;
            byte[] bytes;
            try (InputStream in = r.getInputStream()) {
                bytes = in.readAllBytes();
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            return toHex(dig);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
