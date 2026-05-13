package com.tcc.pjb.backend.integration.judicial.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class JudicialPropertySecretResolver implements JudicialSecretResolver {

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    public JudicialPropertySecretResolver(Environment environment, ResourceLoader resourceLoader) {
        this.environment = Objects.requireNonNull(environment);
        this.resourceLoader = Objects.requireNonNull(resourceLoader);
    }

    @Override
    public String resolve(String value) {
        String candidate = normalized(value);
        if (candidate == null) {
            return null;
        }
        if (candidate.startsWith("{env:")) {
            return fromEnvironment(extractBracketPayload(candidate, "env:"));
        }
        if (candidate.startsWith("{sys:")) {
            return fromSystemProperty(extractBracketPayload(candidate, "sys:"));
        }
        if (candidate.startsWith("{file:")) {
            return fromResource(extractBracketPayload(candidate, "file:"));
        }
        if (candidate.startsWith("{classpath:")) {
            return fromResource("classpath:" + extractBracketPayload(candidate, "classpath:"));
        }
        if (candidate.startsWith("{base64:")) {
            return decodeBase64(extractBracketPayload(candidate, "base64:"));
        }
        if (candidate.startsWith("{literal:")) {
            return extractBracketPayload(candidate, "literal:");
        }
        if (candidate.startsWith("{empty:")) {
            return "";
        }
        if (candidate.startsWith("env:")) {
            return fromEnvironment(candidate.substring(4));
        }
        if (candidate.startsWith("sys:")) {
            return fromSystemProperty(candidate.substring(4));
        }
        if (candidate.startsWith("file:")) {
            return fromResource(candidate);
        }
        if (candidate.startsWith("classpath:")) {
            return fromResource(candidate);
        }
        if (candidate.startsWith("base64:")) {
            return decodeBase64(candidate.substring(7));
        }
        if (candidate.startsWith("literal:")) {
            return candidate.substring(8);
        }
        if (candidate.startsWith("empty:")) {
            return "";
        }
        return candidate;
    }

    private String fromEnvironment(String key) {
        String resolvedKey = normalized(key);
        if (resolvedKey == null) {
            return null;
        }
        return normalized(environment.getProperty(resolvedKey));
    }

    private String fromSystemProperty(String key) {
        String resolvedKey = normalized(key);
        if (resolvedKey == null) {
            return null;
        }
        return normalized(System.getProperty(resolvedKey));
    }

    private String fromResource(String location) {
        String resolvedLocation = normalized(location);
        if (resolvedLocation == null) {
            return null;
        }
        Resource resource = resourceLoader.getResource(resolvedLocation.startsWith("file:") || resolvedLocation.startsWith("classpath:") ? resolvedLocation : "file:" + resolvedLocation);
        if (!resource.exists()) {
            throw new JudicialConnectorCryptographicException("Secret resource not found at " + resolvedLocation + '.');
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new JudicialConnectorCryptographicException("Unable to resolve secret resource at " + resolvedLocation + '.', ex);
        }
    }

    private String decodeBase64(String value) {
        String candidate = normalized(value);
        if (candidate == null) {
            return null;
        }
        try {
            return new String(Base64.getMimeDecoder().decode(candidate), StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ex) {
            throw new JudicialConnectorCryptographicException("Invalid base64 secret value.", ex);
        }
    }

    private String extractBracketPayload(String value, String prefix) {
        if (!value.endsWith("}")) {
            return null;
        }
        return value.substring(prefix.length() + 1, value.length() - 1).trim();
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }
}
