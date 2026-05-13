package com.tcc.pjb.backend.ai.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiAuditLedger {

    private final ObjectMapper mapper;
    private final CryptoVaultService vault;

    private final Path ledgerPath;
    private final boolean enabled;
    private final boolean encryptPayload;

    private final AtomicReference<String> lastHash = new AtomicReference<>("GENESIS");

    public AiAuditLedger(
            ObjectMapper mapper,
            CryptoVaultService vault,
            @Value("${pjb.audit.ledger.path:./data/ai-audit-ledger.jsonl}") String ledgerPath,
            @Value("${pjb.audit.ledger.enabled:false}") boolean enabled,
            @Value("${pjb.audit.ledger.encrypt:true}") boolean encryptPayload
    ) {
        this.mapper = mapper;
        this.vault = vault;
        this.ledgerPath = Path.of(ledgerPath);
        this.enabled = enabled;
        this.encryptPayload = encryptPayload;
        if (enabled) {
            ensureParentDir();
        }
    }

    public void append(String action, Map<String, ?> data) {
        if (!enabled) return;
        if (action == null || action.isBlank()) action = "unknown";

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("ts", Instant.now().toString());
        event.put("action", action);
        event.put("data", data == null ? Map.of() : data);

        String json = safeJson(event);
        String payload = encryptPayload ? safeEncrypt(json) : json;
        String prev = lastHash.get();
        String hash = sha256(prev + "|" + payload);

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("id", event.get("id"));
        line.put("ts", event.get("ts"));
        line.put("action", action);
        line.put("prevHash", prev);
        line.put("hash", hash);
        line.put("encrypted", encryptPayload);
        line.put("payload", payload);

        String lineJson = safeJson(line) + "\n";

        synchronized (this) {
            try {
                Files.writeString(
                        ledgerPath,
                        lineJson,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
                lastHash.set(hash);
            } catch (IOException e) {
                log.warn("Audit ledger write failed: {}", e.getMessage());
            }
        }
    }

    private void ensureParentDir() {
        try {
            Path parent = ledgerPath.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException ignored) {
        }
    }

    private String safeJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"json_serialization_failed\"}";
        }
    }

    private String safeEncrypt(String plainJson) {
        try {
            return vault.blindarDado(plainJson);
        } catch (Exception e) {
            return "ENCRYPT_FAIL_SHA256:" + sha256(plainJson);
        }
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
