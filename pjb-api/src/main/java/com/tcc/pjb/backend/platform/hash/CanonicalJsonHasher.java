package com.tcc.pjb.backend.platform.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tcc.pjb.backend.core.util.Gzip;

@Component
public final class CanonicalJsonHasher {

    private final ObjectWriter canonicalWriter;
    private final Clock clock;

    public CanonicalJsonHasher(ObjectMapper objectMapper, Clock pjbClock) {
        Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(pjbClock, "pjbClock");

        ObjectMapper m = objectMapper.copy();
        m.setConfig(m.getSerializationConfig()
                .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        m.setConfig(m.getDeserializationConfig()
                .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        this.canonicalWriter = m.writer();
    }

    public Fingerprint fingerprint(Object value) {
        Instant ts = Instant.now(clock);
        try {
            byte[] json = canonicalWriter.writeValueAsBytes(value);
            byte[] gz = Gzip.gzip(json);
            String sha256 = sha256Hex(json);
            return new Fingerprint(sha256, json.length, gz.length, ts);
        } catch (Exception e) {
            
            String fallback = sha256Hex(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            return new Fingerprint(fallback, -1, -1, ts);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            
            return "sha256_unavailable";
        }
    }
}
